use std::str::FromStr;

use lazy_static::lazy_static;
use libraries::types::FungibleToken;
use near_sdk::AccountId;

lazy_static! {
    pub static ref WNEAR: FungibleToken = FungibleToken::new(
        "NEAR".into(),
        "wNEAR".into(),
        Some(AccountId::from_str("wnear.near").unwrap()),
        "0x1.near".into(),
    );
    pub static ref BALN: FungibleToken = FungibleToken::new(
        "BALN".into(), 
        "BALN".into(), 
        None, 
        "0x1.icon".into()
    );
}
