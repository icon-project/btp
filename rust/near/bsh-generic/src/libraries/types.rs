use near_sdk::borsh::{self, BorshDeserialize, BorshSerialize};
use near_sdk::serde::{Deserialize, Serialize};

/**
 *List of all structs being used to encode and decode RLP Messages
 */

/// SPR = State Hash + Patch Receipt Hash + Receipt Hash
#[derive(BorshDeserialize, BorshSerialize, Clone, Debug, Deserialize, Serialize)]
#[serde(crate = "near_sdk::serde")]
pub struct SPR {
    pub state_hash: Vec<u8>,
    pub patch_receipt_hash: Vec<u8>,
    pub receipt_hash: Vec<u8>,
}

/// BlockHeader
#[derive(BorshDeserialize, BorshSerialize, Clone, Debug, Deserialize, Serialize)]
#[serde(crate = "near_sdk::serde")]
pub struct BlockHeader {
    pub version: u64,
    pub height: u64,
    pub timestamp: u64,
    pub proposer: Vec<u8>,
    pub prev_hash: Vec<u8>,
    pub vote_hash: Vec<u8>,
    pub next_validators: Vec<u8>,
    pub patch_tx_hash: Vec<u8>,
    pub tx_hash: Vec<u8>,
    pub logs_bloom: Vec<u8>,
    pub spr: SPR,
    /// Add to check whether or not SPR is an empty struct.
    /// Will not be included in serializing thereafter
    pub is_spr_empty: bool,
}

/// TS = Timestamp + Signature
#[derive(BorshDeserialize, BorshSerialize, Clone, Debug, Deserialize, Serialize)]
#[serde(crate = "near_sdk::serde")]
pub struct TS {
    pub timestamp: u64,
    pub signature: Vec<u8>,
}

/// BPSI = Block Part Set ID
#[derive(BorshDeserialize, BorshSerialize, Clone, Debug, Deserialize, Serialize)]
#[serde(crate = "near_sdk::serde")]
pub struct BPSI {
    pub n: u64,
    pub b: Vec<u8>,
}

#[derive(BorshDeserialize, BorshSerialize, Clone, Debug, Deserialize, Serialize)]
#[serde(crate = "near_sdk::serde")]
pub struct Votes {
    pub round: u64,
    pub block_part_set_id: BPSI,
    pub ts: Vec<TS>,
}

#[derive(BorshDeserialize, BorshSerialize, Clone, Debug, Deserialize, Serialize)]
#[serde(crate = "near_sdk::serde")]
pub struct BlockWitness {
    pub height: u64,
    pub witnesses: Vec<Vec<u8>>,
}

#[derive(BorshDeserialize, BorshSerialize, Clone, Debug, Deserialize, Serialize)]
#[serde(crate = "near_sdk::serde")]
pub struct EventProof {
    pub index: u64,
    pub event_mpt_node: Vec<Vec<u8>>,
}

#[derive(BorshDeserialize, BorshSerialize, Clone, Debug, Deserialize, Serialize)]
#[serde(crate = "near_sdk::serde")]
pub struct BlockUpdate {
    pub bh: BlockHeader,
    pub votes: Votes,
    pub validators: Vec<Vec<u8>>,
}

#[derive(BorshDeserialize, BorshSerialize, Clone, Debug, Deserialize, Serialize)]
#[serde(crate = "near_sdk::serde")]
pub struct ReceiptProof {
    pub index: u64,
    pub tx_receipts: Vec<Vec<u8>>,
    pub ep: Vec<EventProof>,
}

#[derive(BorshDeserialize, BorshSerialize, Clone, Debug, Deserialize, Serialize)]
#[serde(crate = "near_sdk::serde")]
pub struct BlockProof {
    pub bh: BlockHeader,
    pub bw: BlockWitness,
}

#[derive(BorshDeserialize, BorshSerialize, Clone, Debug, Deserialize, Serialize)]
#[serde(crate = "near_sdk::serde")]
pub struct RelayMessage {
    pub bu_array: Vec<BlockUpdate>,
    pub bp: BlockProof,
    /// Add to check in a case BlockProof is an empty struct.
    /// When RLP RelayMessage, this field will not be serialized
    pub is_bp_empty: bool,
    pub rp: Vec<ReceiptProof>,
    /// Add to check in a case ReceiptProof is an empty struct.
    /// When RLP RelayMessage, this field will not be serialized
    pub is_rp_empty: bool,
}

/**
 *List of all structs being used by a BSH contract
 */

#[derive(BorshDeserialize, BorshSerialize, Clone, Debug, Deserialize, Serialize)]
#[serde(crate = "near_sdk::serde")]
pub enum ServiceType {
    RequestCoinTransfer,
    RequestCoinRegister,
    ResponseHandleService,
    UnknownType,
}

#[derive(BorshDeserialize, BorshSerialize, Clone, Debug, Deserialize, Serialize)]
#[serde(crate = "near_sdk::serde")]
pub struct PendingTransferCoin {
    pub from: String,
    pub to: String,
    pub coin_names: Vec<String>,
    pub amounts: Vec<u64>,
    pub fees: Vec<u64>,
}

#[derive(BorshDeserialize, BorshSerialize, Clone, Debug, Deserialize, Serialize)]
#[serde(crate = "near_sdk::serde")]
pub struct TransferCoin {
    pub from: String,
    pub to: String,
    pub assets: Vec<Asset>,
}

#[derive(BorshDeserialize, BorshSerialize, Clone, Debug, Deserialize, Serialize)]
#[serde(crate = "near_sdk::serde")]
pub struct Asset {
    pub coin_name: String,
    pub value: u64,
}

#[derive(BorshDeserialize, BorshSerialize, Clone, Debug, Deserialize, Serialize)]
#[serde(crate = "near_sdk::serde")]
pub struct AssetTransferDetail {
    pub coin_name: String,
    pub value: u64,
    pub fee: u64,
}

#[derive(BorshDeserialize, BorshSerialize, Clone, Debug, Deserialize, Serialize)]
#[serde(crate = "near_sdk::serde")]
pub struct Response {
    pub code: u64,
    pub message: String,
}

#[derive(BorshDeserialize, BorshSerialize, Clone, Debug, Deserialize, Serialize)]
#[serde(crate = "near_sdk::serde")]
pub struct ServiceMessage {
    pub service_type: ServiceType,
    pub data: Vec<u8>,
}

#[derive(BorshDeserialize, BorshSerialize, Clone, Debug, Deserialize, Serialize)]
#[serde(crate = "near_sdk::serde")]
pub struct Coin {
    pub id: u64,
    pub symbol: String,
    pub decimals: u64,
}

#[derive(BorshDeserialize, BorshSerialize, Clone, Debug, Deserialize, Serialize)]
#[serde(crate = "near_sdk::serde")]
pub struct Balance {
    pub locked_balance: u64,
    pub refundable_balance: u64,
}

#[derive(BorshDeserialize, BorshSerialize, Clone, Debug, Deserialize, Serialize)]
#[serde(crate = "near_sdk::serde")]
pub struct Request {
    pub service_name: String,
    /// Address
    pub bsh: Vec<u8>,
}

// TODO: List of all structs being used by a BMC contract
