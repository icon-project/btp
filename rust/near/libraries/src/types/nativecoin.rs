use near_sdk::borsh::{self, BorshDeserialize, BorshSerialize};
use near_sdk::serde::{Deserialize, Serialize, Deserializer};
use crate::types::{token::TokenMetadata, btp_address::Network, TokenName};
use near_sdk::json_types::U128;

#[derive(BorshDeserialize, BorshSerialize, Clone, Deserialize, Serialize, Debug, PartialEq, Eq)]
#[serde(crate = "near_sdk::serde")]
pub struct NativeCoin {
    name: String,
    symbol: String,
    network: Network
}

fn deserialize_u128<'de, D>(deserializer: D) -> Result<u128, D::Error>
where
    D: Deserializer<'de>,
{
    <U128 as Deserialize>::deserialize(deserializer).map(|s| s.into())
}

impl NativeCoin {
    pub fn new(
        name: String,
        symbol: String,
        network: Network
    ) -> NativeCoin {
        Self {
            name,
            symbol,
            network
        }
    }
}

impl TokenMetadata for NativeCoin {
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