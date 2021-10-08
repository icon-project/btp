use lazy_static::lazy_static;
pub use std::collections::HashSet;

use test_helper::{
    types::{Context, BmcContract, BmvContract, BshContract, Contract, Bmc, Bmv, Bsh},
};

lazy_static! {
    pub static ref BMC_CONTRACT: Contract<'static, Bmc> = BmcContract::new("bmc", "res/BMC_CONTRACT.wasm");
    pub static ref BMV_CONTRACT: Contract<'static, Bmv> = BmvContract::new("bmv", "res/BMV_CONTRACT.wasm");
    pub static ref GENERIC_BSH_CONTRACT: Contract<'static, Bsh> = BshContract::new("bsh", "res/GENERIC_BSH_CONTRACT.wasm");
    pub static ref BSH_CONTRACT: Contract<'static, Bsh> = BshContract::new("bsh", "res/BSH_CONTRACT.wasm");
}

pub static NEW_CONTEXT: fn() -> Context = || Context::new();
pub static BMC_CONTRACT_IS_DEPLOYED: fn(Context) -> Context = |context: Context| BMC_CONTRACT.deploy(context);
pub static BSH_CONTRACT_IS_DEPLOYED: fn(Context) -> Context = |context: Context| GENERIC_BSH_CONTRACT.deploy(context);
pub static BMV_CONTRACT_IS_DEPLOYED: fn(Context) -> Context = |context: Context| BMV_CONTRACT.deploy(context);
pub static TOKEN_BSH_CONTRATC_IS_DEPLOYED: fn(Context) -> Context = |context: Context| BSH_CONTRACT.deploy(context);