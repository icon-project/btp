use near_sdk::borsh::{self, BorshDeserialize, BorshSerialize};
use near_sdk::env::*;
use near_sdk::AccountId;
use std::collections::HashMap;

#[derive(BorshDeserialize, BorshSerialize)]
pub struct Owners(pub HashMap<AccountId, bool>);

pub trait Owner {
    fn new() -> Self;
    fn add_owner(&mut self, address: &AccountId) -> Result<bool, String>;
    fn remove_owner(&mut self, address: &AccountId) -> Result<bool, String>;
    fn is_owner(&self, address: AccountId) -> bool;
}

impl Owner for Owners {
    fn new() -> Self {
        assert!(
            current_account_id() == signer_account_id(),
            "Dont have ppermission to call this method"
        );
        let mut owners = HashMap::new();
        owners.insert(signer_account_id(), true);

        Self(owners)
    }

    fn add_owner(&mut self, address: &AccountId) -> Result<bool, String> {
        assert!(
            self.0.contains_key(&signer_account_id()),
            "Caller does not have ppermission to add another owner"
        );

        return self
            .0
            .insert(address.to_string(), true)
            .ok_or("Added Owner already present".to_string());
    }
    fn remove_owner(&mut self, address: &AccountId) -> Result<bool, String> {
        assert!(
            self.0.contains_key(&signer_account_id()),
            "Caller does not have ppermission to add another owner"
        );
        assert!(self.0.len() > 1, "BMCRevertNotExistsPermission");

        return self
            .0
            .remove(&address.to_string())
            .ok_or("Owner not present".to_string());
    }

    fn is_owner(&self, address: AccountId) -> bool {
        self.0.contains_key(&address)
    }
}
