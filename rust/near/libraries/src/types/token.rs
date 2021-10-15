use near_sdk::borsh::{self, BorshDeserialize, BorshSerialize};
use near_sdk::serde::{Deserialize, Serialize};
use crate::types::btp_address::Network;

pub type TokenName = String;

pub type TokenId = Vec<u8>;

pub trait TokenMetadata {
    fn name(&self) -> &TokenName;
    fn network(&self) -> &Network;
    fn symbol(&self) -> &String;
    fn fee_numerator(&self) -> u128;
    fn fee_numerator_mut(&mut self) -> &u128;
    fn denominator(&self) -> u128;
}

#[derive(BorshDeserialize, BorshSerialize, Clone, Deserialize, Serialize, Debug, PartialEq, Eq)]
#[serde(crate = "near_sdk::serde")]
pub struct Token<T: TokenMetadata> {
    pub metadata: T
}

impl<T: TokenMetadata> Token<T>  {
    pub fn new(token: T) -> Self {
        Self {
            metadata: token
        }
    }

    pub fn name(&self) -> &String {
        self.metadata.name()
    }

    pub fn network(&self) -> &String {
        self.metadata.network()
    }

    pub fn symbol(&self) -> &String {
        self.metadata.symbol()
    }

    pub fn fee_numerator(&self) -> u128 {
        self.metadata.fee_numerator()
    }

    pub fn fee_numerator_mut(&mut self) -> &u128 {
        &self.metadata.fee_numerator_mut()
    }

    pub fn denominator(&self) -> u128 {
        self.metadata.denominator()
    }
}

