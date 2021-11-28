use futures::executor::LocalPool;
use near_crypto::InMemorySigner;
use near_primitives::{types::FunctionArgs, views::FinalExecutionStatus, errors::TxExecutionError};
use workspaces::{self, AccountId};
use serde_json::Value;

pub fn call(
    signer: &InMemorySigner,
    account_id: &AccountId,
    contract_id: &AccountId,
    method: &str,
    value: Option<Value>,
) -> Result<(), TxExecutionError> {
    let mut pool = LocalPool::new();
    pool.run_until(async {
        let request = workspaces::call(
            signer,
            account_id.to_owned(),
            contract_id.to_owned(),
            method.to_owned(),
            value
                .unwrap_or_default()
                .to_string()
                .into(),
            None,
        )
        .await
        .unwrap();
        match &request.status {
            FinalExecutionStatus::Failure(_) => {
                return Err(request.status.as_failure().unwrap())
            }
            _ => return Ok(())
        }
    })
}

pub fn view(contract_id: &AccountId, method: &str, value: Option<Value>) -> serde_json::Value {
    let mut pool = LocalPool::new();
    pool.run_until(async {
        workspaces::view(
            contract_id.to_owned(),
            method.to_owned(),
            FunctionArgs::from(
                value
                    .unwrap_or_default()
                    .to_string()
                    .into_bytes(),
            ),
        )
        .await
        .unwrap()
    })
}

#[macro_export]
macro_rules! invoke_call {
    ($self: ident, $context: ident, $method: tt) => {
        let outcome = crate::actions::call(
            $context.signer().get(),
            $context.signer().account_id(),
            $context.contracts().get($self.name()).account_id(),
            $method,
            None,
        );
        if outcome.is_err() {
            $context.add_method_errors($method, outcome.unwrap_error());
        };
    };
    ($self: ident, $context: ident, $method: tt, $param: ident) => {
        let outcome = crate::actions::call(
            $context.signer().get(),
            $context.signer().account_id(),
            $context.contracts().get($self.name()).account_id(),
            $method,
            Some($context.$param($method)),
        );
        if outcome.is_err() {
            $context.add_method_errors($method, outcome.unwrap_err());
        }
    };
}

#[macro_export]
macro_rules! invoke_view {
    ($self: ident, $context: ident, $method: tt) => {
        let response = crate::actions::view(
            $context.contracts().get($self.name()).account_id(),
            $method,
            None,
        );
        $context.add_method_responses($method, response);
    };
    ($self: ident, $context: ident, $method: tt, $param: ident) => {
        let response = crate::actions::view(
            $context.contracts().get($self.name()).account_id(),
            $method,
            Some($context.$param($method)),
        );
        $context.add_method_responses($method, response);
    };
}
