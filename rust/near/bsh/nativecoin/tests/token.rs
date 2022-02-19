use std::convert::TryFrom;

use lazy_static::lazy_static;
use libraries::types::{AssetMetadataExtras, WrappedNativeCoin};
use near_sdk::AccountId;

lazy_static! {
    pub static ref NATIVE_COIN: WrappedNativeCoin =
        WrappedNativeCoin::new("NEAR".into(), "NEAR".into(), None, "0x1.near".into(), None);
    pub static ref ICON_COIN: WrappedNativeCoin = WrappedNativeCoin::new(
        "ICON".into(),
        "ICX".into(),
        Some(AccountId::try_from("icx.near".to_string()).unwrap()),
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
