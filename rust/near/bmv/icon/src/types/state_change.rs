use super::Validator;
use libraries::types::Hash;

pub enum StateChange {
    SetValidators { validators: Vec<Validator> },
    MtaAddBlockHash { block_hash: Hash }
}

pub type StateChanges = Vec<StateChange>;