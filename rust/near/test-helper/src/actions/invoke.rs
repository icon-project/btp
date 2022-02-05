use futures::executor::LocalPool;
use near_crypto::InMemorySigner;
use near_primitives::{types::FunctionArgs, views::FinalExecutionStatus, errors::TxExecutionError, types::Gas};
use workspaces::{self, AccountId};
use serde_json::Value;

pub fn call(
    signer: &InMemorySigner,
    account_id: &AccountId,
    contract_id: &AccountId,
    method: &str,
    value: Option<Value>,
    gas: Option<Gas>
) -> Result<(), String> {
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
            gas
        )
        .await
        .unwrap();
        match &request.status {
            FinalExecutionStatus::Failure(_) => {
                return Err(request.status.as_failure().unwrap().to_string())
            }
            _ => return Ok(())
        }
    })
}

pub fn view(contract_id: &AccountId, method: &str, value: Option<Value>) -> Result<serde_json::Value, String> {
    let mut pool = LocalPool::new();
    pool.run_until(async {
        let result = workspaces::view(
            contract_id.to_owned(),
            method.to_owned(),
            FunctionArgs::from(
                value
                    .unwrap_or_default()
                    .to_string()
                    .into_bytes(),
            ),
        )
        .await;
        match result {
            Ok(value) => Ok(value),
            Err(tx_error) => Err(tx_error)
        }
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
            None
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
            None
        );
        if outcome.is_err() {
            $context.add_method_errors($method, outcome.unwrap_err());
        }
    };
    ($self: ident, $context: ident, $method: tt, $param: ident, $gas: ident) => {
        let outcome = crate::actions::call(
            $context.signer().get(),
            $context.signer().account_id(),
            $context.contracts().get($self.name()).account_id(),
            $method,
            Some($context.$param($method)),
            Some($gas)
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
        if response.is_err() {
            $context.add_method_errors($method, response.unwrap_err());
        } else {
            $context.add_method_responses($method, response.unwrap());
        }
    };
    ($self: ident, $context: ident, $method: tt, $param: ident) => {
        let response = crate::actions::view(
            $context.contracts().get($self.name()).account_id(),
            $method,
            Some($context.$param($method)),
        );
        if response.is_err() {
            $context.add_method_errors($method, response.unwrap_err());
        } else {
            $context.add_method_responses($method, response.unwrap());
        }
    };
}