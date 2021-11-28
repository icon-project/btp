use lazy_static::lazy_static;
use serde_json::json;
pub use std::collections::HashSet;

use test_helper::types::{Bmc, BmcContract, Bmv, BmvContract, Bsh, BshContract, Context, Contract};

lazy_static! {
    pub static ref BMC_CONTRACT: Contract<'static, Bmc> =
        BmcContract::new("bmc", "res/BMC_CONTRACT.wasm");
    pub static ref BMV_CONTRACT: Contract<'static, Bmv> =
        BmvContract::new("bmv", "res/BMC_CONTRACT.wasm");
    pub static ref GENERIC_BSH_CONTRACT: Contract<'static, Bsh> =
        BshContract::new("bsh", "res/BMC_CONTRACT.wasm");
    pub static ref BSH_CONTRACT: Contract<'static, Bsh> =
        BshContract::new("bsh", "res/BMC_CONTRACT.wasm");
}

pub static NEW_CONTEXT: fn() -> Context = || Context::new();

pub static BMC_CONTRACT_IS_DEPLOYED: fn(Context) -> Context =
    |context: Context| BMC_CONTRACT.deploy(context);
pub static BSH_CONTRACT_IS_DEPLOYED: fn(Context) -> Context =
    |context: Context| BSH_CONTRACT.deploy(context);
pub static BMV_CONTRACT_IS_DEPLOYED: fn(Context) -> Context =
    |context: Context| BMV_CONTRACT.deploy(context);

pub static BMC_CONTRACT_INITIALZIED: fn(Context) -> Context = |mut context: Context| {
    context.add_method_params(
        "new",
        json!({
            "network": "0x1.near",
            "block_interval": 1500
        }),
    );
    BMC_CONTRACT.initialize(context)
};

pub static BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED: fn(Context) -> Context = |context: Context| {
    context
        .pipe(BMC_CONTRACT_IS_DEPLOYED)
        .pipe(BMC_CONTRACT_INITIALZIED)
};
