use near_crypto::InMemorySigner;
use runner::dev_deploy;
use std::path::Path;
use tokio::time::{sleep, Duration};
use tokio::runtime::Handle;
pub use super::types::{Context, Signer};

const BMC_CONTRACT: &str = "res/BMC_CONTRACT.wasm";
const BMV_CONTRACT: &str = "res/BMV_CONTRACT.wasm";
const GENERIC_BSH_CONTRACT: &str = "res/GENERIC_BSH_CONTRACT.wasm";
const BSH_CONTRACT: &str = "res/BSH_CONTRACT.wasm";

async fn deploy_contract(path: &str) -> Result<(String, InMemorySigner), String> {
    sleep(Duration::from_millis(18000)).await;
    dev_deploy(Path::new(path)).await
}

pub fn new_context() -> Context {
    Context::new()
}

pub fn bmc_contract_deployed(mut context: Context) -> Context {
    let handle = Handle::current();
    let (account_id, signer) = tokio::task::block_in_place(move || handle.block_on(async { 
        deploy_contract(BMC_CONTRACT).await.unwrap()
    }));
    let contract_owner = Signer::new(account_id, signer);
    context.contracts.add("bmc", &contract_owner);
    context
}