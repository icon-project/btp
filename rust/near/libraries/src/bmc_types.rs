//! BMC Data Types

use near_sdk::borsh::{self, BorshDeserialize, BorshSerialize};
use near_sdk::serde::{Deserialize, Serialize};
use near_sdk::BorshStorageKey;

#[derive(
    BorshDeserialize, BorshSerialize, BorshStorageKey, Clone, Debug, Deserialize, Serialize,
)]
#[serde(crate = "near_sdk::serde")]
pub enum BmcStorageKey {
    BmcGeneric,
    BmcManagement,
}

#[derive(
    BorshDeserialize, BorshSerialize, BorshStorageKey, Clone, Debug, Deserialize, Serialize,
)]
#[serde(crate = "near_sdk::serde")]
pub enum BmcEvents {
    Message {
        // an address of the next BMC (it could be a destination BMC)
        next: String,
        // a sequence number of BMC (not a sequence number of BSH)
        seq: u128,
        msg: Vec<u8>,
    },
    // emit errors in BTP messages processing
    ErrorOnBtpError {
        svc: String,
        sn: i64,
        code: u64,
        err_msg: String,
        svc_err_code: u64,
        svc_err_msg: String,
    },
}

#[derive(
    BorshDeserialize, BorshSerialize, Clone, Debug, Default, Deserialize, PartialEq, Serialize,
)]
#[serde(crate = "near_sdk::serde")]
pub struct VerifierStats {
    /// MTA = Merkle Trie Accumulator
    pub height_mta: u128,
    pub offset_mta: u128,
    /// Block height of last verified message
    /// which is BTP-Message contained
    pub last_height: u128,
    pub extra: u128,
}

#[derive(
    BorshDeserialize, BorshSerialize, Clone, Debug, Default, Deserialize, PartialEq, Serialize,
)]
#[serde(crate = "near_sdk::serde")]
pub struct Service {
    pub svc: String,
    pub addr: String,
}

#[derive(
    BorshDeserialize, BorshSerialize, Clone, Debug, Default, Deserialize, PartialEq, Serialize,
)]
#[serde(crate = "near_sdk::serde")]
pub struct Verifier {
    pub net: String,
    pub addr: String,
}

#[derive(
    BorshDeserialize, BorshSerialize, Clone, Debug, Default, Deserialize, PartialEq, Serialize,
)]
#[serde(crate = "near_sdk::serde")]
pub struct Route {
    /// BTP address of destination BMC
    pub dst: String,
    /// BTP address of a BMC before reaching dst BMC
    pub next: String,
}

#[derive(
    BorshDeserialize, BorshSerialize, Clone, Debug, Default, Deserialize, PartialEq, Serialize,
)]
#[serde(crate = "near_sdk::serde")]
pub struct Link {
    /// Address of multiple relay handles for this link network
    pub relays: Vec<String>,
    /// A BTP address of the next BMC that can be reached using this link
    pub reachable: Vec<String>,
    pub rx_seq: u128,
    pub tx_seq: u128,
    pub block_interval_src: u128,
    pub block_interval_dst: u128,
    pub max_aggregation: u128,
    pub delay_limit: u128,
    pub relay_idx: u128,
    pub rotate_height: u128,
    pub rx_height: u128,
    pub rx_height_src: u128,
    pub is_connected: bool,
}

#[derive(
    BorshDeserialize, BorshSerialize, Clone, Debug, Default, Deserialize, PartialEq, Serialize,
)]
#[serde(crate = "near_sdk::serde")]
pub struct LinkStats {
    pub rx_seq: u128,
    pub tx_seq: u128,
    pub verifier: VerifierStats,
    pub relays: Vec<RelayStats>,
    pub relay_idx: u128,
    pub rotate_height: u128,
    pub rotate_term: u128,
    pub delay_limit: u128,
    pub max_aggregation: u128,
    pub rx_height_src: u128,
    pub rx_height: u128,
    pub block_interval_src: u128,
    pub block_interval_dst: u128,
    pub current_height: u128,
}

#[derive(
    BorshDeserialize, BorshSerialize, Clone, Debug, Default, Deserialize, PartialEq, Serialize,
)]
#[serde(crate = "near_sdk::serde")]
pub struct RelayStats {
    pub addr: String,
    pub block_count: u128,
    pub msg_count: u128,
}

#[derive(
    BorshDeserialize, BorshSerialize, Clone, Debug, Default, Deserialize, PartialEq, Serialize,
)]
#[serde(crate = "near_sdk::serde")]
pub struct BmcMessage {
    /// an address of BMC (e.g. btp://1234.PARA/0x1234)
    pub src: String,
    /// an address of destination BMC
    pub dst: String,
    /// service name of BSH
    pub svc: String,
    /// sequence number of BMC
    pub sn: i64,
    /// serialized service message from BSH
    pub message: Vec<u8>,
}

#[derive(
    BorshDeserialize, BorshSerialize, Clone, Debug, Default, Deserialize, PartialEq, Serialize,
)]
#[serde(crate = "near_sdk::serde")]
pub struct BmcService {
    pub service_type: String,
    pub payload: Vec<u8>,
}

#[derive(
    BorshDeserialize, BorshSerialize, Clone, Debug, Default, Deserialize, PartialEq, Serialize,
)]
#[serde(crate = "near_sdk::serde")]
pub struct GatherFeeMessage {
    /// BTP address of Fee Aggregator
    pub fa: String,
    /// a list of services
    pub svcs: Vec<String>,
}

#[derive(
    BorshDeserialize, BorshSerialize, Clone, Debug, Default, Deserialize, PartialEq, Serialize,
)]
#[serde(crate = "near_sdk::serde")]
pub struct Tuple {
    pub prev: String,
    pub to: String,
}
