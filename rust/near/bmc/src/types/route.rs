use near_sdk::borsh::{self, BorshDeserialize, BorshSerialize};
use near_sdk::collections::UnorderedMap;
use serde::Serialize;

#[derive(BorshDeserialize, BorshSerialize)]
pub struct Routes(UnorderedMap<String, String>);

#[derive(Serialize, Debug, Eq, PartialEq, Hash)]
pub struct Route {
    dst: String,
    next: String,
}

impl Routes {
    pub fn new() -> Self {
        Self(UnorderedMap::new(b"routes".to_vec()))
    }

    pub fn add(&mut self, dst: &str, next: &str) {
        self.0.insert(&dst.to_string(), &next.to_string());
    }

    pub fn remove(&mut self, dst: &str) {
        self.0.remove(&dst.to_string());
    }

    pub fn get(&self, dst: &str) -> Option<String> {
        if let Some(next) = self.0.get(&dst.to_string()) {
            return Some(next.to_string());
        }
        None
    }

    pub fn contains(&self, dst: &str) -> bool {
        return self.0.get(&dst.to_string()).is_some();
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
    use btp_common::BTPAddress;
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
    fn add_route() {
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
        routes.add(&dst.to_string(), &next.to_string());
        let result = routes.get(&dst.to_string());
        assert_eq!(
            result,
            Some(
                "btp://0x1.bsc/88bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4"
                    .to_string()
            )
        );
    }

    #[test]
    fn get_route() {
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
        routes.add(&dst_1.to_string(), &next_1.to_string());
        routes.add(&dst_2.to_string(), &next_2.to_string());
        routes.add(&dst_3.to_string(), &next_3.to_string());
        let result = routes.get(&dst_2.to_string());
        assert_eq!(
            result,
            Some("btp://0x3.iconee/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string())
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
        routes.add(&dst_1.to_string(), &next_1.to_string());
        routes.add(&dst_2.to_string(), &next_2.to_string());
        routes.add(&dst_3.to_string(), &next_3.to_string());
        routes.remove(&dst_2.to_string());
        let result = routes.get(&dst_2.to_string());
        assert_eq!(result, None);
    }

    #[test]
    fn contains_route() {
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
        routes.add(&dst.to_string(), &next.to_string());
        let result = routes.contains(&dst.to_string());
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
        routes.add(&dst_1.to_string(), &next_1.to_string());
        routes.add(&dst_2.to_string(), &next_2.to_string());
        routes.add(&dst_3.to_string(), &next_3.to_string());
        let routes = routes.to_vec();
        let expected_routes = vec![
            Route {
                dst: "btp://0x1.icon/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string(),
                next:
                    "btp://0x1.bsc/88bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4"
                        .to_string(),
            },
            Route {
                dst: "btp://0x1.pra/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string(),
                next: "btp://0x3.iconee/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string(),
            },
            Route {
                dst: "btp://0x5.pra/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string(),
                next: "btp://0x3.iconee/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string(),
            },
        ];
        let result: HashSet<_> = routes.iter().collect();
        let expected: HashSet<_> = expected_routes.iter().collect();
        assert_eq!(result, expected);
    }
}
