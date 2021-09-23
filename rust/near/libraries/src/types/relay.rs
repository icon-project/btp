use super::BTPAddress;
use near_sdk::borsh::{self, BorshDeserialize, BorshSerialize};
use near_sdk::collections::UnorderedSet;
use near_sdk::env::keccak256;
use near_sdk::AccountId;

#[derive(BorshDeserialize, BorshSerialize)]
pub struct Relays(UnorderedSet<AccountId>);

impl Default for Relays {
    fn default() -> Relays {
        let relays: UnorderedSet<AccountId> =
            UnorderedSet::new(keccak256("relay".to_string().as_bytes()));
        Self(relays)
    }
}

impl Eq for Relays {}
impl PartialEq for Relays {
    fn eq(&self, other: &Self) -> bool {
        self.0.to_vec() == other.0.to_vec()
    }
}

impl std::fmt::Debug for Relays {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        f.debug_list().entries(self.0.iter()).finish()
    }
}

impl Relays {
    pub fn new(link: &BTPAddress) -> Self {
        link.to_string().push_str("_relay");
        Self(UnorderedSet::new(keccak256(link.to_string().as_bytes())))
    }

    pub fn as_mut(&mut self) -> &mut Self {
        self
    }

    pub fn set(&mut self, account_ids: &Vec<AccountId>) {
        self.clear();

        for account_id in account_ids {
            self.add(account_id);
        }
    }

    pub fn add(&mut self, account_id: &AccountId) {
        self.0.insert(&account_id);
    }

    pub fn clear(&mut self) {
        self.0.clear();
    }

    pub fn contains(&self, account_id: &AccountId) -> bool {
        self.0.contains(&account_id)
    }

    pub fn remove(&mut self, account_id: &AccountId) {
        self.0.remove(&account_id);
    }

    pub fn to_vec(&self) -> Vec<AccountId> {
        if !self.0.is_empty() {
            return self.0.to_vec();
        }
        return vec![];
    }
}

#[cfg(test)]
mod tests {
    use super::*;
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
    fn add_relay() {
        let context = get_context(vec![], false);
        testing_env!(context);
        let link = BTPAddress::new(
            "btp://0x1.near/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string(),
        );
        let mut relays = Relays::new(&link);
        let relay = "88bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4"
            .parse::<AccountId>()
            .unwrap();
        relays.add(&relay);
        let result = relays.contains(&relay);
        assert_eq!(result, true);
    }

    #[test]
    fn add_existing_relay() {
        let context = get_context(vec![], false);
        testing_env!(context);
        let link = BTPAddress::new(
            "btp://0x1.near/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string(),
        );
        let mut relays = Relays::new(&link);
        let relay_1 = "88bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4"
            .parse::<AccountId>()
            .unwrap();
        let relay_1_duplicate = "88bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4"
            .parse::<AccountId>()
            .unwrap();
        relays.add(&relay_1);
        relays.add(&relay_1_duplicate);
        let result = relays.to_vec();
        let expected: Vec<AccountId> = vec![
            "88bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4"
                .parse::<AccountId>()
                .unwrap(),
        ];
        assert_eq!(result, expected);
    }

    #[test]
    fn remove_relay() {
        let context = get_context(vec![], false);
        testing_env!(context);
        let link = BTPAddress::new(
            "btp://0x1.near/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string(),
        );
        let mut relays = Relays::new(&link);
        let relay = "88bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4"
            .parse::<AccountId>()
            .unwrap();
        relays.add(&relay);
        relays.remove(&relay);
        let result = relays.contains(&relay);
        assert_eq!(result, false);
    }

    #[test]
    fn remove_relay_non_existing() {
        let context = get_context(vec![], false);
        testing_env!(context);
        let link = BTPAddress::new(
            "btp://0x1.near/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string(),
        );
        let mut relays = Relays::new(&link);
        let relay_1 = "88bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e8"
            .parse::<AccountId>()
            .unwrap();
        let relay_2 = "88bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4"
            .parse::<AccountId>()
            .unwrap();
        relays.add(&relay_1);
        relays.remove(&relay_2);
        let result = relays.contains(&relay_2);
        assert_eq!(result, false);
    }

    #[test]
    fn clear_relays() {
        let context = get_context(vec![], false);
        testing_env!(context);
        let link = BTPAddress::new(
            "btp://0x1.near/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string(),
        );
        let mut relays = Relays::new(&link);
        let relay = "88bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4"
            .parse::<AccountId>()
            .unwrap();
        relays.add(&relay);
        relays.clear();
        let result = relays.to_vec();
        let expected: Vec<AccountId> = vec![];
        assert_eq!(result, expected);
    }

    #[test]
    fn to_vec_relays() {
        let context = get_context(vec![], false);
        testing_env!(context);
        let link = BTPAddress::new(
            "btp://0x1.near/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string(),
        );
        let mut relays = Relays::new(&link);
        let relay_1 = "88bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4"
            .parse::<AccountId>()
            .unwrap();
        let relay_2 = "78bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4"
            .parse::<AccountId>()
            .unwrap();
        let relay_3 = "68bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4"
            .parse::<AccountId>()
            .unwrap();
        relays.add(&relay_1);
        relays.add(&relay_2);
        relays.add(&relay_3);
        let result = relays.to_vec();
        let expected: Vec<AccountId> = vec![
            "88bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4"
                .parse::<AccountId>()
                .unwrap(),
            "78bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4"
                .parse::<AccountId>()
                .unwrap(),
            "68bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4"
                .parse::<AccountId>()
                .unwrap(),
        ];
        assert_eq!(result, expected);
    }
}
