use super::*;
use std::collections::HashMap;

#[derive(Default)]
pub struct Contracts(HashMap<String, Signer>);

impl Contracts {
    pub fn add(&mut self, name: &str, signer: &Signer) {
        self.0.insert(name.to_owned(), signer.to_owned());
    }
}

pub struct Contract<'a> {
    name: &'a str,
    source: &'a str
}

impl Contract<'_> {
    pub fn new(name: &'static str, source: &'static str) -> Contract<'static> {
        Contract { name, source }
    }

    pub fn name(&self) -> &str {
        self.name
    }

    pub fn source(&self) -> &str {
        self.source
    }
}