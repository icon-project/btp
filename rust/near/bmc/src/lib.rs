use near_sdk::borsh::{self, BorshDeserialize, BorshSerialize};
use near_sdk::{env, near_bindgen};
use std::collections::HashMap;

#[global_allocator]
static ALLOC: near_sdk::wee_alloc::WeeAlloc = near_sdk::wee_alloc::WeeAlloc::INIT;

#[near_bindgen]
#[derive(BorshDeserialize, BorshSerialize)]
pub struct BTPMessageCenter {
    message: Message,
    links: HashMap<Vec<u8>, Link>,
}

#[derive(BorshDeserialize, BorshSerialize)]
pub struct Link {
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

#[derive(Default, BorshDeserialize, BorshSerialize)]
pub struct LinkStatus {
    rx_seq: u64,
    tx_seq: u64,
    verifier: Verifier,
    relays: HashMap<Vec<u8>, Relay>,
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
}

#[derive(Default, BorshDeserialize, BorshSerialize)]
pub struct Verifier {
    mta_height: u64,
    mta_offset: u64,
    last_height: u64,
}

#[derive(Default, BorshDeserialize, BorshSerialize)]
pub struct Relay {
    address: Vec<u8>,
    block_count: u64,
    message_count: u64,
}
#[derive(Default, BorshDeserialize, BorshSerialize)]
pub struct Message {
    payload: Vec<u8>,
}

#[near_bindgen]
impl Default for BTPMessageCenter {
    fn default() -> Self {
        let message: Message = Default::default();
        let links: HashMap<Vec<u8>, Link> = Default::default();
        Self {
            message,
            links
        }
    }
}

#[near_bindgen]
impl BTPMessageCenter {
    pub fn get_message(&self) -> String {
        return String::from_utf8(self.message.payload.clone()).unwrap_or_default();
    }

    pub fn send_message(&mut self, message: String) {
        self.message.payload = message.as_bytes().to_vec()
    }
}
#[cfg(test)]
mod tests {
    use super::*;
    use near_sdk::MockedBlockchain;
    use near_sdk::{testing_env, VMContext};

    fn get_context(input: Vec<u8>, is_view: bool) -> VMContext {
        VMContext {
            current_account_id: "alice.testnet".to_string(),
            signer_account_id: "robert.testnet".to_string(),
            signer_account_pk: vec![0, 1, 2],
            predecessor_account_id: "jane.testnet".to_string(),
            input,
            block_index: 0,
            block_timestamp: 0,
            account_balance: 0,
            account_locked_balance: 0,
            storage_usage: 0,
            attached_deposit: 0,
            prepaid_gas: 10u64.pow(18),
            random_seed: vec![0, 1, 2],
            is_view,
            output_data_receivers: vec![],
            epoch_height: 19,
        }
    }

    #[test]
    fn send_message() {
        let context = get_context(vec![], false);
        testing_env!(context);
        let mut contract = BTPMessageCenter { ..Default::default() };
        let mut s = String::from("");
        contract.send_message("dddddd".to_string());
        assert_eq!("dddddd".to_string(), contract.get_message());
    }
}
