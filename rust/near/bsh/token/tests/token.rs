use std::str::FromStr;

use lazy_static::lazy_static;
use libraries::types::{AssetMetadataExtras, WrappedFungibleToken};
use near_sdk::AccountId;

lazy_static! {
    pub static ref WNEAR: WrappedFungibleToken = WrappedFungibleToken::new(
        "NEAR".into(),
        "wNEAR".into(),
        Some(AccountId::from_str("wnear.near").unwrap()),
        "0x1.near".into(),
        Some(AssetMetadataExtras {
            icon: None,
            decimals: 24,
            reference: None,
            reference_hash: None,
            spec: "ft-1.0.0".to_string()
        })
    );
    pub static ref BALN: WrappedFungibleToken = WrappedFungibleToken::new(
        "BALN".into(),
        "BALN".into(),
        Some(AccountId::from_str("baln.icon").unwrap()),
        "0x1.icon".into(),
        Some(AssetMetadataExtras {
            icon: None,
            decimals: 24,
            reference: None,
            reference_hash: None,
            spec: "ft-1.0.0".to_string()
        })
    );
}
