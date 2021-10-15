use lazy_static::lazy_static;
use libraries::types::NativeCoin;

lazy_static! {
    pub static ref NATIVE_COIN: NativeCoin =
        NativeCoin::new("NEAR".into(), "NEAR".into(), 10000, 100000, "0x1.near".into());
    pub static ref ICON_COIN: NativeCoin =
        NativeCoin::new("ICON".into(), "ICX".into(), 10000, 100000, "0x1.icon".into());
}
