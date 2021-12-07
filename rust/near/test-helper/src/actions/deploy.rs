use crate::types::{Bmc, Bmv, TokenBsh, NativeCoinBsh, Context, Contract, Signer};
use duplicate::duplicate;
use futures::executor::LocalPool;
use workspaces::{dev_deploy, AccountId, InMemorySigner};
use std::path::Path;

pub async fn deploy(path: &str) -> anyhow::Result<(AccountId, InMemorySigner)> {
    dev_deploy(Path::new(path)).await
}

#[duplicate(
    contract_type;
    [ Bmc ];
    [ Bmv ];
    [ TokenBsh ];
    [ NativeCoinBsh ];
)]
impl Contract<'_, contract_type> {
    pub fn deploy(&self, mut context: Context) -> Context {
        let mut pool = LocalPool::new();
        let (account_id, signer) = pool.run_until(async { deploy(self.source()).await.unwrap() });
        let contract_owner = Signer::new(signer);
        context.contracts_mut().add(self.name(), &contract_owner);
        context.set_signer(&contract_owner);
        context
    }
}
