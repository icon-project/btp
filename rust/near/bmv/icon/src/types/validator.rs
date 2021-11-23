use libraries::rlp::{self, Decodable, Encodable};
use near_sdk::borsh::{self, BorshDeserialize, BorshSerialize};
use near_sdk::serde::Serialize;
use std::convert::TryFrom;
use std::iter::FromIterator;

use super::RlpBytes;

#[derive(Default, PartialEq, Eq, Debug, BorshDeserialize, BorshSerialize, Clone)]
pub struct Validators(Vec<Validator>);

impl Validators {
    pub fn new() -> Self {
        Self(Vec::new())
    }

    pub fn set(&mut self, validators: &Vec<Validator>) {
        self.0.clone_from(validators)
    }

    pub fn is_empty(&self) -> bool {
        self.0.is_empty()
    }

    pub fn contains(&self, validator: &Validator) -> bool {
        self.0.contains(validator)
    }

    pub fn len(&self) -> usize {
        self.0.len()
    }

    pub fn get(&self) -> &Vec<Validator> {
        &self.0
    }
}

impl AsRef<Validators> for Validators {
    fn as_ref(&self) -> &Self {
        &self
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

impl Encodable for Validators {
    fn rlp_append(&self, stream: &mut rlp::RlpStream) {
        stream.begin_list(self.get().len());
        self.get().iter().for_each(|validator| {
            stream.append(validator);
        });
    }
}

impl From<&Validators> for RlpBytes {
    fn from(validators: &Validators) -> Self {
        rlp::encode(validators).to_vec()
    }
}

pub type Validator = Vec<u8>;
