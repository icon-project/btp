use crate::types::TokenId;
use near_sdk::borsh::{self, BorshDeserialize, BorshSerialize};
use near_sdk::collections::LookupMap;
use near_sdk::serde::{Deserialize, Serialize};
use near_sdk::AccountId;
use near_sdk::Balance;

#[derive(
    Debug, Default, BorshDeserialize, BorshSerialize, PartialEq, Eq, Clone, Serialize, Deserialize,
)]
#[serde(crate = "near_sdk::serde")]
pub struct AccountBalance {
    deposit: Balance,
    refundable: Balance,
    locked: Balance,
}

impl AccountBalance {
    pub fn deposit(&self) -> Balance {
        self.deposit
    }

    pub fn locked(&self) -> Balance {
        self.locked
    }

    pub fn refundable(&self) -> Balance {
        self.refundable
    }

    pub fn deposit_mut(&mut self) -> &mut Balance {
        &mut self.deposit
    }

    pub fn locked_mut(&mut self) -> &mut Balance {
        &mut self.locked
    }

    pub fn refundable_mut(&mut self) -> &mut Balance {
        &mut self.refundable
    }
}

pub trait Transfer {
    fn add(&mut self, rhs: u128) -> Result<&mut Self, String>;
    fn sub(&mut self, rhs: u128) -> Result<&mut Self, String>;
    fn mul(&mut self, rhs: u128) -> Result<&mut Self, String>;
    fn div(&mut self, rhs: u128) -> Result<&mut Self, String>;
}

impl Transfer for Balance {
    fn add(&mut self, rhs: u128) -> Result<&mut Self, String> {
        self.clone_from(
            &&self
                .checked_add(rhs)
                .ok_or_else(|| "overflow occured".to_string())?,
        );
        Ok(self)
    }

    fn sub(&mut self, rhs: u128) -> Result<&mut Self, String> {
        self.clone_from(
            &&self
                .checked_sub(rhs)
                .ok_or_else(|| "underflow occured".to_string())?,
        );
        Ok(self)
    }

    fn mul(&mut self, rhs: u128) -> Result<&mut Self, String> {
        self.clone_from(
            &self
                .checked_mul(rhs)
                .ok_or_else(|| "overflow occured".to_string())?,
        );
        Ok(self)
    }

    fn div(&mut self, rhs: u128) -> Result<&mut Self, String> {
        self.clone_from(
            &&self
                .checked_div(rhs)
                .ok_or_else(|| "underflow occured".to_string())?,
        );
        Ok(self)
    }
}

#[derive(BorshDeserialize, BorshSerialize)]
pub struct Balances(LookupMap<(AccountId, TokenId), AccountBalance>);

impl Balances {
    pub fn new() -> Self {
        Self(LookupMap::new(b"balances".to_vec()))
    }

    pub fn add(&mut self, account: &AccountId, token_id: &TokenId) {
        if !self.contains(account, token_id) {
            self.0.insert(
                &(account.to_owned(), token_id.to_owned()),
                &AccountBalance::default(),
            );
        }
    }

    pub fn remove(&mut self, account: &AccountId, token_id: &TokenId) {
        self.0.remove(&(account.to_owned(), token_id.to_owned()));
    }

    pub fn get(&self, account: &AccountId, token_id: &TokenId) -> Option<AccountBalance> {
        if let Some(balance) = self.0.get(&(account.to_owned(), token_id.to_owned())) {
            return Some(balance);
        }
        None
    }

    pub fn contains(&self, account: &AccountId, token_id: &TokenId) -> bool {
        return self
            .0
            .contains_key(&(account.to_owned(), token_id.to_owned()));
    }

    pub fn set(
        &mut self,
        account: &AccountId,
        token_id: &TokenId,
        account_balance: AccountBalance,
    ) {
        self.0
            .insert(&(account.to_owned(), token_id.to_owned()), &account_balance);
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use near_sdk::AccountId;
    use near_sdk::{testing_env, VMContext};
    use std::vec;

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
    fn add_balance() {
        let context = get_context(vec![], false);
        testing_env!(context);
        let mut balances = Balances::new();
        let account = "88bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4"
            .parse::<AccountId>()
            .unwrap();
        balances.add(&account, &"ABC Token".to_string().as_bytes().to_vec());
        let result = balances.contains(&account, &"ABC Token".to_string().as_bytes().to_vec());
        assert_eq!(result, true);
    }

    #[test]
    fn add_balance_exisitng() {
        let context = get_context(vec![], false);
        testing_env!(context);
        let mut balances = Balances::new();
        let account = "88bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4"
            .parse::<AccountId>()
            .unwrap();

        balances.add(&account, &"ABC Token".to_string().as_bytes().to_vec());

        let mut account_balance = balances
            .get(&account, &"ABC Token".to_string().as_bytes().to_vec())
            .unwrap();
        account_balance.deposit_mut().add(1000).unwrap();

        balances.set(
            &account,
            &"ABC Token".to_string().as_bytes().to_vec(),
            account_balance,
        );
        balances.add(&account, &"ABC Token".to_string().as_bytes().to_vec());

        let result = balances
            .get(&account, &"ABC Token".to_string().as_bytes().to_vec())
            .unwrap();
        assert_eq!(result.deposit(), 1000);
    }

    #[test]
    fn remove_balance() {
        let context = get_context(vec![], false);
        testing_env!(context);
        let mut balances = Balances::new();
        let account = "88bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4"
            .parse::<AccountId>()
            .unwrap();

        balances.add(&account, &"ABC Token".to_string().as_bytes().to_vec());
        balances.remove(&account, &"ABC Token".to_string().as_bytes().to_vec());

        let result = balances.contains(&account, &"ABC Token".to_string().as_bytes().to_vec());
        assert_eq!(result, false);
    }

    #[test]
    fn remove_balance_non_exisitng() {
        let context = get_context(vec![], false);
        testing_env!(context);
        let mut balances = Balances::new();
        let account = "88bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4"
            .parse::<AccountId>()
            .unwrap();

        balances.remove(&account, &"ABC Token".to_string().as_bytes().to_vec());

        let result = balances.contains(&account, &"ABC Token".to_string().as_bytes().to_vec());
        assert_eq!(result, false);
    }

    #[test]
    fn set_balance() {
        let context = get_context(vec![], false);
        testing_env!(context);
        let mut balances = Balances::new();
        let account = "88bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4"
            .parse::<AccountId>()
            .unwrap();

        balances.add(&account, &"ABC Token".to_string().as_bytes().to_vec());

        let mut account_balance = balances
            .get(&account, &"ABC Token".to_string().as_bytes().to_vec())
            .unwrap();
        account_balance.deposit_mut().add(1000).unwrap();

        balances.set(
            &account,
            &"ABC Token".to_string().as_bytes().to_vec(),
            account_balance,
        );

        let result = balances
            .get(&account, &"ABC Token".to_string().as_bytes().to_vec())
            .unwrap();
        assert_eq!(result.deposit(), 1000);
    }

    #[test]
    fn deposit_add() {
        let context = get_context(vec![], false);
        testing_env!(context);
        let mut balances = Balances::new();
        let account = "88bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4"
            .parse::<AccountId>()
            .unwrap();
        balances.add(&account, &"ABC Token".to_string().as_bytes().to_vec());

        let mut account_balance = balances
            .get(&account, &"ABC Token".to_string().as_bytes().to_vec())
            .unwrap();
        account_balance.deposit_mut().add(1000).unwrap();

        balances.set(
            &account,
            &"ABC Token".to_string().as_bytes().to_vec(),
            account_balance,
        );

        let result = balances
            .get(&account, &"ABC Token".to_string().as_bytes().to_vec())
            .unwrap();
        assert_eq!(result.deposit(), 1000);
    }

    #[test]
    fn deposit_add_overflow() {
        let context = get_context(vec![], false);
        testing_env!(context);
        let mut balances = Balances::new();
        let account = "88bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4"
            .parse::<AccountId>()
            .unwrap();
        balances.add(&account, &"ABC Token".to_string().as_bytes().to_vec());

        let mut account_balance = balances
            .get(&account, &"ABC Token".to_string().as_bytes().to_vec())
            .unwrap();
        account_balance.deposit_mut().add(u128::MAX).unwrap();

        assert_eq!(
            account_balance.deposit_mut().add(1),
            Err("overflow occured".to_string())
        )
    }

    #[test]
    fn deposit_sub_underflow() {
        let context = get_context(vec![], false);
        testing_env!(context);
        let mut balances = Balances::new();
        let account = "88bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4"
            .parse::<AccountId>()
            .unwrap();
        balances.add(&account, &"ABC Token".to_string().as_bytes().to_vec());
        let mut account_balance = balances
            .get(&account, &"ABC Token".to_string().as_bytes().to_vec())
            .unwrap();

        assert_eq!(
            account_balance.deposit_mut().sub(1),
            Err("underflow occured".to_string())
        )
    }

    #[test]
    fn locked_balance_add() {
        let context = get_context(vec![], false);
        testing_env!(context);
        let mut balances = Balances::new();
        let account = "88bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4"
            .parse::<AccountId>()
            .unwrap();
        balances.add(&account, &"ABC Token".to_string().as_bytes().to_vec());

        let mut account_balance = balances
            .get(&account, &"ABC Token".to_string().as_bytes().to_vec())
            .unwrap();

        account_balance
            .locked_mut()
            .add(1000)
            .unwrap();

        balances.set(
            &account,
            &"ABC Token".to_string().as_bytes().to_vec(),
            account_balance,
        );

        let result = balances
            .get(&account, &"ABC Token".to_string().as_bytes().to_vec())
            .unwrap();

        assert_eq!(result.locked(), 1000);
    }

    #[test]
    fn locked_balance_sub() {
        let context = get_context(vec![], false);
        testing_env!(context);
        let mut balances = Balances::new();
        let account = "88bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4"
            .parse::<AccountId>()
            .unwrap();

        balances.add(&account, &"ABC Token".to_string().as_bytes().to_vec());

        let mut account_balance = balances
            .get(&account, &"ABC Token".to_string().as_bytes().to_vec())
            .unwrap();

        account_balance
            .locked_mut()
            .add(1000)
            .unwrap();

        balances.set(
            &account,
            &"ABC Token".to_string().as_bytes().to_vec(),
            account_balance,
        );

        let result = balances
            .get(&account, &"ABC Token".to_string().as_bytes().to_vec())
            .unwrap();

        assert_eq!(result.locked(), 1000);

        let mut account_balance = balances
            .get(&account, &"ABC Token".to_string().as_bytes().to_vec())
            .unwrap();

        account_balance.locked_mut().sub(1).unwrap();

        balances.set(
            &account,
            &"ABC Token".to_string().as_bytes().to_vec(),
            account_balance,
        );

        let result = balances
            .get(&account, &"ABC Token".to_string().as_bytes().to_vec())
            .unwrap();

        assert_eq!(result.locked(), 999);
    }

    #[test]
    fn refundable_balance_add() {
        let context = get_context(vec![], false);
        testing_env!(context);
        let mut balances = Balances::new();
        let account = "88bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4"
            .parse::<AccountId>()
            .unwrap();
        balances.add(&account, &"ABC Token".to_string().as_bytes().to_vec());

        let mut account_balance = balances
            .get(&account, &"ABC Token".to_string().as_bytes().to_vec())
            .unwrap();

        account_balance
            .refundable_mut()
            .add(1000)
            .unwrap();

        balances.set(
            &account,
            &"ABC Token".to_string().as_bytes().to_vec(),
            account_balance,
        );

        let result = balances
            .get(&account, &"ABC Token".to_string().as_bytes().to_vec())
            .unwrap();

        assert_eq!(result.refundable(), 1000);
    }

    #[test]
    fn refundable_balance_sub() {
        let context = get_context(vec![], false);
        testing_env!(context);
        let mut balances = Balances::new();
        let account = "88bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4"
            .parse::<AccountId>()
            .unwrap();

        balances.add(&account, &"ABC Token".to_string().as_bytes().to_vec());

        let mut account_balance = balances
            .get(&account, &"ABC Token".to_string().as_bytes().to_vec())
            .unwrap();

        account_balance
            .refundable_mut()
            .add(1000)
            .unwrap();

        balances.set(
            &account,
            &"ABC Token".to_string().as_bytes().to_vec(),
            account_balance,
        );

        let result = balances
            .get(&account, &"ABC Token".to_string().as_bytes().to_vec())
            .unwrap();

        assert_eq!(result.refundable(), 1000);

        let mut account_balance = balances
            .get(&account, &"ABC Token".to_string().as_bytes().to_vec())
            .unwrap();

        account_balance
            .refundable_mut()
            .sub(1)
            .unwrap();

        balances.set(
            &account,
            &"ABC Token".to_string().as_bytes().to_vec(),
            account_balance,
        );

        let result = balances
            .get(&account, &"ABC Token".to_string().as_bytes().to_vec())
            .unwrap();

        assert_eq!(result.refundable(), 999);
    }
}
