//! BMC Management Contract

use crate::bmc_types::*;
use crate::Bmc;
//use btp_common::BTPAddress;
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
        todo!()
    }
}

impl Bmc for BmcManagement {
    /*** BMC Generic ***/

    /// Get BMC BTP address
    fn get_bmc_btp_address(&self) -> String {
        unimplemented!()
    }

    /// Verify and decode RelayMessage with BMV, and dispatch BTP Messages to registered BSHs
    /// Caller must be a registered relayer.
    fn handle_relay_message(&mut self, _prev: &str, _msg: &str) {
        unimplemented!()
    }

    fn decode_msg_and_validate_relay(
        &mut self,
        _prev: &str,
        _msg: &str,
    ) -> Result<Vec<Vec<u8>>, &str> {
        unimplemented!()
    }

    fn decode_btp_message(&mut self, _rlp: &[u8]) -> Result<BmcMessage, String> {
        unimplemented!()
    }

    fn handle_message_internal(&mut self, _prev: &str, _msg: &BmcMessage) -> Result<(), &str> {
        unimplemented!()
    }

    fn send_message_internal(&mut self, _to: &str, _serialized_msg: &[u8]) -> Result<(), &str> {
        unimplemented!()
    }

    fn send_error_internal(
        &mut self,
        _prev: &str,
        _msg: &BmcMessage,
        _err_code: u32,
        _err_msg: &str,
    ) -> Result<(), &str> {
        unimplemented!()
    }

    /// Send the message to a specific network
    /// Caller must be a registered BSH
    fn send_message(&mut self, _to: &str, _svc: &str, _sn: i64, _msg: &[u8]) -> Result<(), &str> {
        unimplemented!()
    }

    /// Get status of BMC
    fn get_status(&self, _link: &str) -> Result<LinkStats, &str> {
        unimplemented!()
    }

    /*** BMC Management ***/

    /// Update BMC generic
    /// Caller must be an owner of BTP network
    fn set_bmc_generic(&mut self, _addr: &str) {
        todo!()
    }

    /// Add another owner
    /// Caller must be an owner of BTP network
    fn add_owner(&mut self, _owner: &str) {
        todo!()
    }

    /// Remove an existing owner
    /// Caller must be an owner of BTP network
    fn remove_owner(&mut self, _owner: &str) {
        todo!()
    }

    /// Check whether one specific address has owner role
    /// Caller can be ANY
    fn is_owner(&self) -> bool {
        todo!()
    }

    /// Register the smart contract for the service
    /// Caller must be an operator of BTP network
    fn add_service(&mut self, _svc: &str, _addr: &str) {
        todo!()
    }

    /// De-register the smart contract for the service
    /// Caller must be an operator of BTP network
    fn remove_service(&mut self, _svc: &str) {
        todo!()
    }

    /// Register BMV for the network
    /// Caller must be an operator of BTP network
    fn add_verifier(&mut self, _net: &str, _addr: &str) {
        todo!()
    }

    /// De-register BMV for the network
    /// Caller must be an operator of BTP network
    fn remove_verifier(&mut self, _net: &str) {
        todo!()
    }

    /// Initialize status information for the link
    /// Caller must be an operator of BTP network
    fn add_link(&mut self, _link: &str) {
        todo!()
    }

    /// Set the link and status information
    /// Caller must be an operator of BTP network
    fn set_link(&mut self, _link: &str, _block_interval: u128, _max_agg: u128, _delay_limit: u128) {
        todo!()
    }

    /// Remove the link and status information
    /// Caller must be an operator of BTP network
    fn remove_link(&mut self, _link: &str) {
        todo!()
    }

    /// Add route to the BMC
    /// Caller must be an operator of BTP network
    fn add_route(&mut self, _dst: &str, _link: &str) {
        todo!()
    }

    /// Remove route to the BMC
    /// Caller must be an operator of BTP network
    fn remove_route(&mut self, _dst: &str) {
        todo!()
    }

    /// Register Relay for the network
    /// Caller must be an operator of BTP network
    fn add_relay(&mut self, _link: &str, _addrs: &[&str]) {
        todo!()
    }

    /// Unregister Relay for the network
    /// Caller must be an operator of BTP network
    fn remove_relay(&mut self, _link: &str, _addrs: &[&str]) {
        todo!()
    }

    /// Get registered services
    /// Returns an array of services
    fn get_services(&self) -> Vec<Service> {
        todo!()
    }

    /// Get registered verifiers
    /// Returns an array of verifiers
    fn get_verifiers(&self) -> Vec<Verifier> {
        todo!()
    }

    /// Get registered links
    /// Returns an array of links (BTP addresses of the BMCs)
    fn get_links(&self) -> Vec<String> {
        todo!()
    }

    /// Get routing information
    /// Returns an array of routes
    fn get_routes(&self) -> Vec<Route> {
        todo!()
    }

    /// Get registered relays
    /// Returns a list of relays
    fn get_relays(&self, _link: &str) -> Vec<String> {
        todo!()
    }

    /// Get BSH services by name. Only called by BMC generic
    /// Returns BSH service address
    fn get_bsh_service_by_name(&self, _service_name: &str) -> String {
        todo!()
    }

    /// Get BMV services by net. Only called by BMC generic
    /// Returns BMV service address
    fn get_bmv_service_by_net(&self, _net: &str) -> String {
        todo!()
    }

    /// Get link info. Only called by BMC generic
    /// Returns link info
    fn get_link(&self, _to: &str) -> Link {
        todo!()
    }

    /// Get rotation sequence by link. Only called by BMC generic
    /// Returns rotation sequence
    fn get_link_rx_seq(&self, _prev: &str) -> u128 {
        todo!()
    }

    /// Get transaction sequence by link. Only called by BMC generic
    /// Returns transaction sequence
    fn get_link_tx_seq(&self, _prev: &str) -> u128 {
        todo!()
    }

    /// Get relays by link. Only called by BMC generic
    /// Returns a list of relays' addresses
    fn get_link_relays(&self, _prev: &str) -> Vec<String> {
        todo!()
    }

    /// Get relays status by link. Only called by BMC generic
    /// Returns relay status of all relays
    fn get_relay_status_by_link(&self, _prev: &str) -> Vec<RelayStats> {
        todo!()
    }

    /// Update rotation sequence by link. Only called by BMC generic
    fn update_link_rx_seq(&mut self, _prev: &str, _val: u128) {
        todo!()
    }

    /// Increase transaction sequence by 1
    fn update_link_tx_seq(&mut self, _prev: &str) {
        todo!()
    }

    /// Add a reachable BTP address to link. Only called by BMC generic
    fn update_link_reachable(&mut self, _prev: &str, _to: &[&str]) {
        todo!()
    }

    /// Remove a reachable BTP address. Only called by BMC generic
    fn delete_link_reachable(&mut self, _prev: &str, _index: u128) {
        todo!()
    }

    /// Update relay status. Only called by BMC generic
    fn update_relay_stats(&mut self, _relay: &str, _block_count_val: u128, _msg_count_val: u128) {
        todo!()
    }

    /// Resolve next BMC. Only called by BMC generic
    /// Returns BTP address of next BMC and destined BMC
    fn resolve_route(&mut self, _dst_net: &str) -> Result<(String, String), &str> {
        todo!()
    }

    /// Rotate relay for relay address. Only called by BMC generic
    /// Returns relay address
    fn rotate_relay(
        &mut self,
        _link: &str,
        _current_height: u128,
        _relay_msg_height: u128,
        _has_msg: bool,
    ) -> String {
        todo!()
    }
}

#[near_bindgen]
impl BmcManagement {
    pub const BLOCK_INTERVAL_MSEC: u32 = 1000;
}
