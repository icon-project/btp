use futures::executor::LocalPool;
use near_crypto::InMemorySigner;
use near_primitives::{borsh::BorshSerialize, types::FunctionArgs};
use runner;
use serde_json::Value;

pub fn call(
    signer: &InMemorySigner,
    account_id: &str,
    contract_id: &str,
    method: &str,
    value: Value,
) {
    let mut pool = LocalPool::new();
    pool.run_until(async {
        runner::call(
            signer,
            account_id.to_owned(),
            contract_id.to_owned(),
            method.to_owned(),
            value.to_string().try_to_vec().unwrap_or_default(),
            None,
        )
        .await
        .unwrap()
    });
}

pub fn view(contract_id: &str, method: &str, value: Value) -> serde_json::Value {
    let mut pool = LocalPool::new();
    pool.run_until(async {
        runner::view(
            contract_id.to_owned(),
            method.to_owned(),
            FunctionArgs::from(value.to_string().try_to_vec().unwrap_or_default()),
        )
        .await
        .unwrap()
    })
}

#[macro_export]
macro_rules! invoke_call {
    ($self: ident, $context: ident, $method: tt, $param: ident) => {
        use crate::actions::call;
        call(
            $context.signer().get(),
            $context.signer().account_id(),
            $context.contracts.get($self.name()).account_id(),
            $method,
            $context.$param($method),
        );
    };
}

#[macro_export]
macro_rules! invoke_view {
    ($self: ident, $context: ident, $method: tt, $param: ident) => {
        use crate::actions::view;
        view(
            $context.contracts.get($self.name()).account_id(),
            $method,
            $context.$param($method),
        )
    };
}
