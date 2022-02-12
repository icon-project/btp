use nativecoin_service::NativeCoinService;
use near_sdk::{env, json_types::U128, testing_env, AccountId, VMContext};
pub mod accounts;
use accounts::*;
use libraries::types::{
    messages::{BtpMessage, TokenServiceMessage, TokenServiceType},
    Account, AccountBalance, AccumulatedAssetFees, BTPAddress, MultiTokenCore, WrappedNativeCoin, Token,
    Math, WrappedI128,
};
mod token;
use libraries::types::{Asset, Request};
use std::convert::TryInto;
use token::*;

fn get_context(
    input: Vec<u8>,
    is_view: bool,
    signer_account_id: AccountId,
    attached_deposit: u128,
    storage_usage: u64,
    account_balance: u128,
) -> VMContext {
    VMContext {
        current_account_id: alice().to_string(),
        signer_account_id: signer_account_id.to_string(),
        signer_account_pk: vec![0, 1, 2],
        predecessor_account_id: signer_account_id.to_string(),
        input,
        block_index: 0,
        block_timestamp: 0,
        account_balance,
        account_locked_balance: 0,
        storage_usage,
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
    let context = |account_id: AccountId, deposit: u128| {
        get_context(vec![], false, account_id, deposit, env::storage_usage(), 0)
    };
    testing_env!(context(alice(), 0));
    let nativecoin = <Token<WrappedNativeCoin>>::new(NATIVE_COIN.to_owned());
    let mut contract = NativeCoinService::new(
        "nativecoin".to_string(),
        bmc(),
        "0x1.near".into(),
        nativecoin.clone(),
        1000.into()
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
    let context = |account_id: AccountId, deposit: u128| {
        get_context(
            vec![],
            false,
            account_id,
            deposit,
            env::storage_usage(),
            1000,
        )
    };
    testing_env!(context(alice(), 0));
    let nativecoin = <Token<WrappedNativeCoin>>::new(NATIVE_COIN.to_owned());
    let mut contract = NativeCoinService::new(
        "nativecoin".to_string(),
        bmc(),
        "0x1.near".into(),
        nativecoin.clone(),
        1000.into()
    );
    let coin_id = contract.coin_id(nativecoin.name().to_owned());
    testing_env!(context(chuck(), 1000));

    contract.deposit();

    let result = contract.balance_of(chuck(), contract.coin_id(nativecoin.name().to_owned()));
    let mut expected = AccountBalance::default();
    expected.deposit_mut().add(1000).unwrap();
    assert_eq!(result, U128::from(expected.deposit()));

    testing_env!(context(chuck(), 1));
    contract.withdraw(coin_id.clone(), U128::from(999));

    testing_env!(context(alice(), 0));
    contract.on_withdraw(chuck(), 999, coin_id.clone(), nativecoin.symbol().to_owned());

    let result = contract.balance_of(chuck(), coin_id.clone());
    let mut expected = AccountBalance::default();
    expected.deposit_mut().add(1).unwrap();
    assert_eq!(result, U128::from(expected.deposit()));
}

#[test]
#[should_panic(expected = "BSHRevertNotMinimumDeposit")]
fn withdraw_native_coin_higher_amount() {
    let context = |account_id: AccountId, deposit: u128| {
        get_context(
            vec![],
            false,
            account_id,
            deposit,
            env::storage_usage(),
            1000,
        )
    };
    testing_env!(context(alice(), 0));
    let nativecoin = <Token<WrappedNativeCoin>>::new(NATIVE_COIN.to_owned());
    let mut contract = NativeCoinService::new(
        "nativecoin".to_string(),
        bmc(),
        "0x1.near".into(),
        nativecoin.clone(),
        1000.into()
    );
    let coin_id = contract.coin_id(nativecoin.name().to_owned());
    testing_env!(context(chuck(), 100));

    contract.deposit();

    let result = contract.balance_of(chuck(), contract.coin_id(nativecoin.name().to_owned()));
    let mut expected = AccountBalance::default();
    expected.deposit_mut().add(100).unwrap();
    assert_eq!(result, U128::from(expected.deposit()));

    testing_env!(context(chuck(), 1));
    contract.withdraw(coin_id.clone(),U128::from(1000));

    let result = contract.balance_of(chuck(), contract.coin_id(nativecoin.name().to_owned()));
    let expected = AccountBalance::default();
    assert_eq!(result, U128::from(expected.deposit()));
}

#[test]
#[cfg(feature = "testable")]
fn external_transfer() {
    let context = |account_id: AccountId, deposit: u128| {
        get_context(vec![], false, account_id, deposit, env::storage_usage(), 0)
    };
    testing_env!(context(alice(), 0));
    let nativecoin = <Token<WrappedNativeCoin>>::new(NATIVE_COIN.to_owned());
    let destination =
        BTPAddress::new("btp://0x1.icon/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string());
    let mut contract = NativeCoinService::new(
        "nativecoin".to_string(),
        bmc(),
        "0x1.near".into(),
        nativecoin.clone(),
        1000.into()
    );
    testing_env!(context(chuck(), 1000));
    let coin_id = contract.coin_id(nativecoin.name().to_owned());

    contract.deposit();
    contract.transfer(coin_id, destination.clone(), U128::from(999));

    let result = contract.account_balance(chuck(), contract.coin_id(nativecoin.name().to_owned()));
    let mut expected = AccountBalance::default();

    expected.deposit_mut().add(1).unwrap();
    expected.locked_mut().add(900).unwrap();
    expected.locked_mut().add(99).unwrap();

    assert_eq!(result, Some(expected));

    let request = contract.last_request().unwrap();
    assert_eq!(
        request,
        Request::new(
            chuck().to_string(),
            destination.account_id().to_string(),
            vec![Asset::new(nativecoin.name().to_owned(), 900, 99)]
        )
    )
}

#[test]
#[cfg(feature = "testable")]
fn handle_success_response_native_coin_external_transfer() {
    let context = |account_id: AccountId, deposit: u128| {
        get_context(vec![], false, account_id, deposit, env::storage_usage(), 0)
    };
    testing_env!(context(alice(), 0));
    let nativecoin = <Token<WrappedNativeCoin>>::new(NATIVE_COIN.to_owned());
    let destination =
        BTPAddress::new("btp://0x1.icon/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string());
    let mut contract = NativeCoinService::new(
        "nativecoin".to_string(),
        bmc(),
        "0x1.near".into(),
        nativecoin.clone(),
        1000.into()
    );
    testing_env!(context(chuck(), 1000));
    let coin_id = contract.coin_id(nativecoin.name().to_owned());

    contract.deposit();
    contract.transfer(coin_id, destination.clone(), U128::from(999));

    let result = contract.account_balance(chuck(), contract.coin_id(nativecoin.name().to_owned()));
    let mut expected = AccountBalance::default();

    expected.deposit_mut().add(1).unwrap();
    expected.locked_mut().add(900).unwrap();
    expected.locked_mut().add(99).unwrap();

    assert_eq!(result, Some(expected));

    let result = contract.balance_of(alice(), contract.coin_id(nativecoin.name().to_owned()));
    assert_eq!(result, U128::from(0));

    let btp_message = &BtpMessage::new(
        BTPAddress::new("btp://0x1.icon/0x12345678".to_string()),
        BTPAddress::new("btp://1234.iconee/0x12345678".to_string()),
        "nativecoin".to_string(),
        WrappedI128::new(1),
        vec![],
        Some(TokenServiceMessage::new(
            TokenServiceType::ResponseHandleService {
                code: 0,
                message: "Transfer Success".to_string(),
            },
        )),
    );

    testing_env!(context(bmc(), 0));
    contract.handle_btp_message(btp_message.try_into().unwrap());

    let result = contract.balance_of(alice(), contract.coin_id(nativecoin.name().to_owned()));
    assert_eq!(result, U128::from(999));

    let result = contract.account_balance(chuck(), contract.coin_id(nativecoin.name().to_owned()));
    let mut expected = AccountBalance::default();
    expected.deposit_mut().add(1).unwrap();

    assert_eq!(result, Some(expected));

    let accumulted_fees = contract.accumulated_fees();

    assert_eq!(
        accumulted_fees,
        vec![AccumulatedAssetFees {
            name: nativecoin.name().to_string(),
            network: nativecoin.network().to_string(),
            accumulated_fees: 99
        }]
    );
}

#[test]
#[cfg(feature = "testable")]
fn handle_success_response_icx_coin_external_transfer() {
    let context = |account_id: AccountId, deposit: u128| {
        get_context(vec![], false, account_id, deposit, env::storage_usage(), 0)
    };
    testing_env!(context(alice(), 0));
    let nativecoin = <Token<WrappedNativeCoin>>::new(NATIVE_COIN.to_owned());
    let destination =
        BTPAddress::new("btp://0x1.icon/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string());

    let mut contract = NativeCoinService::new(
        "nativecoin".to_string(),
        bmc(),
        "0x1.near".into(),
        nativecoin.clone(),
        1000.into()
    );

    let icx_coin = <Token<WrappedNativeCoin>>::new(ICON_COIN.to_owned());
    contract.register(icx_coin.clone());

    let btp_message = &BtpMessage::new(
        BTPAddress::new("btp://0x1.icon/0x12345678".to_string()),
        BTPAddress::new("btp://1234.iconee/0x12345678".to_string()),
        "nativecoin".to_string(),
        WrappedI128::new(1),
        vec![],
        Some(TokenServiceMessage::new(
            TokenServiceType::RequestTokenTransfer {
                sender: destination.account_id().to_string(),
                receiver: chuck().to_string(),
                assets: vec![Asset::new(icx_coin.name().to_owned(), 900, 99)],
            },
        )),
    );

    testing_env!(context(bmc(), 0));
    contract.handle_btp_message(btp_message.try_into().unwrap());

    testing_env!(context(chuck(), 0));
    let coin_id = contract.coin_id(icx_coin.name().to_owned());

    contract.on_mint(900,coin_id.clone(),icx_coin.symbol().to_string(),chuck().clone());

    contract.transfer(coin_id, destination.clone(), U128::from(800));

    let result = contract.account_balance(chuck(), contract.coin_id(icx_coin.name().to_owned()));
    let mut expected = AccountBalance::default();
    expected.deposit_mut().add(100).unwrap();
    expected.locked_mut().add(800).unwrap();

    assert_eq!(result, Some(expected));

    let result = contract.balance_of(alice(), contract.coin_id(icx_coin.name().to_owned()));
    assert_eq!(result, U128::from(0));

    let btp_message = &BtpMessage::new(
        BTPAddress::new("btp://0x1.icon/0x12345678".to_string()),
        BTPAddress::new("btp://1234.iconee/0x12345678".to_string()),
        "nativecoin".to_string(),
        WrappedI128::new(contract.serial_no()),
        vec![],
        Some(TokenServiceMessage::new(
            TokenServiceType::ResponseHandleService {
                code: 0,
                message: "Transfer Success".to_string(),
            },
        )),
    );

    testing_env!(context(bmc(), 0));
    contract.handle_btp_message(btp_message.try_into().unwrap());

    let result = contract.balance_of(alice(), contract.coin_id(icx_coin.name().to_owned()));
    assert_eq!(result, U128::from(80));

    let result = contract.account_balance(chuck(), contract.coin_id(icx_coin.name().to_owned()));
    let mut expected = AccountBalance::default();
    expected.deposit_mut().add(100).unwrap();

    assert_eq!(result, Some(expected));

    let accumulted_fees = contract.accumulated_fees();

    assert_eq!(
        accumulted_fees,
        vec![
            AccumulatedAssetFees {
                name: nativecoin.name().to_string(),
                network: nativecoin.network().to_string(),
                accumulated_fees: 0
            },
            AccumulatedAssetFees {
                name: icx_coin.name().to_string(),
                network: icx_coin.network().to_string(),
                accumulated_fees: 80
            }
        ]
    );
}

#[test]
#[cfg(feature = "testable")]
fn handle_failure_response_native_coin_external_transfer() {
    use libraries::types::AccumulatedAssetFees;

    let context = |account_id: AccountId, deposit: u128| {
        get_context(vec![], false, account_id, deposit, env::storage_usage(), 0)
    };
    testing_env!(context(alice(), 0));
    let nativecoin = <Token<WrappedNativeCoin>>::new(NATIVE_COIN.to_owned());
    let destination =
        BTPAddress::new("btp://0x1.icon/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string());
    let mut contract = NativeCoinService::new(
        "nativecoin".to_string(),
        bmc(),
        "0x1.near".into(),
        nativecoin.clone(),
        1000.into()
    );
    testing_env!(context(chuck(), 1000));
    let coin_id = contract.coin_id(nativecoin.name().to_owned());

    contract.deposit();
    contract.transfer(coin_id, destination.clone(), U128::from(999));

    let result = contract.account_balance(chuck(), contract.coin_id(nativecoin.name().to_owned()));
    let mut expected = AccountBalance::default();

    expected.deposit_mut().add(1).unwrap();
    expected.locked_mut().add(900).unwrap();
    expected.locked_mut().add(99).unwrap();

    assert_eq!(result, Some(expected));

    let result = contract.balance_of(alice(), contract.coin_id(nativecoin.name().to_owned()));
    assert_eq!(result, U128::from(0));

    let btp_message = &BtpMessage::new(
        BTPAddress::new("btp://0x1.icon/0x12345678".to_string()),
        BTPAddress::new("btp://1234.iconee/0x12345678".to_string()),
        "nativecoin".to_string(),
        WrappedI128::new(1),
        vec![],
        Some(TokenServiceMessage::new(
            TokenServiceType::ResponseHandleService {
                code: 1,
                message: "Transfer Failed".to_string(),
            },
        )),
    );

    testing_env!(context(bmc(), 0));
    contract.handle_btp_message(btp_message.try_into().unwrap());

    let result = contract.balance_of(alice(), contract.coin_id(nativecoin.name().to_owned()));
    assert_eq!(result, U128::from(99));

    let result = contract.account_balance(chuck(), contract.coin_id(nativecoin.name().to_owned()));
    let mut expected = AccountBalance::default();
    expected.deposit_mut().add(1).unwrap();
    expected.refundable_mut().add(900).unwrap();

    assert_eq!(result, Some(expected));

    let accumulted_fees = contract.accumulated_fees();

    assert_eq!(
        accumulted_fees,
        vec![AccumulatedAssetFees {
            name: nativecoin.name().to_string(),
            network: nativecoin.network().to_string(),
            accumulated_fees: 99
        }]
    );
}

#[test]
#[cfg(feature = "testable")]
fn handle_failure_response_icx_coin_external_transfer() {
    use libraries::types::AccumulatedAssetFees;

    let context = |account_id: AccountId, deposit: u128| {
        get_context(vec![], false, account_id, deposit, env::storage_usage(), 0)
    };
    testing_env!(context(alice(), 0));
    let nativecoin = <Token<WrappedNativeCoin>>::new(NATIVE_COIN.to_owned());
    let destination =
        BTPAddress::new("btp://0x1.icon/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string());
    let mut contract = NativeCoinService::new(
        "nativecoin".to_string(),
        bmc(),
        "0x1.near".into(),
        nativecoin.clone(),
        1000.into()
    );

    let icx_coin = <Token<WrappedNativeCoin>>::new(ICON_COIN.to_owned());
    contract.register(icx_coin.clone());

    let btp_message = &BtpMessage::new(
        BTPAddress::new("btp://0x1.icon/0x12345678".to_string()),
        BTPAddress::new("btp://1234.iconee/0x12345678".to_string()),
        "nativecoin".to_string(),
        WrappedI128::new(1),
        vec![],
        Some(TokenServiceMessage::new(
            TokenServiceType::RequestTokenTransfer {
                sender: destination.account_id().to_string(),
                receiver: chuck().to_string(),
                assets: vec![Asset::new(icx_coin.name().to_owned(), 900, 99)],
            },
        )),
    );

    testing_env!(context(bmc(), 0));
    contract.handle_btp_message(btp_message.try_into().unwrap());

    let coin_id = contract.coin_id(icx_coin.name().to_owned());

    contract.on_mint(900,coin_id.clone(),icx_coin.symbol().to_string(),chuck().clone());

    testing_env!(context(chuck(), 0));
    contract.transfer(coin_id.clone(), destination.clone(), U128::from(800));

    testing_env!(context(chuck(), 0));
    

    let result = contract.account_balance(chuck(), contract.coin_id(icx_coin.name().to_owned()));
    let mut expected = AccountBalance::default();
    expected.deposit_mut().add(100).unwrap();
    expected.locked_mut().add(800).unwrap();

    assert_eq!(result, Some(expected));

    let result = contract.balance_of(alice(), contract.coin_id(icx_coin.name().to_owned()));
    assert_eq!(result, U128::from(0));

    let btp_message = &BtpMessage::new(
        BTPAddress::new("btp://0x1.icon/0x12345678".to_string()),
        BTPAddress::new("btp://1234.iconee/0x12345678".to_string()),
        "nativecoin".to_string(),
        WrappedI128::new(contract.serial_no()),
        vec![],
        Some(TokenServiceMessage::new(
            TokenServiceType::ResponseHandleService {
                code: 1,
                message: "Transfer Failed".to_string(),
            },
        )),
    );

    testing_env!(context(bmc(), 0));
    contract.handle_btp_message(btp_message.try_into().unwrap());

    let result = contract.balance_of(alice(), contract.coin_id(icx_coin.name().to_owned()));
    assert_eq!(result, U128::from(80));

    let result = contract.account_balance(chuck(), contract.coin_id(icx_coin.name().to_owned()));
    let mut expected = AccountBalance::default();
    expected.deposit_mut().add(100).unwrap();
    expected.refundable_mut().add(720).unwrap();

    assert_eq!(result, Some(expected));
    let accumulted_fees = contract.accumulated_fees();

    assert_eq!(
        accumulted_fees,
        vec![
            AccumulatedAssetFees {
                name: nativecoin.name().to_string(),
                network: nativecoin.network().to_string(),
                accumulated_fees: 0
            },
            AccumulatedAssetFees {
                name: icx_coin.name().to_string(),
                network: icx_coin.network().to_string(),
                accumulated_fees: 80
            }
        ]
    );
}

#[test]
#[cfg(feature = "testable")]
fn reclaim_icx_coin() {
    let context = |account_id: AccountId, deposit: u128| {
        get_context(vec![], false, account_id, deposit, env::storage_usage(), 0)
    };
    testing_env!(context(alice(), 0));
    let nativecoin = <Token<WrappedNativeCoin>>::new(NATIVE_COIN.to_owned());
    let destination =
        BTPAddress::new("btp://0x1.icon/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string());
    let mut contract = NativeCoinService::new(
        "nativecoin".to_string(),
        bmc(),
        "0x1.near".into(),
        nativecoin.clone(),
        1000.into()
    );

    let icx_coin = <Token<WrappedNativeCoin>>::new(ICON_COIN.to_owned());
    contract.register(icx_coin.clone());

    let btp_message = &BtpMessage::new(
        BTPAddress::new("btp://0x1.icon/0x12345678".to_string()),
        BTPAddress::new("btp://1234.iconee/0x12345678".to_string()),
        "nativecoin".to_string(),
        WrappedI128::new(1),
        vec![],
        Some(TokenServiceMessage::new(
            TokenServiceType::RequestTokenTransfer {
                sender: destination.account_id().to_string(),
                receiver: chuck().to_string(),
                assets: vec![Asset::new(icx_coin.name().to_owned(), 900, 99)],
            },
        )),
    );

    testing_env!(context(bmc(), 0));
    contract.handle_btp_message(btp_message.try_into().unwrap());

    testing_env!(context(chuck(), 0));
    let coin_id = contract.coin_id(icx_coin.name().to_owned());
    contract.on_mint(900,coin_id.clone(),icx_coin.symbol().to_string(),chuck());
    
    testing_env!(context(chuck(), 0));
    contract.transfer(coin_id.clone(), destination.clone(), U128::from(800));

    let btp_message = &BtpMessage::new(
        BTPAddress::new("btp://0x1.icon/0x12345678".to_string()),
        BTPAddress::new("btp://1234.iconee/0x12345678".to_string()),
        "nativecoin".to_string(),
        WrappedI128::new(contract.serial_no()),
        vec![],
        Some(TokenServiceMessage::new(
            TokenServiceType::ResponseHandleService {
                code: 1,
                message: "Transfer Failed".to_string(),
            },
        )),
    );

    testing_env!(context(bmc(), 0));
    contract.handle_btp_message(btp_message.try_into().unwrap());

    testing_env!(context(chuck(), 0));
    contract.reclaim(coin_id.clone(), U128::from(700));

    let result = contract.account_balance(chuck(), coin_id.clone());
    let mut expected = AccountBalance::default();
    expected.deposit_mut().add(800).unwrap();
    expected.refundable_mut().add(20).unwrap();
    
    assert_eq!(result, Some(expected));
}

#[test]
#[should_panic(expected = "BSHRevertNotMinimumDeposit")]
fn external_transfer_higher_amount() {
    let context = |account_id: AccountId, deposit: u128| {
        get_context(vec![], false, account_id, deposit, env::storage_usage(), 0)
    };
    testing_env!(context(alice(), 0));
    let nativecoin = <Token<WrappedNativeCoin>>::new(NATIVE_COIN.to_owned());
    let destination =
        BTPAddress::new("btp://0x1.icon/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string());
    let mut contract = NativeCoinService::new(
        "nativecoin".to_string(),
        bmc(),
        "0x1.near".into(),
        nativecoin.clone(),
        1000.into()
    );
    testing_env!(context(chuck(), 1000));
    let coin_id = contract.coin_id(nativecoin.name().to_owned());

    contract.deposit();
    contract.transfer(coin_id, destination, U128::from(1001));
}

#[test]
#[should_panic(expected = "BSHRevertNotExistsToken")]
fn external_transfer_unregistered_coin() {
    let context = |account_id: AccountId, deposit: u128| {
        get_context(vec![], false, account_id, deposit, env::storage_usage(), 0)
    };
    testing_env!(context(alice(), 0));
    let nativecoin = <Token<WrappedNativeCoin>>::new(NATIVE_COIN.to_owned());
    let icx_coin = <Token<WrappedNativeCoin>>::new(ICON_COIN.to_owned());
    let destination =
        BTPAddress::new("btp://0x1.icon/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string());
    let mut contract = NativeCoinService::new(
        "nativecoin".to_string(),
        bmc(),
        "0x1.near".into(),
        nativecoin.clone(),
        1000.into()
    );
    testing_env!(context(chuck(), 1000));

    let icx_coin_id = contract.coin_id(icx_coin.name().to_owned());

    contract.deposit();
    contract.transfer(icx_coin_id, destination, U128::from(1001));
}

#[test]
#[should_panic(expected = "BSHRevertNotMinimumDeposit")]
fn external_transfer_nil_balance() {
    let context = |account_id: AccountId, deposit: u128| {
        get_context(vec![], false, account_id, deposit, env::storage_usage(), 0)
    };
    testing_env!(context(alice(), 0));
    let nativecoin = <Token<WrappedNativeCoin>>::new(NATIVE_COIN.to_owned());
    let icx_coin = <Token<WrappedNativeCoin>>::new(ICON_COIN.to_owned());
    let destination =
        BTPAddress::new("btp://0x1.icon/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string());
    let mut contract = NativeCoinService::new(
        "nativecoin".to_string(),
        bmc(),
        "0x1.near".into(),
        nativecoin.clone(),
        1000.into()
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
    let context = |account_id: AccountId, deposit: u128| {
        get_context(vec![], false, account_id, deposit, env::storage_usage(), 0)
    };
    testing_env!(context(alice(), 0));
    let nativecoin = <Token<WrappedNativeCoin>>::new(NATIVE_COIN.to_owned());
    let destination =
        BTPAddress::new("btp://0x1.icon/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string());
    let mut contract = NativeCoinService::new(
        "nativecoin".to_string(),
        bmc(),
        "0x1.near".into(),
        nativecoin.clone(),
        1000.into()
    );
    testing_env!(context(chuck(), 1000));
    let coin_id = contract.coin_id(nativecoin.name().to_owned());

    contract.deposit();
    contract.transfer_batch(vec![coin_id], destination, vec![U128::from(999)]);
    // TODO: Add other tokens
    let result = contract.account_balance(chuck(), contract.coin_id(nativecoin.name().to_owned()));
    let mut expected = AccountBalance::default();

    expected.deposit_mut().add(1).unwrap();
    expected.locked_mut().add(900).unwrap();
    expected.locked_mut().add(99).unwrap();

    assert_eq!(result, Some(expected));
}

#[test]
#[should_panic(expected = "BSHRevertNotMinimumDeposit")]
fn external_transfer_batch_higher_amount() {
    let context = |account_id: AccountId, deposit: u128| {
        get_context(vec![], false, account_id, deposit, env::storage_usage(), 0)
    };
    testing_env!(context(alice(), 0));
    let nativecoin = <Token<WrappedNativeCoin>>::new(NATIVE_COIN.to_owned());
    let destination =
        BTPAddress::new("btp://0x1.icon/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string());
    let mut contract = NativeCoinService::new(
        "nativecoin".to_string(),
        bmc(),
        "0x1.near".into(),
        nativecoin.clone(),
        1000.into()
    );
    testing_env!(context(chuck(), 1000));
    let coin_id = contract.coin_id(nativecoin.name().to_owned());

    contract.deposit();
    contract.transfer_batch(vec![coin_id], destination, vec![U128::from(1001)]);
}

#[test]
#[should_panic(expected = "BSHRevertNotExistsToken")]
fn external_transfer_batch_unregistered_coin() {
    let context = |account_id: AccountId, deposit: u128| {
        get_context(vec![], false, account_id, deposit, env::storage_usage(), 0)
    };
    testing_env!(context(alice(), 0));
    let nativecoin = <Token<WrappedNativeCoin>>::new(NATIVE_COIN.to_owned());
    let icx_coin = <Token<WrappedNativeCoin>>::new(ICON_COIN.to_owned());
    let destination =
        BTPAddress::new("btp://0x1.icon/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string());
    let mut contract = NativeCoinService::new(
        "nativecoin".to_string(),
        bmc(),
        "0x1.near".into(),
        nativecoin.clone(),
        1000.into()
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
    let context = |account_id: AccountId, deposit: u128| {
        get_context(vec![], false, account_id, deposit, env::storage_usage(), 0)
    };
    testing_env!(context(alice(), 0));
    let nativecoin = <Token<WrappedNativeCoin>>::new(NATIVE_COIN.to_owned());
    let icx_coin = <Token<WrappedNativeCoin>>::new(ICON_COIN.to_owned());
    let destination =
        BTPAddress::new("btp://0x1.icon/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string());
    let mut contract = NativeCoinService::new(
        "nativecoin".to_string(),
        bmc(),
        "0x1.near".into(),
        nativecoin.clone(),
        1000.into()
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
