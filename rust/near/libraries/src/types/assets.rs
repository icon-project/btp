use crate::types::{asset::AssetMetadata, Asset, AssetId};
use near_sdk::borsh::{self, BorshDeserialize, BorshSerialize};
use near_sdk::collections::{LookupMap, UnorderedSet};
use near_sdk::serde::Serialize;
// use std::collections::HashMap;

#[derive(Debug, Clone, PartialEq, PartialOrd, Ord, Eq, Serialize, Hash)]
#[serde(crate = "near_sdk::serde")]
pub struct AssetItem {
    pub name: String,
    pub network: String,
    pub symbol: String,
}

#[derive(BorshDeserialize, BorshSerialize)]
pub struct Assets<T: AssetMetadata> {
    list: UnorderedSet<AssetId>,
    metadata: Metadata<T>,
}

#[derive(BorshDeserialize, BorshSerialize)]
pub struct Metadata<T: AssetMetadata>(LookupMap<AssetId, Asset<T>>);

impl<T: BorshDeserialize + BorshSerialize + AssetMetadata> Metadata<T> {
    fn new() -> Self {
        Self(LookupMap::new(b"tokens_metadata".to_vec()))
    }

    fn add(&mut self, asset_id: &AssetId, asset: &Asset<T>) {
        self.0.insert(asset_id, asset);
    }

    fn remove(&mut self, asset_id: &AssetId) {
        self.0.remove(asset_id);
    }

    fn get(&self, asset_id: &AssetId) -> Option<Asset<T>> {
        if let Some(asset) = self.0.get(asset_id) {
            return Some(asset);
        }
        None
    }
}

impl<T: BorshDeserialize + BorshSerialize + AssetMetadata> Assets<T> {
    pub fn new() -> Self {
        Self {
            list: UnorderedSet::new(b"tokens_list".to_vec()),
            metadata: Metadata::new(),
            // supply: HashMap::new(),
        }
    }

    pub fn add(&mut self, asset_id: &AssetId, asset: &Asset<T>) {
        self.list.insert(asset_id);
        self.metadata.add(asset_id, asset);
    }

    pub fn remove(&mut self, asset_id: &AssetId) {
        self.list.remove(asset_id);
        self.metadata.remove(asset_id);
    }

    pub fn contains(&self, asset_id: &AssetId) -> bool {
        self.list.contains(asset_id)
    }

    pub fn get(&self, asset_id: &AssetId) -> Option<Asset<T>> {
        self.metadata.get(asset_id)
    }

    pub fn set(&mut self, asset_id: &AssetId, asset: &Asset<T>) {
        self.metadata.add(asset_id, asset)
    }

    pub fn to_vec(&self) -> Vec<AssetItem> {
        self.list
            .to_vec()
            .iter()
            .map(|asset_id| {
                let metdata = self.metadata.get(asset_id).unwrap();
                AssetItem {
                    name: metdata.name().clone(),
                    network: metdata.network().clone(),
                    symbol: metdata.symbol().clone(),
                }
            })
            .collect::<Vec<AssetItem>>()
    }
}

#[cfg(test)]
mod tests {
    use crate::types::{asset::*,assets::*};
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
        let mut tokens = Assets::new();
        let native_coin = WrappedNativeCoin::new(
            "ABC Asset".to_string(),
            "ABC".to_string(),
            None,
            "0x1.near".to_string(),
            None
        );

        tokens.add(
            &"ABC Asset".to_string().as_bytes().to_vec(),
            &<Asset<WrappedNativeCoin>>::new(native_coin.clone()),
        );

        let result = tokens.contains(&"ABC Asset".to_string().as_bytes().to_vec());
        assert_eq!(result, true);

        let result = tokens.get(&"ABC Asset".to_string().as_bytes().to_vec());
        assert_eq!(result, Some(<Asset<WrappedNativeCoin>>::new(native_coin)));
    }

    #[test]
    fn add_existing_token() {
        let context = get_context(vec![], false);
        testing_env!(context);
        let mut tokens = Assets::new();
        let native_coin = WrappedNativeCoin::new(
            "ABC Asset".to_string(),
            "ABC".to_string(),
            None,
            "0x1.near".to_string(),
            None
        );

        tokens.add(&"ABC Asset".to_string().as_bytes().to_vec(), &<Asset<WrappedNativeCoin>>::new(native_coin.clone()));
        tokens.add(&"ABC Asset".to_string().as_bytes().to_vec(), &<Asset<WrappedNativeCoin>>::new(native_coin.clone()));
        
        let result = tokens.to_vec();

        let expected: Vec<AssetItem> = vec![AssetItem {
            name: "ABC Asset".to_string(),
            network: "0x1.near".to_string(),
            symbol: "ABC".to_string(),
        }];
        assert_eq!(result, expected);
    }

    #[test]
    fn remove_token() {
        let context = get_context(vec![], false);
        testing_env!(context);
        let mut tokens = Assets::new();
        let native_coin = WrappedNativeCoin::new(
           "ABC Asset".to_string(),
            "ABC".to_string(),
            None,
            "0x1.near".to_string(),
            None
        );

        tokens.add(&"ABC Asset".to_string().as_bytes().to_vec(), &<Asset<WrappedNativeCoin>>::new(native_coin.clone()));

        tokens.remove(&"ABC Asset".to_string().as_bytes().to_vec());
        let result = tokens.contains(&"ABC Asset".to_string().as_bytes().to_vec());
        assert_eq!(result, false);

        let result = tokens.get(&"ABC Asset".to_string().as_bytes().to_vec());
        assert_eq!(result, None);
    }

    #[test]
    fn remove_token_non_existing() {
        let context = get_context(vec![], false);
        testing_env!(context);
        let mut tokens = <Assets<WrappedNativeCoin>>::new();
        tokens.remove(&"ABC Asset".to_string().as_bytes().to_vec());
        let result = tokens.contains(&"ABC Asset".to_string().as_bytes().to_vec());
        assert_eq!(result, false);
    }

    #[test]
    fn to_vec_tokens() {
        let context = get_context(vec![], false);
        testing_env!(context);
        let mut tokens = <Assets<WrappedNativeCoin>>::new();
        let native_coin_1 = WrappedNativeCoin::new(
            "ABC Asset".to_string(),
            "ABC".to_string(),
            None,
            "0x1.near".to_string(),
            None
        );
        let native_coin_2 = WrappedNativeCoin::new(
            "DEF Asset".to_string(),
            "DEF".to_string(),
            None,
            "0x1.bsc".to_string(),
            None
        );

        tokens.add(
            &"ABC Asset".to_string().as_bytes().to_vec(),
            &<Asset<WrappedNativeCoin>>::new(native_coin_1),
        );
        tokens.add(
            &"DEF Asset".to_string().as_bytes().to_vec(),
            &<Asset<WrappedNativeCoin>>::new(native_coin_2),
        );
        let tokens = tokens.to_vec();
        let expected_tokens: Vec<AssetItem> = vec![
            AssetItem {
                name: "ABC Asset".to_string(),
                network: "0x1.near".to_string(),
                symbol: "ABC".to_string(),
            },
            AssetItem {
                name: "DEF Asset".to_string(),
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
        let mut tokens = <Assets<WrappedNativeCoin>>::new();
        let native_coin = WrappedNativeCoin::new(
            "ABC Asset".to_string(),
            "ABC".to_string(),
            None,
            "0x1.near".to_string(),
            None
        );
        tokens.add(
            &"ABC Asset".to_string().as_bytes().to_vec(),
            &<Asset<WrappedNativeCoin>>::new(native_coin),
        );
        let tokens = serde_json::to_value(tokens.to_vec()).unwrap();
        assert_eq!(
            tokens,
            serde_json::json!(
                [
                    {
                        "name": "ABC Asset",
                        "network": "0x1.near",
                        "symbol": "ABC"
                    }
                ]
            )
        );
    }
}
