use super::{Address, BTPAddress};
use near_sdk::borsh::{self, BorshDeserialize, BorshSerialize};
use near_sdk::collections::{LookupMap, UnorderedMap};
use near_sdk::serde::Serialize;
use rand::seq::SliceRandom;
use std::collections::HashSet;

#[derive(BorshDeserialize, BorshSerialize)]
pub struct Routes {
    networks: Networks,
    links: Links,
}

#[derive(Serialize, Debug, Eq, PartialEq, Hash)]
pub struct Route {
    dst: BTPAddress,
    next: BTPAddress,
}

impl Routes {
    pub fn new() -> Self {
        Self {
            networks: Networks::new(),
            links: Links::new(),
        }
    }

    pub fn links(&self) -> &Links {
        &self.links
    }

    pub fn networks(&self) -> &Networks {
        &self.networks
    }

    pub fn add(&mut self, destination: &BTPAddress, link: &BTPAddress) {
        self.networks.add(destination);
        self.links.add(destination, link);
    }

    pub fn remove(&mut self, destination: &BTPAddress) {
        self.networks.remove(destination);
        self.links.remove(destination);
    }
}

#[derive(BorshDeserialize, BorshSerialize)]
pub struct Networks(LookupMap<String, HashSet<BTPAddress>>);

#[derive(BorshDeserialize, BorshSerialize)]
pub struct Links(UnorderedMap<BTPAddress, BTPAddress>);

impl Networks {
    fn new() -> Self {
        Self(LookupMap::new(b"network_routes".to_vec()))
    }

    fn add(&mut self, destination: &BTPAddress) {
        let mut list = self
            .0
            .get(&destination.network_address().unwrap())
            .unwrap_or_default();
        list.insert(destination.to_owned());
        self.0
            .insert(&destination.network_address().unwrap(), &list);
    }

    fn remove(&mut self, destination: &BTPAddress) {
        let mut list = self
            .0
            .get(&destination.network_address().unwrap())
            .unwrap_or_default();
        list.remove(&destination.to_owned());

        if list.is_empty() {
            self.0.remove(&destination.network_address().unwrap());
        } else {
            self.0
                .insert(&destination.network_address().unwrap(), &list);
        }
    }

    pub fn get(&self, network: &str) -> Option<BTPAddress> {
        // TODO: Map Destination and services available
        let list = self.0.get(&network.to_string()).unwrap_or_default();
        let mut rng = rand::thread_rng();
        list.into_iter()
            .collect::<Vec<BTPAddress>>()
            .choose(&mut rng)
            .map(|destination| destination.to_owned())
    }

    pub fn contains(&self, network: &str) -> bool {
        let list = self.0.get(&network.to_string()).unwrap_or_default();
        return !list.is_empty();
    }
}

impl Links {
    fn new() -> Self {
        Self(UnorderedMap::new(b"link_routes".to_vec()))
    }

    fn add(&mut self, destination: &BTPAddress, link: &BTPAddress) {
        self.0.insert(&destination, &link);
    }

    fn remove(&mut self, destination: &BTPAddress) {
        self.0.remove(&destination);
    }

    pub fn get(&self, destination: &BTPAddress) -> Option<BTPAddress> {
        if let Some(link) = self.0.get(&destination) {
            return Some(link);
        }
        None
    }

    pub fn contains(&self, destination: &BTPAddress) -> bool {
        return self.0.get(&destination).is_some();
    }

    pub fn to_vec(&self) -> Vec<Route> {
        if !self.0.is_empty() {
            return self
                .0
                .iter()
                .map(|v| Route {
                    dst: v.0,
                    next: v.1,
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
    fn add_route() {
        let context = get_context(vec![], false);
        testing_env!(context);
        let destination = BTPAddress::new(
            "btp://0x1.icon/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string(),
        );
        let link = BTPAddress::new(
            "btp://0x1.bsc/88bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4"
                .to_string(),
        );
        let mut routes = Routes::new();
        routes.add(&destination, &link);
        let link = routes.links.get(&destination);
        assert_eq!(
            link,
            Some(BTPAddress::new(
                "btp://0x1.bsc/88bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4"
                    .to_string(),
            ))
        );
        let network = routes.networks.get(&destination.network_address().unwrap());
        assert_eq!(
            network,
            Some(BTPAddress::new(
                "btp://0x1.icon/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string(),
            ))
        );
    }

    #[test]
    fn get_link_route() {
        let context = get_context(vec![], false);
        testing_env!(context);
        let dst_1 = BTPAddress::new(
            "btp://0x1.icon/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string(),
        );
        let next_1 = BTPAddress::new(
            "btp://0x1.bsc/88bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4"
                .to_string(),
        );
        let dst_2 =
            BTPAddress::new("btp://0x1.pra/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string());
        let next_2 = BTPAddress::new(
            "btp://0x3.iconee/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string(),
        );
        let dst_3 =
            BTPAddress::new("btp://0x5.pra/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string());
        let next_3 = BTPAddress::new(
            "btp://0x3.iconee/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string(),
        );
        let mut routes = Routes::new();
        routes.add(&dst_1, &next_1);
        routes.add(&dst_2, &next_2);
        routes.add(&dst_3, &next_3);
        let result = routes.links.get(&dst_2);
        assert_eq!(
            result,
            Some(BTPAddress::new(
                "btp://0x3.iconee/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string(),
            ))
        );
    }

    #[test]
    fn get_network_route() {
        let context = get_context(vec![], false);
        testing_env!(context);
        let dst_1 = BTPAddress::new(
            "btp://0x1.icon/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string(),
        );
        let next_1 = BTPAddress::new(
            "btp://0x1.bsc/88bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4"
                .to_string(),
        );
        let dst_2 =
            BTPAddress::new("btp://0x1.pra/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string());
        let next_2 = BTPAddress::new(
            "btp://0x3.iconee/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string(),
        );
        let dst_3 =
            BTPAddress::new("btp://0x5.pra/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string());
        let next_3 = BTPAddress::new(
            "btp://0x3.iconee/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string(),
        );
        let mut routes = Routes::new();
        routes.add(&dst_1, &next_1);
        routes.add(&dst_2, &next_2);
        routes.add(&dst_3, &next_3);
        let result = routes.networks.get(&dst_2.network_address().unwrap());
        assert_eq!(
            result,
            Some(BTPAddress::new(
                "btp://0x1.pra/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string(),
            ))
        );
    }

    #[test]
    fn remove_route() {
        let context = get_context(vec![], false);
        testing_env!(context);
        let dst_1 = BTPAddress::new(
            "btp://0x1.icon/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string(),
        );
        let next_1 = BTPAddress::new(
            "btp://0x1.bsc/88bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4"
                .to_string(),
        );
        let dst_2 =
            BTPAddress::new("btp://0x1.pra/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string());
        let next_2 = BTPAddress::new(
            "btp://0x3.iconee/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string(),
        );
        let dst_3 =
            BTPAddress::new("btp://0x5.pra/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string());
        let next_3 = BTPAddress::new(
            "btp://0x3.iconee/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string(),
        );
        let mut routes = Routes::new();
        routes.add(&dst_1, &next_1);
        routes.add(&dst_2, &next_2);
        routes.add(&dst_3, &next_3);
        routes.remove(&dst_2);
        let links = routes.links.get(&dst_2);
        assert_eq!(links, None);
        let networks = routes.networks.get(&dst_2.network_address().unwrap());
        assert_eq!(networks, None);
    }

    #[test]
    fn contains_link_route() {
        let context = get_context(vec![], false);
        testing_env!(context);
        let dst = BTPAddress::new(
            "btp://0x1.icon/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string(),
        );
        let next = BTPAddress::new(
            "btp://0x1.bsc/88bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4"
                .to_string(),
        );
        let mut routes = Routes::new();
        routes.add(&dst, &next);
        let result = routes.links.contains(&dst);
        assert_eq!(result, true);
    }

    #[test]
    fn contains_network_route() {
        let context = get_context(vec![], false);
        testing_env!(context);
        let dst = BTPAddress::new(
            "btp://0x1.icon/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string(),
        );
        let next = BTPAddress::new(
            "btp://0x1.bsc/88bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4"
                .to_string(),
        );
        let mut routes = Routes::new();
        routes.add(&dst, &next);
        let result = routes.networks.contains(&dst.network_address().unwrap());
        assert_eq!(result, true);
    }

    #[test]
    fn to_vec_route() {
        let context = get_context(vec![], false);
        testing_env!(context);
        let dst_1 = BTPAddress::new(
            "btp://0x1.icon/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string(),
        );
        let next_1 = BTPAddress::new(
            "btp://0x1.bsc/88bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4"
                .to_string(),
        );
        let dst_2 =
            BTPAddress::new("btp://0x1.pra/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string());
        let next_2 = BTPAddress::new(
            "btp://0x3.iconee/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string(),
        );
        let dst_3 =
            BTPAddress::new("btp://0x5.pra/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string());
        let next_3 = BTPAddress::new(
            "btp://0x3.iconee/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string(),
        );
        let mut routes = Routes::new();
        routes.add(&dst_1, &next_1);
        routes.add(&dst_2, &next_2);
        routes.add(&dst_3, &next_3);
        let routes = routes.links.to_vec();
        let expected_routes = vec![
            Route {
                dst: BTPAddress::new(
                    "btp://0x1.icon/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string(),
                ),
                next:
                BTPAddress::new(
                    "btp://0x1.bsc/88bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4"
                        .to_string(),
                ),
            },
            Route {
                dst: BTPAddress::new("btp://0x1.pra/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string()),
                next: BTPAddress::new(
                    "btp://0x3.iconee/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string(),
                ),
            },
            Route {
                dst: BTPAddress::new("btp://0x5.pra/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string()),
                next: BTPAddress::new(
                    "btp://0x3.iconee/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string(),
                ),
            },
        ];
        let result: HashSet<_> = routes.iter().collect();
        let expected: HashSet<_> = expected_routes.iter().collect();
        assert_eq!(result, expected);
    }
}
