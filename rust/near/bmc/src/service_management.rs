use super::*;

#[near_bindgen]
impl BtpMessageCenter {
        // * * * * * * * * * * * * * * * * *
    // * * * * * * * * * * * * * * * * *
    // * * * * Service Management  * * *
    // * * * * * * * * * * * * * * * * *
    // * * * * * * * * * * * * * * * * *

    pub fn approve_service(&mut self, name: String, approve: bool) {
        self.assert_have_permission();
        self.assert_service_does_not_exists(&name);
        self.assert_request_exists(&name);
        if let Some(service) = self.bsh.requests.get(&name) {
            if approve {
                self.bsh.services.add(&name, &service);
            }
            self.bsh.requests.remove(&name);
        };
    }

    /// Register the smart contract for the service
    /// Caller must be an operator of BTP network
    pub fn request_service(&mut self, name: String, service: AccountId) {
        self.assert_request_does_not_exists(&name);
        self.assert_service_does_not_exists(&name);
        self.bsh.requests.add(&name, &service);
    }

    /// De-register the service from BSH
    /// Caller must be an operator of BTP network    
    pub fn remove_service(&mut self, name: String) {
        self.assert_have_permission();
        self.assert_service_exists(&name);
        self.bsh.services.remove(&name);
    }

    pub fn get_requests(&self) -> String {
        to_value(self.bsh.requests.to_vec()).unwrap().to_string()
    }

    /// Get registered services
    /// Returns an array of services
    pub fn get_services(&self) -> String {
        to_value(self.bsh.services.to_vec()).unwrap().to_string()
    }
}