use super::*;

#[derive(Default)]
pub struct Context {
    pub contracts: Contracts,
    signer: Signer
}

impl Context {
    pub fn new() -> Context {
        Context {
            ..Default::default()
        }
    }

    pub fn set_signer(&mut self, signer: &Signer) {
        self.signer.clone_from(signer);
    }
}