use btp_common::btp_address::Address;
use btp_common::errors::BshError;
use libraries::types::{Account, AccountBalance, AccumulatedAssetFees, TransferableAsset, BTPAddress, AssetId,Asset,Assets,AssetFees, AssetMetadata};
use libraries::{
    types::messages::BtpMessage, types::messages::SerializedMessage,
    types::messages::TokenServiceMessage, types::messages::TokenServiceType, types::Balances,
    types::Math, types::Network, types::Owners,
    types::Requests, types::StorageBalances,
    types::WrappedFungibleToken,
};
use near_sdk::borsh::{self, BorshDeserialize, BorshSerialize};
use near_sdk::collections::LazyOption;
use near_sdk::json_types::Base64VecU8;
use near_sdk::serde_json::{json, to_value, Value};
use near_sdk::PromiseOrValue;
use near_sdk::{assert_one_yocto, AccountId};
use near_sdk::{
    env, json_types::U128, log, near_bindgen, require, Gas, PanicOnDefault, Promise, PromiseResult,
};
use std::convert::TryFrom;
use std::convert::TryInto;
mod external;
use external::*;
mod accounting;
mod assertion;
mod estimate;
mod fee_management;
mod messaging;
mod owner_management;
mod token_management;
mod transfer;
mod types;
mod util;
pub use types::RegisteredTokens;
pub type TokenFee = AssetFees;
pub type TokenId = AssetId;
pub type Token = Asset<WrappedFungibleToken>;
pub type Tokens = Assets<WrappedFungibleToken>;

pub static FEE_DENOMINATOR: u128 = 10_u128.pow(4);

#[near_bindgen]
#[derive(BorshDeserialize, BorshSerialize, PanicOnDefault)]
pub struct TokenService {
    network: Network,
    owners: Owners,
    tokens: Tokens,
    balances: Balances,
    storage_balances: StorageBalances,
    token_fees: TokenFee,
    requests: Requests,
    serial_no: i128,
    bmc: AccountId,
    name: String,
    fee_numerator: u128,
    registered_tokens: RegisteredTokens,

    #[cfg(feature = "testable")]
    pub message: LazyOption<Base64VecU8>,
}

#[near_bindgen]
impl TokenService {
    #[init]
    pub fn new(service_name: String, bmc: AccountId, network: String, fee_numerator: U128) -> Self {
        require!(!env::state_exists(), "Already initialized");
        let mut owners = Owners::new();
        owners.add(&env::current_account_id());
        Self {
            network,
            owners,
            tokens: <Tokens>::new(),
            balances: Balances::new(),
            storage_balances: StorageBalances::new(),
            token_fees: TokenFee::new(),
            serial_no: Default::default(),
            requests: Requests::new(),
            bmc,
            name: service_name,
            fee_numerator: fee_numerator.into(),
            registered_tokens: RegisteredTokens::new(),

            #[cfg(feature = "testable")]
            message: LazyOption::new(b"message".to_vec(), None),
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
        balance.deposit_mut().add(amount - storage_cost).unwrap();
    }
}
