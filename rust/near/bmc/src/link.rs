/* 
use btp_common::{Address, BTPAddress};
use near_sdk::borsh::{self, BorshDeserialize, BorshSerialize};
use near_sdk::collections::LookupMap;
use near_sdk::{env, near_bindgen};
use std::sync::Mutex;

use std::collections::HashMap;

lazy_static! {
    static ref keytovalue: Mutex<Vec<Vec<u8>>> = Mutex::new(Vec::new());
}

#[derive(BorshDeserialize, BorshSerialize)]
pub struct Links(pub LookupMap<Vec<u8>, LinkProps>);

#[derive(BorshDeserialize, BorshSerialize)]
pub struct LinkProps {
    rx_seq: u64,
    tx_seq: u64,
    relays: Vec<u8>,
    reachable: Vec<u8>,
    block_interval_src: u128,
    block_interval_dst: u128,
    max_aggregation: u128,
    delay_limit: u128,
    relay_index: u64,
    rotate_height: u64,
    rotate_term: u64,
    rx_height_src: u64,
    connected: bool,
}

pub trait Link {
    fn new() -> Self;
    fn add_link(&mut self, link_add: &BTPAddress) -> bool;
    fn remove_link(&mut self, link_add: &BTPAddress) -> bool;
    fn set_link(
        &mut self,
        link_add: &BTPAddress,
        block_interval: u128,
        mxagg: u128,
        delimit: u128,
    ) -> bool;
    fn get_links(&self) -> Result<Vec<u8>, String>;
    fn get_status(&self, link_add: &BTPAddress) -> Result<LinkProps, String>;
    fn init_linkprops(&self) -> LinkProps;
}

impl Link for Links {
    fn new() -> Self {
        let links: LookupMap<Vec<u8>, LinkProps> = LookupMap::new(b"links".to_vec());

        Self(links)
    }

    fn init_linkprops(&self) -> LinkProps {
        let rx_seq = 0;
        let tx_seq = 0;
        let relays = b"".to_vec();
        let reachable = b"".to_vec();
        let block_interval_src = 0;

        let block_interval_dst = 0;
        let max_aggregation = 0;
        let delay_limit = 0;
        let relay_index = 0;
        let rotate_height = 0;
        let rotate_term = 0;
        let rx_height_src = 0;
        let connected = true;

        LinkProps {
            rx_seq,
            tx_seq,
            relays,
            reachable,
            block_interval_src,
            block_interval_dst,
            max_aggregation,
            delay_limit,
            relay_index,
            rotate_height,
            rotate_term,
            rx_height_src,
            connected,
        }
    }

    fn add_link(&mut self, link_add: &BTPAddress) -> bool {
        let linkprop = self.init_linkprops();
        let key = link_add.0.clone().into_bytes();

        //TO-DO
        //validate caller has necessary permission
        // check if verifiers are already added

        if link_add.is_valid().ok().unwrap() {
            if !self.0.contains_key(&key) {
                self.0.insert(&key, &linkprop);

                keytovalue.lock().unwrap().push(key);

                return true;
            }
        }

        return false;
    }

    fn remove_link(&mut self, link_add: &BTPAddress) -> bool {
        //TO-DO
        //validate caller has necessary permission

        if link_add.is_valid().ok().unwrap() {
            if self.0.contains_key(&link_add.0.clone().into_bytes()) {
                self.0.remove(&link_add.0.clone().into_bytes());

                return true;
            }
        }

        return false;
    }
    fn get_links(&self) -> Result<Vec<u8>, String> {
        for key in keytovalue.lock().unwrap().iter() {
            return Ok(key.to_vec());
        }

        return Err("links are empty".to_string());
    }

    fn set_link(
        &mut self,
        link_add: &BTPAddress,
        block_interval: u128,
        mxagg: u128,
        delimit: u128,
    ) -> bool {
        //TO-DO
        //validate caller has necessary permission
        //Check if either max_aggregation < 1 or delay_limit
        //Add link status information to the link based on rotate term

        if link_add.is_valid().ok().unwrap() {
            if !self.0.contains_key(&link_add.0.clone().into_bytes()) {
                let linkprop = LinkProps {
                    block_interval_src: block_interval,
                    max_aggregation: mxagg,
                    delay_limit: delimit,
                    ..self.init_linkprops()
                };

                self.0.insert(&link_add.0.clone().into_bytes(), &linkprop);

                return true;
            }
        }

        return false;
    }

    fn get_status(&self, link_add: &BTPAddress) -> Result<LinkProps, String> {
        if self.0.contains_key(&link_add.0.clone().into_bytes()) {
            let linkprop = self.0.get(&link_add.0.clone().into_bytes());

            return Ok(linkprop.unwrap());
        }

        return Err("Not found".to_string());
    }
}
*/