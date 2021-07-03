/*
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
    missing_docs,
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
    clippy::wrong_pub_self_convention,
    clippy::unwrap_used,
    trivial_casts,
    trivial_numeric_casts,
    unused_extern_crates,
    unused_import_braces,
    unused_qualifications,
    unused_results
)]
*/

pub mod libraries;

use near_sdk::borsh::{self, BorshDeserialize, BorshSerialize};
use near_sdk::serde::{Deserialize, Serialize};
use near_sdk::{env, metadata, near_bindgen, setup_alloc};
use std::collections::HashMap;

setup_alloc!();

// MOCK SETUP

metadata! {
    #[near_bindgen]
    #[derive(BorshDeserialize, BorshSerialize, Clone, Debug, Deserialize, Serialize)]
    #[serde(crate = "near_sdk::serde")]
    pub struct Token {
        balances: HashMap<Vec<u8>, u64>,
        allowances: HashMap<Vec<u8>, u64>,
        pub owner: Vec<u8>,
        pub ticker: String,
        pub max_supply: u64,
    }
}
