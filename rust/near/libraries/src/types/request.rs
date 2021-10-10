use crate::types::messages::SerializedMessage;
use near_sdk::borsh::{self, BorshDeserialize, BorshSerialize};
use near_sdk::collections::UnorderedMap;
use near_sdk::serde::Serialize;

#[derive(BorshDeserialize, BorshSerialize)]
pub struct Request {}

#[derive(BorshDeserialize, BorshSerialize)]
pub struct Requests(UnorderedMap<u128, SerializedMessage>);

impl Requests {
    pub fn new() -> Self {
        Self(UnorderedMap::new(b"requests".to_vec()))
    }

    pub fn add(&mut self, serial_no: u128, message: &SerializedMessage) {
        self.0.insert(&serial_no, message);
    }

    pub fn remove(&mut self, serial_no: u128) {
        self.0.remove(&serial_no);
    }

    pub fn get(&self, serial_no: u128) -> Option<SerializedMessage> {
        if let Some(message) = self.0.get(&serial_no) {
            return Some(message);
        }
        None
    }

    pub fn contains(&self, serial_no: u128) -> bool {
        return self.0.get(&serial_no).is_some();
    }
}
