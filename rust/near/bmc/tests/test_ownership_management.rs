use near_sdk::MockedBlockchain;
use near_sdk::{testing_env, VMContext, AccountId};
use bmc::BTPMessageCenter;
use std::collections::HashSet;
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
fn add_owner_new_owner_pass(){
    let context = |v: AccountId| (get_context(vec![], false, v));
    testing_env!(context(bmc()));
    let mut contract = BTPMessageCenter {
        ..Default::default()
    };

    contract.add_owner(alice());

    let owners = contract.owners.to_vec();
    let result: HashSet<_> = owners.iter().collect();
    let expected_owners: Vec<String> = vec![
        alice(),
        bmc(),
    ];
    let expected: HashSet<_> = expected_owners.iter().collect();
    assert_eq!(result, expected);
}

#[test]
#[should_panic(expected = "BMCRevertAlreadyExistsOwner")]
fn add_owner_exisinting_owner_fail(){
    let context = |v: AccountId| (get_context(vec![], false, v));
    testing_env!(context(bmc()));
    let mut contract = BTPMessageCenter {
        ..Default::default()
    };

    contract.add_owner(bmc());
}

#[test]
#[should_panic(expected = "BMCRevertNotExistsPermission")]
fn add_owner_permission_fail(){
    let context = |v: AccountId| (get_context(vec![], false, v));
    testing_env!(context(bmc()));
    let mut contract = BTPMessageCenter {
        ..Default::default()
    };
    testing_env!(context(alice()));
    contract.add_owner(bob());
}

#[test]
fn remove_owner_existing_owner_pass(){
    let context = |v: AccountId| (get_context(vec![], false, v));
    testing_env!(context(bmc()));
    let mut contract = BTPMessageCenter {
        ..Default::default()
    };

    contract.add_owner(alice());
    contract.add_owner(bob());

    contract.remove_owner(alice());
    let owners = contract.owners.to_vec();
    let result: HashSet<_> = owners.iter().collect();
    let expected_owners: Vec<String> = vec![
        bob(),
        bmc(),
    ];
    let expected: HashSet<_> = expected_owners.iter().collect();
    assert_eq!(result, expected);
}

#[test]
#[should_panic(expected = "BMCRevertNotExistsPermission")]
fn remove_owner_permission_fail(){
    let context = |v: AccountId| (get_context(vec![], false, v));
    testing_env!(context(bmc()));
    let mut contract = BTPMessageCenter {
        ..Default::default()
    };

    contract.add_owner(alice());
    contract.add_owner(bob());

    testing_env!(context(carol()));
    contract.add_owner(bob());
}

#[test]
#[should_panic(expected = "BMCRevertLastOwner")]
fn remove_owner_last_owner_fail(){
    let context = |v: AccountId| (get_context(vec![], false, v));
    testing_env!(context(bmc()));
    let mut contract = BTPMessageCenter {
        ..Default::default()
    };

    contract.remove_owner(bmc());
}

#[test]
#[should_panic(expected = "BMCRevertNotExistsOwner")]
fn remove_owner_non_exisitng_owner_fail(){
    let context = |v: AccountId| (get_context(vec![], false, v));
    testing_env!(context(bmc()));
    let mut contract = BTPMessageCenter {
        ..Default::default()
    };

    contract.remove_owner(alice());
}

#[test]
fn has_permission_existing_owner(){
    let context = |v: AccountId| (get_context(vec![], false, v));
    testing_env!(context(bmc()));
    let mut contract = BTPMessageCenter {
        ..Default::default()
    };
    contract.add_owner(alice());
    testing_env!(context(alice()));
    contract.has_permission();
}

#[test]
#[should_panic(expected = "BMCRevertNotExistsPermission")]
fn has_permission_none_existing_owner(){
    let context = |v: AccountId| (get_context(vec![], false, v));
    testing_env!(context(bmc()));
    let mut contract = BTPMessageCenter {
        ..Default::default()
    };
    contract.add_owner(alice());
    testing_env!(context(bob()));
    contract.has_permission();
}