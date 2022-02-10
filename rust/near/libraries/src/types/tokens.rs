use crate::types::{token::TokenMetadata, Token, TokenId};
use near_sdk::borsh::{self, BorshDeserialize, BorshSerialize};
use near_sdk::collections::{LookupMap, UnorderedSet};
use near_sdk::serde::Serialize;
// use std::collections::HashMap;

#[derive(Debug, Clone, PartialEq, PartialOrd, Ord, Eq, Serialize, Hash)]
#[serde(crate = "near_sdk::serde")]
pub struct TokenItem {
    pub name: String,
    pub network: String,
    pub symbol: String,
}

#[derive(BorshDeserialize, BorshSerialize)]
pub struct Tokens<T: TokenMetadata> {
    list: UnorderedSet<TokenId>,
    metadata: Metadata<T>,
    // supply: HashMap<TokenId, u128>,
}

#[derive(BorshDeserialize, BorshSerialize)]
pub struct Metadata<T: TokenMetadata>(LookupMap<TokenId, Token<T>>);

impl<T: BorshDeserialize + BorshSerialize + TokenMetadata> Metadata<T> {
    fn new() -> Self {
        Self(LookupMap::new(b"tokens_metadata".to_vec()))
    }

    fn add(&mut self, token_id: &TokenId, token: &Token<T>) {
        self.0.insert(token_id, token);
    }

    fn remove(&mut self, token_id: &TokenId) {
        self.0.remove(token_id);
    }

    fn get(&self, token_id: &TokenId) -> Option<Token<T>> {
        if let Some(token) = self.0.get(token_id) {
            return Some(token);
        }
        None
    }
}

impl<T: BorshDeserialize + BorshSerialize + TokenMetadata> Tokens<T> {
    pub fn new() -> Self {
        Self {
            list: UnorderedSet::new(b"tokens_list".to_vec()),
            metadata: Metadata::new(),
            // supply: HashMap::new(),
        }
    }

    pub fn add(&mut self, token_id: &TokenId, token: &Token<T>) {
        self.list.insert(token_id);
        self.metadata.add(token_id, token);
    }

    pub fn remove(&mut self, token_id: &TokenId) {
        self.list.remove(token_id);
        self.metadata.remove(token_id);
    }

    pub fn contains(&self, token_id: &TokenId) -> bool {
        self.list.contains(token_id)
    }

    pub fn get(&self, token_id: &TokenId) -> Option<Token<T>> {
        self.metadata.get(token_id)
    }

    pub fn set(&mut self, token_id: &TokenId, token: &Token<T>) {
        self.metadata.add(token_id, token)
    }

    pub fn to_vec(&self) -> Vec<TokenItem> {
        self.list
            .to_vec()
            .iter()
            .map(|token_id| {
                let metdata = self.metadata.get(token_id).unwrap();
                TokenItem {
                    name: metdata.name().clone(),
                    network: metdata.network().clone(),
                    symbol: metdata.symbol().clone(),
                }
            })
            .collect::<Vec<TokenItem>>()
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::types::WrappedNativeCoin;
    use near_sdk::{serde_json, testing_env, VMContext};
    use std::{collections::HashSet};

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
    fn add_token() {
        let context = get_context(vec![], false);
        testing_env!(context);
        let mut tokens = Tokens::new();
        let native_coin = WrappedNativeCoin::new(
            "ABC Token".to_string(),
            "ABC".to_string(),
            None,
            "0x1.near".to_string(),
            None
        );

        tokens.add(
            &"ABC Token".to_string().as_bytes().to_vec(),
            &<Token<WrappedNativeCoin>>::new(native_coin.clone()),
        );

        let result = tokens.contains(&"ABC Token".to_string().as_bytes().to_vec());
        assert_eq!(result, true);

        let result = tokens.get(&"ABC Token".to_string().as_bytes().to_vec());
        assert_eq!(result, Some(<Token<WrappedNativeCoin>>::new(native_coin)));
    }

    #[test]
    fn add_existing_token() {
        let context = get_context(vec![], false);
        testing_env!(context);
        let mut tokens = Tokens::new();
        let native_coin = WrappedNativeCoin::new(
            "ABC Token".to_string(),
            "ABC".to_string(),
            None,
            "0x1.near".to_string(),
            None
        );

        tokens.add(&"ABC Token".to_string().as_bytes().to_vec(), &<Token<WrappedNativeCoin>>::new(native_coin.clone()));
        tokens.add(&"ABC Token".to_string().as_bytes().to_vec(), &<Token<WrappedNativeCoin>>::new(native_coin.clone()));
        
        let result = tokens.to_vec();

        let expected: Vec<TokenItem> = vec![TokenItem {
            name: "ABC Token".to_string(),
            network: "0x1.near".to_string(),
            symbol: "ABC".to_string(),
        }];
        assert_eq!(result, expected);
    }

    #[test]
    fn remove_token() {
        let context = get_context(vec![], false);
        testing_env!(context);
        let mut tokens = Tokens::new();
        let native_coin = WrappedNativeCoin::new(
           "ABC Token".to_string(),
            "ABC".to_string(),
            None,
            "0x1.near".to_string(),
            None
        );

        tokens.add(&"ABC Token".to_string().as_bytes().to_vec(), &<Token<WrappedNativeCoin>>::new(native_coin.clone()));

        tokens.remove(&"ABC Token".to_string().as_bytes().to_vec());
        let result = tokens.contains(&"ABC Token".to_string().as_bytes().to_vec());
        assert_eq!(result, false);

        let result = tokens.get(&"ABC Token".to_string().as_bytes().to_vec());
        assert_eq!(result, None);
    }

    #[test]
    fn remove_token_non_existing() {
        let context = get_context(vec![], false);
        testing_env!(context);
        let mut tokens = <Tokens<WrappedNativeCoin>>::new();
        tokens.remove(&"ABC Token".to_string().as_bytes().to_vec());
        let result = tokens.contains(&"ABC Token".to_string().as_bytes().to_vec());
        assert_eq!(result, false);
    }

    #[test]
    fn to_vec_tokens() {
        let context = get_context(vec![], false);
        testing_env!(context);
        let mut tokens = <Tokens<WrappedNativeCoin>>::new();
        let native_coin_1 = WrappedNativeCoin::new(
            "ABC Token".to_string(),
            "ABC".to_string(),
            None,
            "0x1.near".to_string(),
            None
        );
        let native_coin_2 = WrappedNativeCoin::new(
            "DEF Token".to_string(),
            "DEF".to_string(),
            None,
            "0x1.bsc".to_string(),
            None
        );

        tokens.add(
            &"ABC Token".to_string().as_bytes().to_vec(),
            &<Token<WrappedNativeCoin>>::new(native_coin_1),
        );
        tokens.add(
            &"DEF Token".to_string().as_bytes().to_vec(),
            &<Token<WrappedNativeCoin>>::new(native_coin_2),
        );
        let tokens = tokens.to_vec();
        let expected_tokens: Vec<TokenItem> = vec![
            TokenItem {
                name: "ABC Token".to_string(),
                network: "0x1.near".to_string(),
                symbol: "ABC".to_string(),
            },
            TokenItem {
                name: "DEF Token".to_string(),
                network: "0x1.bsc".to_string(),
                symbol: "DEF".to_string(),
            },
        ];
        let result: HashSet<_> = tokens.iter().collect();
        let expected: HashSet<_> = expected_tokens.iter().collect();
        assert_eq!(result, expected);
    }

    #[test]
    fn to_vec_tokens_value() {
        let context = get_context(vec![], false);
        testing_env!(context);
        let mut tokens = <Tokens<WrappedNativeCoin>>::new();
        let native_coin = WrappedNativeCoin::new(
            "ABC Token".to_string(),
            "ABC".to_string(),
            None,
            "0x1.near".to_string(),
            None
        );
        tokens.add(
            &"ABC Token".to_string().as_bytes().to_vec(),
            &<Token<WrappedNativeCoin>>::new(native_coin),
        );
        let tokens = serde_json::to_value(tokens.to_vec()).unwrap();
        assert_eq!(
            tokens,
            serde_json::json!(
                [
                    {
                        "name": "ABC Token",
                        "network": "0x1.near",
                        "symbol": "ABC"
                    }
                ]
            )
        );
    }
}
