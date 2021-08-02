use near_sdk::MockedBlockchain;
use near_sdk::{testing_env, VMContext, AccountId};
use bmc::BTPMessageCenter;
pub mod accounts;
use accounts::*;

fn get_context(input: Vec<u8>, is_view: bool, signer_account_id: AccountId) -> VMContext {
    VMContext {
        current_account_id: bmc(),
        signer_account_id: signer_account_id,
        signer_account_pk: vec![0, 1, 2],
        predecessor_account_id: bsh(),
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
fn request_service_new_request_pass() {
    let context = |v: AccountId| (get_context(vec![], false, v));
    testing_env!(context(bsh()));
    let mut contract = BTPMessageCenter {
        ..Default::default()
    };
    contract.request_service("test".to_string(), "sssssssss.ss".to_string());

    testing_env!(context(alice()));
    let requests = contract.get_requests();
    assert_eq!(
        requests,
        "[{\"name\":\"test\",\"address\":\"sssssssss.ss\"}]"
    );
}

#[test]
#[should_panic(expected = "BMCRevertRequestPending")]
fn request_service_existing_request_fail() {
    let context = |v: AccountId| (get_context(vec![], false, v));
    testing_env!(context(bsh()));
    let mut contract = BTPMessageCenter {
        ..Default::default()
    };
    contract.request_service("test".to_string(), "sssssssss.s".to_string());
    contract.request_service("test".to_string(), "sssssssss.s".to_string());
}

#[test]
#[should_panic(expected = "BMCRevertInvalidAddress")]
fn request_service_invalid_address_fail() {
    let context = |v: AccountId| (get_context(vec![], false, v));
    testing_env!(context(bsh()));
    let mut contract = BTPMessageCenter {
        ..Default::default()
    };
    contract.request_service("test".to_string(), "sssssssss.".to_string());
}

#[test]
#[should_panic(expected = "BMCRevertAlreadyExistsBSH")]
fn request_service_existing_service_fail() {
    let context = |v: AccountId| (get_context(vec![], false, v));
    testing_env!(context(bsh()));
    let mut contract = BTPMessageCenter {
        ..Default::default()
    };
    contract.request_service("test".to_string(), "sssssssss.s".to_string());

    testing_env!(context(bmc()));
    contract.approve_service("test".to_string(), true);

    testing_env!(context(bsh()));
    contract.request_service("test".to_string(), "sssssssss.s".to_string());
}

#[test]
fn get_requests_pass() {
    let context = |v: AccountId| (get_context(vec![], false, v));
    testing_env!(context(bsh()));
    let mut contract = BTPMessageCenter {
        ..Default::default()
    };
    contract.request_service("test".to_string(), "sssssssss.s".to_string());

    testing_env!(context(alice()));
    assert_eq!(contract.get_requests(), "[{\"name\":\"test\",\"address\":\"sssssssss.s\"}]");
}

#[test]
fn approve_service_approve_pass(){
    let context = |v: AccountId| (get_context(vec![], false, v));
    testing_env!(context(bsh()));
    let mut contract = BTPMessageCenter {
        ..Default::default()
    };
    contract.request_service("test".to_string(), "sssssssss.s".to_string());

    testing_env!(context(bmc()));
    contract.approve_service("test".to_string(), true);

    testing_env!(context(alice()));
    assert_eq!(contract.get_services(), "[{\"name\":\"test\",\"address\":\"sssssssss.s\"}]");
}

#[test]
fn approve_service_reject_pass(){
    let context = |v: AccountId| (get_context(vec![], false, v));
    testing_env!(context(bsh()));
    let mut contract = BTPMessageCenter {
        ..Default::default()
    };
    contract.request_service("test".to_string(), "sssssssss.s".to_string());

    testing_env!(context(bmc()));
    contract.approve_service("test".to_string(), false);

    testing_env!(context(alice()));
    assert_eq!(contract.get_requests(), "[]");
}


#[test]
#[should_panic(expected = "BMCRevertNotExistsPermission")]
fn approve_service_permission_fail(){
    let context = |v: AccountId| (get_context(vec![], false, v));
    testing_env!(context(bsh()));
    let mut contract = BTPMessageCenter {
        ..Default::default()
    };
    contract.request_service("test".to_string(), "sssssssss.s".to_string());
    contract.approve_service("test".to_string(), true);
}


#[test]
fn remove_service_existing_service_pass(){
    let context = |v: AccountId| (get_context(vec![], false, v));
    testing_env!(context(bsh()));
    let mut contract = BTPMessageCenter {
        ..Default::default()
    };
    contract.request_service("test".to_string(), "sssssssss.s".to_string());

    testing_env!(context(bmc()));
    contract.approve_service("test".to_string(), true);

    testing_env!(context(bmc()));
    contract.remove_service("test".to_string());

    testing_env!(context(alice()));
    assert_eq!(contract.get_services(), "[]");
}

#[test]
#[should_panic(expected = "BMCRevertNotExistsPermission")]
fn remove_service_permission_fail(){
    let context = |v: AccountId| (get_context(vec![], false, v));
    testing_env!(context(bsh()));
    let mut contract = BTPMessageCenter {
        ..Default::default()
    };
    contract.request_service("test".to_string(), "sssssssss.s".to_string());

    testing_env!(context(bmc()));
    contract.approve_service("test".to_string(), false);

    testing_env!(context(bsh()));
    contract.remove_service("test".to_string());
}

#[test]
#[should_panic(expected = "BMCRevertNotExistBSH")]
fn remove_service_non_existing_service_fail(){
    let context = |v: AccountId| (get_context(vec![], false, v));
    testing_env!(context(bsh()));
    let mut contract = BTPMessageCenter {
        ..Default::default()
    };
    contract.request_service("test".to_string(), "sssssssss.s".to_string());

    testing_env!(context(bmc()));
    contract.approve_service("test".to_string(), false);

    testing_env!(context(bmc()));
    contract.remove_service("test1".to_string());
}

#[test]
fn get_services_pass() {
    let context = |v: AccountId| (get_context(vec![], false, v));
    testing_env!(context(bsh()));
    let mut contract = BTPMessageCenter {
        ..Default::default()
    };
    contract.request_service("test".to_string(), "sssssssss.s".to_string());

    testing_env!(context(bmc()));
    contract.approve_service("test".to_string(), true);
    assert_eq!(contract.get_services(), "[{\"name\":\"test\",\"address\":\"sssssssss.s\"}]");
}
