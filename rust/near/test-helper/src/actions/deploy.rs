use crate::types::{Contract, Bmc, Bmv, Bsh, Context, Signer};
use near_crypto::InMemorySigner;
use runner::dev_deploy;
use std::path::Path;
use tokio::time::{sleep, Duration};
use futures::executor::LocalPool;
use duplicate::duplicate;

pub async fn deploy(path: &str) -> Result<(String, InMemorySigner), String> {
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
        let (account_id, signer) =
            pool.run_until(async { deploy(self.source()).await.unwrap() });
        let contract_owner = Signer::new(account_id, signer);
        context.contracts.add(self.name(), &contract_owner);
        context
    }
}
