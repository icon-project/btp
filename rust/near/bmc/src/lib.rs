//! BMC

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
pub mod bmc_generic;
pub mod bmc_management;
pub mod bmc_types;
pub use bmc_generic::BmcGeneric;
pub use bmc_management::BmcManagement;
pub use bmc_types::*;

/// Interface for BMC
pub trait BMC {
    /*** BMC Generic ***/

    /// Get BMC BTP address
    fn get_bmc_btp_address(&self) -> String;
    /// Verify and decode RelayMessage with BMV, and dispatch BTP Messages to registered BSHs
    /// Caller must be a registered relayer.
    fn handle_relay_message(&mut self, prev: &str, msg: &str);
    fn decode_msg_and_validate_relay(&mut self, prev: &str, msg: &str) -> Vec<Vec<u8>>;
    fn decode_btp_message(&mut self, rlp: &[u8]) -> Result<BmcMessage, String>;
    fn handle_message_internal(&mut self, prev: &str, msg: &BmcMessage);
    fn send_message_internal(&mut self, to: &str, serialized_msg: &[u8]);
    fn send_error_internal(&mut self, prev: &str, msg: BmcMessage, err_code: u32, err_msg: &str);
    /// Send the message to a specific network
    /// Caller must be a registered BSH
    fn send_message(&mut self, to: &str, svc: &str, sn: u64, msg: &[u8]);
    /// Get status of BMC
    fn get_status(&self, link: &str) -> LinkStats;

    /*** BMC Management ***/

    /// Update BMC generic
    /// Caller must be an owner of BTP network
    fn set_bmc_generic(&mut self, addr: &str);
    /// Add another owner
    /// Caller must be an owner of BTP network
    fn add_owner(&mut self, owner: &str);
    /// Remove an existing owner
    /// Caller must be an owner of BTP network
    fn remove_owner(&mut self, owner: &str);
    /// Check whether one specific address has owner role
    /// Caller can be ANY
    fn is_owner(&self) -> bool;
    /// Register the smart contract for the service
    /// Caller must be an operator of BTP network
    fn add_service(&mut self, svc: &str, addr: &str);
    /// De-register the smart contract for the service
    /// Caller must be an operator of BTP network
    fn remove_service(&mut self, svc: &str);
    /// Register BMV for the network
    /// Caller must be an operator of BTP network
    fn add_verifier(&mut self, net: &str, addr: &str);
    /// De-register BMV for the network
    /// Caller must be an operator of BTP network
    fn remove_verifier(&mut self, net: &str);
    /// Initialize status information for the link
    /// Caller must be an operator of BTP network
    fn add_link(&mut self, link: &str);
    /// Set the link and status information
    /// Caller must be an operator of BTP network
    fn set_link(&mut self, link: &str, block_interval: u128, max_agg: u128, delay_limit: u128);
    /// Remove the link and status information
    /// Caller must be an operator of BTP network
    fn remove_link(&mut self, link: &str);
    /// Add route to the BMC
    /// Caller must be an operator of BTP network
    fn add_route(&mut self, dst: &str, link: &str);
    /// Remove route to the BMC
    /// Caller must be an operator of BTP network
    fn remove_route(&mut self, dst: &str);
    /// Register Relay for the network
    /// Caller must be an operator of BTP network
    fn add_relay(&mut self, link: &str, addrs: &[&str]);
    /// Unregister Relay for the network
    /// Caller must be an operator of BTP network
    fn remove_relay(&mut self, link: &str, addrs: &[&str]);
    /// Get registered services
    /// Returns an array of services
    fn get_services(&self) -> Vec<Service>;
    /// Get registered verifiers
    /// Returns an array of verifiers
    fn get_verifiers(&self) -> Vec<Verifier>;
    /// Get registered links
    /// Returns an array of links (BTP addresses of the BMCs)
    fn get_links(&self) -> Vec<String>;
    /// Get routing information
    /// Returns an array of routes
    fn get_routes(&self) -> Vec<Route>;
    /// Get registered relays
    /// Returns a list of relays
    fn get_relays(&self, link: &str) -> Vec<String>;
    /// Get BSH services by name. Only called by BMC generic
    /// Returns BSH service address
    fn get_bsh_service_by_name(&self, service_name: &str) -> String;
    /// Get BMV services by net. Only called by BMC generic
    /// Returns BMV service address
    fn get_bmv_service_by_net(&self, net: &str) -> String;
    /// Get link info. Only called by BMC generic
    /// Returns link info
    fn get_link(&self, to: &str) -> Link;
    /// Get rotation sequence by link. Only called by BMC generic
    /// Returns rotation sequence
    fn get_link_rx_seq(&self, prev: &str) -> u128;
    /// Get transaction sequence by link. Only called by BMC generic
    /// Returns transaction sequence
    fn get_link_tx_seq(&self, prev: &str) -> u128;
    /// Get relays by link. Only called by BMC generic
    /// Returns a list of relays' addresses
    fn get_link_relays(&self, prev: &str) -> Vec<String>;
    /// Get relays status by link. Only called by BMC generic
    /// Returns relay status of all relays
    fn get_relay_status_by_link(&self, prev: &str) -> Vec<RelayStats>;
    /// Update rotation sequence by link. Only called by BMC generic
    fn update_link_rx_seq(&mut self, prev: &str, val: u128);
    /// Increase transaction sequence by 1
    fn update_link_tx_seq(&mut self, prev: &str);
    /// Add a reachable BTP address to link. Only called by BMC generic
    fn update_link_reachable(&mut self, prev: &str, to: &str);
    /// Remove a reachable BTP address. Only called by BMC generic
    fn delete_link_reachable(&mut self, prev: &str, index: u128);
    /// Update relay status. Only called by BMC generic
    fn update_relay_stats(&mut self, relay: &str, block_count_val: u128, msg_count_val: u128);
    /// Resolve next BMC. Only called by BMC generic
    /// Returns BTP address of next BMC and destined BMC
    fn resolve_route(&mut self, dst_net: &str) -> (String, String);
    /// Rotate relay for relay address. Only called by BMC generic
    /// Returns relay address
    fn rotate_relay(
        &mut self,
        link: &str,
        current_height: u128,
        relay_msg_height: u128,
        has_msg: bool,
    ) -> String;
}
