use std::collections::HashMap;
use super::*;

#[derive(Default)]
pub struct Accounts(HashMap<String, Signer>);

impl Accounts {
    pub fn add(&mut self, name: &str, signer: &Signer) {
        self.0.insert(name.to_owned(), signer.to_owned());
    }

    pub fn get(&self, name: &str) -> &Signer {
        self.0.get(name).unwrap()
    }

    pub fn as_mut(&mut self) -> &mut Self {
        self
    }
}