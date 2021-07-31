//! Owner Management Module

use super::*;

#[near_bindgen]
impl BTPMessageCenter {

    #[owner]
    pub fn add_owner(&mut self, address: AccountId) {
        self.owners.add(&address);
    }

    pub fn remove_owner() {}

    #[private]
    pub fn get_owners(&self) -> String {
        to_value(self.owners.to_vec()).unwrap().to_string()
    }

    #[private]
    pub fn check_permission(&self) {
        assert!(self.owners.contains(&env::signer_account_id()), "{}", BMCError::NotExistsPermission);
    }
}
