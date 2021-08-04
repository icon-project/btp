//! BMC Generic Contract

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
pub struct BmcGeneric {
    // a network address BMV, i.e. btp://1234.pra/0xabcd
    bmc_btp_address: String,
    bmc_management: String,
}

impl Default for BmcGeneric {
    fn default() -> Self {
        Self {
            bmc_btp_address: "".to_string(),
            bmc_management: "".to_string(),
        }
    }
}

#[near_bindgen]
impl BmcGeneric {
    pub const UNKNOWN_ERR: u32 = 0;
    pub const BMC_ERR: u32 = 10;
    pub const BMV_ERR: u32 = 25;
    pub const BSH_ERR: u32 = 40;

    #[init]
    pub fn new(network: &str, bmc_mgt_addr: &str) -> Self {
        let bmc_btp_address = format!("btp://{}/{}", network, env::current_account_id());
        Self {
            bmc_btp_address,
            bmc_management: bmc_mgt_addr.to_string(),
        }
    }

    /// Get BMC BTP address
    pub fn get_bmc_btp_address(&self) -> String {
        self.bmc_btp_address.to_string()
    }

    /// Verify and decode RelayMessage with BMV, and dispatch BTP Messages to registered BSHs
    /// Caller must be a registered relayer.
    pub fn handle_relay_message(_prev: &str, _msg: &str) {
        todo!()
    }

    fn decode_msg_and_validate_relay(_prev: &str, _msg: &str) -> Vec<u8> {
        todo!()
    }

    fn decode_btp_message(_rlp: &[u8]) -> BmcMessage {
        todo!()
    }

    fn handle_message_internal(_prev: &str, _msg: BmcMessage) {
        todo!()
    }

    fn send_message_internal(_to: &str, _serialized_msg: &[u8]) {
        todo!()
    }

    fn send_error_internal(_prev: &str, _msg: BmcMessage, _err_code: u32, _err_msg: &str) {
        todo!()
    }

    /// Send the message to a specific network
    /// Caller must be a registered BSH
    pub fn send_message(_to: &str, _svc: &str, _sn: u64, _msg: &[u8]) {
        todo!()
    }

    /// Get status of BMC
    pub fn get_status(_link: &str) -> LinkStats {
        todo!()
    }
}
