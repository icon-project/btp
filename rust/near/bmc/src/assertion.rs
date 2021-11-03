use super::*;

impl BtpMessageCenter {
    // * * * * * * * * * * * * * * * * *
    // * * * * * * * * * * * * * * * * *
    // * * * * Internal Validations  * *
    // * * * * * * * * * * * * * * * * *
    // * * * * * * * * * * * * * * * * *

    /// Check whether signer account id is an owner
    pub fn assert_have_permission(&self) {
        require!(
            self.owners.contains(&env::predecessor_account_id()),
            format!("{}", BmcError::PermissionNotExist)
        );
    }

    pub fn assert_link_exists(&self, link: &BTPAddress) {
        require!(
            self.links.contains(link),
            format!("{}", BmcError::LinkNotExist)
        );
    }

    pub fn assert_link_does_not_exists(&self, link: &BTPAddress) {
        require!(
            !self.links.contains(link),
            format!("{}", BmcError::LinkExist)
        );
    }

    pub fn assert_owner_exists(&self, account: &AccountId) {
        require!(
            self.owners.contains(&account),
            format!("{}", BmcError::OwnerNotExist)
        );
    }

    pub fn assert_owner_does_not_exists(&self, account: &AccountId) {
        require!(
            !self.owners.contains(account),
            format!("{}", BmcError::OwnerExist)
        );
    }

    pub fn assert_owner_is_not_last_owner(&self) {
        assert!(self.owners.len() > 1, "{}", BmcError::LastOwner);
    }

    pub fn assert_request_exists(&self, name: &str) {
        require!(
            self.bsh.requests.contains(name),
            format!("{}", BmcError::RequestNotExist)
        );
    }

    pub fn assert_request_does_not_exists(&self, name: &str) {
        require!(
            !self.bsh.requests.contains(name),
            format!("{}", BmcError::RequestExist)
        );
    }

    pub fn assert_route_exists(&self, destination: &BTPAddress) {
        require!(
            self.routes.contains(destination),
            format!("{}", BmcError::RouteNotExist)
        );
    }

    pub fn assert_route_does_not_exists(&self, destination: &BTPAddress) {
        require!(
            !self.routes.contains(destination),
            format!("{}", BmcError::RouteExist)
        );
    }

    pub fn assert_sender_is_authorized_service(&self, service: &str) {
        require!(
            self.bsh.services.get(service) == Some(&env::predecessor_account_id()),
            format!("{}", BmcError::PermissionNotExist)
        );
    }

    pub fn assert_service_exists(&self, name: &str) {
        require!(
            self.bsh.services.contains(name),
            format!("{}", BmcError::ServiceNotExist)
        );
    }

    pub fn assert_service_does_not_exists(&self, name: &str) {
        require!(
            !self.bsh.services.contains(name),
            format!("{}", BmcError::ServiceExist)
        );
    }

    pub fn assert_verifier_exists(&self, network: &str) {
        require!(
            self.bmv.contains(network),
            format!("{}", BmcError::VerifierNotExist)
        );
    }

    pub fn assert_verifier_does_not_exists(&self, network: &str) {
        require!(
            !self.bmv.contains(network),
            format!("{}", BmcError::VerifierExist)
        );
    }
}
