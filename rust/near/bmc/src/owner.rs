//! Owner Management Module

use super::*;

#[near_bindgen]
impl BTPMessageCenter {

    #[owner]
    pub fn add_owner(&mut self, address: AccountId) {
        assert!(
            !self.owners.contains(&address),
            "{}",
            BMCError::OwnerExist
        );
        self.owners.add(&address);
    }

    #[owner]
    pub fn remove_owner(&mut self, address: AccountId) {
        assert!(self.owners.contains(&address), "{}", BMCError::NotExistsOwner);
        assert!(self.owners.len() > 1, "{}", BMCError::LastOwner);
        self.owners.remove(&address)
    }

    #[private]
    pub fn has_permission(&self) {
        assert!(self.owners.contains(&env::signer_account_id()), "{}", BMCError::NotExistsPermission);
    }
}
