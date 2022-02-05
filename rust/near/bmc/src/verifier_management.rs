use super::*;

#[near_bindgen]
impl BtpMessageCenter {
    // * * * * * * * * * * * * * * * * *
    // * * * * * * * * * * * * * * * * *
    // * * * * Verifier Management * * *
    // * * * * * * * * * * * * * * * * *
    // * * * * * * * * * * * * * * * * *

    pub fn add_verifier(&mut self, network: String, verifier: AccountId) {
        self.assert_have_permission();
        self.assert_verifier_does_not_exists(&network);
        self.bmv.add(&network, &verifier);
    }

    pub fn remove_verifier(&mut self, network: String) {
        self.assert_have_permission();
        self.assert_verifier_exists(&network);
        self.bmv.remove(&network)
    }

    pub fn get_verifiers(&self) -> Vec<Verifier> {
        self.bmv.to_vec()
    }
}
