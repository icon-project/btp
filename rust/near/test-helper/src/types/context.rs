use super::*;
use serde_json::Value;
use std::collections::HashMap;

#[derive(Default)]
pub struct Context {
    contracts: Contracts,
    accounts: Accounts,
    signer: Signer,
    method_params: HashMap<String, Value>,
    method_responses: HashMap<String, Value>,
}

impl Context {
    pub fn new() -> Context {
        Context {
            ..Default::default()
        }
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

    pub fn signer(&self) -> &Signer {
        &self.signer
    }

    pub fn set_signer(&mut self, signer: &Signer) {
        self.signer.clone_from(signer);
    }

    pub fn add_method_params(&mut self, key: &str, value: Value) {
        self.method_params.insert(key.to_string(), value);
    }

    pub fn add_method_responses(&mut self, key: &str, value: Value) {
        self.method_responses.insert(key.to_string(), value);
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
}
