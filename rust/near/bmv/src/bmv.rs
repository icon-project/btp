//! BMV

use near_sdk::borsh::{self, BorshDeserialize, BorshSerialize};
use near_sdk::{near_bindgen, setup_alloc};

setup_alloc!();
/// This struct implements `Default`: https://github.com/near/near-sdk-rs#writing-rust-contract
#[near_bindgen]
#[derive(BorshDeserialize, BorshSerialize, Clone)]
pub struct Bmv {
    todo: String,
}

impl Default for Bmv {
    fn default() -> Self {
        todo!()
    }
}

#[near_bindgen]
impl Bmv {
    #[init]
    pub fn new(addr: &str) -> Self {
        Self {
            todo: addr.to_string(),
        }
    }

    pub fn get_mta(&self) -> String {
        todo!()
    }

    pub fn get_connected_bmc(&self) -> String {
        todo!()
    }

    pub fn get_net_address(&self) -> String {
        todo!()
    }

    pub fn get_validators(&self) -> (Vec<u32>, Vec<String>) {
        todo!()
    }

    /// Used by the relay to resolve next BTP Message to send. Called by BMC
    pub fn get_status(&self) -> (u128, u128, u128) {
        todo!()
    }

    /// Decode Relay Messages and process BTP Messages.
    /// If there is an error, then it sends a BTP Message containing the Error Message.
    /// BTP Messages with old sequence numbers are ignored. A BTP Message containing future sequence number will fail.
    pub fn handle_relay_message(
        &mut self,
        _bmc: &str,
        _prev: &str,
        _seq: u128,
        _msg: &str,
    ) -> Vec<Vec<u8>> {
        todo!()
    }
}
