use crate::types::{btp_address::Network, token::TokenMetadata, TokenName};
use near_sdk::borsh::{self, BorshDeserialize, BorshSerialize};
use near_sdk::serde::{Deserialize, Serialize, Deserializer};
use near_sdk::AccountId;
use near_sdk::json_types::U128;

#[derive(BorshDeserialize, BorshSerialize, Clone, Deserialize, Serialize)]
#[serde(crate = "near_sdk::serde")]
pub struct FungibleToken {
    name: String,
    symbol: String,
    #[serde(deserialize_with = "deserialize_u128")]
    fee_numerator: u128,
    #[serde(deserialize_with = "deserialize_u128")]
    denominator: u128,
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
        fee_numerator: u128,
        denominator: u128,
        network: Network,
    ) -> FungibleToken {
        Self {
            name,
            symbol,
            fee_numerator,
            uri,
            denominator,
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
    
    fn fee_numerator(&self) -> u128 {
        self.fee_numerator
    }

    fn fee_numerator_mut(&mut self) -> &u128 {
        &self.fee_numerator
    }

    fn denominator(&self) -> u128 {
        self.denominator
    }

    fn metadata(&self) -> &Self {
        self
    }
}
