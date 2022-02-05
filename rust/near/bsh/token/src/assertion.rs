use super::*;

impl TokenService {
    // * * * * * * * * * * * * * * * * *
    // * * * * * * * * * * * * * * * * *
    // * * * * Internal Validations  * *
    // * * * * * * * * * * * * * * * * *
    // * * * * * * * * * * * * * * * * *

    pub fn assert_predecessor_is_bmc(&self) {
        require!(
            env::predecessor_account_id() == *self.bmc(),
            format!("{}", BshError::NotBmc)
        )
    }

    pub fn assert_token_id_len_match_amount_len(&self, token_ids: &Vec<TokenId>, amounts: &Vec<U128>) {
        require!(
            token_ids.len() == amounts.len(),
            format!(
                "{}",
                BshError::InvalidCount {
                    message: "Token Ids and amounts".to_string()
                }
            ),
        );
    }

    pub fn assert_transfer_amounts_len_match_returned_amount_len(
        &self,
        amounts: &Vec<U128>,
        returned_amount: &Vec<U128>,
    ) {
        require!(
            returned_amount.len() == amounts.len(),
            format!(
                "{}",
                BshError::InvalidCount {
                    message: "Transfer amounts and returned amounts".to_string()
                }
            ),
        );
    }

    pub fn assert_valid_fee_ratio(&self, fee_numerator: u128, token: &Token<WrappedFungibleToken>) {
        require!(
            fee_numerator <= token.denominator(),
            format!("{}", BshError::InvalidSetting),
        );
    }

    pub fn assert_valid_service(&self, service: &String) {
        require!(
            self.name() == service,
            format!("{}", BshError::InvalidService)
        )
    }

    /// Check whether signer account id is an owner
    pub fn assert_have_permission(&self) {
        require!(
            self.owners.contains(&env::predecessor_account_id()),
            format!("{}", BshError::PermissionNotExist)
        );
    }

    pub fn assert_have_minimum_amount(&self, amount: u128) {
        require!(
            amount > 0,
            format!("{}", BshError::NotMinimumAmount)
        );
    }

    pub fn assert_have_sufficient_balance(&self, amount: u128) {
        require!(
            env::account_balance() > amount,
            format!(
                "{}",
                BshError::NotMinimumBalance {
                    account: env::current_account_id().to_string()
                }
            )
        );
    }

    pub fn assert_have_sufficient_deposit(
        &self,
        account: &AccountId,
        token_id: &TokenId,
        amount: u128,
        fees: Option<u128>,
    ) {
        let amount = std::cmp::max(amount, fees.unwrap_or_default());
        if let Some(balance) = self.balances.get(&account, &token_id) {
            require!(
                balance.deposit() >= amount,
                format!("{}", BshError::NotMinimumDeposit)
            );
        } else {
            env::panic_str(format!("{}", BshError::NotMinimumDeposit).as_str());
        }
    }

    pub fn assert_have_sufficient_refundable(
        &self,
        account: &AccountId,
        token_id: &TokenId,
        amount: u128,
    ) {
        if let Some(balance) = self.balances.get(&account, &token_id) {
            require!(
                balance.refundable() >= amount,
                format!("{}", BshError::NotMinimumRefundable)
            );
        } else {
            env::panic_str(format!("{}", BshError::NotMinimumRefundable).as_str());
        }
    }

    pub fn assert_sender_is_not_receiver(&self, sender_id: &AccountId, receiver_id: &AccountId) {
        require!(
            sender_id != receiver_id,
            format!("{}", BshError::SameSenderReceiver)
        );
    }

    pub fn assert_owner_exists(&self, account: &AccountId) {
        require!(
            self.owners.contains(&account),
            format!("{}", BshError::OwnerNotExist)
        );
    }

    pub fn assert_owner_does_not_exists(&self, account: &AccountId) {
        require!(
            !self.owners.contains(account),
            format!("{}", BshError::OwnerExist)
        );
    }

    pub fn assert_owner_is_not_last_owner(&self) {
        require!(self.owners.len() > 1, format!("{}", BshError::LastOwner));
    }

    pub fn assert_token_does_not_exists(&self, token: &Token<WrappedFungibleToken>) {
        let token = self.tokens.get(&Self::hash_token_id(token.name()));
        require!(token.is_none(), format!("{}", BshError::TokenExist))
    }

    pub fn assert_tokens_exists(&self, token_ids: &Vec<TokenId>) {
        let mut unregistered_tokens: Vec<TokenId> = vec![];
        token_ids.iter().for_each(|token_id| {
            if !self.tokens.contains(&token_id) {
                unregistered_tokens.push(token_id.to_owned())
            }
        });

        require!(
            unregistered_tokens.len() == 0,
            format!(
                "{}",
                BshError::TokenNotExist {
                    message: unregistered_tokens
                        .iter()
                        .map(|token_id| format!("{:x?}", token_id))
                        .collect::<Vec<String>>()
                        .join(", "),
                }
            ),
        );
    }

    pub fn assert_token_registered(&self, token_account: &AccountId) {
        require!(
            self.registered_tokens.contains(token_account),
            format!(
                "{}",
                BshError::TokenNotRegistered
            )
        )
    }
}
