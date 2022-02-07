use near_sdk::{testing_env, AccountId, VMContext};
use std::collections::HashSet;
use token_service::TokenService;
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
fn add_owner_new_owner() {
    let context = |v: AccountId| (get_context(vec![], false, v));
    testing_env!(context(alice()));
    let mut contract = TokenService::new("TokenBSH".to_string(), bmc(), "0x1.near".into(),1000.into());

    contract.add_owner(carol());

    let owners = contract.get_owners();
    let result: HashSet<_> = owners.iter().collect();
    let expected_owners: Vec<AccountId> = vec![alice(), carol()];
    let expected: HashSet<_> = expected_owners.iter().collect();
    assert_eq!(result, expected);
}

#[test]
#[should_panic(expected = "BSHRevertAlreadyExistsOwner")]
fn add_owner_existing_owner() {
    let context = |v: AccountId| (get_context(vec![], false, v));
    testing_env!(context(alice()));
    let mut contract = TokenService::new("TokenBSH".to_string(), bmc(), "0x1.near".into(),1000.into());

    contract.add_owner(alice());
}

#[test]
#[should_panic(expected = "BSHRevertNotExistsPermission")]
fn add_owner_permission() {
    let context = |v: AccountId| (get_context(vec![], false, v));
    testing_env!(context(alice()));
    let mut contract = TokenService::new("TokenBSH".to_string(), bmc(), "0x1.near".into(),1000.into());
    testing_env!(context(chuck()));
    contract.add_owner(carol());
}

#[test]
fn remove_owner_existing_owner() {
    let context = |v: AccountId| (get_context(vec![], false, v));
    testing_env!(context(alice()));
    let mut contract = TokenService::new("TokenBSH".to_string(), bmc(), "0x1.near".into(),1000.into());

    contract.add_owner(carol());
    contract.add_owner(charlie());

    contract.remove_owner(alice());
    let owners = contract.get_owners();
    let result: HashSet<_> = owners.iter().collect();
    let expected_owners: Vec<AccountId> = vec![carol(), charlie()];
    let expected: HashSet<_> = expected_owners.iter().collect();
    assert_eq!(result, expected);
}

#[test]
#[should_panic(expected = "BSHRevertNotExistsPermission")]
fn remove_owner_permission() {
    let context = |v: AccountId| (get_context(vec![], false, v));
    testing_env!(context(alice()));
    let mut contract = TokenService::new("TokenBSH".to_string(), bmc(), "0x1.near".into(),1000.into());

    contract.add_owner(carol());

    testing_env!(context(chuck()));
    contract.add_owner(charlie());
}

#[test]
#[should_panic(expected = "BSHRevertLastOwner")]
fn remove_owner_last_owner() {
    let context = |v: AccountId| (get_context(vec![], false, v));
    testing_env!(context(alice()));
    let mut contract = TokenService::new("TokenBSH".to_string(), bmc(), "0x1.near".into(),1000.into());

    contract.remove_owner(alice());
}

#[test]
#[should_panic(expected = "BSHRevertNotExistsOwner")]
fn remove_owner_non_existing_owner() {
    let context = |v: AccountId| (get_context(vec![], false, v));
    testing_env!(context(alice()));
    let mut contract = TokenService::new("TokenBSH".to_string(), bmc(), "0x1.near".into(),1000.into());

    contract.remove_owner(carol());
}
