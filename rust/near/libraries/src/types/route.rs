use super::{Address, BTPAddress};
use near_sdk::borsh::{self, BorshDeserialize, BorshSerialize};
use near_sdk::collections::UnorderedMap;
use near_sdk::serde::{Serialize, Deserialize};
use std::collections::HashMap;
use std::collections::HashSet;
use near_sdk::serde_json::{Value, json};

#[derive(Serialize, Deserialize, Debug, Eq, PartialEq, Hash)]
pub struct Route {
    destination: BTPAddress,
    next: BTPAddress,
}

impl From<Route> for Value {
    fn from(route: Route) -> Self {
        json!({
            "dst": route.destination,
            "next": route.next
        })
    }
}

#[derive(BorshDeserialize, BorshSerialize)]
pub struct Routes(UnorderedMap<String, HashMap<BTPAddress, BTPAddress>>);

impl Routes {
    pub fn new() -> Self {
        Self(UnorderedMap::new(b"routes".to_vec()))
    }

    pub fn add(&mut self, destination: &BTPAddress, link: &BTPAddress) {
        let mut list = self.0.get(&destination.network_address().unwrap()).unwrap_or_default();
        list.insert(destination.to_owned(), link.to_owned());
        self.0.insert(&destination.network_address().unwrap(), &list);
    }

    pub fn remove(&mut self, destination: &BTPAddress) {
        let mut list = self.0.get(&destination.network_address().unwrap()).unwrap_or_default();
        list.remove(&destination);

        if list.is_empty() {
            self.0.remove(&destination.network_address().unwrap());
        } else {
            self.0.insert(&destination.network_address().unwrap(), &list);
        }
    }

    pub fn get(&self, destination: &BTPAddress) -> Option<BTPAddress> {
        let list = self.0.get(&destination.network_address().unwrap()).unwrap_or_default();
        list.get(destination).map(|link| link.to_owned())
    }

    pub fn contains_network(&self, network: &str) -> bool {
        self.0.get(&network.to_string()).is_some()
    }

    pub fn contains(&self, destination: &BTPAddress) -> bool {
        let list = self.0.get(&destination.network_address().unwrap()).unwrap_or_default();
        list.contains_key(destination)
    }

    pub fn to_vec(&self) -> Vec<Route> {
        let mut routes: HashSet<Route> = HashSet::new();
        if !self.0.is_empty() {
            self.0.iter().for_each(|network| {
                network.1.into_iter().for_each(|(destination, next)| {
                    routes.insert(Route { destination, next });
                });
            });
        }
        routes.into_iter().collect()
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
        let link = routes.get(&destination);
        assert_eq!(
            link,
            Some(BTPAddress::new(
                "btp://0x1.bsc/88bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4"
                    .to_string(),
            ))
        );
    }

    #[test]
    fn get_route() {
        let context = get_context(vec![], false);
        testing_env!(context);
        let destination_1 = BTPAddress::new(
            "btp://0x1.icon/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string(),
        );
        let next_1 = BTPAddress::new(
            "btp://0x1.bsc/88bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4"
                .to_string(),
        );
        let destination_2 =
            BTPAddress::new("btp://0x1.pra/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string());
        let next_2 = BTPAddress::new(
            "btp://0x3.iconee/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string(),
        );
        let destination_3 =
            BTPAddress::new("btp://0x5.pra/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string());
        let next_3 = BTPAddress::new(
            "btp://0x3.iconee/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string(),
        );
        let mut routes = Routes::new();
        routes.add(&destination_1, &next_1);
        routes.add(&destination_2, &next_2);
        routes.add(&destination_3, &next_3);
        let result = routes.get(&destination_2);
        assert_eq!(
            result,
            Some(BTPAddress::new(
                "btp://0x3.iconee/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string(),
            ))
        );
    }

    #[test]
    fn remove_route() {
        let context = get_context(vec![], false);
        testing_env!(context);
        let destination_1 = BTPAddress::new(
            "btp://0x1.icon/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string(),
        );
        let next_1 = BTPAddress::new(
            "btp://0x1.bsc/88bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4"
                .to_string(),
        );
        let destination_2 =
            BTPAddress::new("btp://0x1.pra/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string());
        let next_2 = BTPAddress::new(
            "btp://0x3.iconee/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string(),
        );
        let destination_3 =
            BTPAddress::new("btp://0x5.pra/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string());
        let next_3 = BTPAddress::new(
            "btp://0x3.iconee/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string(),
        );
        let mut routes = Routes::new();
        routes.add(&destination_1, &next_1);
        routes.add(&destination_2, &next_2);
        routes.add(&destination_3, &next_3);
        routes.remove(&destination_2);
        let links = routes.get(&destination_2);
        assert_eq!(links, None);
    }

    #[test]
    fn contains_route() {
        let context = get_context(vec![], false);
        testing_env!(context);
        let destination_1 = BTPAddress::new(
            "btp://0x1.icon/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string(),
        );
        let next_1 = BTPAddress::new(
            "btp://0x1.bsc/88bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4"
                .to_string(),
        );
        let destination_2 =
            BTPAddress::new("btp://0x1.pra/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string());
        let next_2 = BTPAddress::new(
            "btp://0x3.iconee/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string(),
        );
        let destination_3 =
            BTPAddress::new("btp://0x5.pra/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string());
        let next_3 = BTPAddress::new(
            "btp://0x3.iconee/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string(),
        );
        let mut routes = Routes::new();
        routes.add(&destination_1, &next_1);
        routes.add(&destination_2, &next_2);
        routes.add(&destination_3, &next_3);
        let result = routes.contains_network(&destination_1.network_address().unwrap());
        assert_eq!(result, true);
        routes.remove(&destination_1);
        let result = routes.contains_network(&destination_1.network_address().unwrap());
        assert_eq!(result, false);
        routes.remove(&destination_2);
        let result = routes.contains_network(&destination_3.network_address().unwrap());
        assert_eq!(result, true);
    }

    #[test]
    fn contains_network() {
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
        let result = routes.contains_network(&dst.network_address().unwrap());
        assert_eq!(result, true);
    }

    #[test]
    fn to_vec_route() {
        let context = get_context(vec![], false);
        testing_env!(context);
        let destination_1 = BTPAddress::new(
            "btp://0x1.icon/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string(),
        );
        let next_1 = BTPAddress::new(
            "btp://0x1.bsc/88bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4"
                .to_string(),
        );
        let destination_2 =
            BTPAddress::new("btp://0x1.pra/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string());
        let next_2 = BTPAddress::new(
            "btp://0x3.iconee/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string(),
        );
        let destination_3 =
            BTPAddress::new("btp://0x5.pra/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string());
        let next_3 = BTPAddress::new(
            "btp://0x3.iconee/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string(),
        );
        let mut routes = Routes::new();
        routes.add(&destination_1, &next_1);
        routes.add(&destination_2, &next_2);
        routes.add(&destination_3, &next_3);
        let routes = routes.to_vec();
        let expected_routes = vec![
            Route {
                destination: BTPAddress::new(
                    "btp://0x1.icon/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string(),
                ),
                next:
                BTPAddress::new(
                    "btp://0x1.bsc/88bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4"
                        .to_string(),
                ),
            },
            Route {
                destination: BTPAddress::new("btp://0x1.pra/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string()),
                next: BTPAddress::new(
                    "btp://0x3.iconee/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string(),
                ),
            },
            Route {
                destination: BTPAddress::new("btp://0x5.pra/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string()),
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
