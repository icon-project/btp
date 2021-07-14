use btp_common::BTPAddress;
use near_sdk::borsh::{self, BorshDeserialize, BorshSerialize};
use near_sdk::collections::UnorderedSet;

#[derive(BorshDeserialize, BorshSerialize)]
pub struct Relays(UnorderedSet<String>);

impl Relays {
    pub fn new(link: &BTPAddress) -> Self {
        link.to_string().push_str("_relay");
        let relays: UnorderedSet<String> = UnorderedSet::new(link.to_string().into_bytes());
        Self(relays)
    }

    pub fn add(&mut self, address: String) -> Result<bool, String> {
        if !self.0.contains(&address) {
            return Ok(self.0.insert(&address));
        }
        return Err("relay already added".to_string());
    }

    pub fn clear(&mut self)  -> Result<(), String> {
        if !self.0.is_empty() {
            return Ok(self.0.clear())
        }
        return Err("no relays present".to_string());
    }

    pub fn remove(&mut self, address: String) -> Result<bool, String> {
        if self.0.contains(&address) {
            return Ok(self.0.remove(&address));
        }
        return Err("relay not added".to_string());
    }

    pub fn to_vec(&self) -> Vec<String> {
        if !self.0.is_empty() {
            return self.0.to_vec();
        }
        return vec![];
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
    fn add_single_relay_pass() {
        let context = get_context(vec![], false);
        testing_env!(context);
        let link = BTPAddress("btp://0x1.near/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string());
        let mut relays = Relays::new(&link);
        relays.add("test".to_string()).expect("Failed");
        assert_eq!(relays.to_vec(), vec!["test".to_string()]);
    }

    #[test]
    fn add_multiple_relays_pass() {
        let context = get_context(vec![], false);
        testing_env!(context);
        let link = BTPAddress("btp://0x1.near/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string());
        let mut relays = Relays::new(&link);
        relays.add("test".to_string()).expect("Failed");
        relays.add("test 2".to_string()).expect("Failed");
        assert_eq!(relays.to_vec(), vec!["test".to_string(), "test 2".to_string()]);
    }

    #[test]
    fn add_existing_relay_fail() {
        let context = get_context(vec![], false);
        testing_env!(context);
        let link = BTPAddress("btp://0x1.near/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string());
        let mut relays = Relays::new(&link);
        relays.add("test".to_string()).expect("Failed");
        assert_eq!(relays.add("test".to_string()), Err("relay already added".to_string()));
    }

    #[test]
    fn remove_existing_relay_pass() {
        let context = get_context(vec![], false);
        testing_env!(context);
        let link = BTPAddress("btp://0x1.near/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string());
        let mut relays = Relays::new(&link);
        relays.add("test".to_string()).expect("Failed");
        relays.add("test 2".to_string()).expect("Failed");
        relays.add("test 3".to_string()).expect("Failed");
        relays.remove("test 2".to_string()).expect("Failed");
        assert_eq!(relays.to_vec(), vec!["test".to_string(), "test 3".to_string()]);
    }

    #[test]
    fn remove_non_existing_relay_fail() {
        let context = get_context(vec![], false);
        testing_env!(context);
        let link = BTPAddress("btp://0x1.near/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string());
        let mut relays = Relays::new(&link);
        relays.add("test".to_string()).expect("Failed");
        relays.add("test 2".to_string()).expect("Failed");
        relays.add("test 3".to_string()).expect("Failed");
        assert_eq!(relays.remove("test 4".to_string()), Err("relay not added".to_string()));
    }

    #[test]
    fn clear_relays_list_pass() {
        let context = get_context(vec![], false);
        testing_env!(context);
        let link = BTPAddress("btp://0x1.near/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string());
        let mut relays = Relays::new(&link);
        relays.add("test".to_string()).expect("Failed");
        relays.add("test 2".to_string()).expect("Failed");
        relays.add("test 3".to_string()).expect("Failed");
        relays.clear().expect("Failed");
        assert_eq!(relays.to_vec(), Vec::<String>::new());
    }

    #[test]
    fn clear_empty_relays_list_fail() {
        let context = get_context(vec![], false);
        testing_env!(context);
        let link = BTPAddress("btp://0x1.near/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string());
        let mut relays = Relays::new(&link);
        assert_eq!(relays.clear(), Err("no relays present".to_string()));
    }
}
