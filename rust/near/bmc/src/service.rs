//! Service Management Module

use super::*;

#[near_bindgen]
impl BTPMessageCenter {
    #[owner]
    pub fn approve_service(&mut self, name: String, approve: bool) {
        assert!(
            !self.bsh.services.contains(&name),
            "{}",
            BMCError::ServiceExist
        );
        assert!(
            self.bsh.requests.contains(&name),
            "{}",
            BMCError::RequestNotExist
        );
        if let Some(service) = self.bsh.requests.get(&name) {
            if approve {
                self.bsh.services.add(&name, &service);
            }
            self.bsh.requests.remove(&name);
        }
    }

    pub fn request_service(&mut self, name: String, service: AccountId) {
        assert!(
            env::is_valid_account_id(service.as_bytes()),
            "{}",
            BMCError::InvalidAddress
        );
        assert!(
            !self.bsh.services.contains(&name),
            "{}",
            BMCError::ServiceExist
        );
        assert!(
            !self.bsh.requests.contains(&name),
            "{}",
            BMCError::RequestExist
        );

        self.bsh.requests.add(&name, &service);
    }

    #[owner]
    pub fn remove_service(&mut self, name: String) {
        assert!(
            self.bsh.services.contains(&name),
            "{}",
            BMCError::ServiceNotExist
        );

        self.bsh.services.remove(&name);
    }

    pub fn get_requests(&self) -> String {
        to_value(self.bsh.requests.to_vec()).unwrap().to_string()
    }

    pub fn get_services(&self) -> String {
        to_value(self.bsh.services.to_vec()).unwrap().to_string()
    }
}
