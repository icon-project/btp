use std::convert::TryFrom;
use btp_common::btp_address::Address;
use btp_common::errors::{BshError};
use libraries::types::messages::Message;
use libraries::types::{
    Account, AccountBalance, AccumulatedAssetFees, Asset, BTPAddress, TokenId,
};
use libraries::{
    types::messages::BtpMessage, types::messages::TokenServiceMessage,
    types::messages::TokenServiceType, types::messages::SerializedMessage, types::Balances,
    types::MultiTokenCore, types::MultiTokenResolver, types::NativeCoin, types::Network,
    types::Owners, types::StorageBalances, types::Token, types::TokenFees,
    types::Tokens, types::Math, types::Requests
};
use near_sdk::borsh::{self, BorshDeserialize, BorshSerialize};
use near_sdk::collections::LazyOption;
use near_sdk::json_types::Base64VecU8;
use near_sdk::serde_json::{to_value, Value};
use near_sdk::{assert_one_yocto, AccountId};
use near_sdk::{
    env,
    json_types::{U128},
    log, near_bindgen, require, Gas, PanicOnDefault, Promise, PromiseResult,
};
use near_sdk::{PromiseOrValue};
use std::convert::TryInto;
use tiny_keccak::{Hasher, Sha3};
mod external;
use external::*;
mod estimate;
mod assertion;
mod util;
mod owner_management;
mod coin_management;
mod accounting;
mod fee_management;
mod transfer;
mod messaging;
mod multi_token;

#[near_bindgen]
#[derive(BorshDeserialize, BorshSerialize, PanicOnDefault)]
pub struct NativeCoinService {
    native_coin_name: String,
    network: Network,
    owners: Owners,
    tokens: Tokens<NativeCoin>,
    balances: Balances,
    storage_balances: StorageBalances,
    token_fees: TokenFees,
    requests: Requests,
    serial_no: i128,
    bmc: AccountId,
    name: String,

    #[cfg(feature = "testable")]
    pub message: LazyOption<Base64VecU8>,
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
        require!(!env::state_exists(), "Already initialized");
        let mut owners = Owners::new();
        owners.add(&env::current_account_id());

        let mut tokens = <Tokens<NativeCoin>>::new();
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

            #[cfg(feature = "testable")]
            message: LazyOption::new(b"message".to_vec(), None)
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
        // TODO
        balance.deposit_mut().add(amount - storage_cost);
    }
}