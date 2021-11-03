//! BMC Data Types

use crate::mta::hash::Hash;
use near_sdk::borsh::{self, BorshDeserialize, BorshSerialize};
use near_sdk::serde::{Deserialize, Serialize};
use near_sdk::{AccountId, BorshStorageKey, IteratorIndex, PublicKey, Timestamp};
use std::collections::HashMap;

#[derive(
    BorshDeserialize, BorshSerialize, BorshStorageKey, Clone, Debug, Deserialize, Serialize,
)]
#[serde(crate = "near_sdk::serde")]
pub enum BmvStorageKey {
    Bmv,
}

#[derive(
    BorshDeserialize, BorshSerialize, Clone, Debug, Default, Deserialize, PartialEq, Serialize,
)]
#[serde(crate = "near_sdk::serde")]
pub struct BmvResult {
    pub state_hash: Hash,
    pub patch_receipt_hash: Hash,
    pub receipt_hash: Hash,
    pub extension_data: Vec<u8>,
}

#[derive(
    BorshDeserialize, BorshSerialize, Clone, Debug, Deserialize, PartialEq, Serialize,
)]
#[serde(crate = "near_sdk::serde")]
pub struct BlockHeader {
    pub block_hash: Hash,
    pub version: u64,
    pub height: u128,
    pub timestamp: Timestamp,
    pub proposer: PublicKey,
    pub prev_hash: Hash,
    pub vote_hash: Hash,
    pub next_validator_hash: Hash,
    pub patch_tx_hash: Vec<u8>,
    pub tx_hash: Vec<u8>,
    pub logs_bloom: Vec<u8>,
    pub result: BmvResult,
    /// Added to check wherther SPR is an empty struct
    /// Will not be included in serialization
    pub is_result_empty: bool,
}

/// TS = Timestamp + Signature
#[derive(
    BorshDeserialize, BorshSerialize, Clone, Debug, Default, Deserialize, PartialEq, Serialize,
)]
#[serde(crate = "near_sdk::serde")]
pub struct Ts {
    pub timestamp: Timestamp,
    pub signature: Vec<u8>,
}

/// BPSI = Block Part Set ID
#[derive(
    BorshDeserialize, BorshSerialize, Clone, Debug, Default, Deserialize, PartialEq, Serialize,
)]
#[serde(crate = "near_sdk::serde")]
pub struct Bpsi {
    pub n: u64,
    pub b: Vec<u8>,
}

#[derive(
    BorshDeserialize, BorshSerialize, Clone, Debug, Default, Deserialize, PartialEq, Serialize,
)]
#[serde(crate = "near_sdk::serde")]
pub struct Validators {
    pub serialized_bytes: Vec<u8>,
    pub validator_hash: Hash,
    pub validator_addrs: Vec<AccountId>,
    pub contained_validators: HashMap<AccountId, bool>,
    pub check_duplicate_votes: HashMap<AccountId, bool>,
}

#[derive(
    BorshDeserialize, BorshSerialize, Clone, Debug, Default, Deserialize, PartialEq, Serialize,
)]
#[serde(crate = "near_sdk::serde")]
pub struct Votes {
    pub round: u64,
    pub block_part_set_id: Bpsi,
    pub ts: Vec<Ts>,
}

#[derive(
    BorshDeserialize, BorshSerialize, Clone, Debug, Default, Deserialize, PartialEq, Serialize,
)]
#[serde(crate = "near_sdk::serde")]
pub struct BlockWitness {
    pub height: u128,
    pub witnesses: Vec<Hash>,
}

#[derive(
    BorshDeserialize, BorshSerialize, Clone, Debug, Default, Deserialize, PartialEq, Serialize,
)]
#[serde(crate = "near_sdk::serde")]
pub struct EventProof {
    pub index: IteratorIndex,
    pub mpt_key: Vec<u8>,
    pub mpt_proofs: Vec<Vec<u8>>,
}

#[derive(
    BorshDeserialize, BorshSerialize, Clone, Debug, Deserialize, PartialEq, Serialize,
)]
#[serde(crate = "near_sdk::serde")]
pub struct BlockUpdate {
    pub block_header: BlockHeader,
    pub votes: Votes,
    pub next_validators: Vec<AccountId>,
    pub next_validators_rlp: Vec<u8>,
    pub next_validators_hash: Hash,
}

#[derive(
    BorshDeserialize, BorshSerialize, Clone, Debug, Default, Deserialize, PartialEq, Serialize,
)]
#[serde(crate = "near_sdk::serde")]
pub struct Receipt {
    pub event_logs: Vec<EventLog>,
    pub event_log_hash: Hash,
}

#[derive(
    BorshDeserialize, BorshSerialize, Clone, Debug, Deserialize, PartialEq, Serialize,
)]
#[serde(crate = "near_sdk::serde")]
pub struct EventLog {
    pub addr: AccountId,
    pub idx: Vec<Vec<u8>>,
    pub data: Vec<Vec<u8>>,
}

#[derive(
    BorshDeserialize, BorshSerialize, Clone, Debug, Default, Deserialize, PartialEq, Serialize,
)]
#[serde(crate = "near_sdk::serde")]
pub struct MessageEvent {
    pub next_bmc: String,
    pub seq: u128,
    pub message: Vec<u8>,
}

#[derive(
    BorshDeserialize, BorshSerialize, Clone, Debug, Default, Deserialize, PartialEq, Serialize,
)]
#[serde(crate = "near_sdk::serde")]
pub struct ReceiptProof {
    pub index: IteratorIndex,
    pub mpt_key: Vec<u8>,
    pub mpt_proofs: Vec<Vec<u8>>,
    pub event_proofs: Vec<EventProof>,
}

#[derive(
    BorshDeserialize, BorshSerialize, Clone, Debug, Deserialize, PartialEq, Serialize,
)]
#[serde(crate = "near_sdk::serde")]
pub struct BlockProof {
    pub block_header: BlockHeader,
    pub block_witness: BlockWitness,
}

#[derive(
    BorshDeserialize, BorshSerialize, Clone, Debug, Deserialize, PartialEq, Serialize,
)]
#[serde(crate = "near_sdk::serde")]
pub struct RelayMessage {
    pub block_updates: Vec<BlockUpdate>,
    pub block_proof: BlockProof,
    /// Added to check in a case BlockProof is an empty struct
    pub is_bp_empty: bool,
}
