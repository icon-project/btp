//! Unit tests for BSH Generic contract

use bsh_generic::{Asset, BshGeneric};

#[test]
fn check_has_pending_request() {
    let bmc = "btp://0x1.near/cx87ed9048b594b95199f326fc76e76a9d33dd665b";
    let bsh_contract = "btp://0x1.near/cx97ed9048b594b95199f326fc76e76a9d33dd665b";
    let service_name = "test-service";
    let bsh = BshGeneric::new(bmc, bsh_contract, service_name);
    assert_eq!(bsh.has_pending_requests().unwrap(), false);
}

#[test]
fn check_send_service_message() {
    let bmc = "btp://0x1.near/cx87ed9048b594b95199f326fc76e76a9d33dd665b";
    let bsh_contract = "btp://0x1.near/cx97ed9048b594b95199f326fc76e76a9d33dd665b";
    let service_name = "test-service";
    let mut bsh = BshGeneric::new(bmc, bsh_contract, service_name);

    let from = "btp://0x1.near/cx77ed9048b594b95199f326fc76e76a9d33dd665b";
    let to = "btp://0x1.near/cx67ed9048b594b95199f326fc76e76a9d33dd665b";
    let coin_names = vec!["btc".to_string(), "ether".to_string(), "usdt".to_string()];
    let values = vec![100, 200, 300];
    let fees = vec![1, 2, 3];
    assert!(bsh
        .send_service_message(from, to, coin_names, values, fees)
        .is_ok());
}

#[test]
fn check_handle_btp_message() {
    let bmc = "btp://0x1.near/cx87ed9048b594b95199f326fc76e76a9d33dd665b";
    let bsh_contract = "btp://0x1.near/cx97ed9048b594b95199f326fc76e76a9d33dd665b";
    let service_name = "test-service";
    let mut bsh = BshGeneric::new(bmc, bsh_contract, service_name);

    let from = "btp://0x1.near/cx77ed9048b594b95199f326fc76e76a9d33dd665b";
    let svc = "test-service";
    let sn = 1;
    let msg = vec![b'1', b'2', b'3'];
    assert!(bsh.handle_btp_message(from, svc, sn, &msg).is_ok());
}

#[test]
fn check_handle_btp_error() {
    let bmc = "btp://0x1.near/cx87ed9048b594b95199f326fc76e76a9d33dd665b";
    let bsh_contract = "btp://0x1.near/cx97ed9048b594b95199f326fc76e76a9d33dd665b";
    let service_name = "test-service";
    let mut bsh = BshGeneric::new(bmc, bsh_contract, service_name);

    let src = "btp://0x1.near/cx77ed9048b594b95199f326fc76e76a9d33dd665b";
    let svc = "test-service";
    let sn = 1;
    let code = 1;
    let msg = "test-msg";
    assert!(bsh.handle_btp_error(src, svc, sn, code, msg).is_ok());
}

#[test]
fn check_handle_response_service() {
    let bmc = "btp://0x1.near/cx87ed9048b594b95199f326fc76e76a9d33dd665b";
    let bsh_contract = "btp://0x1.near/cx97ed9048b594b95199f326fc76e76a9d33dd665b";
    let service_name = "test-service";
    let mut bsh = BshGeneric::new(bmc, bsh_contract, service_name);

    let sn = 1;
    let code = 1;
    let msg = "test-msg";
    assert!(bsh.handle_response_service(sn, code, msg).is_ok());
}

#[test]
fn check_handle_request_service() {
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
    assert!(bsh.handle_request_service(to, assets).is_ok());
}

#[test]
fn check_handle_fee_gathering() {
    let bmc = "btp://0x1.near/cx87ed9048b594b95199f326fc76e76a9d33dd665b";
    let bsh_contract = "btp://0x1.near/cx97ed9048b594b95199f326fc76e76a9d33dd665b";
    let service_name = "test-service";
    let mut bsh = BshGeneric::new(bmc, bsh_contract, service_name);

    let fa = "btp://0x1.near/cx77ed9048b594b95199f326fc76e76a9d33dd665b";
    let svc = "test-service";
    assert!(bsh.handle_fee_gathering(svc, fa).is_ok());
}
