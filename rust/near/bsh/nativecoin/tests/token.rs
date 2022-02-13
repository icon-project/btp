use std::convert::TryFrom;

use lazy_static::lazy_static;
use libraries::types::WrappedNativeCoin;
use near_sdk::AccountId;

lazy_static! {
    pub static ref NATIVE_COIN: WrappedNativeCoin =
        WrappedNativeCoin::new("NEAR".into(), "NEAR".into(), None, "0x1.near".into(), None);
    pub static ref ICON_COIN: WrappedNativeCoin =
        WrappedNativeCoin::new("ICON".into(), "ICX".into(), Some(AccountId::try_from("icx.near".to_string()).unwrap()), "0x1.icon".into(), None);
}
