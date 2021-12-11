use crate::types::BTPAddress;
use near_sdk::json_types::{Base64VecU8, U128};
use near_sdk::{
    borsh::{self, BorshDeserialize, BorshSerialize},
    serde::{Deserialize, Serialize},
};

#[derive(Serialize, Deserialize, BorshDeserialize, BorshSerialize)]
#[serde(crate = "near_sdk::serde")]
pub struct Message {
    next: BTPAddress,
    sequence: U128,
    message: Base64VecU8,
}

impl Message {
    pub fn new(next: BTPAddress, sequence: U128, message: Base64VecU8) -> Self {
        Self {
            next,
            sequence,
            message,
        }
    }

    pub fn sequence(&self) -> U128 {
        self.sequence
    }

    pub fn next(&self) -> &BTPAddress {
        &self.next
    }

    pub fn message(&self) -> &Base64VecU8 {
        &self.message
    }
}
