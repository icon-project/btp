use btp_common::BTPAddress;
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
    delay_limit: u64,
    max_aggregation: u64,
    rx_height_src: u64,
    rx_height: u64,
    block_interval_src: u64,
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
        let links: UnorderedMap<String, Link> = UnorderedMap::new(b"links".to_vec());
        Self(links)
    }

    pub fn insert(&mut self, link: &BTPAddress) -> Result<bool, String> {
        if self.0.get(&link.to_string()).is_none() {
            let link_property = Link {
                rx_seq: 0,
                tx_seq: 0,
                verifier: Verifier {
                    mta_height: 0,
                    mta_offset: 0,
                    last_height: 0,
                },
                relays: Relays::new(link),
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
            return Ok(self.0.insert(&link.to_string(), &link_property).is_some());
        }
        Err("link already added".to_string())
    }

    pub fn remove(&mut self, link: &BTPAddress) -> Result<bool, String> {
        if self.0.get(&link.to_string()).is_some() {
            return Ok(self.0.remove(&link.0.clone()).is_some());
        }
        Err("link does not exist".to_string())
    }

    pub fn to_vec(&self) -> Vec<String> {
        if !self.0.is_empty() {
            return self.0.keys().collect();
        }
        vec![]
    }

    pub fn get(&self, link: &BTPAddress) -> Result<Link, String> {
        if let Some(link) = self.0.get(&link.to_string()) {
            return Ok(link);
        }
        Err("link does not exist".to_string())
    }

    pub fn set(
        &mut self,
        link_param: &BTPAddress,
        block_interval: Option<u64>,
        max_aggregation: Option<u64>,
        delay_limit: Option<u64>,
        relays: Option<Vec<String>>,
    ) -> Result<bool, String> {
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
                for relay in relays.iter() {
                    match link.relays.add(relay.clone()) {
                        Ok(_) => (),
                        Err(_) => (),
                    }
                }
            }
            return Ok(self.0.insert(&link_param.to_string(), &link).is_some());
        }
        Err("link does not exist".to_string())
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
    fn add_link_pass() {
        let context = get_context(vec![], false);
        testing_env!(context);
        let link =
            BTPAddress("btp://0x1.near/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string());
        let mut links = Links::new();
        links.insert(&link).expect("Failed");
        let expected = Link {
            ..Default::default()
        };
        assert_eq!(links.get(&link).unwrap(), expected);
    }

    #[test]
    fn add_existing_link_fail() {
        let context = get_context(vec![], false);
        testing_env!(context);
        let link =
            BTPAddress("btp://0x1.near/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string());
        let mut links = Links::new();
        links.insert(&link).expect("Failed");
        assert_eq!(links.insert(&link), Err("link already added".to_string()));
    }

    #[test]
    fn add_link_relay_pass() {
        let context = get_context(vec![], false);
        testing_env!(context);
        let link_param =
            BTPAddress("btp://0x1.near/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string());
        let mut links = Links::new();
        links.insert(&link_param).expect("Failed");
        links
            .set(
                &link_param,
                None,
                None,
                None,
                Some(vec!["test".to_string()]),
            )
            .expect("Failed");
        let mut expected = Link {
            ..Default::default()
        };
        expected.relays.add("test".to_string()).expect("Failed");
        assert_eq!(links.get(&link_param).unwrap(), expected);
    }
}
