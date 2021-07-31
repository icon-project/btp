//! This crate is a Rust port of Google's high-performance hash
//! map, adapted to make it a drop-in replacement for Rust's standard `HashMap`
//! and `HashSet` types.

use near_sdk::borsh::{self, BorshDeserialize, BorshSerialize};
use near_sdk::collections::{UnorderedMap, UnorderedSet};
use near_sdk::AccountId;
use near_sdk::{env, log, near_bindgen, setup_alloc, PanicOnDefault, serde_json};
use std::collections::HashMap;
use serde_json::{json, Value, to_value, from_str};
mod service;
mod owner;
mod types;

use btp_common::{BTPAddress, BMCError, owner};
use types::{Owners, BSH, Links, Routes};

setup_alloc!();

#[near_bindgen]
#[derive(BorshDeserialize, BorshSerialize)]
pub struct BTPMessageCenter {
    pub message: Message,
    pub links: Links,
    pub routes: Routes,
    pub bsh: BSH,
    pub owners: Owners,
}

#[derive(Default, BorshDeserialize, BorshSerialize)]
pub struct Message {
    payload: Vec<u8>,
}

impl Default for BTPMessageCenter {
    fn default() -> Self {
        let message: Message = Default::default();
        let links = Links::new();
        let routes = Routes::new();
        let bsh = BSH::new();
        let mut owners = Owners::new();
        owners.add(&env::current_account_id());
        Self {
            message,
            links,
            routes,
            bsh,
            owners,
        }
    }
}

#[near_bindgen]
impl BTPMessageCenter {
    // Link Management
    pub fn get_status(&self) {}

    pub fn add_link(&self) {}
    pub fn remove_link(&self) {}
    pub fn set_link(&self) {}
    pub fn get_links(&self) {}

    // Route Management
    pub fn add_route(&self) {}
    pub fn remove_route(&self) {}
    pub fn get_routes(&self) {}

    // Verifier Management
    pub fn add_verifier() {}
    pub fn remove_verifier() {}
    pub fn get_verifiers() {}

    // Messaging
    pub fn handle_relay_message() {}
    pub fn send_message() {}

    // Relay Management
    pub fn add_relay() {}
    pub fn remove_relay() {}
    pub fn get_relays() {}
    // pub fn get_message(&self) -> String {
    //     return String::from_utf8(self.message.payload.clone()).unwrap_or_default();
    // }

    // pub fn send_message(&mut self, message: String) {
    //     self.message.payload = message.as_bytes().to_vec()
    // }

    // pub fn add_link(&mut self, link: &BTPAddress) {
    //     assert!(
    //         self.owners.is_owner(env::signer_account_id()),
    //         "BMCRevertUnauthorized"
    //     );

    //     self.links
    //         .add_link(link)
    //         .expect_err("BMCRevertUnauthorized");
    // }
    // pub fn add_routes(&mut self, dst: &BTPAddress, link: &BTPAddress) {
    //     self.routes
    //         .add_route(dst, link)
    //         .expect_err("Not able to add route");
    // }

    // pub fn remove_route(&mut self, dst: &BTPAddress) {
    //     assert!(
    //         self.owners.is_owner(env::signer_account_id()),
    //         "BMCRevertUnauthorized"
    //     );
    //     self.routes.remove_route(dst).expect_err("Failed to remove");
    // }

    // pub fn add_owner(&mut self, address: &AccountId) {
    //     assert!(
    //         self.owners.is_owner(env::signer_account_id()),
    //         "BMCRevertUnauthorized"
    //     );

    //     self.owners
    //         .add_owner(address)
    //         .expect_err("failed to add owner");
    // }

    // pub fn add_relays(&mut self, link: &BTPAddress, address: &Vec<AccountId>) -> bool {
    //     self.links
    //         .set(&link, None, None, None, Some(address.to_vec()))
    //         .is_ok()
    // }

    // pub fn add_relay(&mut self, link: &BTPAddress, address: &AccountId) -> bool {
    //     match self.links.get(&link) {
    //         Ok(mut link_property) => {
    //             let _ = link_property.relays.add(address.to_string()).is_ok();
    //             return self
    //                 .links
    //                 .set(&link, None, None, None, Some(link_property.relays.to_vec()))
    //                 .is_ok();
    //         }
    //         Err(error) => {
    //             log!(error);
    //         }
    //     }
    //     false
    // }

    // pub fn remove_relay(&self,link:&BTPAddress, address: &Vec<AccountId>) -> bool {

    // }

    // pub fn get_relays(&self, link: &BTPAddress) -> Vec<String> {
    //     match self.links.get(&link) {
    //         Ok(link_property) => {
    //             return link_property.relays.to_vec();
    //         }

    //         Err(err) => {
    //             vec![]
    //         }
    //     }

    // let link = self.links.0.get(key: &K);

    // if !link.relays.0.is_empty(){
    //    for v in link.relays.0.iter(){

    //    return v;
    //    }
    // }
    // }

    // pub fn approve_service(&mut self, service: String, is_accepted: bool) -> bool {
    //     match self.bsh.requests.get(&service) {
    //         Ok(address) => {
    //             if is_accepted {
    //                 match self.bsh.services.add(&service, &address) {
    //                     Ok(true) => {
    //                         return self.bsh.requests.remove(&service).is_ok();
    //                     }
    //                     _ => {
    //                         return self.bsh.requests.remove(&service).is_ok();
    //                     }
    //                 };
    //             }

    //             return false;
    //         }
    //         Err(error) => {

    //             log!(error);
    //             return false;
    //         }
    //     }

    // for i in 0..pendingrq.len() {
    //     if pendingrq[i] == service.clone() {
    //         if is_accepted {
    //             match self.bsh.services.add(service.clone(), pendingrq[i].clone()) {
    //                 Ok(true) => println!("service Added"),
    //                 Ok(false) => println!("service not added"),
    //                 Err(err) => println!("{}", err),
    //             }
    //         }

    //         self.bsh.requests.remove(pendingrq[i].clone());
    //     }

    //     log!("BMCRevertNotExistRequest");
    // }
    // }

    // pub fn remove_service(&mut self, service: String) {
    // }

    // pub fn get_services(&self) {
    //     // match self.bsh.services.get() {
    //     //     Ok(value) => println!("{:?}", value),
    //     //     Err(err) => println!("{}", err),
    //     // }
    // }
}