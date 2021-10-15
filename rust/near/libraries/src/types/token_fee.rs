use crate::types::{Transfer, TokenId};
use near_sdk::borsh::{self, BorshDeserialize, BorshSerialize};
use near_sdk::serde::{Deserialize, Serialize};
use near_sdk::AccountId;
use near_sdk::Balance;
use std::collections::HashMap;

type TokenFee = u128;

#[derive(BorshDeserialize, BorshSerialize)]
pub struct TokenFees(HashMap<TokenId, TokenFee>);

impl TokenFees {
    pub fn new() -> Self {
        Self(HashMap::new())
    }

    pub fn add(&mut self, token_id: TokenId) {
        self.0.insert(token_id, u128::default());
    }

    pub fn remove(&mut self, token_id: &TokenId) {
        self.0.remove(token_id);
    }

    pub fn get(&self, token_id: &TokenId) -> Option<&TokenFee> {
        if let Some(token_fee) = self.0.get(token_id) {
            return Some(token_fee);
        }
        None
    }

    pub fn set(&mut self, token_id: TokenId, token_fee: TokenFee) {
        self.0.insert(token_id, token_fee);
    }
}
