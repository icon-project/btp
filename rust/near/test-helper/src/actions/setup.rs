use crate::types::Context;
use tokio::runtime::Handle;
use workspaces::prelude::*;
use workspaces::{Account};

pub fn create_account(context: &Context) -> Account {
    let handle = Handle::current();
    tokio::task::block_in_place(|| {
        handle.block_on(async { context.worker().dev_create_account().await.unwrap() })
    })
}
