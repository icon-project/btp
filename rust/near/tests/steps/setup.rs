use futures::executor::LocalPool;
use test_helper::{
    actions::deploy_contract,
    types::{Context, Signer},
};

const BMC_CONTRACT: &str = "res/BMC_CONTRACT.wasm";
const BMV_CONTRACT: &str = "res/BMV_CONTRACT.wasm";
const GENERIC_BSH_CONTRACT: &str = "res/GENERIC_BSH_CONTRACT.wasm";
const BSH_CONTRACT: &str = "res/BSH_CONTRACT.wasm";

fn bmc_contract_deployed(mut context: Context) -> Context {
    let mut pool = LocalPool::new();
    let (account_id, signer) =
        pool.run_until(async { deploy_contract(BMC_CONTRACT).await.unwrap() });
    let contract_owner = Signer::new(account_id, signer);
    context.contracts.add("bmc", &contract_owner);
    context
}

pub static NEW_CONTEXT: fn() -> Context = || Context::new();
pub static BMC_CONTRACT_DEPLOYED: fn(Context) -> Context = bmc_contract_deployed;
