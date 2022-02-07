use crate::types::{btp_address::Network, token::TokenMetadata, TokenName};
use near_sdk::borsh::{self, BorshDeserialize, BorshSerialize};
use near_sdk::json_types::{Base64VecU8, U128};
use near_sdk::serde::{Deserialize, Deserializer, Serialize};
use near_sdk::AccountId;

#[derive(BorshDeserialize, BorshSerialize, Clone, Deserialize, Serialize)]
#[serde(crate = "near_sdk::serde")]
pub struct FungibleToken {
    name: String,
    symbol: String,
    uri: Option<AccountId>,
    network: Network,
}

fn deserialize_u128<'de, D>(deserializer: D) -> Result<u128, D::Error>
where
    D: Deserializer<'de>,
{
    <U128 as Deserialize>::deserialize(deserializer).map(|s| s.into())
}

impl FungibleToken {
    pub fn new(
        name: TokenName,
        symbol: String,
        uri: Option<AccountId>,
        network: Network,
    ) -> FungibleToken {
        Self {
            name,
            symbol,
            uri,
            network,
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

impl TokenMetadata for FungibleToken {
    fn name(&self) -> &TokenName {
        &self.name
    }

    fn network(&self) -> &Network {
        &self.network
    }

    fn symbol(&self) -> &String {
        &self.symbol
    }

    fn metadata(&self) -> &Self {
        self
    }
}
