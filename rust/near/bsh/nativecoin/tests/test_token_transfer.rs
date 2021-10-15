use nativecoin_service::NativeCoinService;
use near_sdk::{env, json_types::U128, testing_env, AccountId, VMContext};
use std::collections::HashSet;
pub mod accounts;
use accounts::*;
use libraries::types::{
    messages::{NativeCoinServiceMessage, NativeCoinServiceType},
    Account, AccountBalance, BTPAddress, MultiTokenCore, NativeCoin, Token, Transfer,
};
mod token;
use std::convert::TryFrom;
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
        account_balance: 2,
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
fn deposit_native_coin() {
    let context = |v: AccountId, d: u128| (get_context(vec![], false, v, d));
    testing_env!(context(alice(), 0));
    let nativecoin = <Token<NativeCoin>>::new(NATIVE_COIN.to_owned());
    let mut contract = NativeCoinService::new(
        "nativecoin".to_string(),
        bmc(),
        "0x1.near".into(),
        nativecoin.clone(),
    );
    testing_env!(context(chuck(), 100));

    contract.deposit();

    let result = contract.balance_of(chuck(), contract.coin_id(nativecoin.name().to_owned()));
    let mut expected = AccountBalance::default();
    expected.deposit_mut().add(100).unwrap();
    assert_eq!(result, U128::from(expected.deposit()))
}

#[test]
fn withdraw_native_coin() {
    let context = |v: AccountId, d: u128| (get_context(vec![], false, v, d));
    testing_env!(context(alice(), 0));
    let nativecoin = <Token<NativeCoin>>::new(NATIVE_COIN.to_owned());
    let mut contract = NativeCoinService::new(
        "nativecoin".to_string(),
        bmc(),
        "0x1.near".into(),
        nativecoin.clone(),
    );
    testing_env!(context(chuck(), 1));

    contract.deposit();

    let result = contract.balance_of(chuck(), contract.coin_id(nativecoin.name().to_owned()));
    let mut expected = AccountBalance::default();
    expected.deposit_mut().add(1).unwrap();
    assert_eq!(result, U128::from(expected.deposit()));

    contract.withdraw(U128::from(1));

    let result = contract.balance_of(chuck(), contract.coin_id(nativecoin.name().to_owned()));
    let expected = AccountBalance::default();
    assert_eq!(result, U128::from(expected.deposit()));
}

#[test]
#[cfg(feature = "testable")]
fn external_transfer() {
    use libraries::types::Asset;

    let context = |v: AccountId, d: u128| (get_context(vec![], false, v, d));
    testing_env!(context(alice(), 0));
    let nativecoin = <Token<NativeCoin>>::new(NATIVE_COIN.to_owned());
    let destination =
        BTPAddress::new("btp://0x1.icon/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string());
    let mut contract = NativeCoinService::new(
        "nativecoin".to_string(),
        bmc(),
        "0x1.near".into(),
        nativecoin.clone(),
    );
    testing_env!(context(chuck(), 1000));
    let coin_id = contract.coin_id(nativecoin.name().to_owned());

    contract.deposit();
    contract.transfer(coin_id, destination.clone(), U128::from(999));

    let result = contract.account_balance(chuck(), contract.coin_id(nativecoin.name().to_owned()));
    let mut expected = AccountBalance::default();

    expected.deposit_mut().add(1).unwrap();
    expected.locked_mut().deposit_mut().add(900).unwrap();
    expected.locked_mut().fees_mut().add(99).unwrap();

    assert_eq!(result, Some(expected));

    let request = contract.last_request().unwrap();
    let result = NativeCoinServiceMessage::try_from(request.data()).unwrap();
    assert_eq!(
        result,
        NativeCoinServiceMessage::new(NativeCoinServiceType::RequestCoinTransfer {
            source: chuck().to_string(),
            destination: destination.account_id().to_string(),
            assets: vec![Asset::new(nativecoin.name().to_owned(), 900, 99)]
        })
    )
}

#[test]
#[should_panic(expected = "BSHRevertNotMinimumDeposit")]
fn external_transfer_higher_amount() {
    let context = |v: AccountId, d: u128| (get_context(vec![], false, v, d));
    testing_env!(context(alice(), 0));
    let nativecoin = <Token<NativeCoin>>::new(NATIVE_COIN.to_owned());
    let destination =
        BTPAddress::new("btp://0x1.icon/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string());
    let mut contract = NativeCoinService::new(
        "nativecoin".to_string(),
        bmc(),
        "0x1.near".into(),
        nativecoin.clone(),
    );
    testing_env!(context(chuck(), 1000));
    let coin_id = contract.coin_id(nativecoin.name().to_owned());

    contract.deposit();
    contract.transfer(coin_id, destination, U128::from(1001));
}

#[test]
#[should_panic(expected = "BSHRevertNotExistsToken")]
fn external_transfer_unregistered_coin() {
    let context = |v: AccountId, d: u128| (get_context(vec![], false, v, d));
    testing_env!(context(alice(), 0));
    let nativecoin = <Token<NativeCoin>>::new(NATIVE_COIN.to_owned());
    let icx_coin = <Token<NativeCoin>>::new(ICON_COIN.to_owned());
    let destination =
        BTPAddress::new("btp://0x1.icon/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string());
    let mut contract = NativeCoinService::new(
        "nativecoin".to_string(),
        bmc(),
        "0x1.near".into(),
        nativecoin.clone(),
    );
    testing_env!(context(chuck(), 1000));

    let icx_coin_id = contract.coin_id(icx_coin.name().to_owned());

    contract.deposit();
    contract.transfer(icx_coin_id, destination, U128::from(1001));
}

#[test]
#[should_panic(expected = "BSHRevertNotMinimumDeposit")]
fn external_transfer_nil_balance() {
    let context = |v: AccountId, d: u128| (get_context(vec![], false, v, d));
    testing_env!(context(alice(), 0));
    let nativecoin = <Token<NativeCoin>>::new(NATIVE_COIN.to_owned());
    let icx_coin = <Token<NativeCoin>>::new(ICON_COIN.to_owned());
    let destination =
        BTPAddress::new("btp://0x1.icon/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string());
    let mut contract = NativeCoinService::new(
        "nativecoin".to_string(),
        bmc(),
        "0x1.near".into(),
        nativecoin.clone(),
    );

    contract.register(icx_coin.clone());
    testing_env!(context(chuck(), 1000));

    let icx_coin_id = contract.coin_id(icx_coin.name().to_owned());

    contract.deposit();
    contract.transfer(icx_coin_id, destination, U128::from(1001));
}

#[test]
#[cfg(feature = "testable")]
fn external_transfer_batch() {
    let context = |v: AccountId, d: u128| (get_context(vec![], false, v, d));
    testing_env!(context(alice(), 0));
    let nativecoin = <Token<NativeCoin>>::new(NATIVE_COIN.to_owned());
    let destination =
        BTPAddress::new("btp://0x1.icon/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string());
    let mut contract = NativeCoinService::new(
        "nativecoin".to_string(),
        bmc(),
        "0x1.near".into(),
        nativecoin.clone(),
    );
    testing_env!(context(chuck(), 1000));
    let coin_id = contract.coin_id(nativecoin.name().to_owned());

    contract.deposit();
    contract.transfer_batch(vec![coin_id], destination, vec![U128::from(999)]);
    // TODO: Add other tokens
    let result = contract.account_balance(chuck(), contract.coin_id(nativecoin.name().to_owned()));
    let mut expected = AccountBalance::default();

    expected.deposit_mut().add(1).unwrap();
    expected.locked_mut().deposit_mut().add(900).unwrap();
    expected.locked_mut().fees_mut().add(99).unwrap();

    assert_eq!(result, Some(expected));
}

#[test]
#[should_panic(expected = "BSHRevertNotMinimumDeposit")]
fn external_transfer_batch_higher_amount() {
    let context = |v: AccountId, d: u128| (get_context(vec![], false, v, d));
    testing_env!(context(alice(), 0));
    let nativecoin = <Token<NativeCoin>>::new(NATIVE_COIN.to_owned());
    let destination =
        BTPAddress::new("btp://0x1.icon/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string());
    let mut contract = NativeCoinService::new(
        "nativecoin".to_string(),
        bmc(),
        "0x1.near".into(),
        nativecoin.clone(),
    );
    testing_env!(context(chuck(), 1000));
    let coin_id = contract.coin_id(nativecoin.name().to_owned());

    contract.deposit();
    contract.transfer_batch(vec![coin_id], destination, vec![U128::from(1001)]);
}

#[test]
#[should_panic(expected = "BSHRevertNotExistsToken")]
fn external_transfer_batch_unregistered_coin() {
    let context = |v: AccountId, d: u128| (get_context(vec![], false, v, d));
    testing_env!(context(alice(), 0));
    let nativecoin = <Token<NativeCoin>>::new(NATIVE_COIN.to_owned());
    let icx_coin = <Token<NativeCoin>>::new(ICON_COIN.to_owned());
    let destination =
        BTPAddress::new("btp://0x1.icon/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string());
    let mut contract = NativeCoinService::new(
        "nativecoin".to_string(),
        bmc(),
        "0x1.near".into(),
        nativecoin.clone(),
    );
    testing_env!(context(chuck(), 1000));
    let coin_id = contract.coin_id(nativecoin.name().to_owned());
    let icx_coin_id = contract.coin_id(icx_coin.name().to_owned());

    contract.deposit();
    contract.transfer_batch(
        vec![coin_id, icx_coin_id],
        destination,
        vec![U128::from(900), U128::from(1)],
    );
}

#[test]
#[should_panic(expected = "BSHRevertNotMinimumDeposit")]
fn external_transfer_batch_nil_balance() {
    let context = |v: AccountId, d: u128| (get_context(vec![], false, v, d));
    testing_env!(context(alice(), 0));
    let nativecoin = <Token<NativeCoin>>::new(NATIVE_COIN.to_owned());
    let icx_coin = <Token<NativeCoin>>::new(ICON_COIN.to_owned());
    let destination =
        BTPAddress::new("btp://0x1.icon/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string());
    let mut contract = NativeCoinService::new(
        "nativecoin".to_string(),
        bmc(),
        "0x1.near".into(),
        nativecoin.clone(),
    );

    contract.register(icx_coin.clone());
    testing_env!(context(chuck(), 1000));
    let coin_id = contract.coin_id(nativecoin.name().to_owned());
    let icx_coin_id = contract.coin_id(icx_coin.name().to_owned());

    contract.deposit();
    contract.transfer_batch(
        vec![coin_id, icx_coin_id],
        destination,
        vec![U128::from(900), U128::from(1)],
    );
}
