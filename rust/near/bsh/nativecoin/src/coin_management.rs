use super::*;

#[near_bindgen]
impl NativeCoinService {
    // * * * * * * * * * * * * * * * * *
    // * * * * * * * * * * * * * * * * *
    // * * * * Coin Management  * * * *
    // * * * * * * * * * * * * * * * * *
    // * * * * * * * * * * * * * * * * *

    /// Register Coin, Accept token meta(name, symbol, network, denominator) as parameters
    // TODO: Complete Documentation
    pub fn register(&mut self, token: Token<NativeCoin>) {
        self.assert_have_permission();
        self.assert_token_does_not_exists(&token);

        let token_id = Self::hash_token_id(token.name());
        // token id is hash(token_name)
        self.tokens.add(&token_id, &token);
        self.token_fees.add(&token_id);

        // Sets initial balance for self
        self.balances.add(&env::current_account_id(), &token_id);
    }

    // TODO: Unregister Token

    pub fn coins(&self) -> Value {
        to_value(self.tokens.to_vec()).unwrap()
    }

    // Hashing to be done out of chain
    pub fn coin_id(&self, coin_name: String) -> TokenId {
        Self::hash_token_id(&coin_name)
    }

    // TODO: Confirm regarding the Fee ratio. Other implementations follow a global fee ratio,
    // but ideally fee ratio and denominations varies between tokens.
    pub fn set_fee_ratio(&mut self, fee_numerator: u128) {
        self.assert_have_permission();
        self.assert_valid_fee_ratio(fee_numerator.into());
        self.fee_numerator.clone_from(&fee_numerator);
    }
    pub fn calculate_token_transfer_fee(&self,amount: u128) -> u128 {
        let fee = (amount * self.fee_numerator) / FEE_DENOMINATOR;
        fee
    }
}

impl NativeCoinService {
    pub fn mint(&mut self, token_id: &TokenId, amount: u128, token: &Token<NativeCoin>) {
        // TODO: Add to supply
        let mut balance = self
            .balances
            .get(&env::current_account_id(), token_id)
            .unwrap();
        balance.deposit_mut().add(amount).unwrap();
        self.balances
            .set(&env::current_account_id(), token_id, balance);

        log!("[Mint] {} {}", amount, token.symbol());
    }

    pub fn burn(&mut self, token_id: &TokenId, amount: u128, token: &Token<NativeCoin>) {
        // TODO: Remove from supply
        let mut balance = self
            .balances
            .get(&env::current_account_id(), token_id)
            .unwrap();
        balance.deposit_mut().sub(amount).unwrap();
        self.balances
            .set(&env::current_account_id(), token_id, balance);

        log!("[Burn] {} {}", amount, token.symbol());
    }

    pub fn verify_mint(&self, token_id: &TokenId, amount: u128) -> Result<(), String> {
        let mut balance = self
            .balances
            .get(&env::current_account_id(), token_id)
            .unwrap();
        balance.deposit_mut().add(amount)?;
        Ok(())
    }
}
