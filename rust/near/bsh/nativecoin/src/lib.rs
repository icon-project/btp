use std::collections::HashMap;
use std::convert::TryFrom;
use std::fmt::format;

use base_service::BaseService;
use btp_common::btp_address::Address;
use btp_common::errors::{BshError, BtpException, Exception};
use libraries::types::{Account, AccountBalance, AccumulatedAssetFees, Asset, BTPAddress, TokenId};
use libraries::{
    types::messages::BtpMessage, types::messages::NativeCoinServiceMessage,
    types::messages::NativeCoinServiceType, types::messages::SerializedMessage, types::Balances,
    types::MultiTokenResolver, types::NativeCoin, types::Network, types::Owners, types::Token,
    types::Tokens, types::Transfer,
};
use near_sdk::borsh::{self, BorshDeserialize, BorshSerialize};
use near_sdk::serde_json::Value;
use near_sdk::AccountId;
use near_sdk::Balance;
use near_sdk::{
    env,
    json_types::{Base64VecU8, U128, U64},
    log, near_bindgen, require, serde_json, Gas, PanicOnDefault, Promise, PromiseResult,
};
use std::convert::TryInto;
use tiny_keccak::{Hasher, Sha3};
mod external;
use external::*;

#[near_bindgen]
#[derive(BorshDeserialize, BorshSerialize, PanicOnDefault)]
pub struct NativeCoinService {
    fixed_fee: u128,
    native_coin_name: String,
    network: Network,
    base_service: BaseService,
    owners: Owners,
    tokens: Tokens<NativeCoin>,
    balances: Balances,
}

#[near_bindgen]
impl NativeCoinService {
    #[init]
    pub fn new(
        service_name: String,
        bmc: AccountId,
        network: String,
        native_coin: Token<NativeCoin>,
        fixed_fee: u128,
    ) -> Self {
        let mut owners = Owners::new();
        owners.add(&env::current_account_id());

        let mut tokens = <Tokens<NativeCoin>>::new();
        let mut balances = Balances::new();
        balances.add(env::current_account_id(), native_coin.to_owned());
        tokens.add(
            Self::hash_token_id(native_coin.name(), native_coin.network()),
            native_coin.to_owned(),
        );
        Self {
            fixed_fee,
            native_coin_name: native_coin.name().to_owned(),
            network,
            owners,
            base_service: BaseService::new(service_name, bmc),
            tokens,
            balances,
        }
    }

    // * * * * * * * * * * * * * * * * *
    // * * * * * * * * * * * * * * * * *
    // * * * * Internal Validations  * *
    // * * * * * * * * * * * * * * * * *
    // * * * * * * * * * * * * * * * * *

    fn assert_predecessor_is_bmc(&self) {
        require!(
            env::predecessor_account_id() == *self.base_service.bmc(),
            format!("{}", BshError::NotBmc)
        )
    }

    fn assert_transfer_amounts_len_match_returned_amount_len(
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

    fn assert_valid_fee_ratio(&self, fee_numerator: u128, token: &Token<NativeCoin>) {
        require!(
            fee_numerator <= token.denominator(),
            format!("{}", BshError::InvalidSetting),
        );
    }

    fn assert_valid_service(&self, service: &String) {
        require!(
            self.base_service.name() == service,
            format!("{}", BshError::InvalidService)
        )
    }

    /// Check whether signer account id is an owner
    fn assert_have_permission(&self) {
        require!(
            self.owners.contains(&env::predecessor_account_id()),
            format!("{}", BshError::PermissionNotExist)
        );
    }

    fn assert_have_deposit(&self) {
        require!(
            env::attached_deposit() > 0,
            format!("{}", BshError::NotMinimumDeposit)
        );
    }

    fn asser_minimum_fixed_fee(&self, amount: Balance) {
        require!(
            amount > 0,
            format!("{}", BshError::NotMinimumDeposit).as_str()
        )
    }

    fn assert_have_sufficient_deposit(
        &self,
        account: &AccountId,
        token: &Token<NativeCoin>,
        amount: u128,
    ) {
        // TODO:
        if let Some(balance) = self.balances.get(account.clone(), token.clone()) {
            require!(
                balance.deposit() > amount, // Include Fees and fixed charges
                format!("{}", BshError::NotMinimumDeposit).as_str()
            );
        } else {
            env::panic_str(format!("{}", BshError::NotMinimumDeposit).as_str());
        }
    }

    fn assert_have_sufficient_external_transfer_deposit(
        &self,
        account: &AccountId,
        token: &Token<NativeCoin>,
        amount: u128,
    ) {
        // TODO:
        if let Some(balance) = self.balances.get(account.clone(), token.clone()) {
            require!(
                balance.deposit() > amount, // Include Fees and fixed charges
                format!("{}", BshError::NotMinimumDeposit)
            );
        } else {
            env::panic_str(format!("{}", BshError::NotMinimumDeposit).as_str());
        }
    }

    fn assert_have_sufficient_withdrawable_deposit(
        &self,
        account: &AccountId,
        token: &Token<NativeCoin>,
        amount: u128,
    ) {
        // TODO:
        if let Some(balance) = self.balances.get(account.clone(), token.clone()) {
            require!(
                balance.deposit() > amount,
                format!("{}", BshError::NotMinimumDeposit)
            );
        } else {
            env::panic_str(format!("{}", BshError::NotMinimumDeposit).as_str());
        }
    }

    fn assert_owner_exists(&self, account: &AccountId) {
        require!(
            self.owners.contains(&account),
            format!("{}", BshError::OwnerNotExist)
        );
    }

    fn assert_owner_does_not_exists(&self, account: &AccountId) {
        require!(
            !self.owners.contains(account),
            format!("{}", BshError::OwnerExist)
        );
    }

    fn assert_owner_is_not_last_owner(&self) {
        require!(self.owners.len() > 1, format!("{}", BshError::LastOwner));
    }

    fn assert_token_does_not_exists(&self, token: &Token<NativeCoin>) {
        let token = self
            .tokens
            .get(&Self::hash_token_id(token.name(), token.network()));
        require!(token.is_none(), format!("{}", BshError::TokenExist))
    }

    // * * * * * * * * * * * * * * * * *
    // * * * * * * * * * * * * * * * * *
    // * * * * * * * Utils * * * * * * *
    // * * * * * * * * * * * * * * * * *
    // * * * * * * * * * * * * * * * * *

    fn hash_token_id(token_name: &String, network: &String) -> TokenId {
        let mut sha3 = Sha3::v256();
        let mut output = [0u8; 32];
        sha3.update(token_name.as_bytes());
        sha3.update(network.as_bytes());
        sha3.finalize(&mut output);
        output.to_vec()
    }

    // * * * * * * * * * * * * * * * * *
    // * * * * * * * * * * * * * * * * *
    // * * * * Owner Management  * * * *
    // * * * * * * * * * * * * * * * * *
    // * * * * * * * * * * * * * * * * *

    /// Add another owner
    /// Caller must be an owner of BTP network
    pub fn add_owner(&mut self, account: AccountId) {
        self.assert_have_permission();
        self.assert_owner_does_not_exists(&account);
        self.owners.add(&account);
    }

    /// Remove an existing owner
    /// Caller must be an owner of BTP network
    pub fn remove_owner(&mut self, account: AccountId) {
        self.assert_have_permission();
        self.assert_owner_exists(&account);
        self.assert_owner_is_not_last_owner();
        self.owners.remove(&account)
    }

    /// Get account ids of registered owners
    /// Caller can be ANY
    pub fn get_owners(&self) -> Vec<AccountId> {
        self.owners.to_vec()
    }

    pub fn set_fee_ratio(&mut self, fee_numerator: u128) {
        self.assert_have_permission();
        let mut token = self
            .tokens
            .get(&Self::hash_token_id(&self.native_coin_name, &self.network))
            .unwrap();
        self.assert_valid_fee_ratio(fee_numerator, &token);
        token.fee_numerator_mut().clone_from(&&fee_numerator);
        self.tokens
            .add(Self::hash_token_id(token.name(), token.network()), token);
    }

    pub fn set_fixed_fee(&mut self, fixed_fee: u128) {
        self.assert_have_permission();
        self.asser_minimum_fixed_fee(fixed_fee);
        self.fixed_fee = fixed_fee;
    }

    pub fn register(&mut self, token: Token<NativeCoin>) {
        self.assert_have_permission();
        self.assert_token_does_not_exists(&token);
        self.tokens.add(
            Self::hash_token_id(token.name(), token.network()),
            token.clone(),
        );
        self.balances.add(env::current_account_id(), token);
    }

    #[payable]
    pub fn deposit(&mut self) {
        let account = env::predecessor_account_id();
        let amount = env::attached_deposit();
        self.assert_have_deposit();

        let token = self
            .tokens
            .get(&Self::hash_token_id(&self.native_coin_name, &self.network))
            .unwrap();

        if let Some(mut balance) = self.balances.get(account.clone(), token.clone()) {
            self.process_deposit(amount, &mut balance);
            self.balances.set(account, token, balance);
        } else {
            let mut balance = AccountBalance::default();
            self.process_deposit(amount, &mut balance);
            self.balances.set(account, token, balance);
        }
    }

    pub fn balance_of(
        &self,
        account: AccountId,
        coin_id: Base64VecU8, // coin_name: String,
                              // coin_network: String
    ) -> Option<AccountBalance> {
        let token = self.tokens.get(&coin_id.clone().into()).expect(
            format!(
                "{}",
                BshError::TokenNotExist {
                    message: serde_json::to_string(&coin_id).unwrap_or_default()
                }
            )
            .as_str(),
        );
        self.balances.get(account.clone(), token.clone())
    }

    // pub fn balance_of_batch(){} //TODO:

    pub fn accumulated_fees(&self) -> Vec<AccumulatedAssetFees> {
        self.tokens
            .to_vec()
            .iter()
            .map(|token| {
                let balance = self
                    .balance_of(
                        env::current_account_id(),
                        Self::hash_token_id(&token.name, &token.network).into(),
                    )
                    .unwrap();

                AccumulatedAssetFees {
                    name: token.name.clone(),
                    network: token.network.clone(),
                    accumulated_fees: balance.refundable().fees(),
                }
            })
            .collect()
    }

    pub fn external_transfer(
        &mut self,
        coin_id: Base64VecU8,
        destination: BTPAddress,
        amount: U128,
    ) {
        let source_account = env::predecessor_account_id();
        let token = self.tokens.get(&coin_id.clone().into()).expect(
            format!(
                "{}",
                BshError::TokenNotExist {
                    message: serde_json::to_string(&coin_id).unwrap_or_default()
                }
            )
            .as_str(),
        );

        self.assert_have_sufficient_external_transfer_deposit(
            &source_account,
            &token,
            amount.into(),
        );

        let mut balance = self
            .balances
            .get(source_account.clone(), token.clone())
            .unwrap();
        let fees = self.process_external_transfer_fees(&mut balance, &token, amount.into());
        self.process_external_transfer(&mut balance, amount.into());

        let message = NativeCoinServiceMessage::new(NativeCoinServiceType::RequestCoinTransfer {
            source: source_account.clone().into(),
            destination: destination.account_id().into(),
            assets: vec![Asset::new(
                Self::hash_token_id(&token.name(), &token.network()),
                balance.refundable().deposit(),
                fees,
            )],
        });

        self.base_service
            .send_service_message(destination.network_address().unwrap(), message.into());
        self.balances.set(source_account, token, balance);
    }

    // pub fn external_transfer_batch(){} //TODO:

    pub fn internal_transfer(
        &mut self,
        coin_id: Base64VecU8,
        // coin_name: String,
        // coin_network: String,
        amount: U128,
        account: AccountId,
    ) {
        let source_account = env::predecessor_account_id();
        let token = self.tokens.get(&coin_id.clone().into()).expect(
            format!(
                "{}",
                BshError::TokenNotExist {
                    message: serde_json::to_string(&coin_id).unwrap_or_default()
                }
            )
            .as_str(),
        );
        self.assert_have_sufficient_deposit(&source_account, &token, amount.into());
        let mut source_balance = self
            .balances
            .get(source_account.clone(), token.clone())
            .unwrap();
        if let Some(mut destination_balance) = self.balances.get(account.clone(), token.clone()) {
            source_balance.deposit_mut().sub(amount.into());
            destination_balance.deposit_mut().add(amount.into());
            self.balances
                .set(source_account, token.clone(), source_balance);
            self.balances
                .set(account, token.clone(), destination_balance);
        } else {
            source_balance.deposit_mut().sub(amount.into());
            let mut destination_balance = AccountBalance::default();
            destination_balance.deposit_mut().add(amount.into());
            // TODO: Calculate Deposit
            self.balances
                .set(source_account, token.clone(), source_balance);
            self.balances
                .set(account, token.clone(), destination_balance);
        }
    }

    fn process_deposit(&mut self, amount: u128, balance: &mut AccountBalance) {
        let storage_cost = 0;
        balance.storage_mut().add(storage_cost);
        balance.deposit_mut().add(amount - storage_cost);
    }

    fn process_external_transfer_fees(
        &mut self,
        balance: &mut AccountBalance,
        token: &Token<NativeCoin>,
        mut amount: Balance,
    ) -> u128 {
        let fees = amount.mul(token.fee_numerator()).div(token.denominator());
        balance.refundable_mut().fees_mut().add(*fees);
        balance.deposit_mut().sub(*fees);
        fees.clone()
    }

    fn process_external_transfer(&mut self, balance: &mut AccountBalance, amount: Balance) {
        balance.deposit_mut().sub(amount);
        balance.refundable_mut().deposit_mut().add(amount);
    }

    pub fn handle_btp_message(&mut self, message: BtpMessage<SerializedMessage>) {
        self.assert_predecessor_is_bmc();
        self.assert_valid_service(message.service());
        let outcome = self.handle_service_message(message.try_into());
        if outcome.is_err() {
            panic!("{}", outcome.unwrap_err()); // TODO
        }
    }

    // TODO: are we receiving source and destination address as btp address??
    fn handle_service_message(
        &mut self,
        message: Result<BtpMessage<NativeCoinServiceMessage>, BshError>,
    ) -> Result<(), BshError> {
        if let Some(service_message) = message.clone()?.message() {
            match service_message.service_type() {
                NativeCoinServiceType::RequestCoinTransfer {
                    ref source,
                    ref destination,
                    ref assets,
                } => self.handle_coin_transfer(message?.source(), source, destination, assets),
                _ => Ok(()), // TODO
            }
        } else {
            unimplemented!() // TODO
        }
    }

    #[payable]
    pub fn withdraw(&mut self) {
        let account = env::predecessor_account_id();
        let token = self
            .tokens
            .get(&Self::hash_token_id(&self.native_coin_name, &self.network))
            .unwrap();
        self.assert_have_sufficient_withdrawable_deposit(&account, &token, 1);
        let mut balance = self.balances.get(account.clone(), token.clone()).unwrap();
        let amount = balance.deposit();
        balance.deposit_mut().sub(amount);
        Promise::new(account).transfer(amount);
    }

    fn handle_coin_transfer(
        &mut self,
        message_source: &BTPAddress,
        source_account: &String,
        destination_account: &String,
        assets: &Vec<Asset>,
    ) -> Result<(), BshError> {
        // Validate is all the assets are registered
        let mut unregistered_tokens: Vec<TokenId> = vec![];
        assets.iter().for_each(|asset| {
            if !self.tokens.contains(&asset.token_id()) {
                unregistered_tokens.push(asset.token_id().to_owned())
            }
        });
        if unregistered_tokens.len() > 0 {
            return Err(BshError::TokenNotExist {
                message: unregistered_tokens
                    .iter()
                    .map(|token_id| format!("{:x?}", token_id))
                    .collect::<Vec<String>>()
                    .join(", "),
            });
        }

        // Validate if all assets are from genuine source
        let mut valid_assets: Vec<Token<NativeCoin>> = Vec::new();
        assets.iter().for_each(|asset| {

        });

        assets.iter().for_each(|asset| {
            let token = self.tokens.get(asset.token_id()).unwrap();
            //if 
            // Mint
            self.mint(token, asset.amount());
            // Transfer

        });
        Ok(())
    }

    fn mint(&mut self, token: Token<NativeCoin>, amount: u128) {
        // TODO: Add to supply
        let mut balance = self
            .balances
            .get(env::current_account_id(), token.clone())
            .unwrap();
        balance.deposit_mut().add(amount);
        self.balances.set(env::current_account_id(), token, balance);
    }

    fn refund_balance_amount(
        &self,
        index: usize,
        returned_amount: &U128,
        token_ids: Vec<TokenId>,
    ) -> U128 {
        let returned_amount: u128 = returned_amount.clone().into();
        if returned_amount == 0 {
            return U128::from(0);
        }
        let mut unregistered_tokens: Vec<TokenId> = vec![];
        token_ids.iter().for_each(|asset| {
            if !self.tokens.contains(&token_ids[index]) {
                unregistered_tokens.push(token_ids[index].to_owned())
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

        U128::from(0)
    }
    // fn mt_transfer(){}
    // fn mt_transfer_call(){}
    //
    // Mint
    // Burn
    // Vault
}

#[near_bindgen]
impl MultiTokenResolver for NativeCoinService {
    #[private]
    fn mt_resolve_transfer(
        &mut self,
        sender_id: AccountId,
        receiver_id: AccountId,
        token_ids: Vec<libraries::types::TokenId>,
        amounts: Vec<U128>,
    ) -> Vec<U128> {
        let returned_amounts: Vec<U128> = match env::promise_result(0) {
            PromiseResult::NotReady => env::abort(),
            PromiseResult::Successful(value) => {
                if let Ok(returned_amount) = near_sdk::serde_json::from_slice::<Vec<U128>>(&value) {
                    self.assert_transfer_amounts_len_match_returned_amount_len(
                        &amounts,
                        &returned_amount,
                    );
                    returned_amount
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
                self.refund_balance_amount(index, returned_amount, token_ids.clone())
            })
            .collect()
    }
}
