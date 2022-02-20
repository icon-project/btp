use btp_common::btp_address::Address;
use btp_common::errors::BshError;
use libraries::types::{
    Account, AccountBalance, AccumulatedAssetFees, TransferableAsset, BTPAddress, AssetId, WrappedNativeCoin,
};
use libraries::{
    types::messages::BtpMessage, types::messages::SerializedMessage,
    types::messages::TokenServiceMessage, types::messages::TokenServiceType, types::Balances,
    types::Math, types::Network, types::Owners,
    types::Requests, types::StorageBalances, types::Asset, types::AssetFees, types::Assets,
};
use near_sdk::borsh::{self, BorshDeserialize, BorshSerialize};
use near_sdk::collections::LazyOption;
use near_sdk::serde_json::{to_value, Value};
use near_sdk::serde_json::json;
use near_sdk::PromiseOrValue;
use near_sdk::{assert_one_yocto, AccountId};
use near_sdk::{
    env,
    json_types::{Base64VecU8, U128},
    log, near_bindgen, require, Gas, PanicOnDefault, Promise, PromiseResult,
};

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
pub type CoinFees = AssetFees;
pub type CoinId = AssetId;
pub type Coin = Asset<WrappedNativeCoin>;
pub type Coins = Assets<WrappedNativeCoin>;

pub static NEP141_CONTRACT: &'static [u8] = include_bytes!("../../../res/NEP141_CONTRACT.wasm");
pub static FEE_DENOMINATOR: u128 = 10_u128.pow(4);

#[near_bindgen]
#[derive(BorshDeserialize, BorshSerialize, PanicOnDefault)]
pub struct NativeCoinService {
    native_coin_name: String,
    network: Network,
    owners: Owners,
    coins: Coins,
    balances: Balances,
    storage_balances: StorageBalances,
    coin_fees: CoinFees,
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
        native_coin: Coin,
        fee_numerator: U128,
    ) -> Self {
        require!(!env::state_exists(), "Already initialized");
        let mut owners = Owners::new();
        owners.add(&env::current_account_id());

        let mut coins = <Coins>::new();
        let mut balances = Balances::new();
        let native_coin_id = Self::hash_coin_id(native_coin.name());

        balances.add(&env::current_account_id(), &native_coin_id);
        coins.add(&native_coin_id, &native_coin);

        let mut coin_fees = CoinFees::new();
        coin_fees.add(&native_coin_id);
        Self {
            native_coin_name: native_coin.name().to_owned(),
            network,
            owners,
            coins,
            balances,
            storage_balances: StorageBalances::new(),
            coin_fees,
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
