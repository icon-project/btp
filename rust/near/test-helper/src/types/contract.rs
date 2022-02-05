use super::*;
use duplicate::duplicate;
use std::collections::HashMap;
use std::marker::PhantomData;

#[derive(Default, Clone)]
pub struct Contracts(HashMap<String, Signer>);

impl Contracts {
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

pub struct Contract<'a, T> {
    name: &'a str,
    source: &'a str,
    phantom: PhantomData<&'a T>,
}

#[duplicate(
    contract_type;
    [ Bmc ];
    [ Bmv ];
    [ NativeCoinBsh ];
    [ TokenBsh ];
  )]
impl Contract<'_, contract_type> {
    fn new(name: &'static str, source: &'static str) -> Contract<'static, contract_type> {
        Contract {
            name,
            source,
            phantom: PhantomData::<&contract_type>,
        }
    }

    pub fn name(&self) -> &str {
        self.name
    }

    pub fn source(&self) -> &str {
        self.source
    }
}

pub struct Bmc {}

pub struct Bmv {}

pub struct NativeCoinBsh {}

pub struct TokenBsh {}

pub struct BmcContract {}

impl BmcContract {
    pub fn new(name: &'static str, source: &'static str) -> Contract<'static, Bmc> {
        Contract::<Bmc>::new(name, source)
    }
}

pub struct BmvContract {}

impl BmvContract {
    pub fn new(name: &'static str, source: &'static str) -> Contract<'static, Bmv> {
        Contract::<Bmv>::new(name, source)
    }
}

pub struct NativeCoinBshContract {}

impl NativeCoinBshContract {
    pub fn new(name: &'static str, source: &'static str) -> Contract<'static, NativeCoinBsh> {
        Contract::<NativeCoinBsh>::new(name, source)
    }
}

pub struct TokenBshContract {}

impl TokenBshContract {
    pub fn new(name: &'static str, source: &'static str) -> Contract<'static, TokenBsh> {
        Contract::<TokenBsh>::new(name, source)
    }
}