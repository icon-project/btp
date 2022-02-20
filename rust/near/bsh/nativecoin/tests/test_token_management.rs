use nativecoin_service::NativeCoinService;
use near_sdk::{json_types::U128, testing_env, AccountId, VMContext, serde_json::to_value, env, PromiseResult};
use std::collections::HashSet;
pub mod accounts;
use accounts::*;
use libraries::types::{AccountBalance, WrappedNativeCoin, Asset, Math, AssetItem};
mod token;
use token::*;
pub type Coin = Asset<WrappedNativeCoin>;

fn get_context(
    input: Vec<u8>,
    is_view: bool,
    signer_account_id: AccountId,
    attached_deposit: u128,
) -> VMContext {
    VMContext {
        current_account_id: alice().to_string(),
        signer_account_id: signer_account_id.to_string(),
        signer_account_pk: vec![0, 1, 2],
        predecessor_account_id: signer_account_id.to_string(),
        input,
        block_index: 0,
        block_timestamp: 0,
        account_balance: 0,
        account_locked_balance: 0,
        storage_usage: 0,
        attached_deposit,
        prepaid_gas: 10u64.pow(18),
        random_seed: vec![0, 1, 2],
        is_view,
        output_data_receivers: vec![],
        epoch_height: 19,
    }
}

#[test]
fn register_token() {
    let context = |v: AccountId, d: u128| (get_context(vec![], false, v, d));
    testing_env!(
        context(alice(), 0),
        Default::default(),
        Default::default(),
        Default::default(),
        vec![PromiseResult::Successful(vec![1_u8])]
    );
    let nativecoin = <Coin>::new(NATIVE_COIN.to_owned());
    let mut contract = NativeCoinService::new(
        "nativecoin".to_string(),
        bmc(),
        "0x1.near".into(),
        nativecoin.clone(),
        1000.into()
    );
    let icx_coin = <Coin>::new(ICON_COIN.to_owned());
    contract.register(icx_coin.clone());
    contract.register_coin_callback(icx_coin.clone());
    
    let result = contract.coins();
    let expected = to_value(vec![
        AssetItem {
            name: nativecoin.name().to_owned(),
            network: nativecoin.network().to_owned(),
            symbol: nativecoin.symbol().to_owned()
        },
        AssetItem {
            name: icx_coin.name().to_owned(),
            network: icx_coin.network().to_owned(),
            symbol: icx_coin.symbol().to_owned()
        }
    ]).unwrap();
    assert_eq!(result, expected);
}

#[test]
#[should_panic(expected = "BSHRevertAlreadyExistsToken")]
fn register_existing_token() {
    let context = |v: AccountId, d: u128| (get_context(vec![], false, v, d));
    testing_env!(
        context(alice(), 0),
        Default::default(),
        Default::default(),
        Default::default(),
        vec![PromiseResult::Successful(vec![1_u8])]
    );
    let nativecoin = <Coin>::new(NATIVE_COIN.to_owned());
    let mut contract = NativeCoinService::new(
        "nativecoin".to_string(),
        bmc(),
        "0x1.near".into(),
        nativecoin.clone(),
        1000.into()
    );
    let icx_coin = <Coin>::new(ICON_COIN.to_owned());
    contract.register(icx_coin.clone());
    contract.register_coin_callback(icx_coin.clone());
    contract.register(icx_coin.clone());
}

#[test]
#[should_panic(expected = "BSHRevertNotExistsPermission")]
fn register_token_permission() {
    let context = |v: AccountId, d: u128| (get_context(vec![], false, v, d));
    testing_env!(
        context(alice(), 0),
        Default::default(),
        Default::default(),
        Default::default(),
        vec![PromiseResult::Successful(vec![1_u8])]
    );
    let nativecoin = <Coin>::new(NATIVE_COIN.to_owned());
    let mut contract = NativeCoinService::new(
        "nativecoin".to_string(),
        bmc(),
        "0x1.near".into(),
        nativecoin.clone(),
        1000.into()
    );
    testing_env!(context(chuck(), 0));
    let icx_coin = <Coin>::new(ICON_COIN.to_owned());
    contract.register(icx_coin.clone());
    contract.register_coin_callback(icx_coin.clone());
}

#[test]
fn get_registered_coin_id() {
    let context = |v: AccountId, d: u128| (get_context(vec![], false, v, d));
    testing_env!(context(alice(), 0));
    let nativecoin = <Coin>::new(NATIVE_COIN.to_owned());
    let mut contract = NativeCoinService::new(
        "nativecoin".to_string(),
        bmc(),
        "0x1.near".into(),
        nativecoin.clone(),
        1000.into()
    );
    let coin_id = contract.coin_id("NEAR".to_string());
    let expected = env::sha256(nativecoin.name().as_bytes());
    assert_eq!(coin_id,expected)
}

#[test]
#[should_panic(expected = "BSHRevertNotExistsToken: [38, 6b, d, cf, f4, cf, 7b, f0, f7, 91, 97, 88, ec, 8f, f2, d6, 98, e5, 32, 16, 2a, e4, 5, 3d, 32, 3b, 8d, 4f, e0, bd, ae, 94]")]
fn get_non_exist_coin_id() {
    let context = |v: AccountId, d: u128| (get_context(vec![], false, v, d));
    testing_env!(context(alice(), 0));
    let nativecoin = <Coin>::new(NATIVE_COIN.to_owned());
    let mut contract = NativeCoinService::new(
        "nativecoin".to_string(),
        bmc(),
        "0x1.near".into(),
        nativecoin.clone(),
        1000.into()
    );
    let coin_id = contract.coin_id("ICON".to_string());
}