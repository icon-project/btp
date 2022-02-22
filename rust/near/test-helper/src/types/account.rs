use std::collections::HashMap;
use workspaces::Account as WorkspaceAccount;

#[derive(Default, Clone)]
pub struct Accounts(HashMap<String, WorkspaceAccount>);

impl Accounts {
    pub fn add(&mut self, name: &str, account: WorkspaceAccount) {
        self.0.insert(name.to_owned(), account);
    }

    pub fn get(&self, name: &str) -> &WorkspaceAccount {
        self.0.get(name).unwrap()
    }

    pub fn as_mut(&mut self) -> &mut Self {
        self
    }
}