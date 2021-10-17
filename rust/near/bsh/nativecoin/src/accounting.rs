use super::*;

#[near_bindgen]
impl NativeCoinService {
    // * * * * * * * * * * * * * * * * *
    // * * * * * * * * * * * * * * * * *
    // * * * * * Transactions  * * * * *
    // * * * * * * * * * * * * * * * * *
    // * * * * * * * * * * * * * * * * *

    #[payable]
    pub fn deposit(&mut self) {
        let account = env::predecessor_account_id();
        let amount = env::attached_deposit();
        self.assert_have_deposit();
        let token_id = Self::hash_token_id(&self.native_coin_name);

        let mut balance = match self.balances.get(&account, &token_id) {
            Some(balance) => balance,
            None => AccountBalance::default(),
        };

        self.process_deposit(amount, &mut balance);
        self.balances.set(&account, &token_id, balance);
    }

    #[payable]
    pub fn withdraw(&mut self, amount: U128) {
        // To Prevent Spam
        assert_one_yocto();

        let amount: u128 = amount.into();
        let account = env::predecessor_account_id();
        let native_coin_id = Self::hash_token_id(&self.native_coin_name);

        // Check if user is having requested amount including the yocto just deposited
        self.assert_have_sufficient_deposit(&account, &native_coin_id, amount, None);

        // Check if current account have sufficient balance
        self.assert_have_sufficient_balance(1 + amount);

        let mut balance = self.balances.get(&account, &native_coin_id).unwrap();
        balance.deposit_mut().sub(amount).unwrap();
        self.balances
            .set(&account.clone(), &native_coin_id, balance);

        Promise::new(account).transfer(amount + 1);
    }

    pub fn reclaim(&mut self, coin_id: TokenId, amount: U128) {
        let amount: u128 = amount.into();
        let account = env::predecessor_account_id();
        self.assert_have_minimum_amount(amount.into());
        self.assert_tokens_exists(&vec![coin_id.clone()]);
        self.assert_have_sufficient_refundable(&account, &coin_id, amount);

        let mut balance = self.balances.get(&account, &coin_id).unwrap();
        balance.refundable_mut().sub(amount).unwrap();
        balance.deposit_mut().add(amount).unwrap();

        self.balances.set(&account, &coin_id, balance);
    }

    pub fn locked_balance_of(&self, owner_id: AccountId, token_id: TokenId) -> U128 {
        self.assert_tokens_exists(&vec![token_id.clone()]);
        let balance = self
            .balances
            .get(&owner_id, &token_id)
            .expect(format!("{}", BshError::AccountNotExist).as_str());
        balance.locked().into()
    }

    pub fn refundable_balance_of(&self, owner_id: AccountId, token_id: TokenId) -> U128 {
        self.assert_tokens_exists(&vec![token_id.clone()]);
        let balance = self
            .balances
            .get(&owner_id, &token_id)
            .expect(format!("{}", BshError::AccountNotExist).as_str());
        balance.refundable().into()
    }

    #[cfg(feature = "testable")]
    pub fn account_balance(
        &self,
        owner_id: AccountId,
        token_id: TokenId,
    ) -> Option<AccountBalance> {
        self.balances.get(&owner_id, &token_id)
    }
}
