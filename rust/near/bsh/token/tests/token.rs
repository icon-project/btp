use std::str::FromStr;

use lazy_static::lazy_static;
use libraries::types::{WrappedFungibleToken,AssetMetadataExtras};
use near_sdk::AccountId;

lazy_static! {
    pub static ref WNEAR: WrappedFungibleToken = WrappedFungibleToken::new(
        "NEAR".into(),
        "wNEAR".into(),
        Some(AccountId::from_str("wnear.near").unwrap()),
        "0x1.near".into(),
        None
        
    );
    pub static ref BALN: WrappedFungibleToken = WrappedFungibleToken::new(
        "BALN".into(),
        "BALN".into(),
        Some(AccountId::from_str("baln.icon").unwrap()),
        "0x1.icon".into(),
        Some(AssetMetadataExtras {
            spec: "ft-1.0.0".to_string(),
            icon: None,
            reference: None,
            reference_hash: None,
            decimals: 24
        })
    );
}
