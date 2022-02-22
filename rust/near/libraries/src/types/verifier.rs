use btp_common::errors::BmvError;
use near_sdk::borsh::{self, BorshDeserialize, BorshSerialize};
use near_sdk::collections::UnorderedMap;
use near_sdk::serde::{Deserialize, Serialize};
use near_sdk::AccountId;

use super::messages::SerializedBtpMessages;

#[derive(BorshDeserialize, BorshSerialize)]
pub struct Bmv(UnorderedMap<String, AccountId>);

#[derive(Serialize, Debug, Eq, PartialEq, Hash, Deserialize)]
pub struct Verifier {
    pub network: String,
    pub verifier: super::AccountId,
}

#[derive(
    Debug, Default, BorshDeserialize, BorshSerialize, Eq, PartialEq, Serialize, Deserialize,
)]
pub struct VerifierStatus {
    mta_height: u64,
    mta_offset: u64,
    last_height: u64,
}

impl VerifierStatus {
    pub fn new(mta_height: u64, mta_offset: u64, last_height: u64) -> Self {
        Self {
            mta_height,
            mta_offset,
            last_height,
        }
    }

    pub fn mta_height(&self) -> u64 {
        self.mta_height
    }

    pub fn mta_offset(&self) -> u64 {
        self.mta_offset
    }

    pub fn last_height(&self) -> u64 {
        self.last_height
    }
}

#[derive(Debug, Eq, PartialEq, Serialize, Deserialize)]
pub struct VerifierResponse {
    pub previous_height: u64,
    pub verifier_status: VerifierStatus,
    pub messages: SerializedBtpMessages,
}

impl Bmv {
    pub fn new() -> Self {
        Self(UnorderedMap::new(b"verifiers".to_vec()))
    }

    pub fn add(&mut self, network: &str, verifier: &AccountId) {
        self.0.insert(&network.to_string(), verifier);
    }

    pub fn remove(&mut self, network: &str) {
        self.0.remove(&network.to_string());
    }

    pub fn get(&self, network: &str) -> Option<AccountId> {
        if let Some(verifier) = self.0.get(&network.to_string()) {
            return Some(verifier);
        }
        None
    }

    pub fn contains(&self, network: &str) -> bool {
        self.0.get(&network.to_string()).is_some()
    }

    pub fn to_vec(&self) -> Vec<Verifier> {
        if !self.0.is_empty() {
            return self
                .0
                .iter()
                .map(|v| Verifier {
                    network: v.0,
                    verifier: v.1,
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
    fn add_verifier() {
        let context = get_context(vec![], false);
        testing_env!(context);
        let network = "icon";
        let verifier = "88bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4"
            .parse::<AccountId>()
            .unwrap();
        let mut verifiers = Bmv::new();
        verifiers.add(network, &verifier);
        let result = verifiers.get(&network);
        assert_eq!(result, Some(verifier));
    }

    #[test]
    fn get_verifier() {
        let context = get_context(vec![], false);
        testing_env!(context);
        let network_1 = "icon";
        let verifier_1 = "88bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4"
            .parse::<AccountId>()
            .unwrap();
        let network_2 = "binance";
        let verifier_2 = "88bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4"
            .parse::<AccountId>()
            .unwrap();
        let network_3 = "polkadot";
        let verifier_3 = "88bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4"
            .parse::<AccountId>()
            .unwrap();
        let mut verifiers = Bmv::new();
        verifiers.add(network_1, &verifier_1);
        verifiers.add(network_2, &verifier_2);
        verifiers.add(network_3, &verifier_3);
        let result = verifiers.get(network_2);
        assert_eq!(
            result,
            Some(
                "88bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4"
                    .parse::<AccountId>()
                    .unwrap()
            )
        );
    }

    #[test]
    fn remove_verifier() {
        let context = get_context(vec![], false);
        testing_env!(context);
        let network_1 = "icon";
        let verifier_1 = "88bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4"
            .parse::<AccountId>()
            .unwrap();
        let network_2 = "binance";
        let verifier_2 = "88bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4"
            .parse::<AccountId>()
            .unwrap();
        let network_3 = "polkadot";
        let verifier_3 = "88bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4"
            .parse::<AccountId>()
            .unwrap();
        let mut verifiers = Bmv::new();
        verifiers.add(&network_1, &verifier_1);
        verifiers.add(&network_2, &verifier_2);
        verifiers.add(&network_3, &verifier_3);
        verifiers.remove(&network_2);
        let result = verifiers.get(&network_2);
        assert_eq!(result, None);
    }

    #[test]
    fn contains_verifier() {
        let context = get_context(vec![], false);
        testing_env!(context);
        let network = "binance";
        let verifier = "88bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4"
            .parse::<AccountId>()
            .unwrap();
        let mut verifiers = Bmv::new();
        verifiers.add(network, &verifier);
        let result = verifiers.contains(network);
        assert_eq!(result, true);
    }

    #[test]
    fn to_vec_verifier() {
        let context = get_context(vec![], false);
        testing_env!(context);
        let network_1 = "icon";
        let verifier_1 = "88bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4"
            .parse::<AccountId>()
            .unwrap();
        let network_2 = "binance";
        let verifier_2 = "88bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4"
            .parse::<AccountId>()
            .unwrap();
        let network_3 = "polkadot";
        let verifier_3 = "88bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4"
            .parse::<AccountId>()
            .unwrap();
        let mut verifiers = Bmv::new();
        verifiers.add(network_1, &verifier_1);
        verifiers.add(network_2, &verifier_2);
        verifiers.add(network_3, &verifier_3);
        let verifiers = verifiers.to_vec();
        let expected_verifiers = vec![
            Verifier {
                network: "icon".to_string(),
                verifier: "88bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4"
                    .parse::<AccountId>()
                    .unwrap(),
            },
            Verifier {
                network: "binance".to_string(),
                verifier: "88bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4"
                    .parse::<AccountId>()
                    .unwrap(),
            },
            Verifier {
                network: "polkadot".to_string(),
                verifier: "88bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4"
                    .parse::<AccountId>()
                    .unwrap(),
            },
        ];
        let result: HashSet<_> = verifiers.iter().collect();
        let expected: HashSet<_> = expected_verifiers.iter().collect();
        assert_eq!(result, expected);
    }
}
