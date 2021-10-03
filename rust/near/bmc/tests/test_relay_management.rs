use bmc::BtpMessageCenter;
use near_sdk::{serde_json::json, testing_env, AccountId, VMContext};
pub mod accounts;
use accounts::*;
use libraries::types::{Address, BTPAddress};

//TODO
fn get_context(input: Vec<u8>, is_view: bool, signer_account_id: AccountId) -> VMContext {
    VMContext {
        current_account_id: alice().to_string(),
        signer_account_id: signer_account_id.to_string(),
        signer_account_pk: vec![0, 1, 2],
        predecessor_account_id: bob().to_string(),
        input,
        block_index: 0,
        block_timestamp: 0,
        account_balance: 0,
        account_locked_balance: 0,
        storage_usage: 0,
        attached_deposit: 0,
        prepaid_gas: 10u64.pow(18),
        random_seed: vec![0, 1, 2],
        is_view,
        output_data_receivers: vec![],
        epoch_height: 19,
    }
}

#[test]
fn add_relay_new_relay_pass() {
    let context = |v: AccountId| (get_context(vec![], false, v));
    testing_env!(context(alice()));
    let mut contract = BtpMessageCenter::new("0x1.near".into());
    let link =
        BTPAddress::new("btp://0x1.icon/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string());

    contract.add_verifier(link.network_address().unwrap(), verifier());
    contract.add_link(link.clone());
    contract.add_relays(
        link.clone(),
        vec![
            "verifier_1.near".parse::<AccountId>().unwrap(),
            "verifier_2.near".parse::<AccountId>().unwrap(),
        ],
    );

    let relays = contract.get_relays(link);
    assert_eq!(relays, json!(["verifier_1.near", "verifier_2.near"]));
}

#[test]
fn add_relays_new_relay_pass() {
    let context = |v: AccountId| (get_context(vec![], false, v));
    testing_env!(context(alice()));
    let mut contract = BtpMessageCenter::new("0x1.near".into());
    let link =
        BTPAddress::new("btp://0x1.icon/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string());

    contract.add_verifier(link.network_address().unwrap(), verifier());
    contract.add_link(link.clone());
    contract.add_relays(
        link.clone(),
        vec![
            "verifier_1.near".parse::<AccountId>().unwrap(),
            "verifier_2.near".parse::<AccountId>().unwrap(),
        ],
    );
    contract.add_relay(
        link.clone(),
        "verifier_3.near".parse::<AccountId>().unwrap(),
    );
    let relays = contract.get_relays(link);
    assert_eq!(
        relays,
        json!(["verifier_1.near", "verifier_2.near", "verifier_3.near"])
    );
}

#[test]
#[should_panic(expected = "BMCRevertRelayExist")]
fn add_relay_existing_relay_fail() {
    let context = |v: AccountId| (get_context(vec![], false, v));
    testing_env!(context(alice()));
    let mut contract = BtpMessageCenter::new("0x1.near".into());
    let link =
        BTPAddress::new("btp://0x1.icon/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string());

    contract.add_verifier(link.network_address().unwrap(), verifier());
    contract.add_link(link.clone());
    contract.add_relays(
        link.clone(),
        vec![
            "verifier_1.near".parse::<AccountId>().unwrap(),
            "verifier_2.near".parse::<AccountId>().unwrap(),
        ],
    );
    contract.add_relay(
        link.clone(),
        "verifier_2.near".parse::<AccountId>().unwrap(),
    );
}

#[test]
#[should_panic(expected = "BMCRevertNotExistsLink")]
fn add_relay_non_existing_link_fail() {
    let context = |v: AccountId| (get_context(vec![], false, v));
    testing_env!(context(alice()));
    let mut contract = BtpMessageCenter::new("0x1.near".into());
    let link =
        BTPAddress::new("btp://0x1.icon/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string());

    contract.add_relays(
        link.clone(),
        vec![
            "verifier_1.near".parse::<AccountId>().unwrap(),
            "verifier_2.near".parse::<AccountId>().unwrap(),
        ],
    );
}

#[test]
fn get_relays_pass() {
    let context = |v: AccountId| (get_context(vec![], false, v));
    testing_env!(context(alice()));
    let mut contract = BtpMessageCenter::new("0x1.near".into());
    let link =
        BTPAddress::new("btp://0x1.icon/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string());
    contract.add_verifier(link.network_address().unwrap(), verifier());
    contract.add_link(link.clone());
    contract.add_relays(
        link.clone(),
        vec![
            "verifier_1.near".parse::<AccountId>().unwrap(),
            "verifier_2.near".parse::<AccountId>().unwrap(),
        ],
    );
    contract.add_relay(
        link.clone(),
        "verifier_3.near".parse::<AccountId>().unwrap(),
    );
    testing_env!(context(bob()));
    let relays = contract.get_relays(link);
    assert_eq!(
        relays,
        json!(["verifier_1.near", "verifier_2.near", "verifier_3.near"])
    );
}

#[test]
#[should_panic(expected = "BMCRevertNotExistsPermission")]
fn add_relays_permission_fail() {
    let context = |v: AccountId| (get_context(vec![], false, v));
    testing_env!(context(alice()));
    let mut contract = BtpMessageCenter::new("0x1.near".into());
    let link =
        BTPAddress::new("btp://0x1.icon/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string());

    contract.add_verifier(link.network_address().unwrap(), verifier());
    contract.add_link(link.clone());
    testing_env!(context(chuck()));
    contract.add_relays(
        link.clone(),
        vec![
            "verifier_1.near".parse::<AccountId>().unwrap(),
            "verifier_2.near".parse::<AccountId>().unwrap(),
        ],
    );
}

#[test]
#[should_panic(expected = "BMCRevertNotExistsPermission")]
fn add_relay_permission_fail() {
    let context = |v: AccountId| (get_context(vec![], false, v));
    testing_env!(context(alice()));
    let mut contract = BtpMessageCenter::new("0x1.near".into());
    let link =
        BTPAddress::new("btp://0x1.icon/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string());

    contract.add_verifier(link.network_address().unwrap(), verifier());
    contract.add_link(link.clone());
    contract.add_relays(
        link.clone(),
        vec![
            "verifier_1.near".parse::<AccountId>().unwrap(),
            "verifier_2.near".parse::<AccountId>().unwrap(),
        ],
    );
    testing_env!(context(chuck()));
    contract.add_relay(
        link.clone(),
        "verifier_3.near".parse::<AccountId>().unwrap(),
    );
}

#[test]
fn remove_relay_existing_relay_pass() {
    let context = |v: AccountId| (get_context(vec![], false, v));
    testing_env!(context(alice()));
    let mut contract = BtpMessageCenter::new("0x1.near".into());
    let link =
        BTPAddress::new("btp://0x1.icon/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string());

    contract.add_verifier(link.network_address().unwrap(), verifier());
    contract.add_link(link.clone());
    contract.add_relays(
        link.clone(),
        vec![
            "verifier_1.near".parse::<AccountId>().unwrap(),
            "verifier_2.near".parse::<AccountId>().unwrap(),
        ],
    );
    contract.remove_relay(
        link.clone(),
        "verifier_1.near".parse::<AccountId>().unwrap(),
    );
    let relays = contract.get_relays(link);
    assert_eq!(relays, json!(["verifier_2.near"]));
}

#[test]
#[should_panic(expected = "BMCRevertNotExistsLink")]
fn remove_relay_non_existing_link_fail() {
    let context = |v: AccountId| (get_context(vec![], false, v));
    testing_env!(context(alice()));
    let mut contract = BtpMessageCenter::new("0x1.near".into());
    let link =
        BTPAddress::new("btp://0x1.icon/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string());
    contract.remove_relay(
        link.clone(),
        "verifier_3.near".parse::<AccountId>().unwrap(),
    );
}

#[test]
#[should_panic(expected = "BMCRevertNotExistsPermission")]
fn remove_relay_permission_fail() {
    let context = |v: AccountId| (get_context(vec![], false, v));
    testing_env!(context(alice()));
    let mut contract = BtpMessageCenter::new("0x1.near".into());
    let link =
        BTPAddress::new("btp://0x1.icon/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string());

    contract.add_verifier(link.network_address().unwrap(), verifier());
    contract.add_link(link.clone());
    contract.add_relays(
        link.clone(),
        vec![
            "verifier_1.near".parse::<AccountId>().unwrap(),
            "verifier_2.near".parse::<AccountId>().unwrap(),
        ],
    );

    testing_env!(context(chuck()));
    contract.remove_relay(
        link.clone(),
        "verifier_1.near".parse::<AccountId>().unwrap(),
    );
}

#[test]
#[should_panic(expected = "BMCRevertNotExistRelay")]
fn remove_relay_non_existing_relay_fail() {
    let context = |v: AccountId| (get_context(vec![], false, v));
    testing_env!(context(alice()));
    let mut contract = BtpMessageCenter::new("0x1.near".into());
    let link =
        BTPAddress::new("btp://0x1.icon/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string());

    contract.add_verifier(link.network_address().unwrap(), verifier());
    contract.add_link(link.clone());
    contract.add_relays(
        link.clone(),
        vec![
            "verifier_1.near".parse::<AccountId>().unwrap(),
            "verifier_2.near".parse::<AccountId>().unwrap(),
        ],
    );
    contract.remove_relay(
        link.clone(),
        "verifier_3.near".parse::<AccountId>().unwrap(),
    );
}

#[ignore]
#[test]
fn rotate_relay() {
    unimplemented!()
}
