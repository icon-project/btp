use btp_common::errors::BmvError;
use libraries::{types::Network, MerkleTreeAccumulator, types::Hash, types::messages::SerializedBtpMessages, types::VerifierResponse, types::VerifierStatus, rlp, types::BTPAddress};
use near_sdk::borsh::{self, BorshDeserialize, BorshSerialize};
use near_sdk::{
    env, json_types::U128, log, near_bindgen, require, AccountId, Gas, PanicOnDefault, Promise,
    PromiseResult, PromiseOrValue
};
use btp_common::errors::Exception;
use near_sdk::json_types::U64;

#[cfg(not(feature = "testable"))]
mod types;

#[cfg(feature = "testable")]
pub mod types;

use types::{Validator, Validators, RelayMessage, BlockHeader, BlockProof, BlockUpdate, VoteMessage, Votes, VOTE_TYPE_PRECOMMIT, ReceiptProof, Sha256, Receipt, ToBmvError, EventLog};
mod assertion;
mod messaging;
mod management;
mod block_validation;
mod receipt_validation;
mod util;

#[near_bindgen]
#[derive(BorshDeserialize, BorshSerialize, PanicOnDefault)]
pub struct BtpMessageVerifier {
    network: Network,
    bmc: AccountId,
    last_height: u64,
    validators: Validators,
    mta: MerkleTreeAccumulator,
    last_known_block_hash: Hash
}

#[near_bindgen]
impl BtpMessageVerifier {
    #[init]
    pub fn new(bmc: AccountId, network: Network, validators: Validators, offset: U64, last_known_block_hash: Hash) -> Self {
        require!(!env::state_exists(), "Already initialized");
        let mta = MerkleTreeAccumulator::new(offset.into());
        Self {
            bmc,
            network,
            last_height: offset.into(),
            validators,
            mta,
            last_known_block_hash
        }
    }

    fn bmc(&self) -> &AccountId {
        &self.bmc
    }
}
