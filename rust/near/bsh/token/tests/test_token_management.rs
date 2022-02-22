use near_sdk::{env, serde_json::to_value, testing_env, AccountId, VMContext,PromiseResult};
use token_service::TokenService;
pub mod accounts;
use accounts::*;
use libraries::types::{Asset, AssetItem, WrappedFungibleToken};
mod token;
use token::*;


pub type Token = Asset<WrappedFungibleToken>;
pub type TokenItem = AssetItem;


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
    let mut contract = TokenService::new(
        "TokenBSH".to_string(),
        bmc(),
        "0x1.near".into(),
        1000.into(),
    );
    let baln = <Token>::new(BALN.to_owned());
    contract.register(baln.clone());
    contract.register_token_callback(baln.clone());
    
    let result = contract.tokens();
    let expected = to_value(vec![TokenItem {
        name: baln.name().to_owned(),
        network: baln.network().to_owned(),
        symbol: baln.symbol().to_owned(),
    }])
    .unwrap();
    assert_eq!(result, expected);
}

#[test]
#[should_panic(expected = "BSHRevertAlreadyExistsToken")]
fn register_existing_token() {
    let context = |v: AccountId, d: u128| (get_context(vec![], false, v, d));
    testing_env!(
        context(alice(),  1_000_000_000_000_000_000_000_000),
        Default::default(),
        Default::default(),
        Default::default(),
        vec![PromiseResult::Successful(vec![1_u8])]
    );
    let mut contract = TokenService::new(
        "nativecoin".to_string(),
        bmc(),
        "0x1.near".into(),
        1000.into(),
    );
    let baln = <Token>::new(BALN.to_owned());
    contract.register(baln.clone());
    contract.register_token_callback(baln.clone());

    contract.register(baln.clone());
}

#[test]
#[should_panic(expected = "BSHRevertNotExistsPermission")]
fn register_token_permission() {
    let context = |v: AccountId, d: u128| (get_context(vec![], false, v, d));
    testing_env!(context(alice(), 0));
    let mut contract = TokenService::new(
        "nativecoin".to_string(),
        bmc(),
        "0x1.near".into(),
        1000.into(),
    );
    testing_env!(context(chuck(), 0));
    let baln = <Token>::new(BALN.to_owned());
    contract.register(baln.clone());
}

#[test]
#[should_panic(
    expected = "BSHRevertNotExistsToken: [38, 6b, d, cf, f4, cf, 7b, f0, f7, 91, 97, 88, ec, 8f, f2, d6, 98, e5, 32, 16, 2a, e4, 5, 3d, 32, 3b, 8d, 4f, e0, bd, ae, 94]"
)]
fn get_non_exist_token_id() {
    let context = |v: AccountId, d: u128| (get_context(vec![], false, v, d));
    testing_env!(context(alice(), 0));
    let mut contract = TokenService::new(
        "nativecoin".to_string(),
        bmc(),
        "0x1.near".into(),
        1000.into(),
    );
    let token_id = contract.token_id("ICON".to_string());
}

#[test]
fn get_registered_token_id() {
    let context = |v: AccountId, d: u128| (get_context(vec![], false, v, d));
    testing_env!(
        context(alice(), 1_000_000_000_000_000_000_000_000),
        Default::default(),
        Default::default(),
        Default::default(),
        vec![PromiseResult::Successful(vec![1_u8])]
    );
    let mut contract = TokenService::new(
        "nativecoin".to_string(),
        bmc(),
        "0x1.near".into(),
        1000.into(),
    );
    let baln = <Token>::new(BALN.to_owned());
    contract.register(baln.clone());
    contract.register_token_callback(baln.clone());

    let token_id = contract.token_id("BALN".to_string());
    let expected = env::sha256(baln.name().as_bytes());
    assert_eq!(token_id, expected)
}
