use super::*;

impl NativeCoinService {
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

    pub fn assert_coin_id_len_match_amount_len(
        &self,
        coin_ids: &Vec<CoinId>,
        amounts: &Vec<U128>,
    ) {
        require!(
            coin_ids.len() == amounts.len(),
            format!(
                "{}",
                BshError::InvalidCount {
                    message: "Coin Ids and amounts".to_string()
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

    pub fn assert_valid_fee_ratio(&self, fee_numerator: u128) {
        require!(
            fee_numerator <= FEE_DENOMINATOR,
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
        require!(amount > 0, format!("{}", BshError::NotMinimumAmount));
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
        coin_id: &CoinId,
        amount: u128,
        fees: Option<u128>,
    ) {
        let amount = std::cmp::max(amount, fees.unwrap_or_default());
        if let Some(balance) = self.balances.get(&account, &coin_id) {
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
        coin_id: &CoinId,
        amount: u128,
    ) {
        if let Some(balance) = self.balances.get(&account, &coin_id) {
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

    pub fn assert_coin_does_not_exists(&self, coin: &Coin) {
        let coin = self.coins.get(&Self::hash_coin_id(coin.name()));
        require!(coin.is_none(), format!("{}", BshError::TokenExist))
    }

    pub fn assert_coins_exists(&self, coin_ids: &Vec<CoinId>) {
        let mut unregistered_coins: Vec<CoinId> = vec![];
        coin_ids.iter().for_each(|coin_id| {
            if !self.coins.contains(&coin_id) {
                unregistered_coins.push(coin_id.to_owned())
            }
        });

        require!(
            unregistered_coins.len() == 0,
            format!(
                "{}",
                BshError::TokenNotExist {
                    message: unregistered_coins
                        .iter()
                        .map(|coin_id| format!("{:x?}", coin_id))
                        .collect::<Vec<String>>()
                        .join(", "),
                }
            ),
        );
    }

    pub fn assert_coin_registered(&self, coin_account: &AccountId) {
        require!(
            self.registered_coins.contains(coin_account),
            format!("{}", BshError::TokenNotRegistered)
        )
    }
}
