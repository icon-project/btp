use lazy_static::lazy_static;
use libraries::types::NativeCoin;

lazy_static! {
    pub static ref NATIVE_COIN: NativeCoin =
        NativeCoin::new("near".into(), "NEAR".into(), 10000, 1000, "0x1.near".into());
}
