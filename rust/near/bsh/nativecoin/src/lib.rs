use btp_common::btp_address::Address;
use btp_common::errors::BshError;
use libraries::types::{
    Account, AccountBalance, AccumulatedAssetFees, Asset, BTPAddress, TokenId, WrappedNativeCoin,
};
use libraries::{
    types::messages::BtpMessage, types::messages::SerializedMessage,
    types::messages::TokenServiceMessage, types::messages::TokenServiceType, types::Balances,
    types::Math, types::MultiTokenCore, types::MultiTokenResolver, types::Network, types::Owners,
    types::Requests, types::StorageBalances, types::Token, types::TokenFees, types::Tokens,
};
use near_sdk::borsh::{self, BorshDeserialize, BorshSerialize};
use near_sdk::collections::LazyOption;
use near_sdk::serde_json::{to_value, Value};
use near_sdk::PromiseOrValue;
use near_sdk::{assert_one_yocto, AccountId};
use near_sdk::{
    env,
    json_types::{Base64VecU8, U128},
    log, near_bindgen, require, Gas, PanicOnDefault, Promise, PromiseResult,
};
use near_sdk::json_types::Base64VecU8;
use near_sdk::collections::LazyOption;
use std::convert::TryFrom;
use std::convert::TryInto;
mod external;
use external::*;
mod accounting;
mod assertion;
mod coin_management;
mod estimate;
mod fee_management;
mod messaging;
mod owner_management;
mod transfer;
mod types;
mod util;
pub use types::RegisteredCoins;

pub static FEE_DENOMINATOR: u128 = 10_u128.pow(4);

#[near_bindgen]
#[derive(BorshDeserialize, BorshSerialize, PanicOnDefault)]
pub struct NativeCoinService {
    native_coin_name: String,
    network: Network,
    owners: Owners,
    tokens: Tokens<WrappedNativeCoin>,
    balances: Balances,
    storage_balances: StorageBalances,
    token_fees: TokenFees,
    requests: Requests,
    serial_no: i128,
    bmc: AccountId,
    name: String,
    fee_numerator: u128,

    #[cfg(feature = "testable")]
    pub message: LazyOption<Base64VecU8>,

    registered_coins: RegisteredCoins,
}

#[near_bindgen]
impl NativeCoinService {
    #[init]
    pub fn new(
        service_name: String,
        bmc: AccountId,
        network: String,
        native_coin: Token<WrappedNativeCoin>,
        fee_numerator: U128,
    ) -> Self {
        require!(!env::state_exists(), "Already initialized");
        let mut owners = Owners::new();
        owners.add(&env::current_account_id());

        let mut tokens = <Tokens<WrappedNativeCoin>>::new();
        let mut balances = Balances::new();
        let native_coin_id = Self::hash_token_id(native_coin.name());

        balances.add(&env::current_account_id(), &native_coin_id);
        tokens.add(&native_coin_id, &native_coin);

        let mut token_fees = TokenFees::new();
        token_fees.add(&native_coin_id);
        Self {
            native_coin_name: native_coin.name().to_owned(),
            network,
            owners,
            tokens,
            balances,
            storage_balances: StorageBalances::new(),
            token_fees,
            serial_no: Default::default(),
            requests: Requests::new(),
            bmc,
            name: service_name,
            fee_numerator: fee_numerator.into(),

            #[cfg(feature = "testable")]
            message: LazyOption::new(b"message".to_vec(), None),
            registered_coins: RegisteredCoins::new(),
        }
    }

    fn bmc(&self) -> &AccountId {
        &self.bmc
    }

    fn name(&self) -> &String {
        &self.name
    }

    #[cfg(feature = "testable")]
    pub fn serial_no(&self) -> i128 {
        self.serial_no
    }

    #[cfg(not(feature = "testable"))]
    fn serial_no(&self) -> i128 {
        self.serial_no
    }

    fn requests(&self) -> &Requests {
        &self.requests
    }

    fn requests_mut(&mut self) -> &mut Requests {
        &mut self.requests
    }

    fn process_deposit(&mut self, amount: u128, balance: &mut AccountBalance) {
        let storage_cost = 0;
        balance.deposit_mut().add(amount - storage_cost).unwrap();
    }
}
