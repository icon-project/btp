use crate::types::{Bmc, Bmv, Bsh, Context, Contract, Signer};
use duplicate::duplicate;
use futures::executor::LocalPool;
use near_crypto::InMemorySigner;
use runner::dev_deploy;
use std::path::Path;
use tokio::time::{sleep, Duration};

pub async fn deploy(path: &str) -> Result<(String, InMemorySigner), String> {
    // Wait is addeded to wait until the sandbox starts up
    // TODO: Use api to query status of the sandbox
    sleep(Duration::from_millis(18000)).await;
    dev_deploy(Path::new(path)).await
}

#[duplicate(
    contract_type;
    [ Bmc ];
    [ Bmv ];
    [ Bsh ];
)]
impl Contract<'_, contract_type> {
    pub fn deploy(&self, mut context: Context) -> Context {
        let mut pool = LocalPool::new();
        let (_, signer) = pool.run_until(async { deploy(self.source()).await.unwrap() });
        let contract_owner = Signer::new(signer);
        context.contracts_mut().add(self.name(), &contract_owner);
        context
    }
}
