use near_sdk::borsh::{self, BorshDeserialize, BorshSerialize};
use near_sdk::env::*;
use near_sdk::log;
use near_sdk::AccountId;
use std::collections::HashMap;

#[derive(BorshDeserialize, BorshSerialize)]
pub struct Owners(HashMap<AccountId, bool>);

impl Owners {
    pub fn new() -> Option<Self> {
        if current_account_id() != signer_account_id() {
            return None;
        }
        let mut owners = HashMap::new();
        owners.insert(signer_account_id(), true);

        return Some(Self(owners));
    }

    pub fn add(&mut self, address: &AccountId) -> Result<bool, String> {
        if self.0.contains_key(&signer_account_id()) {
            if !self.0.contains_key(address) {
                self.0.insert(address.to_string(), true);

                return Ok(true);
            }
            log!("Added owner already present");
            return Ok(false);
        }

        return Err("Caller does not have ppermission to add another owner".to_string());
    }
    pub fn remove(&mut self, address: &AccountId) -> Result<bool, String> {
        if self.0.contains_key(&signer_account_id()) {
            if self.0.len() > 1 {
                self.0.remove(&address.to_string());

                return Ok(true);
            }
            return Err("BMCRevertNotExistsOwner".to_string());
        }

        return Err("Caller does not have ppermission to add another owner".to_string());
    }

    pub fn is(&self, address: AccountId) -> bool {
        self.0.contains_key(&address)
    }
}
