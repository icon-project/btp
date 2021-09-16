use super::*;
use std::collections::HashMap;

#[derive(Default)]
pub struct Contracts(HashMap<String, Signer>);

impl Contracts {
    pub fn add(&mut self, name: &str, signer: &Signer) {
        self.0.insert(name.to_owned(), signer.to_owned());
    }
}
