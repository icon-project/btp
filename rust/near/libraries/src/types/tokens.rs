use near_sdk::borsh::{self, BorshDeserialize, BorshSerialize};
use near_sdk::serde::{Deserialize, Serialize};
use near_sdk::collections::UnorderedMap;
use crate::types::{Token, token::TokenMetadata, TokenId};

pub struct Item {
    pub name: String,
    pub network: String,
}

#[derive(BorshDeserialize, BorshSerialize)]
pub struct Tokens<T: BorshDeserialize + BorshSerialize + TokenMetadata>(UnorderedMap<TokenId, Token<T>>);

impl<T: BorshDeserialize + BorshSerialize + TokenMetadata> Tokens<T> {
    pub fn new() -> Self {
        Self(UnorderedMap::new(b"tokens".to_vec()))
    }

    pub fn add(&mut self, token_id: TokenId, token: Token<T>)
    where T: TokenMetadata
    {
        // TODO: Propose to make this a hashed string token name + network 
        self.0.insert(
            &token_id,
            &token,
        );
    }

    pub fn remove(&mut self, token_id: &TokenId)
    {
        self.0.remove(token_id);
    }

    pub fn get(&self, token_id: &TokenId) -> Option<Token<T>> {
        if let Some(token) = self.0.get(token_id) {
            return Some(token);
        }
        None
    }

    pub fn contains(&self, token_id: &TokenId) -> bool {
        return self.0.get(token_id).is_some();
    }

    pub fn to_vec(&self) -> Vec<Item> {
        if !self.0.is_empty() {
            return self
                .0
                .iter()
                .map(|v| Item {
                    name: v.1.name().clone(),
                    network: v.1.network().clone(),
                })
                .collect();
        }
        vec![]
    }
}