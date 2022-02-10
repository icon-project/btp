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

    pub fn assert_link_does_not_have_route_connection(&self, link: &BTPAddress) {
        require!(
            !self
                .connections
                .contains(&Connection::Route(link.network_address().unwrap())),
            format!("{}", BmcError::LinkRouteExist)
        )
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
        require!(self.owners.len() > 1, format!("{}", BmcError::LastOwner));
    }

    pub fn assert_relay_is_registered(&self, link: &BTPAddress) {
        let link = self.links.get(link).unwrap();
        require!(
            link.relays().contains(&env::predecessor_account_id()),
            format!(
                "{}",
                BmcError::Unauthorized {
                    message: "not registered relay"
                }
            )
        )
    }

    pub fn assert_relay_is_valid(&self, accepted_relay: &AccountId) {
        require!(
            &env::predecessor_account_id() == accepted_relay,
            format!(
                "{}",
                BmcError::Unauthorized {
                    message: "invalid relay"
                }
            )
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
            self.services.get(service) == Some(&env::predecessor_account_id()),
            format!("{}", BmcError::PermissionNotExist)
        );
    }

    pub fn assert_service_exists(&self, name: &str) {
        require!(
            self.services.contains(name),
            format!("{}", BmcError::ServiceNotExist)
        );
    }

    pub fn assert_service_does_not_exists(&self, name: &str) {
        require!(
            !self.services.contains(name),
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

    pub fn assert_valid_set_link_param(&self, max_aggregation: u64, delay_limit: u64) {
        require!(
            max_aggregation >= 1 && delay_limit >= 1,
            format!("{}", BmcError::InvalidParam)
        );
    }

    pub fn assert_relay_not_exists(&self, link: &BTPAddress, relay: &AccountId) {
        if let Some(link_property) = self.links.get(&link) {
            require!(
                !link_property.relays().contains(&relay),
                format!(
                    "{}",
                    BmcError::RelayExist {
                        link: link.to_string()
                    }
                )
            );
        }
    }

    pub fn assert_relay_exists(&self, link: &BTPAddress, relay: &AccountId) {
        if let Some(link_property) = self.links.get(&link).as_mut() {
            require!(
                link_property.relays().contains(&relay),
                format!(
                    "{}",
                    BmcError::RelayNotExist {
                        link: link.to_string()
                    }
                )
            );
        }
    }
}
