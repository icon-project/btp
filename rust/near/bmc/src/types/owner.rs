use near_sdk::borsh::{self, BorshDeserialize, BorshSerialize};
use near_sdk::AccountId;
use std::collections::HashMap;

#[derive(BorshDeserialize, BorshSerialize)]
pub struct Owners(HashMap<AccountId, bool>);

impl Owners {
    pub fn new() -> Self {
        Self(HashMap::new())
    }

    pub fn add(&mut self, address: &AccountId) {
        self.0.insert(address.to_string(), true);
    }

    pub fn remove(&mut self, address: &AccountId) {
        self.0.remove(&address.to_string());
    }

    pub fn contains(&self, address: &AccountId) -> bool {
        self.0.contains_key(&address.to_string())
    }

    pub fn to_vec(&self) -> Vec<String> {
        if !self.0.is_empty() {
            return self.0.keys().cloned().collect::<Vec<String>>();
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
    fn add_owner() {
        let context = get_context(vec![], false);
        testing_env!(context);
        let mut owners = Owners::new();
        owners.add(&"88bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4".to_string());
        let result = owners.contains(
            &"88bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4".to_string(),
        );
        assert_eq!(result, true);
    }

    #[test]
    fn add_existing_owner() {
        let context = get_context(vec![], false);
        testing_env!(context);
        let mut owners = Owners::new();
        let owner_1 = "88bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4".to_string();
        let owner_1_duplicate = "88bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4".to_string();

        owners.add(&owner_1.to_string());
        owners.add(&owner_1_duplicate.to_string());
        let result = owners.to_vec();
        let expected: Vec<String> =
            vec!["88bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4".to_string()];
        assert_eq!(result, expected);
    }

    #[test]
    fn remove_owner() {
        let context = get_context(vec![], false);
        testing_env!(context);
        let mut owners = Owners::new();
        let owner = "88bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4".to_string();
        owners.add(&owner.to_string());
        owners.remove(&owner.to_string());
        let result = owners.contains(&owner.to_string());
        assert_eq!(result, false);
    }

    #[test]
    fn remove_owner_non_existing() {
        let context = get_context(vec![], false);
        testing_env!(context);
        let mut owners = Owners::new();
        let owner_1 = "88bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e8".to_string();
        let owner_2 = "78bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4".to_string();
        owners.add(&owner_1.to_string());
        owners.remove(&owner_2.to_string());
        let result = owners.contains(&owner_2.to_string());
        assert_eq!(result, false);
    }
    
    #[test]
    fn to_vec_owners() {
        let context = get_context(vec![], false);
        testing_env!(context);
        let mut owners = Owners::new();
        let owner_1 = "88bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4".to_string();
        let owner_2 = "78bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4".to_string();
        let owner_3 = "68bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4".to_string();
        owners.add(&owner_1.to_string());
        owners.add(&owner_2.to_string());
        owners.add(&owner_3.to_string());
        let owners = owners.to_vec();
        let expected_owners: Vec<String> = vec![
            "88bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4".to_string(),
            "78bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4".to_string(),
            "68bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4".to_string(),
        ];
        let result: HashSet<_> = owners.iter().collect();
        let expected: HashSet<_> = expected_owners.iter().collect();
        assert_eq!(result, expected);
    }
}
