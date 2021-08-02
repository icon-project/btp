use near_sdk::borsh::{self, BorshDeserialize, BorshSerialize};
use near_sdk::serde::Serialize;
use std::{collections::HashMap, hash::Hash};
use std::collections::HashSet;

#[derive(Serialize, Debug, Eq, PartialEq, Hash)]
pub struct Service {
    name: String,
    address: String,
}

#[derive(BorshDeserialize, BorshSerialize)]
pub struct BSH {
    pub services: Services,
    pub requests: Requests,
}

impl BSH {
    pub fn new() -> Self {
        Self {
            services: Services::new(),
            requests: Requests::new(),
        }
    }
}

#[derive(BorshDeserialize, BorshSerialize)]
pub struct Services(HashMap<String, String>);

#[derive(BorshDeserialize, BorshSerialize)]
pub struct Requests(HashMap<String, String>);

impl Requests {
    pub fn new() -> Self {
        Self(HashMap::new())
    }

    pub fn add(&mut self, name: &str, address: &str) {
        self.0.insert(name.to_string(), address.to_string());
    }

    pub fn remove(&mut self, name: &str) {
        if self.0.contains_key(name) {
            self.0.remove(name);
        }
    }

    pub fn contains(&self, name: &str) -> bool {
        return self.0.contains_key(name);
    }

    pub fn get(&self, name: &str) -> Option<String> {
        if let Some(service) = self.0.get(name) {
            return Some(service.to_string());
        }
        None
    }

    pub fn to_vec(&self) -> Vec<Service> {
        if !self.0.is_empty() {
            return self
                .0
                .clone()
                .into_iter()
                .map(|v| Service {
                    name: v.0,
                    address: v.1,
                })
                .collect();
        }
        vec![]
    }
}

impl Services {
    pub fn new() -> Self {
        let services = HashMap::new();
        Self(services)
    }

    pub fn add(&mut self, name: &str, address: &str) {
        self.0.insert(name.to_string(), address.to_string());
    }

    pub fn remove(&mut self, name: &str) {
        if self.0.contains_key(name) {
            self.0.remove(name);
        }
    }

    pub fn contains(&self, name: &str) -> bool {
        return self.0.contains_key(name);
    }

    pub fn get(&self, name: &str) -> Option<String> {
        if let Some(service) = self.0.get(name) {
            return Some(service.to_string());
        }
        None
    }

    pub fn to_vec(&self) -> Vec<Service> {
        if !self.0.is_empty() {
            return self
                .0
                .clone()
                .into_iter()
                .map(|v| Service {
                    name: v.0,
                    address: v.1,
                })
                .collect();
        }
        vec![]
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use near_sdk::MockedBlockchain;
    use near_sdk::{testing_env, VMContext};
    use std::collections::HashSet;

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
    fn add_pending_request() {
        let context = get_context(vec![], false);
        testing_env!(context);
        let name = String::from("service_1");
        let address =
            String::from("88bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4");
        let mut bsh = BSH::new();
        bsh.requests.add(&name, &address);
        let result = bsh.requests.get(&name);
        assert_eq!(
            result,
            Some("88bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4".to_string())
        );
    }

    #[test]
    fn add_service() {
        let context = get_context(vec![], false);
        testing_env!(context);
        let name = String::from("service_1");
        let address =
            String::from("88bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4");
        let mut bsh = BSH::new();
        bsh.services.add(&name, &address);
        let result = bsh.services.get(&name);
        assert_eq!(
            result,
            Some("88bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4".to_string())
        );
    }

    #[test]
    fn get_pending_request() {
        let context = get_context(vec![], false);
        testing_env!(context);
        let name_1 = String::from("service_1");
        let address_1 =
            String::from("88bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4");
        let name_2 = String::from("service_2");
        let address_2 =
            String::from("68bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4");
        let name_3 = String::from("service_3");
        let address_3 =
            String::from("78bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4");
        let mut bsh = BSH::new();
        bsh.requests.add(&name_1, &address_1);
        bsh.requests.add(&name_2, &address_2);
        bsh.requests.add(&name_3, &address_3);
        let result = bsh.requests.get(&name_2);
        assert_eq!(
            result,
            Some("68bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4".to_string())
        );
    }

    #[test]
    fn get_service() {
        let context = get_context(vec![], false);
        testing_env!(context);
        let name_1 = String::from("service_1");
        let address_1 =
            String::from("88bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4");
        let name_2 = String::from("service_2");
        let address_2 =
            String::from("68bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4");
        let name_3 = String::from("service_3");
        let address_3 =
            String::from("78bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4");
        let mut bsh = BSH::new();
        bsh.services.add(&name_1, &address_1);
        bsh.services.add(&name_2, &address_2);
        bsh.services.add(&name_3, &address_3);
        let result = bsh.services.get(&name_2);
        assert_eq!(
            result,
            Some("68bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4".to_string())
        );
    }

    #[test]
    fn remove_pending_request() {
        let context = get_context(vec![], false);
        testing_env!(context);
        let name_1 = String::from("service_1");
        let address_1 =
            String::from("88bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4");
        let name_2 = String::from("service_2");
        let address_2 =
            String::from("68bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4");
        let name_3 = String::from("service_3");
        let address_3 =
            String::from("78bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4");
        let mut bsh = BSH::new();
        bsh.requests.add(&name_1, &address_1);
        bsh.requests.add(&name_2, &address_2);
        bsh.requests.add(&name_3, &address_3);
        bsh.requests.remove(&name_2);
        let result = bsh.requests.get(&name_2);
        assert_eq!(result, None);
    }

    #[test]
    fn remove_service() {
        let context = get_context(vec![], false);
        testing_env!(context);
        let name_1 = String::from("service_1");
        let address_1 =
            String::from("88bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4");
        let name_2 = String::from("service_2");
        let address_2 =
            String::from("68bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4");
        let name_3 = String::from("service_3");
        let address_3 =
            String::from("78bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4");
        let mut bsh = BSH::new();
        bsh.services.add(&name_1, &address_1);
        bsh.services.add(&name_2, &address_2);
        bsh.services.add(&name_3, &address_3);
        bsh.services.remove(&name_2);
        let result = bsh.services.get(&name_2);
        assert_eq!(result, None);
    }

    #[test]
    fn contains_pending_request() {
        let context = get_context(vec![], false);
        testing_env!(context);
        let name = String::from("service_1");
        let address =
            String::from("88bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4");
        let mut bsh = BSH::new();
        bsh.requests.add(&name, &address);
        let result = bsh.requests.contains(&name);
        assert_eq!(result, true);
    }

    #[test]
    fn contains_service() {
        let context = get_context(vec![], false);
        testing_env!(context);
        let name = String::from("service_1");
        let address =
            String::from("88bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4");
        let mut bsh = BSH::new();
        bsh.services.add(&name, &address);
        let result = bsh.services.contains(&name);
        assert_eq!(result, true);
    }

    #[test]
    fn to_vec_pending_request() {
        let context = get_context(vec![], false);
        testing_env!(context);
        let name_1 = String::from("service_1");
        let address_1 =
            String::from("88bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4");
        let name_2 = String::from("service_2");
        let address_2 =
            String::from("68bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4");
        let name_3 = String::from("service_3");
        let address_3 =
            String::from("78bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4");
        let mut bsh = BSH::new();
        bsh.requests.add(&name_1, &address_1);
        bsh.requests.add(&name_2, &address_2);
        bsh.requests.add(&name_3, &address_3);
        let requests = bsh.requests.to_vec();
        let expected_requests = vec![
            Service {
                name: "service_1".to_string(),
                address: "88bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4"
                    .to_string(),
            },
            Service {
                name: "service_2".to_string(),
                address: "68bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4"
                    .to_string(),
            },
            Service {
                name: "service_3".to_string(),
                address: "78bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4"
                    .to_string(),
            },
        ];
        let result: HashSet<_> = requests.iter().collect();
        let expected: HashSet<_> = expected_requests.iter().collect();
        assert_eq!(result, expected);
    }

    #[test]
    fn to_vec_service() {
        let context = get_context(vec![], false);
        testing_env!(context);
        let name_1 = String::from("service_1");
        let address_1 =
            String::from("88bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4");
        let name_2 = String::from("service_2");
        let address_2 =
            String::from("68bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4");
        let name_3 = String::from("service_3");
        let address_3 =
            String::from("78bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4");
        let mut bsh = BSH::new();
        bsh.services.add(&name_1, &address_1);
        bsh.services.add(&name_2, &address_2);
        bsh.services.add(&name_3, &address_3);
        let services = bsh.services.to_vec();
        let expected_services = vec![
            Service {
                name: "service_1".to_string(),
                address: "88bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4"
                    .to_string(),
            },
            Service {
                name: "service_2".to_string(),
                address: "68bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4"
                    .to_string(),
            },
            Service {
                name: "service_3".to_string(),
                address: "78bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4"
                    .to_string(),
            },
        ];
        let result: HashSet<_> = services.iter().collect();
        let expected: HashSet<_> = expected_services.iter().collect();

        assert_eq!(result, expected);
    }
}
