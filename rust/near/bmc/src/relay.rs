//! Relay Management Module

use super::*;

#[near_bindgen]
impl BTPMessageCenter {
    pub fn rotate_relay() {
        unimplemented!();
    }

    #[owner]
    pub fn add_relays(&mut self, link: String, address_list: String) {
        let link = BTPAddress::new(link);
        assert!(link.is_valid().is_ok(), "{}{}", BMCError::Generic, BTPError::InvalidBTPAddress { description: link.is_valid().unwrap_err() });
        assert!(
            self.links.contains(&link.to_string()),
            "{}",
            BMCError::NotExistsLink
        );
        let address_list = from_str(&address_list);
        assert!(address_list.is_ok());
        let relays: Vec<String> = address_list.unwrap();
        self.links.set(&link.to_string(), None, None, None, Some(relays));
    }

        // Relay Management
        // pub fn add_relay() {}
        // pub fn remove_relay() {}
        // pub fn get_relays() {}
}
