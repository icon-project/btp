use crate::types::{btp_address::Network, asset::AssetMetadata};
use near_sdk::borsh::{self, BorshDeserialize, BorshSerialize};
use near_sdk::json_types::{Base64VecU8, U128};
use near_sdk::serde::{Deserialize, Deserializer, Serialize};
use near_sdk::AccountId;

#[derive(BorshDeserialize, BorshSerialize, Clone, Debug, Deserialize, Serialize, PartialEq)]
#[serde(crate = "near_sdk::serde")]
pub struct AssetMetadataExtras {
    pub spec: String,
    pub icon: Option<String>,
    pub reference: Option<String>,
    pub reference_hash: Option<Base64VecU8>,
}

#[derive(BorshDeserialize, BorshSerialize, Clone, Debug, Deserialize, Serialize, PartialEq)]
#[serde(crate = "near_sdk::serde")]
pub struct FungibleToken {
    name: String,
    symbol: String,
    uri: Option<AccountId>,
    network: Network,
    extras: Option<AssetMetadataExtras>
}

fn deserialize_u128<'de, D>(deserializer: D) -> Result<u128, D::Error>
where
    D: Deserializer<'de>,
{
    <U128 as Deserialize>::deserialize(deserializer).map(|s| s.into())
}

impl FungibleToken {
    pub fn new(
        name: String,
        symbol: String,
        uri: Option<AccountId>,
        network: Network,
        extras: Option<AssetMetadataExtras>
    ) -> FungibleToken {
        Self {
            name,
            symbol,
            uri,
            network,
            extras
        }
    }
}

impl FungibleToken {
    pub fn uri(&self) -> &Option<AccountId> {
        &self.uri
    }

    pub fn uri_deref(&self) -> Option<AccountId> {
        self.uri.clone()
    }
}

impl AssetMetadata for FungibleToken {
    fn name(&self) -> &String {
        &self.name
    }

    fn network(&self) -> &Network {
        &self.network
    }

    fn symbol(&self) -> &String {
        &self.symbol
    }

    fn extras(&self) -> &Option<AssetMetadataExtras>{
        &self.extras
    }

    fn metadata(&self) -> &Self {
        self
    }
}
