use bmc::BtpMessageCenter;
use near_sdk::{testing_env, AccountId, VMContext};
pub mod accounts;
use accounts::*;

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
fn add_verifier_new_verifier_pass() {
    let context = |v: AccountId| (get_context(vec![], false, v));
    testing_env!(context(alice()));
    let mut contract = BtpMessageCenter::new("0x1.near".into());
    contract.add_verifier(
        "test".to_string(),
        "sssssssss.ss".parse::<AccountId>().unwrap(),
    );

    let verifiers = contract.get_verifiers();
    assert_eq!(
        verifiers,
        "[{\"network\":\"test\",\"verifier\":\"sssssssss.ss\"}]"
    );
}

#[test]
#[should_panic(expected = "BMCRevertAlreadyExistsBMV")]
fn add_verifier_existing_verifier_fail() {
    let context = |v: AccountId| (get_context(vec![], false, v));
    testing_env!(context(alice()));
    let mut contract = BtpMessageCenter::new("0x1.near".into());
    contract.add_verifier(
        "test".to_string(),
        "sssssssss.s".parse::<AccountId>().unwrap(),
    );

    contract.add_verifier(
        "test".to_string(),
        "sssssssss.s".parse::<AccountId>().unwrap(),
    );
}

#[test]
fn get_verifiers_pass() {
    let context = |v: AccountId| (get_context(vec![], false, v));
    testing_env!(context(alice()));
    let mut contract = BtpMessageCenter::new("0x1.near".into());
    contract.add_verifier(
        "test".to_string(),
        "sssssssss.s".parse::<AccountId>().unwrap(),
    );

    testing_env!(context(bob()));
    assert_eq!(
        contract.get_verifiers(),
        "[{\"network\":\"test\",\"verifier\":\"sssssssss.s\"}]"
    );
}

#[test]
#[should_panic(expected = "BMCRevertNotExistsPermission")]
fn add_verifier_permission_fail() {
    let context = |v: AccountId| (get_context(vec![], false, v));
    testing_env!(context(bob()));
    let mut contract = BtpMessageCenter::new("0x1.near".into());
    contract.add_verifier(
        "test".to_string(),
        "sssssssss.s".parse::<AccountId>().unwrap(),
    );
}

#[test]
fn remove_verifier_existing_verifier_pass() {
    let context = |v: AccountId| (get_context(vec![], false, v));
    testing_env!(context(alice()));
    let mut contract = BtpMessageCenter::new("0x1.near".into());
    contract.add_verifier(
        "test".to_string(),
        "sssssssss.s".parse::<AccountId>().unwrap(),
    );

    contract.remove_verifier("test".to_string());
}

#[test]
#[should_panic(expected = "BMCRevertNotExistsPermission")]
fn remove_verifier_permission_fail() {
    let context = |v: AccountId| (get_context(vec![], false, v));
    testing_env!(context(alice()));
    let mut contract = BtpMessageCenter::new("0x1.near".into());
    contract.add_verifier(
        "test".to_string(),
        "sssssssss.s".parse::<AccountId>().unwrap(),
    );

    testing_env!(context(bob()));
    contract.remove_verifier("test".to_string());
}

#[test]
#[should_panic(expected = "BMCRevertNotExistBMV")]
fn remove_verifier_non_existing_verifier_fail() {
    let context = |v: AccountId| (get_context(vec![], false, v));
    testing_env!(context(alice()));
    let mut contract = BtpMessageCenter::new("0x1.near".into());
    contract.add_verifier(
        "test".to_string(),
        "sssssssss.s".parse::<AccountId>().unwrap(),
    );

    testing_env!(context(alice()));
    contract.remove_verifier("test1".to_string());
}
