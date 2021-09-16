use lazy_static::lazy_static;

use test_helper::{
    types::{Context, Contract},
};

lazy_static! {
    pub static ref BMC_CONTRACT: Contract<'static> = Contract::new("bmc", "res/BMC_CONTRACT.wasm");
    pub static ref BMV_CONTRACT: Contract<'static> = Contract::new("bmv", "res/BMV_CONTRACT.wasm");
    pub static ref GENERIC_BSH_CONTRACT: Contract<'static> = Contract::new("bsh", "res/GENERIC_BSH_CONTRACT.wasm");
    pub static ref BSH_CONTRACT: Contract<'static> = Contract::new("bsh", "res/BSH_CONTRACT.wasm");
}

pub static NEW_CONTEXT: fn() -> Context = || Context::new();
pub static BMC_CONTRACT_DEPLOYED: fn(Context) -> Context = |context: Context| BMC_CONTRACT.deploy_contract(context);
