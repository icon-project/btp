use crate::types::{btp_address::Network, token::TokenMetadata, Token, TokenName};
use near_sdk::borsh::{self, BorshDeserialize, BorshSerialize};
use near_sdk::collections::LookupMap;
use near_sdk::AccountId;
use near_sdk::Balance;
use near_sdk::serde::{Serialize, Deserialize};
#[derive(Debug, Default, BorshDeserialize, BorshSerialize, PartialEq, Eq, Clone, Serialize, Deserialize)]
pub struct AccountBalance {
    deposit: Balance,
    locked: Balance,
    refundable: Refundable,
    storage: Balance,
}

#[derive(Debug, Default, BorshDeserialize, BorshSerialize, PartialEq, Eq, Clone, Serialize, Deserialize)]
pub struct Refundable {
    deposit: Balance,
    fees: Balance,
}

impl Refundable {
    pub fn deposit(&self) -> Balance {
        self.deposit
    }

    pub fn deposit_mut(&mut self) -> &mut Balance {
        &mut self.deposit
    }

    pub fn fees(&self) -> Balance {
        self.fees
    }

    pub fn fees_mut(&mut self) -> &mut Balance {
        &mut self.fees
    }
}

impl AccountBalance {
    pub fn deposit(&self) -> Balance {
        self.deposit
    }

    pub fn locked(&self) -> Balance {
        self.locked
    }

    pub fn refundable(&self) -> &Refundable {
        &self.refundable
    }

    pub fn storage(&self) -> Balance {
        self.storage
    }

    pub fn deposit_mut(&mut self) -> &mut Balance {
        &mut self.deposit
    }

    pub fn locked_mut(&mut self) -> &mut Balance {
        &mut self.locked
    }

    pub fn refundable_mut(&mut self) -> &mut Refundable {
        &mut self.refundable
    }

    pub fn storage_mut(&mut self) -> &mut Balance {
        &mut self.storage
    }
}

pub trait Transfer {
    fn add(&mut self, rhs: u128) -> &mut Self;
    fn sub(&mut self, rhs: u128) -> &mut Self;
    fn mul(&mut self, rhs: u128) -> &mut Self;
    fn div(&mut self, rhs: u128) -> &mut Self;
}

// TODO: Return result
impl Transfer for Balance {
    fn add(&mut self, rhs: u128) -> &mut Self {
        self.clone_from(&&self.checked_add(rhs).unwrap());
        self
    }

    fn sub(&mut self, rhs: u128) -> &mut Self {
        self.clone_from(&&self.checked_sub(rhs).unwrap());
        self
    }

    fn mul(&mut self, rhs: u128) -> &mut Self {
        self.clone_from(&&self.checked_mul(rhs).unwrap());
        self
    }

    fn div(&mut self, rhs: u128) -> &mut Self {
        self.clone_from(&&self.checked_div(rhs).unwrap());
        self
    }
}

#[derive(BorshDeserialize, BorshSerialize)]
pub struct Balances(LookupMap<(AccountId, TokenName, Network), AccountBalance>);

impl Balances {
    pub fn new() -> Self {
        Self(LookupMap::new(b"balances".to_vec()))
    }

    pub fn add<T>(&mut self, account: AccountId, token: Token<T>)
    where
        T: TokenMetadata,
    {
        self.0.insert(
            &(
                account,
                token.metadata.name().to_owned(),
                token.metadata.network().to_owned(),
            ),
            &AccountBalance::default(),
        );
    }

    pub fn remove<T>(&mut self, account: AccountId, token: Token<T>)
    where
        T: TokenMetadata,
    {
        self.0.remove(&(
            account,
            token.metadata.name().to_owned(),
            token.metadata.network().to_owned(),
        ));
    }

    pub fn get<T>(&self, account: &AccountId, token: &Token<T>) -> Option<AccountBalance>
    where
        T: TokenMetadata,
    {
        if let Some(balance) = self.0.get(&(
            account.clone(),
            token.metadata.name().to_owned(),
            token.metadata.network().to_owned(),
        )) {
            return Some(balance);
        }
        None
    }

    pub fn set<T>(&mut self, account: AccountId, token: Token<T>, account_balance: AccountBalance)
    where
        T: TokenMetadata,
    {
        self.0.insert(
            &(
                account,
                token.metadata.name().to_owned(),
                token.metadata.network().to_owned(),
            ),
            &account_balance,
        );
    }
}
