use crate::types::{btp_address::Network, token::TokenMetadata, TokenName};
use near_sdk::borsh::{self, BorshDeserialize, BorshSerialize};
use near_sdk::serde::{Deserialize, Serialize};
use near_sdk::AccountId;

#[derive(BorshDeserialize, BorshSerialize, Clone, Deserialize, Serialize)]
#[serde(crate = "near_sdk::serde")]
pub struct FungibleToken {
    name: String,
    symbol: String,
    fee_numerator: u128,
    uri: Option<AccountId>,
    denominator: u128,
    network: Network,
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
