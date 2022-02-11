use super::*;

#[near_bindgen]
impl TokenService {
    // * * * * * * * * * * * * * * * * *
    // * * * * * * * * * * * * * * * * *
    // * * * * Token Management  * * * *
    // * * * * * * * * * * * * * * * * *
    // * * * * * * * * * * * * * * * * *

    /// Register Token, Accept token meta(name, symbol, network, denominator) as parameters
    // TODO: Complete Documentation
    pub fn register(&mut self, token: Token<WrappedFungibleToken>) {
        self.assert_have_permission();
        self.assert_token_does_not_exists(&token);

        let token_id = Self::hash_token_id(token.name());
        // token id is hash(token_name)
        self.tokens.add(&token_id, &token);
        self.token_fees.add(&token_id);

        if token.network() == &self.network {
            self.registered_tokens.add(
                &token.metadata().uri_deref().expect("Token Account Missing"),
                &token_id,
            );
        };

        // Add fungible token list
        // Sets initial balance for self
        self.balances.add(&env::current_account_id(), &token_id);
    }

    // TODO: Unregister Token

    pub fn tokens(&self) -> Value {
        to_value(self.tokens.to_vec()).unwrap()
    }

    // Hashing to be done out of chain
    pub fn token_id(&self, token_name: String) -> TokenId {
        let token_id = Self::hash_token_id(&token_name);
        self.assert_tokens_exists(&vec![token_id.clone()]);
        token_id
    }

    #[private]
    pub fn on_mint(
        &mut self,
        amount: u128,
        token_id: TokenId,
        token_symbol: String,
        receiver_id: AccountId,
    ) {
        let mut balance = self.balances.get(&receiver_id, &token_id).unwrap();
        balance.deposit_mut().add(amount).unwrap();
        self.balances.set(&receiver_id, &token_id, balance);

        log!("[Mint] {} {}", amount, token_symbol);
    }
}

impl TokenService {
    pub fn mint(
        &mut self,
        token_id: &TokenId,
        amount: u128,
        token: &Token<WrappedFungibleToken>,
        receiver_id: &AccountId,
    ) {
        //Review Required
        ext_nep141::mint(
            amount.into(),
            token.metadata().uri().to_owned().unwrap(),
            estimate::NO_DEPOSIT,
            estimate::GAS_FOR_MINT,
        )
        .then(ext_self::on_mint(
            amount,
            token_id.to_vec(),
            token.symbol().to_string(),
            receiver_id.clone(),
            env::current_account_id(),
            estimate::NO_DEPOSIT,
            estimate::GAS_FOR_ON_MINT,
        ));
    }

    pub fn burn(&mut self, token_id: &TokenId, amount: u128, token: &Token<WrappedFungibleToken>) {
        // TODO: Remove from supply
        let mut balance = self
            .balances
            .get(&env::current_account_id(), token_id)
            .unwrap();
        balance.deposit_mut().sub(amount).unwrap();
        self.balances
            .set(&env::current_account_id(), token_id, balance);

        ext_nep141::burn(
            amount.into(),
            token.metadata().uri().to_owned().unwrap(),
            estimate::NO_DEPOSIT,
            env::prepaid_gas(),
        );
        //TODO: Handle Promise

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
