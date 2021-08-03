use near_sdk::borsh::{self, BorshDeserialize, BorshSerialize};
use near_sdk::collections::UnorderedMap;

#[path = "./relay.rs"]
mod relay;
use relay::Relays;

#[derive(BorshDeserialize, BorshSerialize)]
pub struct Links(UnorderedMap<String, Link>);

#[derive(Debug, Default, BorshDeserialize, BorshSerialize, Eq, PartialEq)]
pub struct Link {
    rx_seq: u64,
    tx_seq: u64,
    verifier: Verifier,
    pub relays: Relays,
    reachable: Vec<u8>,
    relay_index: u64,
    rotate_height: u64,
    rotate_term: u64,
    pub delay_limit: u64,
    max_aggregation: u64,
    rx_height_src: u64,
    rx_height: u64,
    pub block_interval_src: u64,
    block_interval_dst: u64,
    current_height: u64,
}

#[derive(Debug, Default, BorshDeserialize, BorshSerialize, Eq, PartialEq)]
pub struct Verifier {
    mta_height: u64,
    mta_offset: u64,
    last_height: u64,
}

impl Links {
    pub fn new() -> Self {
        Self(UnorderedMap::new(b"links".to_vec()))
    }

    pub fn add(&mut self, link: &str) {
        let value = Link {
            rx_seq: 0,
            tx_seq: 0,
            verifier: Verifier {
                mta_height: 0,
                mta_offset: 0,
                last_height: 0,
            },
            relays: Relays::new(&link.to_string()),
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
        };
        self.0.insert(&link.to_string(), &value);
    }

    pub fn remove(&mut self, link: &str) {
        self.0.remove(&link.to_string());
    }

    pub fn to_vec(&self) -> Vec<String> {
        if !self.0.is_empty() {
            return self.0.keys().collect();
        }
        vec![]
    }

    pub fn get(&self, link: &str) -> Option<Link> {
        if let Some(value) = self.0.get(&link.to_string()) {
            return Some(value);
        }
        None
    }
    
    pub fn contains(&self, link: &str) -> bool {
        return self.0.get(&link.to_string()).is_some();
    }
    
    pub fn set(
        &mut self,
        link_param: &str,
        block_interval: Option<u64>,
        max_aggregation: Option<u64>,
        delay_limit: Option<u64>,
        relays: Option<Vec<String>>,
    ) {
        if let Some(mut link) = self.0.get(&link_param.to_string()) {
            if let Some(max_aggregation) = max_aggregation {
                link.max_aggregation = max_aggregation;
            }
            if let Some(block_interval) = block_interval {
                link.block_interval_src = block_interval;
            }
            if let Some(delay_limit) = delay_limit {
                link.delay_limit = delay_limit;
            }
            if let Some(relays) = relays {
                link.relays.clear();
                for relay in relays.iter() {
                    link.relays.add(relay);
                }
            }
            self.0.insert(&link_param.to_string(), &link);
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use near_sdk::MockedBlockchain;
    use near_sdk::{testing_env, VMContext};
    use btp_common::{BTPAddress};

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
    fn add_link() {
        let context = get_context(vec![], false);
        testing_env!(context);
        let link = BTPAddress::new(
            "btp://0x1.near/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string(),
        );
        let mut links = Links::new();
        links.add(&link.to_string());
        let expected = Link {
            ..Default::default()
        };
        assert_eq!(links.get(&link.to_string()).unwrap(), expected);
    }

    #[test]
    fn add_link_relays_pass() {
        let context = get_context(vec![], false);
        testing_env!(context);
        let link = BTPAddress::new(
            "btp://0x1.near/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string(),
        );
        let mut links = Links::new();
        links.add(&link.to_string());
        links
            .set(
                &link.to_string(),
                None,
                None,
                None,
                Some(vec!["88bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4".to_string()]),
            );
        let mut expected = Link {
            ..Default::default()
        };
        expected.relays.add(&"88bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4".to_string());
        assert_eq!(links.get(&link.to_string()).unwrap(), expected);
    }

    #[test]
    fn remove_link() {
        let context = get_context(vec![], false);
        testing_env!(context);
        let mut links = Links::new();
        let link = BTPAddress::new(
            "btp://0x1.near/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string(),
        );
        links.add(&link.to_string());
        links.remove(&link.to_string());
        let result = links.contains(&link.to_string());
        assert_eq!(result, false);
    }

    #[test]
    fn remove_link_non_existing() {
        let context = get_context(vec![], false);
        testing_env!(context);
        let mut links = Links::new();
        let link_1 = BTPAddress::new(
            "btp://0x1.near/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string(),
        );
        let link_2 = BTPAddress::new(
            "btp://0x1.icon/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string()
        );
        links.add(&link_1.to_string());
        links.remove(&link_2.to_string());
        let result = links.contains(&link_2.to_string());
        assert_eq!(result, false);
    }

    #[test]
    fn to_vec_links() {
        let context = get_context(vec![], false);
        testing_env!(context);
        let mut links = Links::new();
        let link_1 = BTPAddress::new(
            "btp://0x1.near/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string()
        );
        let link_2 = BTPAddress::new(
            "btp://0x1.icon/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string()
        );
        let link_3 = BTPAddress::new(
            "btp://0x1.iconee/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string()
        );
        links.add(&link_1.to_string());
        links.add(&link_2.to_string());
        links.add(&link_3.to_string());
        let result = links.to_vec();
        let expected: Vec<String> = vec![
            "btp://0x1.near/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string(),
            "btp://0x1.icon/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string(),
            "btp://0x1.iconee/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string(),
        ];
        assert_eq!(result, expected);
    }

}
