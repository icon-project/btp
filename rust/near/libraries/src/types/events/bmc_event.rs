use crate::types::messages::{BtpMessage, SerializedMessage};
use crate::types::BTPAddress;
use near_sdk::json_types::{Base64VecU8, U128};
use near_sdk::{
    borsh::{self, BorshDeserialize, BorshSerialize},
    collections::LazyOption,
    serde::{Deserialize, Serialize},
};
use std::convert::TryInto;

#[derive(BorshDeserialize, BorshSerialize)]
pub struct BmcEvent {
    message: LazyOption<Message>,
    error: LazyOption<Error>,
}

impl BmcEvent {
    pub fn new() -> Self {
        Self {
            message: LazyOption::new(b"message".to_vec(), None),
            error: LazyOption::new(b"error".to_vec(), None),
        }
    }

    pub fn amend_event(
        &mut self,
        sequence: u128,
        next: BTPAddress,
        message: BtpMessage<SerializedMessage>,
    ) {
        self.message.set(&Message {
            next,
            sequence: sequence.into(),
            message: <Vec<u8>>::from(message).into(),
        });
    }

    pub fn amend_error(&mut self) {}

    pub fn get_message(&self) -> Result<BtpMessage<SerializedMessage>, String> {
        self.message.get().ok_or("Not Found")?.message.0.try_into()
    }

    pub fn get_error(&self){

    }
}

#[derive(Serialize, Deserialize, BorshDeserialize, BorshSerialize)]
#[serde(crate = "near_sdk::serde")]
pub struct Message {
    next: BTPAddress,
    sequence: U128,
    message: Base64VecU8,
}

#[derive(Serialize, Deserialize, BorshDeserialize, BorshSerialize)]
#[serde(crate = "near_sdk::serde")]
pub struct Error {
    service: String,
    sequence: U128,
    code: U128,
    message: String,
    btp_error_code: U128,
    btp_error_message: String,
}
