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
    pub fn register(&mut self, coin: Coin) {
        self.assert_have_permission();
        self.assert_coin_does_not_exists(&coin);

        let coin_id = Self::hash_coin_id(coin.name());
        // coin id is hash(coin_name)
        self.coins.add(&coin_id, &coin);
        self.coin_fees.add(&coin_id);
        if coin.network() != &self.network {
            self.registered_coins.add(
                &coin.metadata().uri_deref().expect("Coin Account Missing"),
                &coin_id,
            );
        };

        // Sets initial balance for self
        self.balances.add(&env::current_account_id(), &coin_id);
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
}

impl NativeCoinService {
    pub fn mint(
        &mut self,
        coin_id: &CoinId,
        amount: u128,
        coin: &Coin,
        receiver_id: AccountId,
    ) {
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
