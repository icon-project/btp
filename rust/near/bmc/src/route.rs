/*
use btp_common::{Address, BTPAddress};
use near_sdk::borsh::{self, BorshDeserialize, BorshSerialize};
use near_sdk::collections::LookupMap;
use near_sdk::{env, near_bindgen};

#[derive(BorshDeserialize, BorshSerialize)]
pub struct Routes(pub LookupMap<String, String>);

pub trait Route {
    fn new() -> Self;
    fn add_route(&mut self, dst: &BTPAddress, link: &BTPAddress) -> bool;
    fn remove_route(&mut self, dst: &BTPAddress) -> bool;
    fn get_routes(&self, dst: &BTPAddress) -> Result<String, String>;
}

impl Routes {
    fn resolve_route(&mut self, dst_net: &BTPAddress) -> Result<(String, String), String> {
        Ok(("hello".to_string(), "helo".to_string()))
    }
}

impl Route for Routes {
    fn new() -> Self {
        let routes: LookupMap<String, String> = LookupMap::new(b"routes".to_vec());
        Self(routes)
    }

    fn add_route(&mut self, dst: &BTPAddress, link: &BTPAddress) -> bool {
        if dst.is_valid().ok().unwrap() && link.is_valid().ok().unwrap() {
            //TODO :
            //validate caller has necessary permission

            if !self.0.contains_key(&dst.0) {
                self.0.insert(&dst.0, &link.0);

                return true;
            }
        }

        return false;
    }

    fn remove_route(&mut self, dst: &BTPAddress) -> bool {
        //TODO :
        //validate caller has necessary permission

        if dst.is_valid().ok().unwrap() {
            if !self.0.contains_key(&dst.0) {
                return false;
            }

            self.0.remove(&dst.0);

            return true;
        }

        return false;
    }
    fn get_routes(&self, dst: &BTPAddress) -> Result<String, String> {
        if !self.0.contains_key(&dst.0) {
            return Err("Route Not found".to_string());

            //TODO
            // resolve route if mo route found
        }

        Ok(self.0.get(&dst.0).unwrap())
    }
}
*/