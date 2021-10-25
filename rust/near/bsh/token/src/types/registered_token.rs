use libraries::types::TokenId;
use near_sdk::borsh::{self, BorshDeserialize, BorshSerialize};
use near_sdk::AccountId;
use std::collections::HashMap;

#[derive(BorshDeserialize, BorshSerialize)]
pub struct RegisteredTokens(HashMap<AccountId, TokenId>);

impl RegisteredTokens {
    pub fn new() -> Self {
        Self(HashMap::new())
    }

    pub fn add(&mut self, token_account: &AccountId, token_id: &TokenId) {
        self.0.insert(token_account.to_owned(), token_id.to_owned());
    }

    pub fn remove(&mut self, token_account: &AccountId) {
        if self.0.contains_key(token_account) {
            self.0.remove(token_account);
        }
    }

    pub fn contains(&self, token_account: &AccountId) -> bool {
        return self.0.contains_key(token_account);
    }

    pub fn get(&self, token_account: &AccountId) -> Option<&TokenId> {
        if let Some(token_id) = self.0.get(token_account) {
            return Some(token_id);
        }
        None
    }
}
