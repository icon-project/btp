use super::Validator;
use libraries::types::Hash;

pub enum StateChange {
    SetValidators { validators: Vec<Validator> },
    MtaAddBlockHash { block_hash: Hash },
    SetLastHeight { height: u64 }
}

pub type StateChanges = Vec<StateChange>;