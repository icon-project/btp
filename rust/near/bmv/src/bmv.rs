//! BMV

use crate::{DataValidator, Verifier};
use btp_common::BTPAddress;
use libraries::bmv_types::*;
use merkle_tree_accumulator::{hash::Hash, mta::MerkleTreeAccumulator};
use near_sdk::{
    borsh::{self, BorshDeserialize, BorshSerialize},
    env, near_bindgen, setup_alloc, AccountId,
};

setup_alloc!();

#[near_bindgen]
#[derive(BorshDeserialize, BorshSerialize, Clone)]
pub struct Bmv {
    bmc_addr: AccountId,
    sub_bmv_addr: AccountId,
    net_addr: AccountId,
    last_block_height: u128,
    last_block_hash: Hash,
    validators: Validators,
    mta: MerkleTreeAccumulator,
}

impl Default for Bmv {
    fn default() -> Self {
        Self {
            bmc_addr: "".to_string(),
            sub_bmv_addr: "".to_string(),
            net_addr: "".to_string(),
            last_block_height: 0,
            last_block_hash: Hash::default(),
            validators: Validators::default(),
            mta: MerkleTreeAccumulator::default(),
        }
    }
}

#[near_bindgen]
impl Bmv {
    #[init]
    #[allow(clippy::too_many_arguments)]
    pub fn initialize(
        bmc_addr: AccountId,
        sub_bmv_addr: AccountId,
        net_addr: AccountId,
        rlp_validators: &[u8],
        offset: usize,
        roots_size: usize,
        cache_size: usize,
        last_block_hash: Hash,
    ) -> Self {
        let mut mta = MerkleTreeAccumulator::default();
        mta.set_offset(offset);
        mta.roots_size = roots_size;
        mta.cache_size = cache_size;
        mta.newer_witness_allowed = true;
        Self {
            bmc_addr,
            sub_bmv_addr,
            net_addr,
            last_block_height: offset as u128,
            last_block_hash,
            validators: Validators::try_from_slice(rlp_validators)
                .expect("Failed to deserialize validators"),
            mta,
        }
    }

    pub fn new(addr: AccountId) -> Self {
        let mut bmv = Self::default();
        bmv.net_addr = addr;
        bmv
    }

    /**
       @return Base64 encode of Merkle Tree
    */
    pub fn get_mta(&self) -> String {
        String::from_utf8(self.mta.to_bytes()).expect("Failed to encode bytes into string")
    }

    /**
       @return connected BMC address
    */
    pub fn get_connected_bmc(&self) -> &AccountId {
        &self.bmc_addr
    }

    /**
       @return network address of the blockchain
    */
    pub fn get_net_address(&self) -> &AccountId {
        &self.net_addr
    }

    /**
       @return hash of RLP encode from given list of validators
       @return list of validators' addresses
    */
    pub fn get_validators(&self) -> (Hash, &Vec<AccountId>) {
        (
            self.validators.validator_hash,
            &self.validators.validator_addrs,
        )
    }

    /**
       @notice Used by the relay to resolve next BTP Message to send.
               Called by BMC.
       @return height height of MerkleTreeAccumulator
       @return offset offset of MerkleTreeAccumulator
       @return last_height block height of last relayed BTP Message
    */
    pub fn get_status(&self) -> (u128, u128, u128) {
        (
            self.mta.height,
            self.mta.offset as u128,
            self.last_block_height,
        )
    }

    /**
       @notice Decodes Relay Messages and process BTP Messages.
               If there is an error, then it sends a BTP Message containing the Error Message.
               BTP Messages with old sequence numbers are ignored. A BTP Message contains future sequence number will fail.
       @param bmc           BTP Address of the BMC handling the message
       @param prev          BTP Address of the previous BMC
       @param seq           next sequence number to get a message
       @param msg           serialized bytes of Relay Message
       @return              List of serialized bytes of a BTP Message
    */
    pub fn handle_relay_message(
        &mut self,
        bmc: AccountId,
        prev: AccountId,
        seq: u128,
        msg: String,
    ) -> Result<Vec<Vec<u8>>, &str> {
        self.check_accessible(bmc.clone(), prev.clone())
            .expect("Error in executing check_accessible");

        let serialized_msg = msg.as_bytes();

        let relay_msg =
            RelayMessage::try_from_slice(&serialized_msg).expect("Failed to decode relay message");

        if relay_msg.block_updates.is_empty() {
            return Err("BMVRevert: Invalid relay message");
        }

        let (receipt_hash, last_height) = self
            .get_last_receipt_hash(&relay_msg)
            .expect("Failed to get last receipt hash");

        let msgs = DataValidator::default()
            .validate_receipt(bmc, prev, seq, serialized_msg, &receipt_hash)
            .expect("Failed to validate receipt");

        if msg.len() > 0 {
            self.last_block_height = last_height;
        }
        Ok(msgs)
    }

    fn get_last_receipt_hash(&mut self, relay_msg: &RelayMessage) -> Result<(Hash, u128), &str> {
        let mut receipt_hash: Hash;
        let mut last_height: u128;
        for i in 0..relay_msg.block_updates.len() {
            // verify height
            if relay_msg.block_updates[i].block_header.height > self.mta.height + 1 {
                return Err("BMVRevertInvalidBlockUpdateHigher");
            }
            if relay_msg.block_updates[i].block_header.height < self.mta.height + 1 {
                return Err("BMVRevertInvalidBlockUpdateLower");
            }

            // verify prev block hash
            if i == 0 && relay_msg.block_updates[i].block_header.prev_hash != self.last_block_hash {
                return Err("BMVRevertInvalidBlockUpdate: Invalid block hash");
            }

            if i == relay_msg.block_updates.len() - 1 {
                receipt_hash = relay_msg.block_updates[i].block_header.result.receipt_hash;
                last_height = relay_msg.block_updates[i].block_header.height;
                self.last_block_hash = relay_msg.block_updates[i].block_header.block_hash;
            }

            if self.validators.validator_hash != relay_msg.block_updates[i].next_validators_hash
                || i == relay_msg.block_updates.len() - 1
            {
                if BlockUpdate::verify_validators(
                    &relay_msg.block_updates[i].clone(),
                    &mut self.validators,
                )
                .unwrap()
                {
                    self.validators =
                        Validators::try_from_slice(&relay_msg.block_updates[i].next_validators_rlp)
                            .unwrap();
                }
            }

            self.mta
                .add(relay_msg.block_updates[i].block_header.block_hash);
        }

        BlockProof::verify_mta_proof(&relay_msg.block_proof, &mut self.mta)
            .expect("Failed to verify MTA proof");
        receipt_hash = relay_msg.block_proof.block_header.result.receipt_hash;
        last_height = relay_msg.block_proof.block_header.height;

        Ok((receipt_hash, last_height))
    }

    fn check_accessible(&self, current_addr: AccountId, from_addr: AccountId) -> Result<(), &str> {
        let net = BTPAddress::new(from_addr)
            .network_address()
            .expect("Failed to get network address");

        if self.net_addr != net {
            return Err("BMVRevert: Invalid previous BMC");
        }
        if self.bmc_addr != env::signer_account_id() {
            return Err("BMVRevert: Invalid BMC");
        }

        let contract_addr = BTPAddress::new(current_addr)
            .contract_address()
            .expect("Failed to get contract address");

        if self.bmc_addr != contract_addr {
            return Err("BMVRevert: Invalid BMC");
        }

        Ok(())
    }
}

impl Verifier for BlockUpdate {}
impl Verifier for BlockProof {}
