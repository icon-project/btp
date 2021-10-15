use std::collections::HashMap;
use std::convert::TryFrom;
use std::fmt::format;

use base_service::BaseService;
use btp_common::btp_address::Address;
use btp_common::errors::{BshError, BtpException, Exception};
use libraries::types::{
    Account, AccountBalance, AccumulatedAssetFees, Asset, BTPAddress, TokenId, TokenName,
};
use libraries::{
    types::messages::BtpMessage, types::messages::NativeCoinServiceMessage,
    types::messages::NativeCoinServiceType, types::messages::SerializedMessage, types::Balances,
    types::MultiTokenCore, types::MultiTokenResolver, types::NativeCoin, types::Network,
    types::Owners, types::StorageBalances, types::Token, types::TokenFees, types::TokenItem,
    types::Tokens, types::Transfer,
};
use near_sdk::borsh::{self, BorshDeserialize, BorshSerialize};
use near_sdk::serde_json::{to_value, Value};
use near_sdk::{assert_one_yocto, AccountId};
use near_sdk::{
    env,
    json_types::{Base64VecU8, U128, U64},
    log, near_bindgen, require, serde_json, Gas, PanicOnDefault, Promise, PromiseResult,
};
use near_sdk::{Balance, PromiseOrValue};
use std::convert::TryInto;
use tiny_keccak::{Hasher, Sha3};
mod external;
use external::*;
mod estimate;

#[near_bindgen]
#[derive(BorshDeserialize, BorshSerialize, PanicOnDefault)]
pub struct NativeCoinService {
    native_coin_name: String,
    network: Network,
    base_service: BaseService,
    owners: Owners,
    tokens: Tokens<NativeCoin>,
    balances: Balances,
    storage_balances: StorageBalances,
}

#[near_bindgen]
impl NativeCoinService {
    #[init]
    pub fn new(
        service_name: String,
        bmc: AccountId,
        network: String,
        native_coin: Token<NativeCoin>,
    ) -> Self {
        let mut owners = Owners::new();
        owners.add(&env::current_account_id());

        let mut tokens = <Tokens<NativeCoin>>::new();
        let mut balances = Balances::new();
        let native_coin_id = Self::hash_token_id(native_coin.name());
        balances.add(&env::current_account_id(), &native_coin_id);
        tokens.add(&native_coin_id, &native_coin);
        Self {
            native_coin_name: native_coin.name().to_owned(),
            network,
            owners,
            base_service: BaseService::new(service_name, bmc),
            tokens,
            balances,
            storage_balances: StorageBalances::new(),
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

    fn assert_token_id_len_match_amount_len(&self, token_ids: &Vec<TokenId>, amounts: &Vec<U128>) {
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

    fn assert_have_sufficient_balance(&self, amount: u128) {
        println!("{}", env::account_balance());
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

    fn assert_have_sufficient_deposit(
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
                format!("{}", BshError::NotMinimumDeposit).as_str()
            );
        } else {
            env::panic_str(format!("{}", BshError::NotMinimumDeposit).as_str());
        }
    }

    fn assert_sender_is_not_receiver(&self, sender_id: &AccountId, receiver_id: &AccountId) {
        require!(
            sender_id != receiver_id,
            format!("{}", BshError::SameSenderReceiver)
        );
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
        let token = self.tokens.get(&Self::hash_token_id(token.name()));
        require!(token.is_none(), format!("{}", BshError::TokenExist))
    }

    fn assert_tokens_exists(&self, token_ids: &Vec<TokenId>) {
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

    // * * * * * * * * * * * * * * * * *
    // * * * * * * * * * * * * * * * * *
    // * * * * * * * Utils * * * * * * *
    // * * * * * * * * * * * * * * * * *
    // * * * * * * * * * * * * * * * * *

    fn hash_token_id(token_name: &String) -> TokenId {
        let mut sha3 = Sha3::v256();
        let mut output = [0u8; 32];
        sha3.update(token_name.as_bytes());
        sha3.finalize(&mut output);
        output.to_vec()
    }

    fn calculate_token_transfer_fee(
        token: &Token<NativeCoin>,
        mut amount: u128,
    ) -> Result<u128, String> {
        Ok(*(amount
            .mul(token.fee_numerator())?
            .div(token.denominator())?))
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
        self.owners.to_vec().into()
    }

    // * * * * * * * * * * * * * * * * *
    // * * * * * * * * * * * * * * * * *
    // * * * * Token Management  * * * *
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
        let token_id = Self::hash_token_id(&self.native_coin_name);
        let mut token = self.tokens.get(&token_id).unwrap();
        self.assert_valid_fee_ratio(fee_numerator, &token);

        token.fee_numerator_mut().clone_from(&&fee_numerator);
        self.tokens.set(&token_id, &token);
    }

    #[payable]
    pub fn deposit(&mut self) {
        let account = env::predecessor_account_id();
        let amount = env::attached_deposit();
        self.assert_have_deposit();
        let token_id = Self::hash_token_id(&self.native_coin_name);

        let mut balance = match self.balances.get(&account, &token_id) {
            Some(balance) => balance,
            None => AccountBalance::default(),
        };

        self.process_deposit(amount, &mut balance);
        self.balances.set(&account, &token_id, balance);
    }

    #[payable]
    pub fn withdraw(&mut self, amount: U128) {
        // To Prevent Spam
        assert_one_yocto();

        let amount: u128 = amount.into();
        let account = env::predecessor_account_id();
        let native_coin_id = Self::hash_token_id(&self.native_coin_name);

        // Check if user is having requested amount including the yocto just deposited
        self.assert_have_sufficient_deposit(&account, &native_coin_id, amount, None);

        // Check if current account have sufficient balance
        self.assert_have_sufficient_balance(1 + amount);

        let mut balance = self.balances.get(&account, &native_coin_id).unwrap();
        balance.deposit_mut().sub(amount).unwrap();
        self.balances
            .set(&account.clone(), &native_coin_id, balance);

        Promise::new(account).transfer(amount + 1);
    }

    // TODO: Reclaim

    pub fn transfer(&mut self, coin_id: TokenId, destination: BTPAddress, amount: U128) {
        let sender_id = env::predecessor_account_id();
        self.assert_tokens_exists(&vec![coin_id.clone()]);

        let asset = self
            .process_external_transfer(&coin_id, &sender_id, amount.into())
            .unwrap();

        self.send_request(sender_id, destination, vec![asset]);
    }

    pub fn transfer_batch(
        &mut self,
        coin_ids: Vec<TokenId>,
        destination: BTPAddress,
        amounts: Vec<U128>,
    ) {
        let sender_id = env::predecessor_account_id();
        self.assert_tokens_exists(&coin_ids);

        let assets = coin_ids
            .iter()
            .enumerate()
            .map(|(index, coin_id)| {
                self.process_external_transfer(coin_id, &sender_id, amounts[index].into())
                    .unwrap()
            })
            .collect::<Vec<Asset>>();

        self.send_request(sender_id, destination, assets);
    }

    fn process_external_transfer(
        &mut self,
        coin_id: &TokenId,
        sender_id: &AccountId,
        mut amount: u128,
    ) -> Result<Asset, String> {
        let token = self.tokens.get(&coin_id).unwrap();
        let fees = Self::calculate_token_transfer_fee(&token, amount)?;

        self.assert_have_sufficient_deposit(&sender_id, &coin_id, amount, Some(fees));

        amount.sub(fees)?;
        let mut balance = self.balances.get(&sender_id, &coin_id).unwrap();

        // Handle Fees
        balance.locked_mut().fees_mut().add(fees)?;
        balance.deposit_mut().sub(fees)?;

        // Handle Deposit
        balance.deposit_mut().sub(amount)?;
        balance.locked_mut().deposit_mut().add(amount)?;

        self.balances.set(&sender_id, &coin_id, balance);

        let balance = self.balances.get(&sender_id, &coin_id).unwrap();
        Ok(Asset::new(
            token.name().clone(),
            balance.locked().deposit(),
            fees,
        ))
    }

    fn send_request(&mut self, sender_id: AccountId, destination: BTPAddress, assets: Vec<Asset>) {
        let message = NativeCoinServiceMessage::new(NativeCoinServiceType::RequestCoinTransfer {
            source: sender_id.clone().into(),
            destination: destination.account_id().into(),
            assets,
        });

        self.base_service
            .send_service_message(destination.network_address().unwrap(), message.into());
    }

    #[cfg(feature = "testable")]
    pub fn account_balance(
        &self,
        owner_id: AccountId,
        token_id: TokenId,
    ) -> Option<AccountBalance> {
        self.balances.get(&owner_id, &token_id)
    }

    #[cfg(feature = "testable")]
    pub fn last_request(&self) -> Option<SerializedMessage> {
        self.base_service.request(self.base_service.serial_no())
    }

    pub fn accumulated_fees(&self) -> Vec<AccumulatedAssetFees> {
        self.tokens
            .to_vec()
            .iter()
            .map(|token| {
                let token_id = Self::hash_token_id(&token.name);
                let token = self.tokens.get(&token_id).unwrap();
                let balance = self
                    .balances
                    .get(&env::current_account_id(), &token_id)
                    .unwrap();
                AccumulatedAssetFees {
                    name: token.name().clone(),
                    network: token.network().clone(),
                    accumulated_fees: balance.locked().fees(),
                }
            })
            .collect()
    }

    fn internal_transfer(
        &mut self,
        sender_id: &AccountId,
        receiver_id: &AccountId,
        token_id: &TokenId,
        amount: u128,
    ) {
        self.assert_sender_is_not_receiver(sender_id, receiver_id);
        self.assert_have_sufficient_deposit(sender_id, token_id, amount, None);

        let mut sender_balance = self.balances.get(sender_id, token_id).unwrap();
        sender_balance.deposit_mut().sub(amount);

        let receiver_balance = match self.balances.get(&receiver_id, token_id) {
            Some(mut balance) => {
                balance.deposit_mut().add(amount);
                balance
            }
            None => {
                let mut balance = AccountBalance::default();
                let storage_deposit = 0_u128; // TODO: Calculate storage deposit
                let amount = amount - storage_deposit;
                balance.deposit_mut().add(amount);
                balance
            }
        };

        self.balances.set(sender_id, token_id, sender_balance);
        self.balances.set(receiver_id, token_id, receiver_balance);
    }

    fn internal_transfer_batch(
        &mut self,
        sender_id: &AccountId,
        receiver_id: &AccountId,
        token_ids: &Vec<TokenId>,
        amounts: &Vec<U128>,
    ) {
        token_ids.iter().enumerate().for_each(|(index, token_id)| {
            self.internal_transfer(sender_id, receiver_id, token_id, amounts[index].into())
        });
    }

    fn process_deposit(&mut self, amount: u128, balance: &mut AccountBalance) {
        let storage_cost = 0;

        balance.deposit_mut().add(amount - storage_cost);
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

    fn handle_coin_transfer(
        &mut self,
        message_source: &BTPAddress,
        sender_id: &String,
        receiver_id: &String,
        assets: &Vec<Asset>,
    ) -> Result<(), BshError> {
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

        // Validate if all assets are from genuine source
        let mut invalid_tokens: Vec<String> = Vec::new();
        let tokens = token_ids
            .into_iter()
            .map(|(asset_index, token_id)| {
                (
                    asset_index,
                    token_id.clone(),
                    self.tokens.get(&token_id).unwrap(),
                )
            })
            .filter(|(index, _, token)| {
                return if token.network() != &message_source.network_address().unwrap()
                    || token.network() != &self.network
                {
                    invalid_tokens.push(assets[index.to_owned()].token().to_owned());
                    false
                } else {
                    true
                };
            })
            .collect::<Vec<(usize, TokenId, Token<NativeCoin>)>>();

        if invalid_tokens.len() > 0 {
            return Err(BshError::Reverted {
                message: "Illegal Token Detected".to_string(),
            });
        }

        tokens.iter().for_each(|(index, token_id, token)| {
            if token.network() != &self.network {
                self.mint(token_id, assets[index.to_owned()].amount());
            }

            #[cfg(feature = "testable")]
            self.internal_transfer(
                &env::current_account_id(),
                &receiver_id,
                token_id,
                assets[index.to_owned()].amount(),
            );

            #[cfg(not(feature = "testable"))]
            ext_self::mt_transfer(
                receiver_id.clone(),
                token_id.to_owned(),
                U128::from(assets[index.to_owned()].amount()),
                None,
                env::current_account_id(),
                estimate::ONE_YOCTO,
                estimate::GAS_FOR_MT_TRANSFER,
            );
        });
        Ok(())
    }

    fn mint(&mut self, token_id: &TokenId, amount: u128) {
        // TODO: Add to supply
        let mut balance = self
            .balances
            .get(&env::current_account_id(), token_id)
            .unwrap();
        balance.deposit_mut().add(amount);
        self.balances
            .set(&env::current_account_id(), token_id, balance);
    }

    fn refund_balance_amount(
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
            let refund_amount = std::cmp::min(receiver_balance.deposit(), unused_amount); // TODO: Rewalk
            receiver_balance.deposit_mut().sub(refund_amount);
            self.balances
                .set(&receiver_id.clone(), token_id, receiver_balance);

            if let Some(mut sender_balance) = self.balances.get(sender_id, token_id) {
                sender_balance.deposit_mut().add(refund_amount);
                self.balances
                    .set(&sender_id.clone(), token_id, sender_balance);
                let amount: u128 = amounts[index].into();
                return U128::from(amount - refund_amount);
            }
        }

        U128::from(0)
    }
}

#[near_bindgen]
impl MultiTokenCore for NativeCoinService {
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
        memo: Option<String>,
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

    fn total_supply(&self, token_id: TokenId) -> U128 {
        todo!();
    }

    fn total_supply_batch(&self, token_ids: Vec<TokenId>) -> Vec<U128> {
        todo!();
    }
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
