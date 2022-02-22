use super::*;

#[near_bindgen]
impl BtpMessageCenter {
    // * * * * * * * * * * * * * * * * *
    // * * * * * * * * * * * * * * * * *
    // * * * * Service Management  * * *
    // * * * * * * * * * * * * * * * * *
    // * * * * * * * * * * * * * * * * *

    /// Register the smart contract for the service
    /// Caller must be an operator of BTP network
    pub fn add_service(&mut self, name: String, service: AccountId) {
        self.assert_have_permission();
        self.assert_service_does_not_exists(&name);
        self.services.add(&name, &service);
    }

    /// De-register the service from BSH
    /// Caller must be an operator of BTP network    
    pub fn remove_service(&mut self, name: String) {
        self.assert_have_permission();
        self.assert_service_exists(&name);
        self.services.remove(&name);
    }

    /// Get registered services
    /// Returns an array of services
    pub fn get_services(&self) -> Vec<Service> {
        self.services.to_vec()
    }
}
