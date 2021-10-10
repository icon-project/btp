use nativecoin_service::NativeCoinService;
use near_sdk::{testing_env, AccountId, VMContext};
use std::collections::HashSet;
pub mod accounts;
use accounts::*;
use libraries::types::{AccountBalance, NativeCoin, Token, Transfer};
mod token;
use token::*;

fn get_context(input: Vec<u8>, is_view: bool, signer_account_id: AccountId, attached_deposit: u128) -> VMContext {
    VMContext {
        current_account_id: alice().to_string(),
        signer_account_id: signer_account_id.to_string(),
        signer_account_pk: vec![0, 1, 2],
        predecessor_account_id: alice().to_string(),
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
fn deposit_native_coin_pass() {
    let context = |v: AccountId, d: u128| (get_context(vec![], false, v, d));
    testing_env!(context(alice(), 0));
    let nativecoin = <Token<NativeCoin>>::new(NATIVE_COIN.to_owned());
    let mut contract = NativeCoinService::new(
        "nativecoin".to_string(),
        bmc(),
        "0x1.near".into(),
        nativecoin.clone(),
    );
    testing_env!(context(alice(), 100));
    contract.deposit();

    let result = contract.balance_of(alice(), nativecoin.name().to_owned(), nativecoin.network().to_owned());
    let mut expected = AccountBalance::default();
    expected.deposit_mut().add(100);
    assert_eq!(result, Some(expected))
}