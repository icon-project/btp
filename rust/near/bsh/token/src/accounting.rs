use super::*;

#[near_bindgen]
impl TokenService {
    // * * * * * * * * * * * * * * * * *
    // * * * * * * * * * * * * * * * * *
    // * * * * * Transactions  * * * * *
    // * * * * * * * * * * * * * * * * *
    // * * * * * * * * * * * * * * * * *

    pub fn ft_on_transfer(
        &mut self,
        sender_id: AccountId,
        amount: U128,
        _msg: String,
    ) -> PromiseOrValue<U128> {
        let amount = amount.into();
        let token_account = env::predecessor_account_id();

        self.assert_have_minimum_amount(amount);
        self.assert_token_registered(&token_account);

        let token_id = self.registered_tokens.get(&token_account).unwrap().clone();
        let mut balance = match self.balances.get(&sender_id, &token_id) {
            Some(balance) => balance,
            None => AccountBalance::default(),
        };

        self.process_deposit(amount, &mut balance);
        self.balances.set(&sender_id, &token_id, balance);

        PromiseOrValue::Value(U128::from(0))
    }

    #[payable]
    pub fn withdraw(&mut self, token_id: TokenId, amount: U128) {
        // To Prevent Spam
        assert_one_yocto();

        let amount: u128 = amount.into();
        let account = env::predecessor_account_id();

        self.assert_have_minimum_amount(amount);
        self.assert_tokens_exists(&vec![token_id.clone()]);
        self.assert_have_sufficient_deposit(&account, &token_id, amount, None);

        let token = self.tokens.get(&token_id).unwrap();

        let mut balance = self.balances.get(&account, &token_id).unwrap();
        balance.deposit_mut().sub(amount).unwrap();
        self.balances.set(&account.clone(), &token_id, balance);

        Promise::new(token.metadata().uri_deref().unwrap()).function_call(
            "ft_transfer".to_string(),
            json!({
                "receiver_id": account,
                "amount": U128::from(amount),
                "memo": ()
            })
            .to_string()
            .into_bytes(),
            estimate::NO_DEPOSIT,
            estimate::GAS_FOR_FT_TRANSFER,
        );
    }

    pub fn reclaim(&mut self, token_id: TokenId, amount: U128) {
        let amount: u128 = amount.into();
        let account = env::predecessor_account_id();
        self.assert_have_minimum_amount(amount.into());
        self.assert_tokens_exists(&vec![token_id.clone()]);
        self.assert_have_sufficient_refundable(&account, &token_id, amount);

        let mut balance = self.balances.get(&account, &token_id).unwrap();
        balance.refundable_mut().sub(amount).unwrap();
        balance.deposit_mut().add(amount).unwrap();

        self.balances.set(&account, &token_id, balance);
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
