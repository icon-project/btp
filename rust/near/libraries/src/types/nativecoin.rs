use near_sdk::borsh::{self, BorshDeserialize, BorshSerialize};
use near_sdk::serde::{Deserialize, Serialize};
use crate::types::{token::TokenMetadata, btp_address::Network, TokenName};

#[derive(BorshDeserialize, BorshSerialize, Clone, Deserialize, Serialize)]
#[serde(crate = "near_sdk::serde")]
pub struct NativeCoin {
    name: String,
    symbol: String,
    fee_numerator: u128,
    denominator: u128,
    network: Network
}

impl NativeCoin {
    pub fn new(
        name: String,
        symbol: String,
        fee_numerator: u128,
        denominator: u128,
        network: Network
    ) -> NativeCoin {
        Self {
            name,
            symbol,
            fee_numerator,
            denominator,
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

    fn fee_numerator(&self) -> u128 {
        self.fee_numerator
    }

    fn fee_numerator_mut(&mut self) -> &u128 {
        &self.fee_numerator
    }

    fn denominator(&self) -> u128 {
        self.denominator
    }
}