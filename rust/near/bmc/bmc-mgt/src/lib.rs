//! BMC Management Contract

#![forbid(
    arithmetic_overflow,
    mutable_transmutes,
    no_mangle_const_items,
    unknown_crate_types
)]
#![warn(
    bad_style,
    deprecated,
    improper_ctypes,
    non_shorthand_field_patterns,
    overflowing_literals,
    stable_features,
    unconditional_recursion,
    unknown_lints,
    unused,
    unused_allocation,
    unused_attributes,
    unused_comparisons,
    unused_features,
    unused_parens,
    unused_variables,
    while_true,
    clippy::unicode_not_nfc,
    clippy::unwrap_used,
    trivial_casts,
    trivial_numeric_casts,
    unused_extern_crates,
    unused_import_braces,
    unused_qualifications,
    unused_results
)]

use btp_common::BTPAddress;
use libraries::bmc_types::*;
use near_sdk::borsh::{self, BorshDeserialize, BorshSerialize};
use near_sdk::collections::UnorderedMap;
use near_sdk::{env, near_bindgen, setup_alloc};

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
        todo!()
    }
}

#[near_bindgen]
impl BmcManagement {
    pub const BLOCK_INTERVAL_MSEC: u32 = 1000;

    /// Update BMC generic
    /// Caller must be an owner of BTP network
    pub fn set_bmc_generic(_addr: &str) {
        todo!()
    }

    /// Add another owner
    /// Caller must be an owner of BTP network
    pub fn add_owner(_owner: &str) {
        todo!()
    }

    /// Remove an existing owner
    /// Caller must be an owner of BTP network
    pub fn remove_owner(_owner: &str) {
        todo!()
    }

    /// Check whether one specific address has owner role
    /// Caller can be ANY
    pub fn is_owner() -> bool {
        todo!()
    }

    /// Register the smart contract for the service
    /// Caller must be an operator of BTP network
    pub fn add_service(_svc: &str, _addr: &str) {
        todo!()
    }

    /// De-register the smart contract for the service
    /// Caller must be an operator of BTP network
    pub fn remove_service(_svc: &str) {
        todo!()
    }

    /// Register BMV for the network
    /// Caller must be an operator of BTP network
    pub fn add_verifier(_net: &str, _addr: &str) {
        todo!()
    }

    /// De-register BMV for the network
    /// Caller must be an operator of BTP network
    pub fn remove_verifier(_net: &str) {
        todo!()
    }

    /// Initialize status information for the link
    /// Caller must be an operator of BTP network
    pub fn add_link(_link: &str) {
        todo!()
    }

    /// Set the link and status information
    /// Caller must be an operator of BTP network
    pub fn set_link(_link: &str, _block_interval: u128, _max_agg: u128, _delay_limit: u128) {
        todo!()
    }

    /// Remove the link and status information
    /// Caller must be an operator of BTP network
    pub fn remove_link(_link: &str) {
        todo!()
    }

    /// Add route to the BMC
    /// Caller must be an operator of BTP network
    pub fn add_route(_dst: &str, _link: &str) {
        todo!()
    }

    /// Remove route to the BMC
    /// Caller must be an operator of BTP network
    pub fn remove_route(_dst: &str) {
        todo!()
    }

    /// Register Relay for the network
    /// Caller must be an operator of BTP network
    pub fn add_relay(_link: &str, _addrs: &[&str]) {
        todo!()
    }

    /// Unregister Relay for the network
    /// Caller must be an operator of BTP network
    pub fn remove_relay(_link: &str, _addrs: &[&str]) {
        todo!()
    }

    /// Get registered services
    /// Returns an array of services
    pub fn get_services() -> Vec<Service> {
        todo!()
    }

    /// Get registered verifiers
    /// Returns an array of verifiers
    pub fn get_verifiers() -> Vec<Verifier> {
        todo!()
    }

    /// Get registered links
    /// Returns an array of links (BTP addresses of the BMCs)
    pub fn get_links() -> Vec<String> {
        todo!()
    }

    /// Get routing information
    /// Returns an array of routes
    pub fn get_routes() -> Vec<Route> {
        todo!()
    }

    /// Get registered relays
    /// Returns a list of relays
    pub fn get_relays(_link: &str) -> Vec<String> {
        todo!()
    }

    /// Get BSH services by name. Only called by BMC generic
    /// Returns BSH service address
    pub fn get_bsh_service_by_name(_service_name: &str) -> String {
        todo!()
    }

    /// Get BMV services by net. Only called by BMC generic
    /// Returns BMV service address
    pub fn get_bmv_service_by_net(_net: &str) -> String {
        todo!()
    }

    /// Get link info. Only called by BMC generic
    /// Returns link info
    pub fn get_link(_to: &str) -> Link {
        todo!()
    }

    /// Get rotation sequence by link. Only called by BMC generic
    /// Returns rotation sequence
    pub fn get_link_rx_seq(_prev: &str) -> u128 {
        todo!()
    }

    /// Get transaction sequence by link. Only called by BMC generic
    /// Returns transaction sequence
    pub fn get_link_tx_seq(_prev: &str) -> u128 {
        todo!()
    }

    /// Get relays by link. Only called by BMC generic
    /// Returns a list of relays' addresses
    pub fn get_link_relays(_prev: &str) -> Vec<String> {
        todo!()
    }

    /// Get relays status by link. Only called by BMC generic
    /// Returns relay status of all relays
    pub fn get_relay_status_by_link(_prev: &str) -> Vec<RelayStats> {
        todo!()
    }

    /// Update rotation sequence by link. Only called by BMC generic
    pub fn update_link_rx_seq(_prev: &str, _val: u128) {
        todo!()
    }

    /// Increase transaction sequence by 1
    pub fn update_link_tx_seq(_prev: &str) {
        todo!()
    }

    /// Add a reachable BTP address to link. Only called by BMC generic
    pub fn update_link_reachable(_prev: &str, _to: &str) {
        todo!()
    }

    /// Remove a reachable BTP address. Only called by BMC generic
    pub fn delete_link_reachable(_prev: &str, _index: u128) {
        todo!()
    }

    /// Update relay status. Only called by BMC generic
    pub fn update_relay_stats(_relay: &str, _block_count_val: u128, _msg_count_val: u128) {
        todo!()
    }

    /// Resolve next BMC. Only called by BMC generic
    /// Returns BTP address of next BMC and destined BMC
    pub fn resolve_route(_dst_net: &str) -> (String, String) {
        todo!()
    }

    /// Rotate relay for relay address. Only called by BMC generic
    /// Returns relay address
    pub fn rotate_relay(
        _link: &str,
        _current_height: u128,
        _relay_msg_height: u128,
        _has_msg: bool,
    ) -> String {
        todo!()
    }
}
