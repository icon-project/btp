use btp_common::errors::BmvError;
use libraries::{types::Network, MerkleTreeAccumulator, types::Hash};
use near_sdk::borsh::{self, BorshDeserialize, BorshSerialize};
use near_sdk::{
    env, json_types::U128, log, near_bindgen, require, AccountId, Gas, PanicOnDefault, Promise,
    PromiseResult, PromiseOrValue
};

mod types;
use types::{Validator, Validators, RelayMessage, BlockHeader, StateChange, StateChanges, BlockProof, BlockUpdate, VoteMessage, Votes, VOTE_TYPE_PRECOMMIT, Response, ReceiptProof};
mod assertion;
mod messaging;
mod state_management;
mod block_validation;
mod receipt_validation;
mod util;

#[near_bindgen]
#[derive(BorshDeserialize, BorshSerialize, PanicOnDefault)]
pub struct BtpMessageVerifier {
    network: Network,
    bmc: AccountId,
    last_height: u128,
    validators: Validators,
    mta: MerkleTreeAccumulator,
}

#[near_bindgen]
impl BtpMessageVerifier {
    #[init]
    pub fn new(bmc: AccountId, network: Network, validators: Vec<Validator>, offset: u128) -> Self {
        require!(!env::state_exists(), "Already initialized");
        let mta = MerkleTreeAccumulator::new(offset);
        let validator_list = validators;
        let mut validators = Validators::new();
        validators.set(&validator_list);
        Self {
            bmc,
            network,
            last_height: offset,
            validators,
            mta,
        }
    }

    fn bmc(&self) -> &AccountId {
        &self.bmc
    }
}
