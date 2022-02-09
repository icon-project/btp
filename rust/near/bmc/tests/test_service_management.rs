use bmc::BtpMessageCenter;
use near_sdk::{testing_env, AccountId, VMContext};
pub mod accounts;
use accounts::*;
use near_sdk::serde_json::{json, to_value};

fn get_context(input: Vec<u8>, is_view: bool, signer_account_id: AccountId) -> VMContext {
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
        attached_deposit: 0,
        prepaid_gas: 10u64.pow(18),
        random_seed: vec![0, 1, 2],
        is_view,
        output_data_receivers: vec![],
        epoch_height: 19,
    }
}

#[test]
fn add_new_service() {
    let context = |v: AccountId| (get_context(vec![], false, v));
    testing_env!(context(alice()));
    let mut contract = BtpMessageCenter::new("0x1.near".into(), 1500);
    contract.add_service(
        "test".to_string(),
        "sssssssss.ss".parse::<AccountId>().unwrap(),
    );

    let services = contract.get_services();
    assert_eq!(
        to_value(services).unwrap(),
        json!([{"name":"test","service":"sssssssss.ss"}])
    );
}

#[test]
#[should_panic(expected = "BMCRevertAlreadyExistsBSH")]
fn add_service_existing_service() {
    let context = |v: AccountId| (get_context(vec![], false, v));
    testing_env!(context(alice()));
    let mut contract = BtpMessageCenter::new("0x1.near".into(), 1500);
    contract.add_service(
        "test".to_string(),
        "sssssssss.s".parse::<AccountId>().unwrap(),
    );

    contract.add_service(
        "test".to_string(),
        "sssssssss.s".parse::<AccountId>().unwrap(),
    );
}

#[test]
#[should_panic(expected = "BMCRevertNotExistsPermission")]
fn add_service_no_permission() {
    let context = |v: AccountId| (get_context(vec![], false, v));
    testing_env!(context(bob()));
    let mut contract = BtpMessageCenter::new("0x1.near".into(), 1500);
    contract.add_service(
        "test".to_string(),
        "sssssssss.s".parse::<AccountId>().unwrap(),
    );
}

#[test]
fn remove_service_existing_service() {
    let context = |v: AccountId| (get_context(vec![], false, v));
    testing_env!(context(alice()));
    let mut contract = BtpMessageCenter::new("0x1.near".into(), 1500);
    contract.add_service(
        "test".to_string(),
        "sssssssss.s".parse::<AccountId>().unwrap(),
    );

    contract.remove_service("test".to_string());

    testing_env!(context(bob()));
    assert_eq!(to_value(contract.get_services()).unwrap(), json!([]));
}

#[test]
#[should_panic(expected = "BMCRevertNotExistsPermission")]
fn remove_service_permission() {
    let context = |v: AccountId| (get_context(vec![], false, v));
    testing_env!(context(alice()));
    let mut contract = BtpMessageCenter::new("0x1.near".into(), 1500);
    contract.add_service(
        "test".to_string(),
        "sssssssss.s".parse::<AccountId>().unwrap(),
    );

    testing_env!(context(bob()));
    contract.remove_service("test".to_string());
}

#[test]
#[should_panic(expected = "BMCRevertNotExistBSH")]
fn remove_service_non_existing_service() {
    let context = |v: AccountId| (get_context(vec![], false, v));
    testing_env!(context(alice()));
    let mut contract = BtpMessageCenter::new("0x1.near".into(), 1500);

    contract.remove_service("test1".to_string());
}

#[test]
fn get_services() {
    let context = |v: AccountId| (get_context(vec![], false, v));
    testing_env!(context(alice()));
    let mut contract = BtpMessageCenter::new("0x1.near".into(), 1500);
    contract.add_service(
        "test".to_string(),
        "sssssssss.s".parse::<AccountId>().unwrap(),
    );

    assert_eq!(
        to_value(contract.get_services()).unwrap(),
        json!([{"name":"test","service":"sssssssss.s"}])
    );
}
