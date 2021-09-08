//! BSH Data Types

use near_sdk::borsh::{self, BorshDeserialize, BorshSerialize};
use near_sdk::serde::{Deserialize, Serialize};
use near_sdk::{AccountId, BorshStorageKey};

#[derive(
    BorshDeserialize, BorshSerialize, BorshStorageKey, Clone, Debug, Deserialize, Serialize,
)]
#[serde(crate = "near_sdk::serde")]
pub enum BshStorageKey {
    BshPeriphery,
    BshToken,
}

#[derive(BorshDeserialize, BorshSerialize, Clone, Debug, Deserialize, Serialize)]
#[serde(crate = "near_sdk::serde")]
pub enum BshEvents {
    SetOwnership {
        promoter: AccountId,
        new_owner: AccountId,
    },
    RemoveOwnership {
        remover: AccountId,
        former_owner: AccountId,
    },
    /// Sends a receipt to user
    TransferStart {
        from: AccountId,
        to: AccountId,
        sn: u64,
        asset_details: Vec<AssetTransferDetail>,
    },
    /// Sends a final notification to a user
    TransferEnd {
        from: AccountId,
        sn: u64,
        code: usize,
        response: String,
    },
    /// Notify that BSH contract has received unknown response
    UnknownResponse { from: String, sn: u64 },
}

#[derive(BorshDeserialize, BorshSerialize, Clone, Debug, Deserialize, PartialEq, Serialize)]
#[serde(crate = "near_sdk::serde")]
pub enum ServiceType {
    RequestCoinTransfer,
    RequestCoinRegister,
    ResponseHandleService,
    UnknownType,
}

#[derive(
    BorshDeserialize, BorshSerialize, Clone, Debug, Default, Deserialize, PartialEq, Serialize,
)]
#[serde(crate = "near_sdk::serde")]
pub struct PendingTransferCoin {
    pub from: AccountId,
    pub to: AccountId,
    pub coin_names: Vec<String>,
    pub amounts: Vec<u128>,
    pub fees: Vec<u128>,
}

#[derive(BorshDeserialize, BorshSerialize, Clone, Debug, Deserialize, Serialize)]
#[serde(crate = "near_sdk::serde")]
pub struct TransferCoin {
    pub from: AccountId,
    pub to: AccountId,
    pub assets: Vec<Asset>,
}

#[derive(BorshDeserialize, BorshSerialize, Clone, Debug, Deserialize, PartialEq, Serialize)]
#[serde(crate = "near_sdk::serde")]
pub struct Asset {
    pub coin_name: String,
    pub value: u128,
}

#[derive(BorshDeserialize, BorshSerialize, Clone, Debug, Deserialize, Serialize)]
#[serde(crate = "near_sdk::serde")]
pub struct AssetTransferDetail {
    pub coin_name: String,
    pub value: u128,
    pub fee: u128,
}

#[derive(BorshDeserialize, BorshSerialize, Clone, Debug, Deserialize, Serialize)]
#[serde(crate = "near_sdk::serde")]
pub struct Response {
    pub code: usize,
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
    pub locked_balance: u128,
    pub refundable_balance: u128,
}

#[derive(BorshDeserialize, BorshSerialize, Clone, Debug, Deserialize, Serialize)]
#[serde(crate = "near_sdk::serde")]
pub struct Request {
    pub service_name: String,
    /// Address
    pub bsh: Vec<u8>,
}
