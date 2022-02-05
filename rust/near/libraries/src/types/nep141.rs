use near_sdk::{json_types::{Base64VecU8}};
use near_contract_standards::fungible_token::metadata::{FungibleTokenMetadata,FungibleTokenMetadataProvider};
use near_sdk::borsh::{self, BorshDeserialize, BorshSerialize};
use near_sdk::serde::{Deserialize, Serialize};


pub struct Nep141 {
    pub spec: String,
    pub name: String,
    pub symbol: String,
    pub icon: Option<String>,
    pub reference: Option<String>,
    pub reference_hash: Option<Base64VecU8>,
    pub decimals: u8,
}

impl Nep141 {
    pub fn new(
        spec: String,
        name: String,
        symbol: String,
        icon: Option<String>,
        reference: Option<String>,
        reference_hash: Option<Base64VecU8>,
        decimals: u8,
    ) -> Nep141 {
        Self {
            spec,
            name,
            symbol,
            icon,
            reference,
            reference_hash,
            decimals,
        }
    }
}

impl FungibleTokenMetadataProvider for Nep141 {
    fn ft_metadata(&self) -> FungibleTokenMetadata {
        FungibleTokenMetadata { 
            spec : self.spec.clone(),
            name: self.name.clone(),
            symbol: self.symbol.clone(),
            icon: self.icon.clone(),
            reference: self.reference.clone(),
            reference_hash: self.reference_hash.clone(),
            decimals: self.decimals.clone()
         }
    }
}