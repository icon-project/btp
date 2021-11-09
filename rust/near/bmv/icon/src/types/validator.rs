use near_sdk::borsh::{self, BorshDeserialize, BorshSerialize};
use near_sdk::serde::Serialize;
use std::collections::HashSet;
use std::iter::FromIterator;
use libraries::rlp::{self, Decodable, Encodable};
use std::convert::TryFrom;

#[derive(Default, PartialEq, Eq, Debug, BorshDeserialize, BorshSerialize)]
pub struct Validators(HashSet<Validator>);

impl Validators {
    pub fn new() -> Self {
        Self(HashSet::new())
    }

    pub fn set(&mut self, validators: &Vec<Validator>) {
        let list = HashSet::from_iter(validators.clone());
        self.0.clone_from(&list)
    }

    pub fn contains(&self, validator: &Validator) -> bool {
        self.0.contains(validator)
    }
}

impl TryFrom<&Vec<Validator>> for Validators {
    type Error = rlp::DecoderError;
    fn try_from(list: &Vec<Validator>) -> Result<Self, Self::Error> {
        let mut validators = Self::new();
        validators.set(list);
        Ok(validators)
    }
}

impl Decodable for Validators {
    fn decode(rlp: &rlp::Rlp) -> Result<Self, rlp::DecoderError> {
        Ok(Self::try_from(&rlp.as_list::<Validator>()?)?)
    }
}

pub type Validator = Vec<u8>;
