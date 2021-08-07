//! BMC Management Contract

use crate::bmc_types::*;
use near_sdk::borsh::{self, BorshDeserialize, BorshSerialize};
use near_sdk::collections::UnorderedMap;
use near_sdk::{near_bindgen, setup_alloc};

setup_alloc!();
/// This struct implements `Default`: https://github.com/near/near-sdk-rs#writing-rust-contract
#[near_bindgen]
#[derive(BorshDeserialize, BorshSerialize)]
pub struct BmcManagement {
    owners: UnorderedMap<String, bool>,
    num_of_owners: u64,
    bsh_services: UnorderedMap<String, String>,
    bmc_services: UnorderedMap<String, String>,
    relay_stats: UnorderedMap<String, RelayStats>,
    routes: UnorderedMap<String, String>,
    links: UnorderedMap<String, Link>,
    list_of_bmv_names: Vec<String>,
    list_of_bsh_names: Vec<String>,
    list_of_route_keys: Vec<String>,
    list_of_link_names: Vec<String>,
    bmc_generic: String,
    pub serial_no: u64,
    addrs: Vec<String>,
    get_route_dst_from_net: UnorderedMap<String, String>,
    get_link_from_net: UnorderedMap<String, String>,
    get_link_from_reachable_net: UnorderedMap<String, Tuple>,
}

impl Default for BmcManagement {
    fn default() -> Self {
        Self {
            owners: UnorderedMap::new(BmcStorageKey::BmcManagement),
            num_of_owners: 0,
            bsh_services: UnorderedMap::new(BmcStorageKey::BmcManagement),
            bmc_services: UnorderedMap::new(BmcStorageKey::BmcManagement),
            relay_stats: UnorderedMap::new(BmcStorageKey::BmcManagement),
            routes: UnorderedMap::new(BmcStorageKey::BmcManagement),
            links: UnorderedMap::new(BmcStorageKey::BmcManagement),
            list_of_bmv_names: vec![],
            list_of_bsh_names: vec![],
            list_of_route_keys: vec![],
            list_of_link_names: vec![],
            bmc_generic: "".to_string(),
            serial_no: 0,
            addrs: vec![],
            get_route_dst_from_net: UnorderedMap::new(BmcStorageKey::BmcManagement),
            get_link_from_net: UnorderedMap::new(BmcStorageKey::BmcManagement),
            get_link_from_reachable_net: UnorderedMap::new(BmcStorageKey::BmcManagement),
        }
    }
}

#[near_bindgen]
impl BmcManagement {
    pub const BLOCK_INTERVAL_MSEC: u32 = 1000;

    /// Update BMC generic
    /// Caller must be an owner of BTP network
    pub fn set_bmc_generic(&mut self, _addr: &str) {
        todo!()
    }

    /// Add another owner
    /// Caller must be an owner of BTP network
    pub fn add_owner(&mut self, _owner: &str) {
        todo!()
    }

    /// Remove an existing owner
    /// Caller must be an owner of BTP network
    pub fn remove_owner(&mut self, _owner: &str) {
        todo!()
    }

    /// Check whether one specific address has owner role
    /// Caller can be ANY
    pub fn is_owner(&self) -> bool {
        todo!()
    }

    /// Register the smart contract for the service
    /// Caller must be an operator of BTP network
    pub fn add_service(&mut self, _svc: &str, _addr: &str) {
        todo!()
    }

    /// De-register the smart contract for the service
    /// Caller must be an operator of BTP network
    pub fn remove_service(&mut self, _svc: &str) {
        todo!()
    }

    /// Register BMV for the network
    /// Caller must be an operator of BTP network
    pub fn add_verifier(&mut self, _net: &str, _addr: &str) {
        todo!()
    }

    /// De-register BMV for the network
    /// Caller must be an operator of BTP network
    pub fn remove_verifier(&mut self, _net: &str) {
        todo!()
    }

    /// Initialize status information for the link
    /// Caller must be an operator of BTP network
    pub fn add_link(&mut self, _link: &str) {
        todo!()
    }

    /// Set the link and status information
    /// Caller must be an operator of BTP network
    pub fn set_link(
        &mut self,
        _link: &str,
        _block_interval: u128,
        _max_agg: u128,
        _delay_limit: u128,
    ) {
        todo!()
    }

    /// Remove the link and status information
    /// Caller must be an operator of BTP network
    pub fn remove_link(&mut self, _link: &str) {
        todo!()
    }

    /// Add route to the BMC
    /// Caller must be an operator of BTP network
    pub fn add_route(&mut self, _dst: &str, _link: &str) {
        todo!()
    }

    /// Remove route to the BMC
    /// Caller must be an operator of BTP network
    pub fn remove_route(&mut self, _dst: &str) {
        todo!()
    }

    /// Register Relay for the network
    /// Caller must be an operator of BTP network
    pub fn add_relay(&mut self, _link: &str, _addrs: &[&str]) {
        todo!()
    }

    /// Unregister Relay for the network
    /// Caller must be an operator of BTP network
    pub fn remove_relay(&mut self, _link: &str, _addrs: &[&str]) {
        todo!()
    }

    /// Get registered services
    /// Returns an array of services
    pub fn get_services(&self) -> Vec<Service> {
        todo!()
    }

    /// Get registered verifiers
    /// Returns an array of verifiers
    pub fn get_verifiers(&self) -> Vec<Verifier> {
        todo!()
    }

    /// Get registered links
    /// Returns an array of links (BTP addresses of the BMCs)
    pub fn get_links(&self) -> Vec<String> {
        todo!()
    }

    /// Get routing information
    /// Returns an array of routes
    pub fn get_routes(&self) -> Vec<Route> {
        todo!()
    }

    /// Get registered relays
    /// Returns a list of relays
    pub fn get_relays(&self, _link: &str) -> Vec<String> {
        todo!()
    }

    /// Get BSH services by name. Only called by BMC generic
    /// Returns BSH service address
    pub fn get_bsh_service_by_name(&self, _service_name: &str) -> String {
        todo!()
    }

    /// Get BMV services by net. Only called by BMC generic
    /// Returns BMV service address
    pub fn get_bmv_service_by_net(&self, _net: &str) -> String {
        todo!()
    }

    /// Get link info. Only called by BMC generic
    /// Returns link info
    pub fn get_link(&self, _to: &str) -> Link {
        todo!()
    }

    /// Get rotation sequence by link. Only called by BMC generic
    /// Returns rotation sequence
    pub fn get_link_rx_seq(&self, _prev: &str) -> u128 {
        todo!()
    }

    /// Get transaction sequence by link. Only called by BMC generic
    /// Returns transaction sequence
    pub fn get_link_tx_seq(&self, _prev: &str) -> u128 {
        todo!()
    }

    /// Get relays by link. Only called by BMC generic
    /// Returns a list of relays' addresses
    pub fn get_link_relays(&self, _prev: &str) -> Vec<String> {
        todo!()
    }

    /// Get relays status by link. Only called by BMC generic
    /// Returns relay status of all relays
    pub fn get_relay_status_by_link(&self, _prev: &str) -> Vec<RelayStats> {
        todo!()
    }

    /// Update rotation sequence by link. Only called by BMC generic
    pub fn update_link_rx_seq(&mut self, _prev: &str, _val: u128) {
        todo!()
    }

    /// Increase transaction sequence by 1
    pub fn update_link_tx_seq(&mut self, _prev: &str) {
        todo!()
    }

    /// Add a reachable BTP address to link. Only called by BMC generic
    pub fn update_link_reachable(&mut self, _prev: &str, _to: &[&str]) {
        todo!()
    }

    /// Remove a reachable BTP address. Only called by BMC generic
    pub fn delete_link_reachable(&mut self, _prev: &str, _index: u128) {
        todo!()
    }

    /// Update relay status. Only called by BMC generic
    pub fn update_relay_stats(
        &mut self,
        _relay: &str,
        _block_count_val: u128,
        _msg_count_val: u128,
    ) {
        todo!()
    }

    /// Resolve next BMC. Only called by BMC generic
    /// Returns BTP address of next BMC and destined BMC
    pub fn resolve_route(&mut self, _dst_net: &str) -> Result<(String, String), &str> {
        todo!()
    }

    /// Rotate relay for relay address. Only called by BMC generic
    /// Returns relay address
    pub fn rotate_relay(
        &mut self,
        _link: &str,
        _current_height: u128,
        _relay_msg_height: u128,
        _has_msg: bool,
    ) -> String {
        todo!()
    }
}
