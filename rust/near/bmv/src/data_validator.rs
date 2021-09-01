//! Data Validator

use crate::Verifier;
use btp_common::BTPAddress;
use libraries::bmv_types::*;
use merkle_tree_accumulator::hash::Hash;
use near_sdk::borsh::{self, BorshDeserialize, BorshSerialize};
use near_sdk::{near_bindgen, setup_alloc, AccountId};

setup_alloc!();
/// This struct implements `Default`: https://github.com/near/near-sdk-rs#writing-rust-contract
#[near_bindgen]
#[derive(BorshDeserialize, BorshSerialize, Clone)]
pub struct DataValidator {
    messages: Vec<Vec<u8>>,
}

impl Default for DataValidator {
    fn default() -> Self {
        Self { messages: vec![] }
    }
}

#[near_bindgen]
impl DataValidator {
    /// Validate receipt proofs and return BTP messages
    /// Return serializedMessages List of serialized bytes of a BTP Message
    pub fn validate_receipt(
        &mut self,
        bmc: AccountId,
        prev: AccountId,
        seq: u128,
        serialized_msg: &[u8],
        receipt_hash: Hash,
    ) -> Result<Vec<Vec<u8>>, &str> {
        let mut next_seq = seq + 1;
        let mut receipt = Receipt {
            event_logs: vec![],
            event_log_hash: Hash::default(),
        };
        let mut message_event = MessageEvent {
            next_bmc: "".to_string(),
            seq: 0,
            message: vec![],
        };
        let receipt_proofs: Vec<ReceiptProof> =
            vec![ReceiptProof::try_from_slice(serialized_msg)
                .expect("Failed to decode receipt proof")];
        let contract_addr = BTPAddress::new(prev)
            .contract_address()
            .expect("Failed to retrieve contract address");
        if !self.messages.is_empty() {
            self.messages.clear();
        }
        for mut receipt_proof in receipt_proofs {
            receipt = receipt_proof.verify_mtp_proof(receipt_hash);
            for mut event_log in receipt.event_logs {
                if event_log.addr != contract_addr {
                    continue;
                }
                message_event = event_log.to_message_event();
                if !message_event.next_bmc.as_bytes().is_empty() {
                    if message_event.seq > next_seq {
                        return Err("BMVRevertInvalidSequenceHigher");
                    } else if message_event.seq < next_seq {
                        return Err("BMVRevertInvalidSequence");
                    } else if message_event.next_bmc
                        == BTPAddress::new(bmc.clone())
                            .contract_address()
                            .expect("Error in retrieving addr")
                    {
                        self.messages.push(message_event.message);
                        next_seq += 1;
                    }
                }
            }
        }

        Ok(self.messages.clone())
    }
}

impl Verifier for ReceiptProof {}
impl Verifier for EventLog {}
