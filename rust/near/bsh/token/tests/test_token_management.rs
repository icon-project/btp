use near_sdk::{serde_json::to_value, testing_env, AccountId, VMContext};
use token_service::TokenService;
pub mod accounts;
use accounts::*;
use libraries::types::{FungibleToken, Token, TokenItem};
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
    let mut contract = TokenService::new("TokenBSH".to_string(), bmc(), "0x1.near".into(),1000.into());
    let baln = <Token<FungibleToken>>::new(BALN.to_owned());
    contract.register(baln.clone());

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
    testing_env!(context(alice(), 0));
    let mut contract = TokenService::new("nativecoin".to_string(), bmc(), "0x1.near".into(),1000.into());
    let baln = <Token<FungibleToken>>::new(BALN.to_owned());
    contract.register(baln.clone());
    contract.register(baln.clone());
}

#[test]
#[should_panic(expected = "BSHRevertNotExistsPermission")]
fn register_token_permission() {
    let context = |v: AccountId, d: u128| (get_context(vec![], false, v, d));
    testing_env!(context(alice(), 0));
    let mut contract = TokenService::new("nativecoin".to_string(), bmc(), "0x1.near".into(),1000.into());
    testing_env!(context(chuck(), 0));
    let baln = <Token<FungibleToken>>::new(BALN.to_owned());
    contract.register(baln.clone());
}
