use super::*;
use serde_json::Value;
use std::collections::HashMap;
use workspaces::{Worker, Account as WorkspaceAccount, Sandbox, sandbox};

pub struct Context {
    worker: Worker<Sandbox>,
    contracts: Contracts,
    accounts: Accounts,
    signer: Option<WorkspaceAccount>,
    method_params: HashMap<String, Value>,
    method_responses: HashMap<String, Value>,
    method_errors: HashMap<String, String>,
}

impl Context {
    pub fn new() -> Context {
        Context {
            worker: sandbox(),
            contracts: Contracts::default(),
            accounts: Accounts::default(),
            signer: None,
            method_params: HashMap::default(),
            method_responses: HashMap::default(),
            method_errors: HashMap::default()
        }
    }

    pub fn worker(&self) -> &Worker<Sandbox> {
        &self.worker
    }

    pub fn pipe(self, function: fn(Self) -> Self) -> Self {
        function(self)
    }

    pub fn contracts(&self) -> &Contracts {
        &self.contracts
    }

    pub fn contracts_mut(&mut self) -> &mut Contracts {
        self.contracts.as_mut()
    }

    pub fn accounts(&self) -> &Accounts {
        &self.accounts
    }

    pub fn accounts_mut(&mut self) -> &mut Accounts {
        self.accounts.as_mut()
    }

    pub fn signer(&self) -> &Option<WorkspaceAccount> {
        &self.signer
    }

    pub fn set_signer(&mut self, signer: &WorkspaceAccount) {
        self.signer.clone_from(&Some(signer.clone()))
    }

    pub fn add_method_params(&mut self, key: &str, value: Value) {
        self.method_params.insert(key.to_string(), value);
    }

    pub fn add_method_responses(&mut self, key: &str, value: Value) {
        self.method_responses.insert(key.to_string(), value);
    }

    pub fn add_method_errors(&mut self, key: &str, tx_execution_error: String) {
        self.method_errors
            .insert(key.to_string(), tx_execution_error);
    }

    pub fn method_params(&self, key: &str) -> Value {
        self.method_params
            .get(key)
            .unwrap_or(&Value::default())
            .to_owned()
    }

    pub fn method_responses(&self, key: &str) -> Value {
        self.method_responses
            .get(key)
            .unwrap_or(&Value::default())
            .to_owned()
    }

    pub fn method_errors(&self, key: &str) -> String {
        self.method_errors.get(key).unwrap().to_owned()
    }
}
