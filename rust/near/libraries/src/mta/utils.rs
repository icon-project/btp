//! Utils

use near_sdk::borsh::{self, BorshDeserialize, BorshSerialize};
use near_sdk::serde::{Deserialize, Serialize};

/// RlpItem
#[derive(BorshDeserialize, BorshSerialize, Clone, Debug, Default, Deserialize, Serialize)]
#[serde(crate = "near_sdk::serde")]
pub struct RlpItem {
    /// Length of the RlpItem
    pub len: usize,
    /// Memory pointer
    pub mem_ptr: u32,
}

impl RlpItem {
    /// Convert from bytes to RlpItem
    pub fn to_rlp_item(bytes: &[u8]) -> Self {
        let len = bytes.len();
        let mem_ptr = bytes.as_ptr() as u32;
        Self { len, mem_ptr }
    }
}
