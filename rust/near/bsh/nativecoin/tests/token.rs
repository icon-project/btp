use lazy_static::lazy_static;
use libraries::types::{WrappedNativeCoin};

lazy_static! {
    pub static ref NATIVE_COIN: WrappedNativeCoin =
        WrappedNativeCoin::new("NEAR".into(), "NEAR".into(),None, 10000, 100000, "0x1.near".into(),None);
    pub static ref ICON_COIN: WrappedNativeCoin =
        WrappedNativeCoin::new("ICON".into(), "ICX".into(), None,10000, 100000, "0x1.icon".into(),None);
}
