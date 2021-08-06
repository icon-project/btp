//! BMV

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

use near_sdk::borsh::{self, BorshDeserialize, BorshSerialize};
use near_sdk::{near_bindgen, setup_alloc};

setup_alloc!();
/// This struct implements `Default`: https://github.com/near/near-sdk-rs#writing-rust-contract
#[near_bindgen]
#[derive(BorshDeserialize, BorshSerialize, Clone)]
pub struct Bmv {
    todo: String,
}

impl Default for Bmv {
    fn default() -> Self {
        todo!()
    }
}

#[near_bindgen]
impl Bmv {
    #[init]
    pub fn new(addr: &str) -> Self {
        Self {
            todo: addr.to_string(),
        }
    }

    pub fn get_mta(&self) -> String {
        todo!()
    }

    pub fn get_connected_bmc(&self) -> String {
        todo!()
    }

    pub fn get_net_address(&self) -> String {
        todo!()
    }

    pub fn get_validators(&self) -> (Vec<u32>, Vec<String>) {
        todo!()
    }

    pub fn get_status(&self) -> (u128, u128, u128) {
        todo!()
    }

    pub fn handle_relay_message(
        &mut self,
        _bmc: &str,
        _prev: &str,
        _seq: u128,
        _msg: &str,
    ) -> Vec<Vec<u8>> {
        todo!()
    }
}
