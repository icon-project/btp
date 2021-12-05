use lazy_static::lazy_static;
use serde_json::json;
pub use std::collections::HashSet;
use hex::decode;
use test_helper::types::{
    Bmc, BmcContract, Bmv, BmvContract, Context, Contract, NativeCoinBsh, NativeCoinBshContract,
    TokenBsh, TokenBshContract,
};

lazy_static! {
    pub static ref BMC_CONTRACT: Contract<'static, Bmc> =
        BmcContract::new("bmc", "res/BMC_CONTRACT.wasm");
    pub static ref BMV_CONTRACT: Contract<'static, Bmv> =
        BmvContract::new("bmv", "res/BMV_ICON_CONTRACT.wasm");
    pub static ref NATIVE_COIN_BSH_CONTRACT: Contract<'static, NativeCoinBsh> =
        NativeCoinBshContract::new("bsh", "res/BMC_CONTRACT.wasm");
    pub static ref TOKEN_BSH_CONTRACT: Contract<'static, TokenBsh> =
        TokenBshContract::new("bsh", "res/BMC_CONTRACT.wasm");
}

pub static ICON_NETWORK: &str = "0x1.icon";
pub static ICON_BMC: &str = "0xc294b1A62E82d3f135A8F9b2f9cAEAA23fbD6Cf5";
pub static NEAR_NETWORK: &str = "0x1.near";

pub static NEW_CONTEXT: fn() -> Context = || Context::new();

pub static BMC_CONTRACT_IS_DEPLOYED: fn(Context) -> Context =
    |context: Context| BMC_CONTRACT.deploy(context);

pub static BMV_CONTRACT_IS_DEPLOYED: fn(Context) -> Context =
    |context: Context| BMV_CONTRACT.deploy(context);

pub static NATIVE_COIN_BSH_CONTRACT_IS_DEPLOYED: fn(Context) -> Context =
    |context: Context| NATIVE_COIN_BSH_CONTRACT.deploy(context);

pub static TOKEN_BSH_CONTRACT_IS_DEPLOYED: fn(Context) -> Context =
    |context: Context| TOKEN_BSH_CONTRACT.deploy(context);

pub static BMC_CONTRACT_INITIALZIED: fn(Context) -> Context = |mut context: Context| {
    context.add_method_params(
        "new",
        json!({
            "network": NEAR_NETWORK,
            "block_interval": 1500
        }),
    );
    BMC_CONTRACT.initialize(context)
};

pub static BMV_CONTRACT_INITIALZIED: fn(Context) -> Context = |mut context: Context| {
    context.add_method_params(
        "new",
        json!({
            "bmc": context.contracts().get("bmc").account_id(),
            "validators": [
                hex::decode("b6b5791be0b5ef67063b3c10b840fb81514db2fd").unwrap()
            ],
            "network": ICON_NETWORK,
            "offset": "1846537"
        }),
    );
    BMV_CONTRACT.initialize(context)
};

pub static BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED: fn(Context) -> Context = |context: Context| {
    context
        .pipe(BMC_CONTRACT_IS_DEPLOYED)
        .pipe(BMC_CONTRACT_INITIALZIED)
};

pub static BMV_CONTRACT_IS_DEPLOYED_AND_INITIALIZED: fn(Context) -> Context = |context: Context| {
    context
        .pipe(BMV_CONTRACT_IS_DEPLOYED)
        .pipe(BMV_CONTRACT_INITIALZIED)
};
