use near_sdk::borsh::{self, BorshDeserialize, BorshSerialize};
use near_sdk::serde::{Serialize, Deserialize};
use near_sdk::AccountId;
use std::{collections::HashMap, hash::Hash};

#[derive(Serialize, Debug, Eq, PartialEq, Hash, Deserialize)]
pub struct Service {
    name: String,
    service: AccountId,
}

#[derive(BorshDeserialize, BorshSerialize)]
pub struct Services(HashMap<String, AccountId>);

impl Services {
    pub fn new() -> Self {
        let services = HashMap::new();
        Self(services)
    }

    pub fn add(&mut self, name: &str, service: &AccountId) {
        self.0.insert(name.to_string(), service.to_owned());
    }

    pub fn remove(&mut self, name: &str) {
        if self.0.contains_key(name) {
            self.0.remove(name);
        }
    }

    pub fn contains(&self, name: &str) -> bool {
        return self.0.contains_key(name);
    }

    pub fn get(&self, name: &str) -> Option<&AccountId> {
        if let Some(service) = self.0.get(name) {
            return Some(service);
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
                    service: v.1,
                })
                .collect();
        }
        vec![]
    }
}

#[cfg(test)]
mod tests {
    use super::*;
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
    fn add_service() {
        let context = get_context(vec![], false);
        testing_env!(context);
        let name = String::from("service_1");
        let service = "88bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4"
            .parse::<AccountId>()
            .unwrap();
        let mut services = Services::new();
        services.add(&name, &service);
        let result = services.get(&name);
        assert_eq!(
            result,
            Some(
                &"88bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4"
                    .parse::<AccountId>()
                    .unwrap()
            )
        );
    }

    #[test]
    fn get_service() {
        let context = get_context(vec![], false);
        testing_env!(context);
        let name_1 = String::from("service_1");
        let service_1 = "88bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4"
            .parse::<AccountId>()
            .unwrap();
        let name_2 = String::from("service_2");
        let service_2 = "68bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4"
            .parse::<AccountId>()
            .unwrap();
        let name_3 = String::from("service_3");
        let service_3 = "78bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4"
            .parse::<AccountId>()
            .unwrap();
        let mut services = Services::new();
        services.add(&name_1, &service_1);
        services.add(&name_2, &service_2);
        services.add(&name_3, &service_3);
        let result = services.get(&name_2);
        assert_eq!(
            result,
            Some(
                &"68bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4"
                    .parse::<AccountId>()
                    .unwrap()
            )
        );
    }

    #[test]
    fn remove_service() {
        let context = get_context(vec![], false);
        testing_env!(context);
        let name_1 = String::from("service_1");
        let service_1 = "88bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4"
            .parse::<AccountId>()
            .unwrap();
        let name_2 = String::from("service_2");
        let service_2 = "68bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4"
            .parse::<AccountId>()
            .unwrap();
        let name_3 = String::from("service_3");
        let service_3 = "78bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4"
            .parse::<AccountId>()
            .unwrap();
        let mut services = Services::new();
        services.add(&name_1, &service_1);
        services.add(&name_2, &service_2);
        services.add(&name_3, &service_3);
        services.remove(&name_2);
        let result = services.get(&name_2);
        assert_eq!(result, None);
    }

    #[test]
    fn contains_service() {
        let context = get_context(vec![], false);
        testing_env!(context);
        let name = String::from("service_1");
        let service = "88bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4"
            .parse::<AccountId>()
            .unwrap();
        let mut services = Services::new();
        services.add(&name, &service);
        let result = services.contains(&name);
        assert_eq!(result, true);
    }

    #[test]
    fn to_vec_service() {
        let context = get_context(vec![], false);
        testing_env!(context);
        let name_1 = String::from("service_1");
        let service_1 = "88bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4"
            .parse::<AccountId>()
            .unwrap();
        let name_2 = String::from("service_2");
        let service_2 = "68bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4"
            .parse::<AccountId>()
            .unwrap();
        let name_3 = String::from("service_3");
        let service_3 = "78bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4"
            .parse::<AccountId>()
            .unwrap();
        let mut services = Services::new();
        services.add(&name_1, &service_1);
        services.add(&name_2, &service_2);
        services.add(&name_3, &service_3);
        let services = services.to_vec();
        let expected_services = vec![
            Service {
                name: "service_1".to_string(),
                service: "88bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4"
                    .parse::<AccountId>()
                    .unwrap(),
            },
            Service {
                name: "service_2".to_string(),
                service: "68bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4"
                    .parse::<AccountId>()
                    .unwrap(),
            },
            Service {
                name: "service_3".to_string(),
                service: "78bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4"
                    .parse::<AccountId>()
                    .unwrap(),
            },
        ];
        let result: HashSet<_> = services.iter().collect();
        let expected: HashSet<_> = expected_services.iter().collect();

        assert_eq!(result, expected);
    }
}
