//! BTP Message Center

use btp_common::{
    emit,
    errors::{BMCError, BTPError},
    messages::BMCMessage,
    owner,
};
use libraries::types::{Address, BTPAddress, Bmv, Bsh, Links, Owners, Routes};
use near_sdk::borsh::{self, BorshDeserialize, BorshSerialize};
use near_sdk::collections::{UnorderedMap, UnorderedSet};
use near_sdk::serde_json::{from_str, json, to_value, Value};
use near_sdk::AccountId;
use near_sdk::{
    env,
    json_types::{Base64VecU8, U128, U64},
    log, near_bindgen, require, serde_json, setup_alloc, PanicOnDefault,
};
use std::collections::HashMap;

#[near_bindgen]
#[derive(BorshDeserialize, BorshSerialize)]
pub struct BTPMessageCenter {
    owners: Owners,
    bsh: Bsh,
    bmv: Bmv,
    links: Links,
    routes: Routes,
}

impl Default for BTPMessageCenter {
    fn default() -> Self {
        let mut owners = Owners::new();
        let bsh = Bsh::new();
        let bmv = Bmv::new();
        let links = Links::new();
        let routes = Routes::new();
        owners.add(&env::current_account_id());
        Self {
            owners,
            bsh,
            bmv,
            links,
            routes
        }
    }
}

#[near_bindgen]
impl BTPMessageCenter {
    // * * * * * * * * * * * * * * * * *
    // * * * * * * * * * * * * * * * * *
    // * * * * Owner Management  * * * *
    // * * * * * * * * * * * * * * * * *
    // * * * * * * * * * * * * * * * * *

    /// Add another owner
    /// Caller must be an owner of BTP network
    pub fn add_owner(&mut self, account: AccountId) {
        self.has_permission();
        require!(
            !self.owners.contains(&account),
            format!("{}", BMCError::OwnerExist)
        );
        self.owners.add(&account);
    }

    /// Remove an existing owner
    /// Caller must be an owner of BTP network
    pub fn remove_owner(&mut self, account: AccountId) {
        self.has_permission();
        require!(
            self.owners.contains(&account),
            format!("{}", BMCError::OwnerNotExist)
        );
        assert!(self.owners.len() > 1, "{}", BMCError::LastOwner);
        self.owners.remove(&account)
    }

    /// Get account ids of registered owners
    /// Caller can be ANY
    pub fn get_owners(&self) -> Vec<AccountId> {
        self.owners.to_vec()
    }

    /// Check whether signer account id is an owner
    fn has_permission(&self) {
        require!(
            self.owners.contains(&env::signer_account_id()),
            format!("{}", BMCError::PermissionNotExist)
        );
    }

    // * * * * * * * * * * * * * * * * *
    // * * * * * * * * * * * * * * * * *
    // * * * * Service Management  * * *
    // * * * * * * * * * * * * * * * * *
    // * * * * * * * * * * * * * * * * *

    pub fn approve_service(&mut self, name: String, approve: bool) {
        self.has_permission();
        require!(
            !self.bsh.services.contains(&name),
            format!("{}", BMCError::ServiceExist)
        );
        require!(
            self.bsh.requests.contains(&name),
            format!("{}", BMCError::RequestNotExist)
        );
        if let Some(service) = self.bsh.requests.get(&name) {
            if approve {
                self.bsh.services.add(&name, &service);
            }
            self.bsh.requests.remove(&name);
        }
    }

    /// Register the smart contract for the service
    /// Caller must be an operator of BTP network
    pub fn request_service(&mut self, name: String, service: AccountId) {
        require!(
            env::is_valid_account_id(service.as_bytes()),
            format!("{}", BMCError::InvalidAddress)
        );
        require!(
            !self.bsh.services.contains(&name),
            format!("{}", BMCError::ServiceExist)
        );
        require!(
            !self.bsh.requests.contains(&name),
            format!("{}", BMCError::RequestExist)
        );

        self.bsh.requests.add(&name, &service);
    }

    /// De-register the service from BSH
    /// Caller must be an operator of BTP network    
    pub fn remove_service(&mut self, name: String) {
        self.has_permission();
        require!(
            self.bsh.services.contains(&name),
            format!("{}", BMCError::ServiceNotExist)
        );

        self.bsh.services.remove(&name);
    }

    pub fn get_requests(&self) -> String {
        to_value(self.bsh.requests.to_vec()).unwrap().to_string()
    }

    /// Get registered services
    /// Returns an array of services
    pub fn get_services(&self) -> String {
        to_value(self.bsh.services.to_vec()).unwrap().to_string()
    }

    // * * * * * * * * * * * * * * * * *
    // * * * * * * * * * * * * * * * * *
    // * * * * Verifier Management * * *
    // * * * * * * * * * * * * * * * * *
    // * * * * * * * * * * * * * * * * *

    /// Register BMV for the network
    /// Caller must be an operator of BTP network
    pub fn add_verifier(&mut self, network: String, verifier: AccountId) {
        self.has_permission();
        require!(
            !self.bmv.contains(&network),
            format!("{}", BMCError::VerifierExist)
        );
        self.bmv.add(&network, &verifier);
    }

    /// De-register BMV for the network
    /// Caller must be an operator of BTP network
    pub fn remove_verifier(&mut self, network: String) {
        self.has_permission();
        require!(
            self.bmv.contains(&network),
            format!("{}", BMCError::VerifierNotExist)
        );
        self.bmv.remove(&network)
    }

    /// Get registered verifiers
    /// Returns an array of verifiers
    pub fn get_verifiers(&self) -> String {
        to_value(self.bmv.to_vec()).unwrap().to_string()
    }

    // * * * * * * * * * * * * * * * * *
    // * * * * * * * * * * * * * * * * *
    // * * * * Link Management * * * * *
    // * * * * * * * * * * * * * * * * *
    // * * * * * * * * * * * * * * * * *

    pub fn add_link(&mut self, link: BTPAddress) {
        self.has_permission();
        require!(
            self.bmv.contains(&link.network_address().unwrap()),
            format!("{}", BMCError::VerifierNotExist)
        );
        require!(
            !self.links.contains(&link),
            format!("{}", BMCError::LinkExist)
        );
        self.links.add(&link);
    }

    pub fn remove_link(&mut self, link: BTPAddress) {
        self.has_permission();
        require!(
            self.links.contains(&link),
            format!("{}", BMCError::LinkNotExist)
        );
        self.links.remove(&link);
    }

    pub fn get_links(&self) -> String {
        to_value(self.links.to_vec()).unwrap().to_string()
    }

    pub fn set_link(&mut self, link: BTPAddress, block_interval: u64, max_aggregation: u64, delay_limit: u64) {
        self.has_permission();
        require!(
            self.links.contains(&link),
            format!("{}", BMCError::LinkNotExist)
        );
        require!(
            max_aggregation >= 1 && delay_limit >= 1,
            format!("{}", BMCError::InvalidParam)
        );
        if let Some(link_property) = self.links.get(&link).as_mut() {
            unimplemented!();
            self.links.set(&link, &link_property);
        }
    }

    pub fn get_status() {
        unimplemented!();
    }

    // * * * * * * * * * * * * * * * * *
    // * * * * * * * * * * * * * * * * *
    // * * * * Route Management  * * * *
    // * * * * * * * * * * * * * * * * *
    // * * * * * * * * * * * * * * * * *

    pub fn add_route(&mut self, destination: BTPAddress, link: BTPAddress) {
        self.has_permission();
        require!(
            !self.routes.links().contains(&destination),
            format!("{}", BMCError::RouteExist)
        );
        self.routes.add(&destination, &link);
    }

    pub fn remove_route(&mut self, destination: BTPAddress, link: BTPAddress) {
        self.has_permission();
        require!(
            self.routes.links().contains(&destination),
            format!("{}", BMCError::RouteNotExist)
        );
        self.routes.remove(&destination);
    }

    pub fn get_routes() {
        unimplemented!();
    }
    
    fn resolve_route() {
        unimplemented!();
    }

    // * * * * * * * * * * * * * * * * *
    // * * * * * * * * * * * * * * * * *
    // * * * * Relay Management  * * * *
    // * * * * * * * * * * * * * * * * *
    // * * * * * * * * * * * * * * * * *

    pub fn add_relays(&mut self, link: BTPAddress, relays: Vec<AccountId>) {
        self.has_permission();
        require!(
            self.links.contains(&link),
            format!("{}", BMCError::LinkNotExist)
        );

        if let Some(link_property) = self.links.get(&link).as_mut() {
            link_property.relays_mut().set(&relays);
            self.links.set(&link, &link_property);
        }
    }

    pub fn add_relay(&mut self, link: BTPAddress, relay: AccountId) {
        self.has_permission();
        require!(
            self.links.contains(&link),
            format!("{}", BMCError::LinkNotExist)
        );

        if let Some(link_property) = self.links.get(&link).as_mut() {
            require!(
                link_property.relays().contains(&relay),
                format!(
                    "{}",
                    BMCError::RelayExist {
                        link: link.to_string()
                    }
                )
            );
            link_property.relays_mut().add(&relay);
            self.links.set(&link, &link_property);
        }
    }

    pub fn remove_relay(&mut self, link: BTPAddress, relay: AccountId) {
        self.has_permission();
        require!(
            self.links.contains(&link),
            format!("{}", BMCError::LinkNotExist)
        );
        if let Some(link_property) = self.links.get(&link).as_mut() {
            require!(
                !link_property.relays().contains(&relay),
                format!(
                    "{}",
                    BMCError::RelayNotExist {
                        link: link.to_string()
                    }
                )
            );
            link_property.relays_mut().remove(&relay);
            self.links.set(&link, &link_property);
        }
    }

    pub fn get_relays(&self, link: BTPAddress) -> String {
        self.has_permission();
        require!(
            self.links.contains(&link),
            format!("{}", BMCError::LinkNotExist)
        );
        if let Some(link_property) = self.links.get(&link).as_mut() {
            to_value(link_property.relays().to_vec())
                .unwrap()
                .to_string()
        } else {
            to_value(Vec::new() as Vec<String>).unwrap().to_string()
        }
    }

    pub fn rotate_relay() {
        unimplemented!();
    }

    // * * * * * * * * * * * * * * * * *
    // * * * * * * * * * * * * * * * * *
    // * * * *    Messaging    * * * * *
    // * * * * * * * * * * * * * * * * *
    // * * * * * * * * * * * * * * * * *
}
