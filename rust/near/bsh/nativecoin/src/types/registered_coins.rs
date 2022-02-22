use libraries::types::AssetId;
use near_sdk::borsh::{self, BorshDeserialize, BorshSerialize};
use near_sdk::AccountId;
use std::collections::HashMap;

#[derive(BorshDeserialize, BorshSerialize)]
pub struct RegisteredCoins(HashMap<AccountId, AssetId>);

impl RegisteredCoins {
    pub fn new() -> Self {
        Self(HashMap::new())
    }

    pub fn add(&mut self, coin_account: &AccountId, coin_id: &AssetId) {
        self.0.insert(coin_account.to_owned(), coin_id.to_owned());
    }

    pub fn remove(&mut self, coin_account: &AccountId) {
        if self.0.contains_key(coin_account) {
            self.0.remove(coin_account);
        }
    }

    pub fn contains(&self, coin_account: &AccountId) -> bool {
        return self.0.contains_key(coin_account);
    }

    pub fn get(&self, coin_account: &AccountId) -> Option<&AssetId> {
        if let Some(coin_id) = self.0.get(coin_account) {
            return Some(coin_id);
        }
        None
    }
}
