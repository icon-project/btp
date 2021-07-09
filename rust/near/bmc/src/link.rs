use btp_common::BTPAddress;
use near_sdk::borsh::{self, BorshDeserialize, BorshSerialize};
use near_sdk::collections::LookupMap;
use std::sync::Mutex;

use std::collections::HashMap;

lazy_static! {
    static ref KEYS: Mutex<Vec<Vec<u8>>> = Mutex::new(Vec::new());
}

#[derive(BorshDeserialize, BorshSerialize)]
pub struct Links(pub LookupMap<Vec<u8>, LinkProps>);

#[derive(Default, BorshDeserialize, BorshSerialize)]
pub struct LinkProps {
    rx_seq: u64,
    tx_seq: u64,
    verifier: Verifier,
    relays: HashMap<Vec<u8>, Relay>,
    reachable: Vec<u8>,
    relay_index: u64,
    rotate_height: u64,
    rotate_term: u64,
    delay_limit: u64,
    max_aggregation: u64,
    rx_height_src: u64,
    rx_height: u64,
    block_interval_src: u64,
    block_interval_dst: u64,
    current_height: u64,
    connected: bool,
}

#[derive(Default, BorshDeserialize, BorshSerialize)]
pub struct Relay {
    address: Vec<u8>,
    block_count: u64,
    message_count: u64,
}

#[derive(Default, BorshDeserialize, BorshSerialize)]
pub struct Verifier {
    mta_height: u64,
    mta_offset: u64,
    last_height: u64,
}

pub trait Link {
    fn new() -> Self;
    fn add_link(&mut self, link: &BTPAddress) -> Result<bool, String>;
    fn remove_link(&mut self, link: &BTPAddress) -> Result<bool, String>;
    fn set_link(
        &mut self,
        link: &BTPAddress,
        block_interval: u64,
        mxagg: u64,
        delimit: u64,
    ) -> Result<bool, String>;
    fn get_links(&self) -> Result<Vec<u8>, String>;
    fn get_status(&self, link: &BTPAddress) -> Result<LinkProps, String>;
    fn init_linkprops(&self) -> LinkProps;
    fn set_link_status(
        &mut self,
        link: &BTPAddress,
        block_interval: u64,
        mxagg: u64,
        delimit: u64,
    ) -> Result<bool, String>;
}

impl Link for Links {
    fn new() -> Self {
        let links: LookupMap<Vec<u8>, LinkProps> = LookupMap::new(b"links".to_vec());

        Self(links)
    }

    fn init_linkprops(&self) -> LinkProps {
        LinkProps {
            rx_seq: 0,
            tx_seq: 0,
            verifier: Verifier {
                mta_height: 0,
                mta_offset: 0,
                last_height: 0,
            },
            relays: HashMap::new(),
            reachable: b"".to_vec(),
            block_interval_src: 0,
            block_interval_dst: 0,
            max_aggregation: 0,
            delay_limit: 0,
            relay_index: 0,
            rotate_height: 0,
            rotate_term: 0,
            rx_height_src: 0,
            rx_height: 0,
            current_height: 0,
            connected: true,
        }
    }

    fn add_link(&mut self, link: &BTPAddress) -> Result<bool, String> {
        let linkprop = self.init_linkprops();
        let key = link.0.clone().into_bytes();

        //TO-DO
        //validate caller has necessary permission
        // check if verifiers are already added

        match link.is_valid() {
            Ok(true) => {
                if !self.0.contains_key(&key) {
                    self.0.insert(&key, &linkprop);

                    KEYS.lock().unwrap().push(key);

                    return Ok(true);
                }
                return Ok(false);
            }
            Ok(false) => return Ok(false),
            Err(res) => return Err(res),
        }
    }

    fn remove_link(&mut self, link: &BTPAddress) -> Result<bool, String> {
        //TO-DO
        //validate caller has necessary permission
        match link.is_valid() {
            Ok(true) => {
                if self.0.contains_key(&link.0.clone().into_bytes()) {
                    self.0.remove(&link.0.clone().into_bytes());

                    return Ok(true);
                }

                return Ok(false);
            }
            Ok(false) => return Ok(false),
            Err(error) => return Err(format!("Unable to remove link {}", error)),
        }
    }
    fn get_links(&self) -> Result<Vec<u8>, String> {
        for key in KEYS.lock().unwrap().iter() {
            return Ok(key.to_vec());
        }

        return Err("links are empty".to_string());
    }

    
    fn get_status(&self, link: &BTPAddress) -> Result<LinkProps, String> {
        if self.0.contains_key(&link.0.clone().into_bytes()) {
            let linkprop = self.0.get(&link.0.clone().into_bytes());

            return Ok(linkprop.unwrap());
        }

        return Err("Not found".to_string());
    }
    fn set_link_status(
        &mut self,
        link: &BTPAddress,
        block_interval: u64,
        mxagg: u64,
        delimit: u64,
    ) -> Result<bool, String> {
        match link.is_valid() {
            Ok(true) => {
                //TO-DO : check owner permission
                //Verify if the caller is owner
                //Check if either max_aggregation < 1 or delay_limit
                //Add link status information to the link based on rotate term

                if self.0.contains_key(&link.0.clone().into_bytes()) {
                    let linkprop = LinkProps {
                        block_interval_src: block_interval,
                        max_aggregation: mxagg,
                        delay_limit: delimit,
                        ..self.init_linkprops()
                    };

                    self.0.insert(&link.0.clone().into_bytes(), &linkprop);

                    return Ok(true);
                }
                return Ok(false);
            }
            Ok(false) => return Ok(false),
            Err(error) => Err(format!("unable to set the link {}", error)),
        }
    }
}
