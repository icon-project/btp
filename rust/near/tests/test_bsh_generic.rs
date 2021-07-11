//! Unit tests for BSH Generic contract
#[cfg(not(target_arch = "wasm32"))]
use bsh_generic::{Asset, BshGeneric};
use near_sdk::test_utils::VMContextBuilder;
use near_sdk::MockedBlockchain;
use near_sdk::{testing_env, VMContext};
use std::convert::TryInto;

fn get_context(is_view: bool) -> VMContext {
    VMContextBuilder::new()
        .signer_account_id("Bsh context".try_into().unwrap())
        .is_view(is_view)
        .build()
}

#[test]
fn check_has_pending_request() {
    let context = get_context(false);
    testing_env!(context);

    let bmc = "btp://0x1.near/cx87ed9048b594b95199f326fc76e76a9d33dd665b";
    let bsh_contract = "btp://0x1.near/cx97ed9048b594b95199f326fc76e76a9d33dd665b";
    let service_name = "test-service";
    let bsh = BshGeneric::new(bmc, bsh_contract, service_name);

    let context = get_context(true);
    testing_env!(context);
    assert_eq!(bsh.has_pending_requests().unwrap(), false);
}

#[test]
fn check_send_service_message() {
    let context = get_context(false);
    testing_env!(context);

    let bmc = "btp://0x1.near/cx87ed9048b594b95199f326fc76e76a9d33dd665b";
    let bsh_contract = "btp://0x1.near/cx97ed9048b594b95199f326fc76e76a9d33dd665b";
    let service_name = "test-service";
    let mut bsh = BshGeneric::new(bmc, bsh_contract, service_name);

    let from = "btp://0x1.near/cx77ed9048b594b95199f326fc76e76a9d33dd665b";
    let to = "btp://0x1.near/cx67ed9048b594b95199f326fc76e76a9d33dd665b";
    let coin_names = vec!["btc".to_string(), "ether".to_string(), "usdt".to_string()];
    let values = vec![100, 200, 300];
    let fees = vec![1, 2, 3];

    let context = get_context(true);
    testing_env!(context);
    assert!(bsh
        .send_service_message(from, to, coin_names, values, fees)
        .is_ok());
}

#[test]
fn check_handle_btp_message() {
    let context = get_context(false);
    testing_env!(context);

    let bmc = "btp://0x1.near/cx87ed9048b594b95199f326fc76e76a9d33dd665b";
    let bsh_contract = "btp://0x1.near/cx97ed9048b594b95199f326fc76e76a9d33dd665b";
    let service_name = "test-service";
    let mut bsh = BshGeneric::new(bmc, bsh_contract, service_name);

    let from = "btp://0x1.near/cx77ed9048b594b95199f326fc76e76a9d33dd665b";
    let svc = "test-service";
    let sn = 1;
    let msg = vec![b'1', b'2', b'3'];

    let context = get_context(true);
    testing_env!(context);
    assert!(bsh.handle_btp_message(from, svc, sn, &msg).is_ok());
}

#[test]
fn check_handle_btp_error() {
    let context = get_context(false);
    testing_env!(context);

    let bmc = "btp://0x1.near/cx87ed9048b594b95199f326fc76e76a9d33dd665b";
    let bsh_contract = "btp://0x1.near/cx97ed9048b594b95199f326fc76e76a9d33dd665b";
    let service_name = "test-service";
    let mut bsh = BshGeneric::new(bmc, bsh_contract, service_name);

    let src = "btp://0x1.near/cx77ed9048b594b95199f326fc76e76a9d33dd665b";
    let svc = "test-service";
    let sn = 1;
    let code = 1;
    let msg = "test-msg";

    let context = get_context(true);
    testing_env!(context);
    assert!(bsh.handle_btp_error(src, svc, sn, code, msg).is_ok());
}

#[test]
fn check_handle_response_service() {
    let context = get_context(false);
    testing_env!(context);

    let bmc = "btp://0x1.near/cx87ed9048b594b95199f326fc76e76a9d33dd665b";
    let bsh_contract = "btp://0x1.near/cx97ed9048b594b95199f326fc76e76a9d33dd665b";
    let service_name = "test-service";
    let mut bsh = BshGeneric::new(bmc, bsh_contract, service_name);

    let sn = 1;
    let code = 1;
    let msg = "test-msg";

    let context = get_context(true);
    testing_env!(context);
    assert!(bsh.handle_response_service(sn, code, msg).is_ok());
}

#[test]
fn check_handle_request_service() {
    let context = get_context(false);
    testing_env!(context);

    let bmc = "btp://0x1.near/cx87ed9048b594b95199f326fc76e76a9d33dd665b";
    let bsh_contract = "btp://0x1.near/cx97ed9048b594b95199f326fc76e76a9d33dd665b";
    let service_name = "test-service";
    let mut bsh = BshGeneric::new(bmc, bsh_contract, service_name);

    let btc = Asset {
        coin_name: "btc".to_string(),
        value: 100,
    };
    let ether = Asset {
        coin_name: "ether".to_string(),
        value: 200,
    };
    let usdt = Asset {
        coin_name: "usdt".to_string(),
        value: 300,
    };
    let assets = vec![btc, ether, usdt];
    let to = "btp://0x1.near/cx67ed9048b594b95199f326fc76e76a9d33dd665b";

    let context = get_context(true);
    testing_env!(context);
    assert!(bsh.handle_request_service(to, assets).is_ok());
}

#[test]
fn check_handle_fee_gathering() {
    let context = get_context(false);
    testing_env!(context);

    let bmc = "btp://0x1.near/cx87ed9048b594b95199f326fc76e76a9d33dd665b";
    let bsh_contract = "btp://0x1.near/cx97ed9048b594b95199f326fc76e76a9d33dd665b";
    let service_name = "test-service";
    let mut bsh = BshGeneric::new(bmc, bsh_contract, service_name);

    let fa = "btp://0x1.near/cx77ed9048b594b95199f326fc76e76a9d33dd665b";
    let svc = "test-service";

    let context = get_context(true);
    testing_env!(context);
    assert!(bsh.handle_fee_gathering(svc, fa).is_ok());
}
