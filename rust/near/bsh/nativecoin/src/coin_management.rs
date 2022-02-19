use super::*;

#[near_bindgen]
impl NativeCoinService {
    // * * * * * * * * * * * * * * * * *
    // * * * * * * * * * * * * * * * * *
    // * * * * Coin Management  * * * *
    // * * * * * * * * * * * * * * * * *
    // * * * * * * * * * * * * * * * * *

    /// Register Coin, Accept coin meta(name, symbol, network, denominator) as parameters
    // TODO: Complete Documentation
    #[payable]
    pub fn register(&mut self, coin: Coin) {
        self.assert_have_permission();
        self.assert_coin_does_not_exists(&coin);
        
        if coin.network() == &self.network {
            self.register_coin_callback(coin);
        } else {
            let coin_metadata = coin.extras().clone().expect("Coin Metadata Missing");
            let promise_idx = env::promise_batch_create(
                &coin.metadata().uri_deref().expect("Coin Account Missing"),
            );
            env::promise_batch_action_create_account(promise_idx);
            env::promise_batch_action_transfer(promise_idx, env::attached_deposit());
            env::promise_batch_action_deploy_contract(promise_idx, NEP141_CONTRACT);
            env::promise_batch_action_function_call(
                promise_idx,
                "new",
                &json!({
                    "owner_id": env::current_account_id(),
                    "total_supply": U128(0),
                    "metadata": {
                        "spec": coin_metadata.spec.clone(),
                        "name": coin.name(),
                        "symbol": coin.symbol(),
                        "icon": coin_metadata.icon.clone(),
                        "reference": coin_metadata.reference.clone(),
                        "reference_hash": coin_metadata.reference_hash.clone(),
                        "decimals": coin_metadata.decimals.clone()
                    }
                })
                .to_string()
                .into_bytes(),
                0,
                estimate::GAS_FOR_RESOLVE_TRANSFER,
            );
            env::promise_then(
                promise_idx,
                env::current_account_id(),
                "register_coin_callback",
                &json!({ "coin": coin }).to_string().into_bytes(),
                0,
                estimate::GAS_FOR_RESOLVE_TRANSFER,
            );
        }
    }

    // TODO: Unregister Token

    pub fn coins(&self) -> Value {
        to_value(self.coins.to_vec()).unwrap()
    }

    // Hashing to be done out of chain
    pub fn coin_id(&self, coin_name: String) -> CoinId {
        let coin_id = Self::hash_coin_id(&coin_name);
        self.assert_coins_exists(&vec![coin_id.clone()]);
        coin_id
    }

    #[private]
    pub fn on_mint(
        &mut self,
        amount: u128,
        coin_id: CoinId,
        coin_symbol: String,
        receiver_id: AccountId,
    ) {
        let mut balance = self
            .balances
            .get(&env::current_account_id(), &coin_id)
            .unwrap();
        balance.deposit_mut().add(amount).unwrap();
        self.balances
            .set(&env::current_account_id(), &coin_id, balance);

        self.internal_transfer(&env::current_account_id(), &receiver_id, &coin_id, amount);

        log!("[Mint] {} {}", amount, coin_symbol);
    }

    #[private]
    pub fn on_burn(&mut self, amount: u128, coin_id: CoinId, coin_symbol: String) {
        let mut balance = self
            .balances
            .get(&env::current_account_id(), &coin_id)
            .unwrap();
        balance.deposit_mut().sub(amount).unwrap();
        self.balances
            .set(&env::current_account_id(), &coin_id, balance);

        log!("[Burn] {} {}", amount, coin_symbol);
    }

    #[private]
    pub fn register_coin_callback(&mut self, coin: Coin) {
        let coin_id = Self::hash_coin_id(coin.name());

        self.coins.add(&coin_id, &coin);
        self.coin_fees.add(&coin_id);

        self.registered_coins.add(
            &coin.metadata().uri_deref().expect("Coin Account Missing"),
            &coin_id,
        );

        self.balances.add(&env::current_account_id(), &coin_id);
    }
}

impl NativeCoinService {
    pub fn mint(&mut self, coin_id: &CoinId, amount: u128, coin: &Coin, receiver_id: AccountId) {
        ext_nep141::mint(
            amount.into(),
            coin.metadata().uri().to_owned().unwrap(),
            estimate::NO_DEPOSIT,
            estimate::GAS_FOR_MT_TRANSFER_CALL,
        )
        .then(ext_self::on_mint(
            amount,
            coin_id.to_vec(),
            coin.symbol().to_string(),
            receiver_id,
            env::current_account_id(),
            estimate::NO_DEPOSIT,
            estimate::GAS_FOR_MT_TRANSFER_CALL,
        ));
    }

    pub fn burn(&mut self, coin_id: &CoinId, amount: u128, coin: &Coin) {
        ext_nep141::burn(
            amount.into(),
            coin.metadata().uri().to_owned().unwrap(),
            estimate::NO_DEPOSIT,
            estimate::GAS_FOR_BURN,
        )
        .then(ext_self::on_burn(
            amount,
            coin_id.to_owned(),
            coin.symbol().to_string(),
            env::current_account_id(),
            estimate::NO_DEPOSIT,
            estimate::GAS_FOR_MT_TRANSFER_CALL,
        ));
    }

    pub fn verify_mint(&self, coin_id: &CoinId, amount: u128) -> Result<(), String> {
        let mut balance = self
            .balances
            .get(&env::current_account_id(), coin_id)
            .unwrap();
        balance.deposit_mut().add(amount)?;
        Ok(())
    }
}
