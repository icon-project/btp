use btp_common::BTPAddress;
use near_sdk::borsh::{self, BorshDeserialize, BorshSerialize};
use near_sdk::collections::{LookupMap, UnorderedMap};
use near_sdk::{env, near_bindgen};

#[derive(BorshDeserialize, BorshSerialize)]
pub struct Routes(pub UnorderedMap<String, String>);

pub trait Route {
    fn new() -> Self;
    fn add_route(&mut self, dst: &BTPAddress, link: &BTPAddress) -> Result<bool, String>;
    fn remove_route(&mut self, dst: &BTPAddress) -> Result<bool, String>;
    fn get_routes(&self) -> Result<(String, String), String>;
}

impl Routes {
    fn resolve_route(&mut self, dst_net: &BTPAddress) -> Result<(String, String), String> {
        Ok(("hello".to_string(), "helo".to_string()))
    }
}

impl Route for Routes {
    fn new() -> Self {
        let routes: UnorderedMap<String, String> = UnorderedMap::new(b"routes".to_vec());
        Self(routes)
    }

    fn add_route(&mut self, dst: &BTPAddress, link: &BTPAddress) -> Result<bool, String> {
        match dst.is_valid() {
            Ok(true) => match link.is_valid() {
                Ok(true) => match self.0.get(&dst.0) {
                    Some(value) => {
                        return Err(format!("value already present {}", value));
                    }

                    None => {
                        self.0.insert(&dst.0, &link.0);
                        return Ok(true);
                    }
                },
                Ok(false) => return Ok(false),
                Err(error) => return Err(error),
            },
            Ok(false) => return Ok(false),

            Err(error) => return Err(error),
        }
    }

    fn remove_route(&mut self, dst: &BTPAddress) -> Result<bool, String> {
        //TODO :
        //validate caller has necessary permission

        match dst.is_valid() {
            Ok(true) => {
                if !self.0.is_empty() {
                    match self.0.get(&dst.0) {
                        Some(value) => {
                            self.0.remove(&dst.0);

                            return Ok(true);
                        }

                        None => {
                            return Err(format!("value already deleted"));
                        }
                    }
                }

                return Ok(false);
            }
            Ok(false) => return Ok(false),
            Err(error) => return Err(error),
        }
    }
    fn get_routes(&self) -> Result<(String, String), String> {
        //needs to be changed

        //TO-DO

        //Add testcases for route and links

        if !self.0.is_empty() {
            for (key, value) in self.0.iter() {
                return Ok((key, value));
            }
        }

        return Err("value not found".to_string());
    }
}
