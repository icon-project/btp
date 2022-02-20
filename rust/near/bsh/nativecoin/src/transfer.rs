use super::*;

#[near_bindgen]
impl NativeCoinService {
    pub fn transfer(&mut self, coin_id: CoinId, destination: BTPAddress, amount: U128) {
        let sender_id = env::predecessor_account_id();
        self.assert_have_minimum_amount(amount.into());
        self.assert_coins_exists(&vec![coin_id.clone()]);

        let asset = self
            .process_external_transfer(&coin_id, &sender_id, amount.into())
            .unwrap();

        self.send_request(sender_id, destination, vec![asset]);
    }

    pub fn transfer_batch(
        &mut self,
        coin_ids: Vec<CoinId>,
        destination: BTPAddress,
        amounts: Vec<U128>,
    ) {
        let sender_id = env::predecessor_account_id();
        self.assert_coins_exists(&coin_ids);

        let assets = coin_ids
            .iter()
            .enumerate()
            .map(|(index, coin_id)| {
                self.assert_have_minimum_amount(amounts[index].into());
                self.process_external_transfer(coin_id, &sender_id, amounts[index].into())
                    .unwrap()
            })
            .collect::<Vec<TransferableAsset>>();

        self.send_request(sender_id, destination, assets);
    }
}

impl NativeCoinService {
    pub fn process_external_transfer(
        &mut self,
        coin_id: &CoinId,
        sender_id: &AccountId,
        mut amount: u128,
    ) -> Result<TransferableAsset, String> {
        let coin = self.coins.get(&coin_id).unwrap();
        let fees = self.calculate_coin_transfer_fee(amount.into());

        self.assert_have_sufficient_deposit(&sender_id, &coin_id, amount, Some(fees));

        amount.sub(fees)?;
        let mut balance = self.balances.get(&sender_id, &coin_id).unwrap();

        // Handle Fees
        balance.locked_mut().add(fees)?;
        balance.deposit_mut().sub(fees)?;

        // Handle Deposit
        balance.deposit_mut().sub(amount)?;
        balance.locked_mut().add(amount)?;

        self.balances.set(&sender_id, &coin_id, balance);

        Ok(TransferableAsset::new(coin.name().clone(), amount, fees))
    }

    pub fn internal_transfer(
        &mut self,
        sender_id: &AccountId,
        receiver_id: &AccountId,
        coin_id: &CoinId,
        amount: u128,
    ) {
        self.assert_sender_is_not_receiver(sender_id, receiver_id);
        self.assert_have_sufficient_deposit(sender_id, coin_id, amount, None);

        let mut sender_balance = self.balances.get(sender_id, coin_id).unwrap();
        sender_balance.deposit_mut().sub(amount).unwrap();

        let receiver_balance = match self.balances.get(&receiver_id, coin_id) {
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

        self.balances.set(sender_id, coin_id, sender_balance);
        self.balances.set(receiver_id, coin_id, receiver_balance);
    }

    pub fn verify_internal_transfer(
        &self,
        sender_id: &AccountId,
        receiver_id: &AccountId,
        coin_id: &CoinId,
        amount: u128,
        sender_balance: &mut AccountBalance,
    ) -> Result<(), String> {
        self.assert_sender_is_not_receiver(sender_id, receiver_id);
        sender_balance.deposit_mut().sub(amount)?;

        match self.balances.get(&receiver_id, coin_id) {
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
        coin_ids: &Vec<CoinId>,
        amounts: &Vec<U128>,
    ) {
        coin_ids.iter().enumerate().for_each(|(index, coin_id)| {
            self.internal_transfer(sender_id, receiver_id, coin_id, amounts[index].into());
        });
    }

    pub fn finalize_external_transfer(&mut self, sender_id: &AccountId, assets: &Vec<TransferableAsset>) {
        assets.iter().for_each(|asset| {
            let coin_id = Self::hash_coin_id(asset.name());
            let coin = self.coins.get(&coin_id).unwrap();
            let mut coin_fee = self.coin_fees.get(&coin_id).unwrap().to_owned();

            let mut sender_balance = self.balances.get(&sender_id, &coin_id).unwrap();
            sender_balance
                .locked_mut()
                .sub(asset.amount() + asset.fees())
                .unwrap();

            self.balances.set(&sender_id, &coin_id, sender_balance);

            let mut current_account_balance = self
                .balances
                .get(&env::current_account_id(), &coin_id)
                .unwrap();
            current_account_balance
                .deposit_mut()
                .add(asset.amount() + asset.fees())
                .unwrap();

            self.balances.set(
                &env::current_account_id(),
                &coin_id,
                current_account_balance,
            );

            coin_fee.add(asset.fees()).unwrap();
            self.coin_fees.set(&coin_id, coin_fee);

            if coin.network() != &self.network {
                self.burn(&coin_id, asset.amount(), &coin);
            }
        });
    }

    pub fn rollback_external_transfer(&mut self, sender_id: &AccountId, assets: &Vec<TransferableAsset>) {
        assets.iter().for_each(|asset| {
            let coin_id = Self::hash_coin_id(asset.name());
            let mut coin_fee = self.coin_fees.get(&coin_id).unwrap().to_owned();
            let mut sender_balance = self.balances.get(&sender_id, &coin_id).unwrap();
            sender_balance
                .locked_mut()
                .sub(asset.amount() + asset.fees())
                .unwrap();
            sender_balance.refundable_mut().add(asset.amount()).unwrap();
            self.balances.set(&sender_id, &coin_id, sender_balance);

            let mut current_account_balance = self
                .balances
                .get(&env::current_account_id(), &coin_id)
                .unwrap();
            current_account_balance
                .deposit_mut()
                .add(asset.fees())
                .unwrap();
            self.balances.set(
                &env::current_account_id(),
                &coin_id,
                current_account_balance,
            );

            coin_fee.add(asset.fees()).unwrap();
            self.coin_fees.set(&coin_id, coin_fee);
        });
    }

    pub fn handle_coin_transfer(
        &mut self,
        message_source: &BTPAddress,
        receiver_id: &String,
        assets: &Vec<TransferableAsset>,
    ) -> Result<Option<TokenServiceMessage>, BshError> {
        let receiver_id = AccountId::try_from(receiver_id.to_owned()).map_err(|error| {
            BshError::InvalidAddress {
                message: error.to_string(),
            }
        })?;

        let mut unregistered_coins: Vec<String> = Vec::new();

        let coin_ids: Vec<(usize, CoinId)> = assets
            .iter()
            .map(|asset| Self::hash_coin_id(asset.name()))
            .enumerate()
            .filter(|(index, coin_id)| {
                return if !self.coins.contains(coin_id) {
                    unregistered_coins.push(assets[index.to_owned()].name().to_owned());
                    false
                } else {
                    true
                };
            })
            .collect();

        if unregistered_coins.len() > 0 {
            return Err(BshError::TokenNotExist {
                message: unregistered_coins.join(", "),
            });
        }

        let coins = coin_ids
            .into_iter()
            .map(|(asset_index, coin_id)| {
                (
                    asset_index,
                    coin_id.clone(),
                    self.coins.get(&coin_id).unwrap(),
                )
            })
            .collect::<Vec<(usize, CoinId, Coin)>>();

        let transferable =
            self.is_coins_transferable(&env::current_account_id(), &receiver_id, &coins, assets);
        if transferable.is_err() {
            return Err(BshError::Reverted {
                message: format!("Coins not transferable: {}", transferable.unwrap_err()),
            });
        }

        coins.iter().for_each(|(index, coin_id, coin)| {
            if coin.network() != &self.network {
                self.mint(
                    coin_id,
                    assets[index.to_owned()].amount(),
                    &coin,
                    receiver_id.clone(),
                );
            } else {
                self.internal_transfer(
                    &env::current_account_id(),
                    &receiver_id,
                    coin_id,
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

    fn is_coins_transferable(
        &self,
        sender_id: &AccountId,
        receiver_id: &AccountId,
        coins: &Vec<(usize, CoinId, Asset<WrappedNativeCoin>)>,
        assets: &Vec<TransferableAsset>,
    ) -> Result<(), String> {
        coins
            .iter()
            .map(|(index, coin_id, coin)| -> Result<(), String> {
                let mut sender_balance = self.balances.get(sender_id, coin_id).unwrap();
                if coin.network() != &self.network {
                    self.verify_mint(coin_id, assets[index.to_owned()].amount())?;
                    sender_balance
                        .deposit_mut()
                        .add(assets[index.to_owned()].amount())?;
                }
                self.verify_internal_transfer(
                    &env::current_account_id(),
                    receiver_id,
                    coin_id,
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
        coin_ids: &Vec<CoinId>,
        sender_id: &AccountId,
        receiver_id: &AccountId,
    ) -> U128 {
        if returned_amount == 0 {
            return U128::from(0);
        }
        let unused_amount = std::cmp::min(amounts[index].into(), returned_amount);
        let coin_id = &coin_ids[index];

        let mut receiver_balance = self
            .balances
            .get(receiver_id, coin_id)
            .expect("Token receiver no longer exists");

        if receiver_balance.deposit() > 0 {
            let refund_amount = std::cmp::min(receiver_balance.deposit(), unused_amount); // TODO: Revisit
            receiver_balance.deposit_mut().sub(refund_amount).unwrap();
            self.balances
                .set(&receiver_id.clone(), coin_id, receiver_balance);

            if let Some(mut sender_balance) = self.balances.get(sender_id, coin_id) {
                sender_balance.deposit_mut().add(refund_amount).unwrap();
                self.balances
                    .set(&sender_id.clone(), coin_id, sender_balance);
                let amount: u128 = amounts[index].into();
                return U128::from(amount - refund_amount);
            }
        }

        U128::from(0)
    }
}
