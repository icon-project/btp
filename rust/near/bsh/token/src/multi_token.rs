use super::*;

#[near_bindgen]
impl MultiTokenCore for TokenService {
    #[payable]
    fn mt_transfer(
        &mut self,
        receiver_id: AccountId,
        token_id: TokenId,
        amount: U128,
        _memo: Option<String>,
    ) {
        assert_one_yocto();
        self.assert_tokens_exists(&vec![token_id.clone()]);
        let sender_id = env::predecessor_account_id();

        self.internal_transfer(&sender_id, &receiver_id, &token_id, amount.into());
    }

    #[payable]
    fn mt_transfer_call(
        &mut self,
        receiver_id: AccountId,
        token_id: TokenId,
        amount: U128,
        _memo: Option<String>,
        msg: String,
    ) -> PromiseOrValue<U128> {
        assert_one_yocto();
        self.assert_tokens_exists(&vec![token_id.clone()]);
        let sender_id = env::predecessor_account_id();

        self.internal_transfer(&sender_id, &receiver_id, &token_id, amount.into());

        ext_receiver::mt_on_transfer(
            sender_id.clone(),
            vec![token_id.clone()],
            vec![amount],
            msg,
            receiver_id.clone(),
            estimate::NO_DEPOSIT,
            estimate::GAS_FOR_MT_TRANSFER_CALL,
        )
        .then(ext_self::mt_resolve_transfer(
            sender_id,
            receiver_id,
            vec![token_id],
            vec![amount],
            env::current_account_id(),
            estimate::NO_DEPOSIT,
            estimate::GAS_FOR_RESOLVE_TRANSFER,
        ))
        .into()
    }

    #[payable]
    fn mt_batch_transfer(
        &mut self,
        receiver_id: AccountId,
        token_ids: Vec<TokenId>,
        amounts: Vec<U128>,
        _memo: Option<String>,
    ) {
        assert_one_yocto();
        self.assert_tokens_exists(&token_ids);
        self.assert_token_id_len_match_amount_len(&token_ids, &amounts);

        let sender_id = env::predecessor_account_id();
        self.internal_transfer_batch(&sender_id, &receiver_id, &token_ids, &amounts);
    }

    #[payable]
    fn mt_batch_transfer_call(
        &mut self,
        receiver_id: AccountId,
        token_ids: Vec<TokenId>,
        amounts: Vec<U128>,
        _memo: Option<String>,
        msg: String,
    ) -> PromiseOrValue<Vec<U128>> {
        assert_one_yocto();
        self.assert_tokens_exists(&token_ids);
        self.assert_token_id_len_match_amount_len(&token_ids, &amounts);

        let sender_id = env::predecessor_account_id();
        self.internal_transfer_batch(&sender_id, &receiver_id, &token_ids, &amounts);

        // TODO make this efficient and calculate gas
        ext_receiver::mt_on_transfer(
            sender_id.clone(),
            token_ids.clone(),
            amounts.clone(),
            msg,
            receiver_id.clone(),
            estimate::NO_DEPOSIT,
            Gas(25_000_000_000_000),
        )
        .then(ext_self::mt_resolve_transfer(
            sender_id,
            receiver_id,
            token_ids,
            amounts,
            env::current_account_id(),
            estimate::NO_DEPOSIT,
            Gas(5_000_000_000_000), //GAS_FOR_RESOLVE_TRANSFER,
        ))
        .into()
    }

    fn balance_of(&self, owner_id: AccountId, token_id: TokenId) -> U128 {
        self.assert_tokens_exists(&vec![token_id.clone()]);
        let balance = self
            .balances
            .get(&owner_id, &token_id)
            .expect(format!("{}", BshError::AccountNotExist).as_str());
        balance.deposit().into()
    }

    fn balance_of_batch(&self, owner_id: AccountId, token_ids: Vec<TokenId>) -> Vec<U128> {
        token_ids
            .iter()
            .map(|token_id| self.balance_of(owner_id.clone(), token_id.clone()))
            .collect()
    }

    fn total_supply(&self, _token_id: TokenId) -> U128 {
        todo!();
    }

    fn total_supply_batch(&self, _token_ids: Vec<TokenId>) -> Vec<U128> {
        todo!();
    }
}

#[near_bindgen]
impl MultiTokenResolver for TokenService {
    #[private]
    fn mt_resolve_transfer(
        &mut self,
        sender_id: AccountId,
        receiver_id: AccountId,
        token_ids: Vec<libraries::types::TokenId>,
        amounts: Vec<U128>,
    ) -> Vec<U128> {
        self.assert_tokens_exists(&token_ids);

        let returned_amounts: Vec<U128> = match env::promise_result(0) {
            PromiseResult::NotReady => env::abort(),
            PromiseResult::Successful(value) => {
                if let Ok(returned_amounts) = near_sdk::serde_json::from_slice::<Vec<U128>>(&value)
                {
                    self.assert_transfer_amounts_len_match_returned_amount_len(
                        &amounts,
                        &returned_amounts,
                    );
                    returned_amounts
                } else {
                    amounts.clone()
                }
            }
            PromiseResult::Failed => amounts.clone(),
        };

        returned_amounts
            .iter()
            .enumerate()
            .map(|(index, returned_amount)| {
                self.refund_balance_amount(
                    index,
                    &amounts,
                    returned_amount.to_owned().into(),
                    &token_ids,
                    &sender_id,
                    &receiver_id,
                )
            })
            .collect()
    }
}
