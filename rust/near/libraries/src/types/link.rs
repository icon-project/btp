use super::{BTPAddress, Relays};
use near_sdk::borsh::{self, BorshDeserialize, BorshSerialize};
use near_sdk::collections::UnorderedMap;
use std::ops::{Deref, DerefMut};

type Link = BTPAddress;

#[derive(Debug, Default, BorshDeserialize, BorshSerialize, Eq, PartialEq)]
pub struct LinkProperty {
    rx_seq: u128,
    tx_seq: u128,
    verifier: Verifier,
    relays: Relays,
    reachable: Vec<u8>,
    relay_index: u64,
    rotate_height: u64,
    rotate_term: u64,
    delay_limit: u64,
    max_aggregation: u64,
    rx_height_src: u128,
    rx_height: u128,
    block_interval_src: u64,
    block_interval_dst: u64,
    current_height: u128,
    current_epoch: u128
}

impl LinkProperty {
    pub fn set_block_interval_src(&mut self, block_interval_src: u64) {
        self.block_interval_src = block_interval_src;
    }

    pub fn set_max_aggregation(&mut self, max_aggregation: u64) {
        self.max_aggregation = max_aggregation;
    }

    pub fn set_delay_limit(&mut self, delay_limit: u64) {
        self.delay_limit = delay_limit;
    }

    pub fn verifier() {
        unimplemented!();
    }

    pub fn verifier_mut(&mut self) {
        unimplemented!();
    }

    pub fn relays_mut(&mut self) -> &mut Relays {
        self.relays.as_mut()
    }

    pub fn relays(&self) -> &Relays {
        &self.relays
    }
}

#[derive(BorshDeserialize, BorshSerialize)]
pub struct Links(UnorderedMap<Link, LinkProperty>);

impl Deref for Links {
    type Target = UnorderedMap<Link, LinkProperty>;

    fn deref(&self) -> &Self::Target {
        &self.0
    }
}

impl DerefMut for Links {
    fn deref_mut(&mut self) -> &mut Self::Target {
        &mut self.0
    }
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

    pub fn add(&mut self, link: &BTPAddress) {
        self.0.insert(
            link,
            &LinkProperty {
                relays: Relays::new(link),
                ..Default::default()
            },
        );
    }

    pub fn set(&mut self, link: &BTPAddress, property: &LinkProperty) {
        self.0.insert(link, property);
    }

    pub fn remove(&mut self, link: &BTPAddress) {
        self.0.remove(&link);
    }

    pub fn to_vec(&self) -> Vec<BTPAddress> {
        if !self.0.is_empty() {
            return self.0.keys().collect::<Vec<BTPAddress>>();
        }
        vec![]
    }

    pub fn get(&self, link: &BTPAddress) -> Option<LinkProperty> {
        if let Some(value) = self.0.get(link) {
            return Some(value);
        }
        None
    }

    pub fn contains(&self, link: &BTPAddress) -> bool {
        return self.0.get(link).is_some();
    }
}

#[cfg(test)]
mod tests {
    use std::vec;

    use super::*;
    use near_sdk::{testing_env, VMContext};
    use near_sdk::AccountId;
    
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
        links.add(&link);
        let expected = LinkProperty {
            ..Default::default()
        };
        assert_eq!(links.get(&link).unwrap(), expected);
    }

    #[test]
    fn add_link_relays_pass() {
        let context = get_context(vec![], false);
        testing_env!(context);
        let link = BTPAddress::new(
            "btp://0x1.near/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string(),
        );
        let mut links = Links::new();
        links.add(&link);

        if let Some(link_property) = links.get(&link).as_mut() {
            link_property.relays.add(
                &"88bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4"
                    .parse::<AccountId>()
                    .unwrap(),
            );
            links.set(&link, &link_property);
        }
        let mut expected = LinkProperty {
            ..Default::default()
        };
        expected.relays.add(
            &"88bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4"
                .parse::<AccountId>()
                .unwrap(),
        );
        assert_eq!(links.get(&link).unwrap(), expected);
    }

    #[test]
    fn set_link_block_interval_src() {
        let context = get_context(vec![], false);
        testing_env!(context);
        let link = BTPAddress::new(
            "btp://0x1.near/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string(),
        );
        let mut links = Links::new();
        links.add(&link);
        if let Some(link_property) = links.get(&link).as_mut() {
            link_property.set_block_interval_src(1000);
            links.set(&link, &link_property);
        }
        let expected = LinkProperty {
            block_interval_src: 1000,
            ..Default::default()
        };
        assert_eq!(links.get(&link).unwrap(), expected);
    }

    #[test]
    fn set_link_max_aggregation_src() {
        let context = get_context(vec![], false);
        testing_env!(context);
        let link = BTPAddress::new(
            "btp://0x1.near/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string(),
        );
        let mut links = Links::new();
        links.add(&link);
        if let Some(link_property) = links.get(&link).as_mut() {
            link_property.set_max_aggregation(10);
            links.set(&link, &link_property);
        }
        let expected = LinkProperty {
            max_aggregation: 10,
            ..Default::default()
        };
        assert_eq!(links.get(&link).unwrap(), expected);
    }

    #[test]
    fn set_link_delay_limit_src() {
        let context = get_context(vec![], false);
        testing_env!(context);
        let link = BTPAddress::new(
            "btp://0x1.near/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string(),
        );
        let mut links = Links::new();
        links.add(&link);
        if let Some(link_property) = links.get(&link).as_mut() {
            link_property.set_delay_limit(100);
            links.set(&link, &link_property);
        }
        let expected = LinkProperty {
            delay_limit: 100,
            ..Default::default()
        };
        assert_eq!(links.get(&link).unwrap(), expected);
    }

    #[test]
    fn set_link_relays_pass() {
        let context = get_context(vec![], false);
        testing_env!(context);
        let link = BTPAddress::new(
            "btp://0x1.near/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string(),
        );
        let mut links = Links::new();
        links.add(&link);

        if let Some(link_property) = links.get(&link).as_mut() {
            link_property.relays.set(&vec![
                "88bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e2"
                    .parse::<AccountId>()
                    .unwrap(),
                "88bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e3"
                    .parse::<AccountId>()
                    .unwrap(),
                "88bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4"
                    .parse::<AccountId>()
                    .unwrap(),
            ]);
            links.set(&link, &link_property);
        }
        let mut expected = LinkProperty {
            ..Default::default()
        };
        expected.relays.set(&vec![
            "88bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e2"
                .parse::<AccountId>()
                .unwrap(),
            "88bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e3"
                .parse::<AccountId>()
                .unwrap(),
            "88bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4"
                .parse::<AccountId>()
                .unwrap(),
        ]);
        assert_eq!(links.get(&link).unwrap(), expected);
    }

    #[test]
    fn remove_link() {
        let context = get_context(vec![], false);
        testing_env!(context);
        let mut links = Links::new();
        let link = BTPAddress::new(
            "btp://0x1.near/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string(),
        );
        links.add(&link);
        links.remove(&link);
        let result = links.contains(&link);
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
            "btp://0x1.icon/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string(),
        );
        links.add(&link_1);
        links.remove(&link_2);
        let result = links.contains(&link_2);
        assert_eq!(result, false);
    }

    #[test]
    fn to_vec_links() {
        let context = get_context(vec![], false);
        testing_env!(context);
        let mut links = Links::new();
        let link_1 = BTPAddress::new(
            "btp://0x1.near/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string(),
        );
        let link_2 = BTPAddress::new(
            "btp://0x1.icon/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string(),
        );
        let link_3 = BTPAddress::new(
            "btp://0x1.iconee/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string(),
        );
        links.add(&link_1);
        links.add(&link_2);
        links.add(&link_3);
        let result = links.to_vec();
        let expected: Vec<BTPAddress> = vec![link_1, link_2, link_3];
        assert_eq!(result, expected);
    }
}
