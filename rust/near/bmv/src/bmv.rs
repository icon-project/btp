//! BMV

use btp_common::BTPAddress;
use libraries::bmv_types::*;
use merkle_tree_accumulator::{hash::Hash, mta::MerkleTreeAccumulator};
use near_sdk::borsh::{self, BorshDeserialize, BorshSerialize};
use near_sdk::{env, near_bindgen, setup_alloc, AccountId};

setup_alloc!();
/// This struct implements `Default`: https://github.com/near/near-sdk-rs#writing-rust-contract
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

    pub fn new(_addr: AccountId) -> Self {
        todo!()
    }

    /// Return base 64 encode of Merkle tree
    pub fn get_mta(&self) -> String {
        String::from_utf8(self.mta.to_bytes()).expect("Failed to encode bytes into string")
    }

    /// Return connected BMC address
    pub fn get_connected_bmc(&self) -> &AccountId {
        &self.bmc_addr
    }

    /// Return network address of the blockchain
    pub fn get_net_address(&self) -> &AccountId {
        &self.net_addr
    }

    /// Return hash of Rlp encode from given list of validators
    /// and a list of validators' addresses
    pub fn get_validators(&self) -> (Hash, &Vec<AccountId>) {
        (
            self.validators.validator_hash,
            &self.validators.validator_addrs,
        )
    }

    /// Used by the relay to resolve next BTP Message to send. Called by BMC
    pub fn get_status(&self) -> (u128, u128, u128) {
        (
            self.mta.height,
            self.mta.offset as u128,
            self.last_block_height,
        )
    }

    /// Decode Relay Messages and process BTP Messages.
    /// If there is an error, then it sends a BTP Message containing the Error Message.
    /// BTP Messages with old sequence numbers are ignored. A BTP Message containing future sequence number will fail.
    pub fn handle_relay_message(
        &mut self,
        bmc: AccountId,
        prev: AccountId,
        seq: u128,
        msg: String,
    ) -> Result<Vec<Vec<u8>>, &str> {
        self.check_accessible(bmc, prev)
            .expect("Error in executing check_accessible");

        let relay_msg =
            RelayMessage::try_from_slice(msg.as_bytes()).expect("Failed to decode relay message");

        if relay_msg.block_updates.is_empty() {
            return Err("BMVRevert: Invalid relay message");
        }

        let (_receipt_hash, last_height) = self
            .get_last_receipt_hash(&relay_msg)
            .expect("Failed to get last receipt hash");

        // TODO
        let msgs: Vec<Vec<u8>> = vec![];

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
            } else if i != 0
                && relay_msg.block_updates[i].block_header.prev_hash
                    != relay_msg.block_updates[i - 1].block_header.block_hash
            {
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
                // TODO
                // if relay_msg.block_updates[i].verify_validators(&self.validators) {
                //     self.validators = Validators::default();
                //     self.validators
                //         .decode_validators(&relay_msg.block_updates[i].next_validators_rlp);
                // }
            }

            self.mta
                .add(relay_msg.block_updates[i].block_header.block_hash);
        }

        // TODO
        // relay_msg.block_proof.verify_mta_proof(self.mta);
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
