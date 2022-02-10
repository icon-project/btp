use crate::types::Context;
use near_primitives::types::{Balance, Gas};
use near_primitives::views::FinalExecutionStatus;
use serde_json::Value;
use tokio::runtime::Handle;
use workspaces::{self, Account, AccountId, Contract};

pub fn call(
    context: &Context,
    account: &Account,
    contract_id: &AccountId,
    method: &str,
    value: Option<Value>,
    gas: Option<Gas>,
    deposit: Option<Balance>,
) -> Result<(), anyhow::Error> {
    let worker = context.worker().clone();
    let handle = Handle::current();
    tokio::task::block_in_place(move || {
        handle.block_on(async {
            let mut request = account.call(&worker, contract_id, method);

            if let Some(args) = value {
                request = request.args_json(args)?;
            }

            if let Some(gas) = gas {
                request = request.gas(gas);
            }

            if let Some(deposit) = deposit {
                request = request.deposit(deposit);
            }

            let request = request.transact().await;
            match request.unwrap().status {
                FinalExecutionStatus::Failure(err) => Err(anyhow::anyhow!(err)),
                _ => return Ok(()),
            }
        })
    })
}

pub fn view(
    context: &Context,
    contract: &Contract,
    method: &str,
    value: Option<Value>,
) -> Result<serde_json::Value, String> {
    let handle = Handle::current();
    let worker = context.worker().clone();
    tokio::task::block_in_place(move || {
        handle.block_on(async {
            let result = contract
                .view(
                    &worker,
                    method,
                    value.unwrap_or_default().to_string().into_bytes(),
                )
                .await;
            match result {
                Ok(value) => Ok(value.json().unwrap()),
                Err(query_error) => Err(query_error.to_string()),
            }
        })
    })
}

#[macro_export]
macro_rules! invoke_call {
    ($self: ident, $context: ident, $method: tt) => {
        let outcome = crate::actions::call(
            &$context,
            $context.signer().as_ref().unwrap(),
            $context.contracts().get($self.name()).id(),
            $method,
            None,
            None,
            None,
        );
        if outcome.is_err() {
            $context.add_method_errors($method, outcome.unwrap_err().to_string());
        };
    };
    ($self: ident, $context: ident, $method: tt, $param: ident) => {
        let outcome = crate::actions::call(
            &$context,
            $context.signer().as_ref().unwrap(),
            $context.contracts().get($self.name()).id(),
            $method,
            Some($context.$param($method)),
            None,
            None,
        );
        if outcome.is_err() {
            $context.add_method_errors($method, outcome.unwrap_err().to_string());
        };
    };
    ($self: ident, $context: ident, $method: tt, $param: ident, $deposit: expr) => {
        let outcome = crate::actions::call(
            &$context,
            $context.signer().as_ref().unwrap(),
            $context.contracts().get($self.name()).id(),
            $method,
            Some($context.$param($method)),
            None,
            $deposit,
        );
        if outcome.is_err() {
            $context.add_method_errors($method, outcome.unwrap_err().to_string());
        }
    };
    ($self: ident, $context: ident, $method: tt, $param: ident, $deposit: expr, $gas: expr) => {
        let outcome = crate::actions::call(
            &$context,
            $context.signer().as_ref().unwrap(),
            $context.contracts().get($self.name()).id(),
            $method,
            Some($context.$param($method)),
            $gas,
            $deposit,
        );
        if outcome.is_err() {
            $context.add_method_errors($method, outcome.unwrap_err().to_string());
        }
    };
}

#[macro_export]
macro_rules! invoke_view {
    ($self: ident, $context: ident, $method: tt) => {
        let response = crate::actions::view(
            &$context,
            $context.contracts().get($self.name()),
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
            &$context,
            $context.contracts().get($self.name()),
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
