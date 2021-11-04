use bmc::BtpMessageCenter;
use near_sdk::{testing_env, AccountId, VMContext};
pub mod accounts;
use accounts::*;

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
fn request_service_new_request() {
    let context = |v: AccountId| (get_context(vec![], false, v));
    testing_env!(context(bob()));
    let mut contract = BtpMessageCenter::new("0x1.near".into(), 1500);
    contract.request_service(
        "test".to_string(),
        "sssssssss.ss".parse::<AccountId>().unwrap(),
    );

    testing_env!(context(alice()));
    let requests = contract.get_requests();
    assert_eq!(
        requests,
        "[{\"name\":\"test\",\"service\":\"sssssssss.ss\"}]"
    );
}

#[test]
#[should_panic(expected = "BMCRevertRequestPending")]
fn request_service_existing_request() {
    let context = |v: AccountId| (get_context(vec![], false, v));
    testing_env!(context(bob()));
    let mut contract = BtpMessageCenter::new("0x1.near".into(), 1500);
    contract.request_service(
        "test".to_string(),
        "sssssssss.s".parse::<AccountId>().unwrap(),
    );
    contract.request_service(
        "test".to_string(),
        "sssssssss.s".parse::<AccountId>().unwrap(),
    );
}

#[test]
#[ignore] // As we are not able to pass in account id params unchecked
#[should_panic(expected = "BMCRevertInvalidAddress")]
fn request_service_invalid_address() {
    let context = |v: AccountId| (get_context(vec![], false, v));
    testing_env!(context(bob()));
    let mut contract = BtpMessageCenter::new("0x1.near".into(), 1500);
    contract.request_service(
        "test".to_string(),
        AccountId::new_unchecked("10-4.8-2".to_string()),
    );
}

#[test]
#[should_panic(expected = "BMCRevertAlreadyExistsBSH")]
fn request_service_existing_service() {
    let context = |v: AccountId| (get_context(vec![], false, v));
    testing_env!(context(bob()));
    let mut contract = BtpMessageCenter::new("0x1.near".into(), 1500);
    contract.request_service(
        "test".to_string(),
        "sssssssss.s".parse::<AccountId>().unwrap(),
    );

    testing_env!(context(alice()));
    contract.approve_service("test".to_string(), true);

    testing_env!(context(bob()));
    contract.request_service(
        "test".to_string(),
        "sssssssss.s".parse::<AccountId>().unwrap(),
    );
}

#[test]
fn get_requests() {
    let context = |v: AccountId| (get_context(vec![], false, v));
    testing_env!(context(bob()));
    let mut contract = BtpMessageCenter::new("0x1.near".into(), 1500);
    contract.request_service(
        "test".to_string(),
        "sssssssss.s".parse::<AccountId>().unwrap(),
    );

    testing_env!(context(alice()));
    assert_eq!(
        contract.get_requests(),
        "[{\"name\":\"test\",\"service\":\"sssssssss.s\"}]"
    );
}

#[test]
fn approve_service_approve() {
    let context = |v: AccountId| (get_context(vec![], false, v));
    testing_env!(context(bob()));
    let mut contract = BtpMessageCenter::new("0x1.near".into(), 1500);
    contract.request_service(
        "test".to_string(),
        "sssssssss.s".parse::<AccountId>().unwrap(),
    );

    testing_env!(context(alice()));
    contract.approve_service("test".to_string(), true);

    testing_env!(context(bob()));
    assert_eq!(
        contract.get_services(),
        "[{\"name\":\"test\",\"service\":\"sssssssss.s\"}]"
    );
}

#[test]
#[should_panic(expected = "BMCRevertNotExistRequest")]
fn approve_service_non_existing_request() {
    let context = |v: AccountId| (get_context(vec![], false, v));
    testing_env!(context(bob()));
    let mut contract = BtpMessageCenter::new("0x1.near".into(), 1500);
    contract.request_service(
        "test".to_string(),
        "sssssssss.s".parse::<AccountId>().unwrap(),
    );

    testing_env!(context(alice()));
    contract.approve_service("test1".to_string(), true);
}

#[test]
fn approve_service_reject() {
    let context = |v: AccountId| (get_context(vec![], false, v));
    testing_env!(context(bob()));
    let mut contract = BtpMessageCenter::new("0x1.near".into(), 1500);
    contract.request_service(
        "test".to_string(),
        "sssssssss.s".parse::<AccountId>().unwrap(),
    );

    testing_env!(context(alice()));
    contract.approve_service("test".to_string(), false);

    testing_env!(context(bob()));
    assert_eq!(contract.get_requests(), "[]");
}

#[test]
#[should_panic(expected = "BMCRevertNotExistsPermission")]
fn approve_service_permission() {
    let context = |v: AccountId| (get_context(vec![], false, v));
    testing_env!(context(bob()));
    let mut contract = BtpMessageCenter::new("0x1.near".into(), 1500);
    contract.request_service(
        "test".to_string(),
        "sssssssss.s".parse::<AccountId>().unwrap(),
    );
    contract.approve_service("test".to_string(), true);
}

#[test]
fn remove_service_existing_service() {
    let context = |v: AccountId| (get_context(vec![], false, v));
    testing_env!(context(bob()));
    let mut contract = BtpMessageCenter::new("0x1.near".into(), 1500);
    contract.request_service(
        "test".to_string(),
        "sssssssss.s".parse::<AccountId>().unwrap(),
    );

    testing_env!(context(alice()));
    contract.approve_service("test".to_string(), true);

    testing_env!(context(alice()));
    contract.remove_service("test".to_string());

    testing_env!(context(bob()));
    assert_eq!(contract.get_services(), "[]");
}

#[test]
#[should_panic(expected = "BMCRevertNotExistsPermission")]
fn remove_service_permission() {
    let context = |v: AccountId| (get_context(vec![], false, v));
    testing_env!(context(bob()));
    let mut contract = BtpMessageCenter::new("0x1.near".into(), 1500);
    contract.request_service(
        "test".to_string(),
        "sssssssss.s".parse::<AccountId>().unwrap(),
    );

    testing_env!(context(alice()));
    contract.approve_service("test".to_string(), false);

    testing_env!(context(bob()));
    contract.remove_service("test".to_string());
}

#[test]
#[should_panic(expected = "BMCRevertNotExistBSH")]
fn remove_service_non_existing_service() {
    let context = |v: AccountId| (get_context(vec![], false, v));
    testing_env!(context(bob()));
    let mut contract = BtpMessageCenter::new("0x1.near".into(), 1500);
    contract.request_service(
        "test".to_string(),
        "sssssssss.s".parse::<AccountId>().unwrap(),
    );

    testing_env!(context(alice()));
    contract.approve_service("test".to_string(), false);

    testing_env!(context(alice()));
    contract.remove_service("test1".to_string());
}

#[test]
fn get_services() {
    let context = |v: AccountId| (get_context(vec![], false, v));
    testing_env!(context(bob()));
    let mut contract = BtpMessageCenter::new("0x1.near".into(), 1500);
    contract.request_service(
        "test".to_string(),
        "sssssssss.s".parse::<AccountId>().unwrap(),
    );

    testing_env!(context(alice()));
    contract.approve_service("test".to_string(), true);
    assert_eq!(
        contract.get_services(),
        "[{\"name\":\"test\",\"service\":\"sssssssss.s\"}]"
    );
}