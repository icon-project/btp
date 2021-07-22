#![cfg(not(target_arch = "wasm32"))]

use bsh_generic::{Asset, BshGeneric, PendingTransferCoin};

#[test]
fn check_has_pending_request() {
    let bsh = BshGeneric::default();
    assert_eq!(bsh.has_pending_requests(), false);
}

#[test]
fn check_that_request_retrieval_works() {
    let mut bsh = BshGeneric::default();
    let pt1 = PendingTransferCoin {
        from: "btp://0x1.near/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string(),
        to: "btp://0x1.near/cx77ed9048b594b95199f326fc76e76a9d33dd665b".to_string(),
        coin_names: vec!["btc".to_string(), "ether".to_string(), "usdt".to_string()],
        amounts: vec![100, 200, 300],
        fees: vec![1, 2, 3],
    };
    let pt2 = PendingTransferCoin {
        from: "btp://0x1.near/cx67ed9048b594b95199f326fc76e76a9d33dd665b".to_string(),
        to: "btp://0x1.near/cx57ed9048b594b95199f326fc76e76a9d33dd665b".to_string(),
        coin_names: vec!["sol".to_string(), "near".to_string(), "dai".to_string()],
        amounts: vec![400, 500, 600],
        fees: vec![4, 5, 6],
    };
    let _ = bsh.requests.insert(&1, &pt1);
    let _ = bsh.requests.insert(&2, &pt2);
    assert_eq!(bsh.requests.get(&2).unwrap(), pt2);
}

#[test]
fn check_that_service_names_match() {
    let bsh = BshGeneric::default();
    let svc = "";
    assert_eq!(bsh.service_name, svc.to_string(), "InvalidSvc");
}

#[test]
fn check_that_serialization_and_deserialization_work() {
    let btc = Asset {
        coin_name: "btc".to_string(),
        value: 100,
    };
    let encoded_btc = near_sdk::borsh::BorshSerialize::try_to_vec(&btc).unwrap();
    let decoded_btc =
        <Asset as near_sdk::borsh::BorshDeserialize>::try_from_slice(&encoded_btc).unwrap();
    assert_eq!(btc, decoded_btc, "Data mismatch!");
}
