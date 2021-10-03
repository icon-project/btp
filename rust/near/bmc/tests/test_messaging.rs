use bmc::BtpMessageCenter;
use near_sdk::{AccountId, VMContext, serde_json::{json, from_value}, testing_env};
use std::collections::HashSet;
pub mod accounts;
use accounts::*;
use libraries::types::{
    messages::BmcServiceMessage, messages::BmcServiceType, messages::BtpMessage,
    messages::SerializedMessage, messages::SerializedBtpMessages, Address, BTPAddress, HashedCollection, WrappedI128,
};

fn get_context(input: Vec<u8>, is_view: bool, signer_account_id: AccountId) -> VMContext {
    VMContext {
        current_account_id: alice().to_string(),
        signer_account_id: signer_account_id.to_string(),
        signer_account_pk: vec![0, 1, 2],
        predecessor_account_id: alice().to_string(),
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
fn handle_serialized_btp_messages_service_message() {
    let context = |v: AccountId| (get_context(vec![], false, v));
    testing_env!(context(alice()));
    let mut contract = BtpMessageCenter::new("0x1.near".into());
}

#[test]
#[cfg(feature = "testable")]
fn handle_internal_service_message_init_pass() {
    let context = |v: AccountId| (get_context(vec![], false, v));
    testing_env!(context(alice()));
    let mut contract = BtpMessageCenter::new("0x1.near".into());
    let link =
        BTPAddress::new("btp://0x1.icon/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string());
    contract.add_verifier(link.network_address().unwrap(), verifier());
    contract.add_link(link.clone());
    let bmc_service_message = BmcServiceMessage::new(BmcServiceType::Init {
        links: vec![
            BTPAddress::new("btp://0x1.pra/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string()),
            BTPAddress::new("btp://0x5.pra/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string()),
        ],
    });
    let btp_message = <BtpMessage<SerializedMessage>>::new(
        BTPAddress::new("btp://0x1.icon/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string()),
        BTPAddress::new("btp://0x1.near/88bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4".to_string()),
        "bmc".to_string(),
        WrappedI128::new(1),
        <Vec<u8>>::from(bmc_service_message.clone()),
        None,
    );

    contract.handle_serialized_btp_messages(link.clone(), vec![btp_message]);
    let reachables = contract.get_reachable_link(link.clone());
    let mut expected = HashedCollection::new();
    expected.add(BTPAddress::new(
        "btp://0x1.pra/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string(),
    ));
    expected.add(BTPAddress::new(
        "btp://0x5.pra/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string(),
    ));
    assert_eq!(reachables, expected);
}

#[test]
#[cfg(feature = "testable")]
fn handle_internal_service_message_link_pass() {
    let context = |v: AccountId| (get_context(vec![], false, v));
    testing_env!(context(alice()));
    let mut contract = BtpMessageCenter::new("0x1.near".into());
    let context = |v: AccountId| (get_context(vec![], false, v));
    testing_env!(context(alice()));
    let mut contract = BtpMessageCenter::new("0x1.near".into());
    let link =
        BTPAddress::new("btp://0x1.icon/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string());
    contract.add_verifier(link.network_address().unwrap(), verifier());
    contract.add_link(link.clone());
    let bmc_service_message_1 = BmcServiceMessage::new(BmcServiceType::Init {
        links: vec![
            BTPAddress::new("btp://0x1.pra/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string()),
            BTPAddress::new("btp://0x5.pra/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string()),
        ],
    });
    let btp_message_1 = <BtpMessage<SerializedMessage>>::new(
        BTPAddress::new("btp://0x1.icon/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string()),
        BTPAddress::new("btp://0x1.near/88bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4".to_string()),
        "bmc".to_string(),
        WrappedI128::new(1),
        <Vec<u8>>::from(bmc_service_message_1.clone()),
        None,
    );

    let bmc_service_message_2 = BmcServiceMessage::new(BmcServiceType::Link {
        link: BTPAddress::new(
            "btp://0x5.bsc/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string(),
        ),
    });
    let btp_message_2 = <BtpMessage<SerializedMessage>>::new(
        BTPAddress::new("btp://0x1.icon/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string()),
        BTPAddress::new("btp://0x1.near/88bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4".to_string()),
        "bmc".to_string(),
        WrappedI128::new(1),
        <Vec<u8>>::from(bmc_service_message_2.clone()),
        None,
    );

    contract.handle_serialized_btp_messages(link.clone(), vec![btp_message_1, btp_message_2]);
    let reachables = contract.get_reachable_link(link.clone());
    let mut expected = HashedCollection::new();
    expected.add(BTPAddress::new(
        "btp://0x1.pra/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string(),
    ));
    expected.add(BTPAddress::new(
        "btp://0x5.pra/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string(),
    ));
    expected.add(BTPAddress::new(
        "btp://0x5.bsc/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string(),
    ));
    assert_eq!(reachables, expected);
}

#[test]
#[cfg(feature = "testable")]
fn handle_internal_service_message_unlink_pass() {
    let context = |v: AccountId| (get_context(vec![], false, v));
    testing_env!(context(alice()));
    let mut contract = BtpMessageCenter::new("0x1.near".into());
    let context = |v: AccountId| (get_context(vec![], false, v));
    testing_env!(context(alice()));
    let mut contract = BtpMessageCenter::new("0x1.near".into());
    let link =
        BTPAddress::new("btp://0x1.icon/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string());
    contract.add_verifier(link.network_address().unwrap(), verifier());
    contract.add_link(link.clone());
    let bmc_service_message_1 = BmcServiceMessage::new(BmcServiceType::Init {
        links: vec![
            BTPAddress::new("btp://0x1.pra/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string()),
            BTPAddress::new("btp://0x5.pra/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string()),
        ],
    });
    let btp_message_1 = <BtpMessage<SerializedMessage>>::new(
        BTPAddress::new("btp://0x1.icon/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string()),
        BTPAddress::new("btp://0x1.near/88bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4".to_string()),
        "bmc".to_string(),
        WrappedI128::new(1),
        <Vec<u8>>::from(bmc_service_message_1.clone()),
        None,
    );

    let bmc_service_message_2 = BmcServiceMessage::new(BmcServiceType::Unlink {
        link: BTPAddress::new(
            "btp://0x5.pra/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string(),
        ),
    });
    let btp_message_2 = <BtpMessage<SerializedMessage>>::new(
        BTPAddress::new("btp://0x1.icon/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string()),
        BTPAddress::new("btp://0x1.near/88bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4".to_string()),
        "bmc".to_string(),
        WrappedI128::new(1),
        <Vec<u8>>::from(bmc_service_message_2.clone()),
        None,
    );

    contract.handle_serialized_btp_messages(link.clone(), vec![btp_message_1, btp_message_2]);
    let reachables = contract.get_reachable_link(link.clone());
    let mut expected = HashedCollection::new();
    expected.add(BTPAddress::new(
        "btp://0x1.pra/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string(),
    ));
    assert_eq!(reachables, expected);
}

#[test]
fn deserialize_serialized_btp_messages_from_json(){
    let btp_message = json!(["-QEUuDlidHA6Ly8weDEuaWNvbi9jeDg3ZWQ5MDQ4YjU5NGI5NTE5OWYzMjZmYzc2ZTc2YTlkMzNkZDY2NWK4TmJ0cDovLzB4NS5wcmEvODhiZDA1NDQyNjg2YmUwYTVkZjdkYTMzYjZmMTA4OWViZmVhMzc2OWIxOWRiYjI0NzdmZTBjZDZlMGYxMjZlNINibWOBgLiB-H-ESW5pdLh4-Hb4dLg4YnRwOi8vMHgxLnByYS9jeDg3ZWQ5MDQ4YjU5NGI5NTE5OWYzMjZmYzc2ZTc2YTlkMzNkZDY2NWK4OGJ0cDovLzB4NS5wcmEvY3g4N2VkOTA0OGI1OTRiOTUxOTlmMzI2ZmM3NmU3NmE5ZDMzZGQ2NjVi"]);
    let serialized_btp_messages: SerializedBtpMessages = from_value(btp_message).unwrap();
    // TODO: Add;
}
