//! BTP Message Center

use btp_common::{
    emit,
    errors::{BMCError, BTPError},
    messages::BMCMessage,
    owner,
};
use libraries::types::{
    Address, BTPAddress, Bmv, Bsh, Connection, Connections, Links, Owners, Routes,
};
use near_sdk::borsh::{self, BorshDeserialize, BorshSerialize};
use near_sdk::collections::{UnorderedMap, UnorderedSet};
use near_sdk::serde_json::{from_str, json, to_value, Value};
use near_sdk::AccountId;
use near_sdk::{
    env,
    json_types::{Base64VecU8, U128, U64},
    log, near_bindgen, require, serde_json, PanicOnDefault,
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
    connections: Connections,
}

impl Default for BTPMessageCenter {
    fn default() -> Self {
        let mut owners = Owners::new();
        let bsh = Bsh::new();
        let bmv = Bmv::new();
        let links = Links::new();
        let routes = Routes::new();
        let connections = Connections::new();
        owners.add(&env::current_account_id());
        Self {
            owners,
            bsh,
            bmv,
            links,
            routes,
            connections,
        }
    }
}

#[near_bindgen]
impl BTPMessageCenter {
    // * * * * * * * * * * * * * * * * *
    // * * * * * * * * * * * * * * * * *
    // * * * * Interval Validations  * *
    // * * * * * * * * * * * * * * * * *
    // * * * * * * * * * * * * * * * * *

    /// Check whether signer account id is an owner
    fn assert_have_permission(&self) {
        require!(
            self.owners.contains(&env::signer_account_id()),
            format!("{}", BMCError::PermissionNotExist)
        );
    }

    fn assert_link_exists(&self, link: &BTPAddress) {
        require!(
            self.links.contains(link),
            format!("{}", BMCError::LinkNotExist)
        );
    }

    fn assert_link_does_not_exists(&self, link: &BTPAddress) {
        require!(
            !self.links.contains(link),
            format!("{}", BMCError::LinkExist)
        );
    }

    fn assert_owner_exists(&self, account: &AccountId) {
        require!(
            self.owners.contains(&account),
            format!("{}", BMCError::OwnerNotExist)
        );
    }

    fn assert_owner_does_not_exists(&self, account: &AccountId) {
        require!(
            !self.owners.contains(account),
            format!("{}", BMCError::OwnerExist)
        );
    }

    fn assert_owner_is_not_last_owner(&self) {
        assert!(self.owners.len() > 1, "{}", BMCError::LastOwner);
    }

    fn assert_request_exists(&self, name: &str) {
        require!(
            self.bsh.requests.contains(name),
            format!("{}", BMCError::RequestNotExist)
        );
    }

    fn assert_request_does_not_exists(&self, name: &str) {
        require!(
            !self.bsh.requests.contains(name),
            format!("{}", BMCError::RequestExist)
        );
    }

    fn assert_route_exists(&self, destination: &BTPAddress) {
        require!(
            self.routes.contains(destination),
            format!("{}", BMCError::RouteNotExist)
        );
    }

    fn assert_route_does_not_exists(&self, destination: &BTPAddress) {
        require!(
            !self.routes.contains(destination),
            format!("{}", BMCError::RouteExist)
        );
    }

    fn assert_service_exists(&self, name: &str) {
        require!(
            self.bsh.services.contains(name),
            format!("{}", BMCError::ServiceNotExist)
        );
    }
    fn assert_service_does_not_exists(&self, name: &str) {
        require!(
            !self.bsh.services.contains(name),
            format!("{}", BMCError::ServiceExist)
        );
    }
    fn assert_verifier_exists(&self, network: &str) {
        require!(
            self.bmv.contains(network),
            format!("{}", BMCError::VerifierNotExist)
        );
    }
    fn assert_verifier_does_not_exists(&self, network: &str) {
        require!(
            !self.bmv.contains(network),
            format!("{}", BMCError::VerifierExist)
        );
    }

    // * * * * * * * * * * * * * * * * *
    // * * * * * * * * * * * * * * * * *
    // * * * * Owner Management  * * * *
    // * * * * * * * * * * * * * * * * *
    // * * * * * * * * * * * * * * * * *

    /// Add another owner
    /// Caller must be an owner of BTP network
    pub fn add_owner(&mut self, account: AccountId) {
        self.assert_have_permission();
        self.assert_owner_does_not_exists(&account);
        self.owners.add(&account);
    }

    /// Remove an existing owner
    /// Caller must be an owner of BTP network
    pub fn remove_owner(&mut self, account: AccountId) {
        self.assert_have_permission();
        self.assert_owner_exists(&account);
        self.assert_owner_is_not_last_owner();
        self.owners.remove(&account)
    }

    /// Get account ids of registered owners
    /// Caller can be ANY
    pub fn get_owners(&self) -> Vec<AccountId> {
        self.owners.to_vec()
    }

    // * * * * * * * * * * * * * * * * *
    // * * * * * * * * * * * * * * * * *
    // * * * * Service Management  * * *
    // * * * * * * * * * * * * * * * * *
    // * * * * * * * * * * * * * * * * *

    pub fn approve_service(&mut self, name: String, approve: bool) {
        self.assert_have_permission();
        self.assert_service_does_not_exists(&name);
        self.assert_request_exists(&name);
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
        self.assert_request_does_not_exists(&name);
        self.assert_service_does_not_exists(&name);
        self.bsh.requests.add(&name, &service);
    }

    /// De-register the service from BSH
    /// Caller must be an operator of BTP network    
    pub fn remove_service(&mut self, name: String) {
        self.assert_have_permission();
        self.assert_service_exists(&name);
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
        self.assert_have_permission();
        self.assert_verifier_does_not_exists(&network);
        self.bmv.add(&network, &verifier);
    }

    /// De-register BMV for the network
    /// Caller must be an operator of BTP network
    pub fn remove_verifier(&mut self, network: String) {
        self.assert_have_permission();
        self.assert_verifier_exists(&network);
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
        self.assert_have_permission();
        self.assert_verifier_exists(&link.network_address().unwrap());
        self.assert_link_does_not_exists(&link);
        self.links.add(&link);
    }

    pub fn remove_link(&mut self, link: BTPAddress) {
        self.assert_have_permission();
        self.assert_link_exists(&link);
        self.links.remove(&link);
    }

    pub fn get_links(&self) -> String {
        to_value(self.links.to_vec()).unwrap().to_string()
    }

    pub fn set_link(
        &mut self,
        link: BTPAddress,
        block_interval: u64,
        max_aggregation: u64,
        delay_limit: u64,
    ) {
        unimplemented!();
        self.assert_have_permission();
        self.assert_link_exists(&link);
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
        self.assert_have_permission();
        self.assert_route_does_not_exists(&destination);
        self.assert_link_exists(&link);
        self.routes.add(&destination, &link);
        self.connections.add(
            &Connection::Route(destination.network_address().unwrap()),
            &link,
        );
    }

    pub fn remove_route(&mut self, destination: BTPAddress) {
        self.assert_have_permission();
        self.assert_route_exists(&destination);
        let link = self.routes.get(&destination).unwrap_or_default();
        self.routes.remove(&destination);
        self.connections.remove(
            &Connection::Route(destination.network_address().unwrap()),
            &link,
        )
    }

    pub fn get_routes(&self) -> String {
        to_value(self.routes.to_vec()).unwrap().to_string()
    }

    fn resolve_route(&self, destination: BTPAddress) -> Option<BTPAddress> {
        //TODO: Revisit
        // Check if part of links
        if self.links.contains(&destination) {
            return Some(destination);
        }
        // Check if part of routes
        if self
            .connections
            .contains(&Connection::Route(destination.network_address().unwrap()))
        {
            return self
                .connections
                .get(&Connection::Route(destination.network_address().unwrap()));
        }
        // Check if part of link reachable
        if self.connections.contains(&Connection::LinkReachable(
            destination.network_address().unwrap(),
        )) {
            return self.connections.get(&Connection::LinkReachable(
                destination.network_address().unwrap(),
            ));
        }
        None
    }

    // * * * * * * * * * * * * * * * * *
    // * * * * * * * * * * * * * * * * *
    // * * * * Relay Management  * * * *
    // * * * * * * * * * * * * * * * * *
    // * * * * * * * * * * * * * * * * *

    pub fn add_relays(&mut self, link: BTPAddress, relays: Vec<AccountId>) {
        self.assert_have_permission();
        self.assert_link_exists(&link);
        if let Some(link_property) = self.links.get(&link).as_mut() {
            link_property.relays_mut().set(&relays);
            self.links.set(&link, &link_property);
        }
    }

    pub fn add_relay(&mut self, link: BTPAddress, relay: AccountId) {
        self.assert_have_permission();
        self.assert_link_exists(&link);

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
        self.assert_have_permission();
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
        self.assert_link_exists(&link);
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
