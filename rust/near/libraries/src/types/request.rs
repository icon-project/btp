use near_sdk::borsh::{self, BorshDeserialize, BorshSerialize};
use near_sdk::collections::UnorderedMap;

use super::Asset;

#[derive(Debug, Eq, PartialEq, BorshDeserialize, BorshSerialize)]
pub struct Request {
    sender: String,
    receiver: String,
    assets: Vec<Asset>,
}

impl Request {
    pub fn new(sender: String, receiver: String, assets: Vec<Asset>) -> Self {
        Self {
            sender,
            receiver,
            assets,
        }
    }

    pub fn sender(&self) -> &String {
        &self.sender
    }

    pub fn receiver(&self) -> &String {
        &self.receiver
    }

    pub fn assets(&self) -> &Vec<Asset> {
        &self.assets
    }
}

#[derive(BorshDeserialize, BorshSerialize)]
pub struct Requests(UnorderedMap<i128, Request>);

impl Requests {
    pub fn new() -> Self {
        Self(UnorderedMap::new(b"requests".to_vec()))
    }

    pub fn add(&mut self, serial_no: i128, request: &Request) {
        self.0.insert(&serial_no, request);
    }

    pub fn remove(&mut self, serial_no: i128) {
        self.0.remove(&serial_no);
    }

    pub fn get(&self, serial_no: i128) -> Option<Request> {
        if let Some(request) = self.0.get(&serial_no) {
            return Some(request);
        }
        None
    }

    pub fn contains(&self, serial_no: i128) -> bool {
        return self.0.get(&serial_no).is_some();
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::types::{
        Asset,
    };
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
    fn add_request() {
        let context = get_context(vec![], false);
        testing_env!(context);
        let mut requests = Requests::new();
        let request = Request::new(
            "88bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4".to_string(),
            "78bd0675686be0a5df7da33b6f1089eghea3769b19dbb2477fe0cd6e0f12667".to_string(),
            vec![Asset::new("ABC".to_string(), 100, 1)],
        );
        requests.add(1, &request);
        let result = requests.get(1).unwrap();
        assert_eq!(result, request);
    }

    #[test]
    fn add_request_existing() {
        let context = get_context(vec![], false);
        testing_env!(context);
        let mut requests = Requests::new();
        let request = Request::new(
            "88bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4".to_string(),
            "78bd0675686be0a5df7da33b6f1089eghea3769b19dbb2477fe0cd6e0f12667".to_string(),
            vec![Asset::new("ABC".to_string(), 100, 1)],
        );
        requests.add(1, &request);
        requests.add(1, &request);
        let result = requests.get(1).unwrap();
        assert_eq!(result, request);
    }

    #[test]
    fn remove_request() {
        let context = get_context(vec![], false);
        testing_env!(context);
        let mut requests = Requests::new();
        let request = Request::new(
            "88bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4".to_string(),
            "78bd0675686be0a5df7da33b6f1089eghea3769b19dbb2477fe0cd6e0f12667".to_string(),
            vec![Asset::new("ABC".to_string(), 100, 1)],
        );
        requests.add(1, &request);
        requests.remove(1);
        let result = requests.get(1);
        assert_eq!(result, None);
    }

    #[test]
    fn remove_request_non_existing() {
        let context = get_context(vec![], false);
        testing_env!(context);
        let mut requests = Requests::new();
        requests.remove(1);
        let result = requests.get(1);
        assert_eq!(result, None);
    }
}
