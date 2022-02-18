use crate::types::{Bmc, Bmv, Context, Contract, NativeCoinBsh, TokenBsh};
use duplicate::duplicate;
use std::path::Path;
use tokio::runtime::Handle;
use workspaces::prelude::*;
use workspaces::{Contract as WorkspaceContract, Sandbox, Worker,DevNetwork};

pub async fn deploy(path: &str, worker: Worker<impl DevNetwork>) -> anyhow::Result<WorkspaceContract> {
    worker.dev_deploy(&std::fs::read(Path::new(path))?).await
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
        let worker = context.worker().clone();
        let handle = Handle::current();
        let contract = tokio::task::block_in_place(move || {
            handle.block_on(async { deploy(self.source(), worker).await.unwrap() })
        });
        context.set_signer(contract.as_account());
        context.contracts_mut().add(self.name(), contract);
        context
    }
}
