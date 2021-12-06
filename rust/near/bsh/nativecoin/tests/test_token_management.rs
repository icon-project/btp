use nativecoin_service::NativeCoinService;
use near_sdk::{json_types::U128, testing_env, AccountId, VMContext, serde_json::to_value};
use std::collections::HashSet;
pub mod accounts;
use accounts::*;
use libraries::types::{AccountBalance, MultiTokenCore, NativeCoin, Token, Math, TokenItem};
mod token;
use token::*;

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
    testing_env!(context(alice(), 0));
    let nativecoin = <Token<NativeCoin>>::new(NATIVE_COIN.to_owned());
    let mut contract = NativeCoinService::new(
        "nativecoin".to_string(),
        bmc(),
        "0x1.near".into(),
        nativecoin.clone(),
    );
    let icx_coin = <Token<NativeCoin>>::new(ICON_COIN.to_owned());
    contract.register(icx_coin.clone());
    
    let result = contract.coins();
    let expected = to_value(vec![
        TokenItem {
            name: nativecoin.name().to_owned(),
            network: nativecoin.network().to_owned(),
            symbol: nativecoin.symbol().to_owned()
        },
        TokenItem {
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
    testing_env!(context(alice(), 0));
    let nativecoin = <Token<NativeCoin>>::new(NATIVE_COIN.to_owned());
    let mut contract = NativeCoinService::new(
        "nativecoin".to_string(),
        bmc(),
        "0x1.near".into(),
        nativecoin.clone(),
    );
    let icx_coin = <Token<NativeCoin>>::new(ICON_COIN.to_owned());
    contract.register(icx_coin.clone());
    contract.register(icx_coin.clone());
}

#[test]
#[should_panic(expected = "BSHRevertNotExistsPermission")]
fn register_token_permission() {
    let context = |v: AccountId, d: u128| (get_context(vec![], false, v, d));
    testing_env!(context(alice(), 0));
    let nativecoin = <Token<NativeCoin>>::new(NATIVE_COIN.to_owned());
    let mut contract = NativeCoinService::new(
        "nativecoin".to_string(),
        bmc(),
        "0x1.near".into(),
        nativecoin.clone(),
    );
    testing_env!(context(chuck(), 0));
    let icx_coin = <Token<NativeCoin>>::new(ICON_COIN.to_owned());
    contract.register(icx_coin.clone());
}
