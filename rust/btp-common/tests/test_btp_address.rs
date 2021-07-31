use btp_common::{BTPAddress};

// TODO: Add more tests

#[test]
fn check_network_id() {
    let address =
        BTPAddress::new("btp://0x1.near/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string());
    assert_eq!("0x1", address.network_id().unwrap());
}

#[test]
fn check_network_address() {
    let address =
        BTPAddress::new("btp://0x1.near/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string());
    assert_eq!("0x1.near", address.network_address().unwrap());
}

#[test]
fn check_network() {
    let address =
        BTPAddress::new("btp://0x1.near/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string());
    assert_eq!(("0x1".to_string(), "near".to_string()), address.network().unwrap());
}

#[test]
fn check_contract_address() {
    let address =
        BTPAddress::new("btp://0x1.near/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string());
    assert_eq!("cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string(), address.contract_address().unwrap());
}

#[test]
fn check_empty_contract_address() {
    let address =
        BTPAddress::new("btp://0x1.near".to_string());
    assert_eq!("empty contract address", address.contract_address().unwrap_err());
}

#[test]
fn check_empty_contract_address_with_slash() {
    let address =
        BTPAddress::new("btp://0x1.near/".to_string());
    assert_eq!("empty contract address", address.contract_address().unwrap_err());
}

#[test]
fn check_valid_btp_address() {
    let address =
        BTPAddress::new("btp://0x1.near/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string());
    assert_eq!(true, address.is_valid().unwrap());
}

#[test]
fn check_invalid_protocol() {
    let address =
        BTPAddress::new("http://0x1.near/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string());
    assert_eq!("not supported protocol http", address.is_valid().unwrap_err());
}

#[test]
fn check_blockchain() {
    let address =
        BTPAddress::new("btp://0x1.near/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string());
    assert_eq!("near", address.blockchain().unwrap());
}

#[test]
fn check_blockchain_wrong_network() {
    let address =
        BTPAddress::new("btp://".to_string());
    assert_eq!("empty", address.blockchain().unwrap());
}