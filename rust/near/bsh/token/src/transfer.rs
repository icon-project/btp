use super::*;

#[near_bindgen]
impl TokenService {
    pub fn transfer(&mut self, token_id: TokenId, destination: BTPAddress, amount: U128) {
        let sender_id = env::predecessor_account_id();
        self.assert_have_minimum_amount(amount.into());
        self.assert_tokens_exists(&vec![token_id.clone()]);

        let asset = self
            .process_external_transfer(&token_id, &sender_id, amount.into())
            .unwrap();

        self.send_request(sender_id, destination, vec![asset]);
    }

    pub fn transfer_batch(
        &mut self,
        token_ids: Vec<TokenId>,
        destination: BTPAddress,
        amounts: Vec<U128>,
    ) {
        let sender_id = env::predecessor_account_id();
        self.assert_tokens_exists(&token_ids);

        let assets = token_ids
            .iter()
            .enumerate()
            .map(|(index, token_id)| {
                self.assert_have_minimum_amount(amounts[index].into());
                self.process_external_transfer(token_id, &sender_id, amounts[index].into())
                    .unwrap()
            })
            .collect::<Vec<Asset>>();

        self.send_request(sender_id, destination, assets);
    }
}

impl TokenService {
    pub fn process_external_transfer(
        &mut self,
        token_id: &TokenId,
        sender_id: &AccountId,
        mut amount: u128,
    ) -> Result<Asset, String> {
        let token = self.tokens.get(&token_id).unwrap();
        let fees = self.calculate_token_transfer_fee(amount.into());

        self.assert_have_sufficient_deposit(&sender_id, &token_id, amount, Some(fees));

        amount.sub(fees)?;
        let mut balance = self.balances.get(&sender_id, &token_id).unwrap();

        // Handle Fees
        balance.locked_mut().add(fees)?;
        balance.deposit_mut().sub(fees)?;

        // Handle Deposit
        balance.deposit_mut().sub(amount)?;
        balance.locked_mut().add(amount)?;

        self.balances.set(&sender_id, &token_id, balance);

        Ok(Asset::new(token.name().clone(), amount, fees))
    }

    pub fn internal_transfer(
        &mut self,
        sender_id: &AccountId,
        receiver_id: &AccountId,
        token_id: &TokenId,
        amount: u128,
    ) {
        log!("Starting Internal Transfer");

        self.assert_sender_is_not_receiver(sender_id, receiver_id);
        self.assert_have_sufficient_deposit(sender_id, token_id, amount, None);

        let mut sender_balance = self.balances.get(sender_id, token_id).unwrap();
        sender_balance.deposit_mut().sub(amount).unwrap();

        let receiver_balance = match self.balances.get(&receiver_id, token_id) {
            Some(mut balance) => {
                balance.deposit_mut().add(amount).unwrap();
                balance
            }
            None => {
                let mut balance = AccountBalance::default();
                let storage_deposit = 0_u128; // TODO: Calculate storage deposit
                let amount = amount - storage_deposit;
                balance.deposit_mut().add(amount).unwrap();
                balance
            }
        };

        self.balances.set(sender_id, token_id, sender_balance);
        self.balances.set(receiver_id, token_id, receiver_balance);
    }

    pub fn verify_internal_transfer(
        &self,
        sender_id: &AccountId,
        receiver_id: &AccountId,
        token_id: &TokenId,
        amount: u128,
        sender_balance: &mut AccountBalance,
    ) -> Result<(), String> {
        self.assert_sender_is_not_receiver(sender_id, receiver_id);
        sender_balance.deposit_mut().sub(amount)?;

        match self.balances.get(&receiver_id, token_id) {
            Some(mut balance) => {
                balance.deposit_mut().add(amount)?;
                balance
            }
            None => {
                let mut balance = AccountBalance::default();
                let storage_deposit = 0_u128; // TODO: Calculate storage deposit
                let amount = amount - storage_deposit;
                balance.deposit_mut().add(amount)?;
                balance
            }
        };
        Ok(())
    }

    pub fn internal_transfer_batch(
        &mut self,
        sender_id: &AccountId,
        receiver_id: &AccountId,
        token_ids: &Vec<TokenId>,
        amounts: &Vec<U128>,
    ) {
        token_ids.iter().enumerate().for_each(|(index, token_id)| {
            self.internal_transfer(sender_id, receiver_id, token_id, amounts[index].into());
        });
    }

    pub fn finalize_external_transfer(&mut self, sender_id: &AccountId, assets: &Vec<Asset>) {
        assets.iter().for_each(|asset| {
            let token_id = Self::hash_token_id(asset.token());
            let token = self.tokens.get(&token_id).unwrap();
            let mut token_fee = self.token_fees.get(&token_id).unwrap().to_owned();

            let mut sender_balance = self.balances.get(&sender_id, &token_id).unwrap();
            sender_balance
                .locked_mut()
                .sub(asset.amount() + asset.fees())
                .unwrap();

            self.balances.set(&sender_id, &token_id, sender_balance);

            let mut current_account_balance = self
                .balances
                .get(&env::current_account_id(), &token_id)
                .unwrap();
            current_account_balance
                .deposit_mut()
                .add(asset.amount() + asset.fees())
                .unwrap();

            self.balances.set(
                &env::current_account_id(),
                &token_id,
                current_account_balance,
            );

            token_fee.add(asset.fees()).unwrap();
            self.token_fees.set(&token_id, token_fee);

            if token.network() != &self.network {
                self.burn(&token_id, asset.amount(), &token);
            }
        });
    }

    pub fn rollback_external_transfer(&mut self, sender_id: &AccountId, assets: &Vec<Asset>) {
        assets.iter().for_each(|asset| {
            let token_id = Self::hash_token_id(asset.token());
            let mut token_fee = self.token_fees.get(&token_id).unwrap().to_owned();
            let mut sender_balance = self.balances.get(&sender_id, &token_id).unwrap();
            sender_balance
                .locked_mut()
                .sub(asset.amount() + asset.fees())
                .unwrap();
            sender_balance.refundable_mut().add(asset.amount()).unwrap();
            self.balances.set(&sender_id, &token_id, sender_balance);

            let mut current_account_balance = self
                .balances
                .get(&env::current_account_id(), &token_id)
                .unwrap();
            current_account_balance
                .deposit_mut()
                .add(asset.fees())
                .unwrap();
            self.balances.set(
                &env::current_account_id(),
                &token_id,
                current_account_balance,
            );

            token_fee.add(asset.fees()).unwrap();
            self.token_fees.set(&token_id, token_fee);
        });
    }

    pub fn handle_token_transfer(
        &mut self,
        message_source: &BTPAddress,
        receiver_id: &String,
        assets: &Vec<Asset>,
    ) -> Result<Option<TokenServiceMessage>, BshError> {
        let receiver_id = AccountId::try_from(receiver_id.to_owned()).map_err(|error| {
            BshError::InvalidAddress {
                message: error.to_string(),
            }
        })?;

        let mut unregistered_tokens: Vec<String> = Vec::new();

        let token_ids: Vec<(usize, TokenId)> = assets
            .iter()
            .map(|asset| Self::hash_token_id(asset.token()))
            .enumerate()
            .filter(|(index, token_id)| {
                return if !self.tokens.contains(token_id) {
                    unregistered_tokens.push(assets[index.to_owned()].token().to_owned());
                    false
                } else {
                    true
                };
            })
            .collect();

        if unregistered_tokens.len() > 0 {
            return Err(BshError::TokenNotExist {
                message: unregistered_tokens.join(", "),
            });
        }

        let tokens = token_ids
            .into_iter()
            .map(|(asset_index, token_id)| {
                (
                    asset_index,
                    token_id.clone(),
                    self.tokens.get(&token_id).unwrap(),
                )
            })
            .collect::<Vec<(usize, TokenId, Token<WrappedFungibleToken>)>>();

        let transferable =
            self.is_tokens_transferable(&env::current_account_id(), &receiver_id, &tokens, assets);
        if transferable.is_err() {
            return Err(BshError::Reverted {
                message: format!("Tokens not transferable: {}", transferable.unwrap_err()),
            });
        }

        tokens.iter().for_each(|(index, token_id, token)| {
            if token.network() != &self.network {
                self.mint(token_id, assets[index.to_owned()].amount(), &token);
            } else {
                self.internal_transfer(
                    &env::current_account_id(),
                    &receiver_id,
                    token_id,
                    assets[index.to_owned()].amount(),
                );
            }
        });

        Ok(Some(TokenServiceMessage::new(
            TokenServiceType::ResponseHandleService {
                code: 0,
                message: "Transfer Success".to_string(),
            },
        )))
    }

    fn is_tokens_transferable(
        &self,
        sender_id: &AccountId,
        receiver_id: &AccountId,
        tokens: &Vec<(usize, TokenId, Token<WrappedFungibleToken>)>,
        assets: &Vec<Asset>,
    ) -> Result<(), String> {
        tokens
            .iter()
            .map(|(index, token_id, token)| -> Result<(), String> {
                let mut sender_balance = self.balances.get(sender_id, token_id).unwrap();
                if token.network() != &self.network {
                    self.verify_mint(token_id, assets[index.to_owned()].amount())?;
                    sender_balance
                        .deposit_mut()
                        .add(assets[index.to_owned()].amount())?;
                }
                self.verify_internal_transfer(
                    &env::current_account_id(),
                    receiver_id,
                    token_id,
                    assets[index.to_owned()].amount(),
                    &mut sender_balance,
                )?;
                Ok(())
            })
            .collect()
    }

    pub fn refund_balance_amount(
        &mut self,
        index: usize,
        amounts: &Vec<U128>,
        returned_amount: u128,
        token_ids: &Vec<TokenId>,
        sender_id: &AccountId,
        receiver_id: &AccountId,
    ) -> U128 {
        if returned_amount == 0 {
            return U128::from(0);
        }
        let unused_amount = std::cmp::min(amounts[index].into(), returned_amount);
        let token_id = &token_ids[index];

        let mut receiver_balance = self
            .balances
            .get(receiver_id, token_id)
            .expect("Token receiver no longer exists");

        if receiver_balance.deposit() > 0 {
            let refund_amount = std::cmp::min(receiver_balance.deposit(), unused_amount); // TODO: Revisit
            receiver_balance.deposit_mut().sub(refund_amount).unwrap();
            self.balances
                .set(&receiver_id.clone(), token_id, receiver_balance);

            if let Some(mut sender_balance) = self.balances.get(sender_id, token_id) {
                sender_balance.deposit_mut().add(refund_amount).unwrap();
                self.balances
                    .set(&sender_id.clone(), token_id, sender_balance);
                let amount: u128 = amounts[index].into();
                return U128::from(amount - refund_amount);
            }
        }

        U128::from(0)
    }
}
