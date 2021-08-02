use bmc::BTPMessageCenter;
use near_sdk::MockedBlockchain;
use near_sdk::{testing_env, AccountId, VMContext};
use std::collections::HashSet;
pub mod accounts;
use accounts::*;
use rlp::{Decodable, DecoderError, Encodable, Rlp, RlpStream};

#[macro_use]
extern crate hex_literal;
use hex_literal::hex;

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

// #[test]
fn add_link_new_link_pass() {
    let context = |v: AccountId| (get_context(vec![], false, v));
    testing_env!(context(bmc()));
    let mut contract = BTPMessageCenter {
        ..Default::default()
    };
    let link = "btp://0x1.near/cx87ed9048b594b95199f326fc76e76a9d33dd665b";
    contract.add_link(link.to_string());
    let owners = contract.owners.to_vec();
    let result: HashSet<_> = owners.iter().collect();
    let expected_owners: Vec<String> = vec![bmc()];

    struct Data<T>(Vec<T>)
    where
        T: Encodable;

    let data = Data(vec!["cat", "dog"]);
    //let serialized = rlp::encode_list(&data.0);
    let r = rlp::RlpStream::new();
    //r.append_raw(bytes: &[u8], item_count: usize);
    // bincode::serialize(&data).unwrap();
    //let expected = hex!("0xa04e454b49dc8a2e2a229e0ce911e9fd4d2aa647de4cf6e0df40cf71bff7283330");

    //assert!(serialized.at(0).unwrap().is_data());
    //let expected = hex!("a04e454b49dc8a2e2a229e0ce911e9fd4d2aa647de4cf6e0df40cf71bff7283330");

    //assert_eq!(serialized.as_raw(), expected);
}

#[test]
fn set_link_block_interval_pass() {
    let context = |v: AccountId| (get_context(vec![], false, v));
    testing_env!(context(bmc()));
    let mut contract = BTPMessageCenter {
        ..Default::default()
    };
    let link = "btp://0x1.near/cx87ed9048b594b95199f326fc76e76a9d33dd665b";
    contract.add_link(link.to_string());
    contract.set_link(link.to_string(), 10, 1, 1);
    let expected = 10;
    if let Some(link) = contract.links.get(&link.to_string()) {
        assert_eq!(link.block_interval_src, expected);
    }
}
