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
#[should_panic(expected = "BMCRevertInvalidAddress: not supported protocol http")]
fn add_relays_invalid_link_fail(){
    let context = |v: AccountId| (get_context(vec![], false, v));
    testing_env!(context(bmc()));
    let mut contract = BTPMessageCenter {
        ..Default::default()
    };
    let invalid_btp =  "http://test.com/testtesttest";
    contract.add_relays(invalid_btp.to_string(), carol());
}

#[test]
#[should_panic(expected = "BMCRevertNotExistsLink")]
fn add_relays_non_existing_link_fail(){
    let context = |v: AccountId| (get_context(vec![], false, v));
    testing_env!(context(bmc()));
    let mut contract = BTPMessageCenter {
        ..Default::default()
    };
    let link =  "btp://0x1.near/cx87ed9048b594b95199f326fc76e76a9d33dd665b";
    contract.add_relays(link.to_string(), carol());
}

#[test]
fn add_relays_existing_link_pass(){
    let context = |v: AccountId| (get_context(vec![], false, v));
    testing_env!(context(bmc()));
    let mut contract = BTPMessageCenter {
        ..Default::default()
    };
    let link =  "btp://0x1.near/cx87ed9048b594b95199f326fc76e76a9d33dd665b";
    contract.add_link(link.to_string());
    contract.add_relays(link.to_string(), format!("[\"{}\"]", carol()));
    let expected: Vec<String> = vec![carol()];
    if let Some(link) = contract.links.get(&link.to_string()) {
        assert_eq!(link.relays.to_vec(), expected);
    }
}

// #[test]
// fn remove