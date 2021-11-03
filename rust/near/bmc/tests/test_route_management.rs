use bmc::BtpMessageCenter;
use near_sdk::{serde_json::json, testing_env, AccountId, VMContext};

pub mod accounts;
use accounts::*;
use libraries::types::{
    messages::BmcServiceMessage, messages::BmcServiceType, messages::BtpMessage,
    messages::SerializedMessage, Address, BTPAddress, HashedCollection, HashedValue, WrappedI128,
};

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
fn add_route_new_route() {
    let context = |v: AccountId| (get_context(vec![], false, v));
    testing_env!(context(alice()));
    let mut contract = BtpMessageCenter::new("0x1.near".into(), 1500);
    let link =
        BTPAddress::new("btp://0x1.icon/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string());
    let destination =
        BTPAddress::new("btp://0x1.bsc/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string());
    contract.add_verifier(link.network_address().unwrap(), verifier());
    contract.add_link(link.clone());

    contract.add_route(destination.clone(), link.clone());

    let result = contract.get_routes();

    assert_eq!(
        result,
        json!([{
            "dst": "btp://0x1.bsc/cx87ed9048b594b95199f326fc76e76a9d33dd665b",
            "next": "btp://0x1.icon/cx87ed9048b594b95199f326fc76e76a9d33dd665b"
        }])
    )
}

#[test]
#[should_panic(expected = "BMCRevertAlreadyExistsRoute")]
fn add_route_existing_route() {
    let context = |v: AccountId| (get_context(vec![], false, v));
    testing_env!(context(alice()));
    let mut contract = BtpMessageCenter::new("0x1.near".into(), 1500);
    let link =
        BTPAddress::new("btp://0x1.icon/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string());
    let destination =
        BTPAddress::new("btp://0x1.bsc/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string());
    contract.add_verifier(link.network_address().unwrap(), verifier());
    contract.add_link(link.clone());
    contract.add_route(destination.clone(), link.clone());
    contract.add_route(destination.clone(), link.clone());
}

#[test]
#[should_panic(expected = "BMCRevertNotExistsLink")]
fn add_route_non_existing_link() {
    let context = |v: AccountId| (get_context(vec![], false, v));
    testing_env!(context(alice()));
    let mut contract = BtpMessageCenter::new("0x1.near".into(), 1500);
    let link =
        BTPAddress::new("btp://0x1.icon/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string());
    let destination =
        BTPAddress::new("btp://0x1.bsc/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string());
    contract.add_route(destination.clone(), link.clone());
}

#[test]
#[should_panic(expected = "BMCRevertNotExistsPermission")]
fn add_route_permission() {
    let context = |v: AccountId| (get_context(vec![], false, v));
    testing_env!(context(alice()));
    let mut contract = BtpMessageCenter::new("0x1.near".into(), 1500);
    let link =
        BTPAddress::new("btp://0x1.icon/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string());
    let destination =
        BTPAddress::new("btp://0x1.bsc/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string());
    testing_env!(context(chuck()));
    contract.add_route(destination.clone(), link.clone());
}

#[test]
fn remove_route() {
    let context = |v: AccountId| (get_context(vec![], false, v));
    testing_env!(context(alice()));
    let mut contract = BtpMessageCenter::new("0x1.near".into(), 1500);
    let link =
        BTPAddress::new("btp://0x1.icon/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string());
    let destination_1 =
        BTPAddress::new("btp://0x1.bsc/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string());
    let destination_2 =
        BTPAddress::new("btp://0x1.pra/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string());
    contract.add_verifier(link.network_address().unwrap(), verifier());
    contract.add_link(link.clone());
    contract.add_route(destination_1.clone(), link.clone());
    contract.add_route(destination_2.clone(), link.clone());
    contract.remove_route(destination_1.clone());
    let result = contract.get_routes();

    assert_eq!(
        result,
        json!([{
            "dst": "btp://0x1.pra/cx87ed9048b594b95199f326fc76e76a9d33dd665b",
            "next": "btp://0x1.icon/cx87ed9048b594b95199f326fc76e76a9d33dd665b"
        }])
    )
}

#[test]
#[should_panic(expected = "BMCRevertNotExistsRoute")]
fn remove_route_non_existing_route() {
    let context = |v: AccountId| (get_context(vec![], false, v));
    testing_env!(context(alice()));
    let mut contract = BtpMessageCenter::new("0x1.near".into(), 1500);
    let link =
        BTPAddress::new("btp://0x1.icon/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string());
    let destination_1 =
        BTPAddress::new("btp://0x1.bsc/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string());
    let destination_2 =
        BTPAddress::new("btp://0x1.pra/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string());
    contract.add_verifier(link.network_address().unwrap(), verifier());
    contract.add_link(link.clone());
    contract.add_route(destination_1.clone(), link.clone());
    contract.remove_route(destination_2.clone());
}

#[test]
#[should_panic(expected = "BMCRevertNotExistsPermission")]
fn remove_route_permission() {
    let context = |v: AccountId| (get_context(vec![], false, v));
    testing_env!(context(alice()));
    let mut contract = BtpMessageCenter::new("0x1.near".into(), 1500);
    let link =
        BTPAddress::new("btp://0x1.icon/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string());
    let destination =
        BTPAddress::new("btp://0x1.bsc/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string());
    contract.add_verifier(link.network_address().unwrap(), verifier());
    contract.add_link(link.clone());
    contract.add_route(destination.clone(), link.clone());
    testing_env!(context(chuck()));
    contract.remove_route(destination.clone());
}

#[test]
fn get_routes() {
    let context = |v: AccountId| (get_context(vec![], false, v));
    testing_env!(context(alice()));
    let mut contract = BtpMessageCenter::new("0x1.near".into(), 1500);
    let link =
        BTPAddress::new("btp://0x1.icon/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string());
    let destination_1 =
        BTPAddress::new("btp://0x1.bsc/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string());
    let destination_2 =
        BTPAddress::new("btp://0x1.pra/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string());
    contract.add_verifier(link.network_address().unwrap(), verifier());
    contract.add_link(link.clone());
    contract.add_route(destination_1.clone(), link.clone());
    contract.add_route(destination_2.clone(), link.clone());

    let routes = contract.get_routes();
    let expected_routes = json!([{
        "dst": "btp://0x1.bsc/cx87ed9048b594b95199f326fc76e76a9d33dd665b",
        "next": "btp://0x1.icon/cx87ed9048b594b95199f326fc76e76a9d33dd665b"
    },{
        "dst": "btp://0x1.pra/cx87ed9048b594b95199f326fc76e76a9d33dd665b",
        "next": "btp://0x1.icon/cx87ed9048b594b95199f326fc76e76a9d33dd665b"
    }]);
    let result = routes
        .as_array()
        .unwrap()
        .to_owned()
        .into_iter()
        .collect::<HashedCollection<HashedValue>>();
    let expected = expected_routes
        .as_array()
        .unwrap()
        .to_owned()
        .into_iter()
        .collect::<HashedCollection<HashedValue>>();

    assert_eq!(result, expected);
}

#[test]
#[cfg(feature = "testable")]
fn resolve_route_link() {
    let context = |v: AccountId| (get_context(vec![], false, v));
    testing_env!(context(alice()));
    let mut contract = BtpMessageCenter::new("0x1.near".into(), 1500);
    let link =
        BTPAddress::new("btp://0x1.icon/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string());
    contract.add_verifier(link.network_address().unwrap(), verifier());
    contract.add_link(link.clone());

    let result = contract.resolve_route_pub(link.clone());
    assert_eq!(result, Some(link));
}

#[test]
#[cfg(feature = "testable")]
fn resolve_route_link_reachable() {
    let context = |v: AccountId| (get_context(vec![], false, v));
    testing_env!(context(alice()));
    let mut contract = BtpMessageCenter::new("0x1.near".into(), 1500);
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
    let result = contract.resolve_route_pub(BTPAddress::new(
        "btp://0x5.pra/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string(),
    ));
    assert_eq!(result, Some(link));
}

#[test]
#[cfg(feature = "testable")]
fn resolve_route_route() {
    let context = |v: AccountId| (get_context(vec![], false, v));
    testing_env!(context(alice()));
    let mut contract = BtpMessageCenter::new("0x1.near".into(), 1500);
    let link_1 =
        BTPAddress::new("btp://0x1.icon/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string());
    let destination_1 =
        BTPAddress::new("btp://0x1.bsc/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string());
    let link_2 =
        BTPAddress::new("btp://0x1.iconee/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string());
    let destination_2 =
        BTPAddress::new("btp://0x1.pra/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string());
    contract.add_verifier(link_1.network_address().unwrap(), verifier());
    contract.add_link(link_1.clone());
    contract.add_route(destination_1.clone(), link_1.clone());

    contract.add_verifier(link_2.network_address().unwrap(), verifier());
    contract.add_link(link_2.clone());
    contract.add_route(destination_2.clone(), link_2.clone());

    let result = contract.resolve_route_pub(destination_1.clone());
    assert_eq!(result, Some(link_1));

    let result = contract.resolve_route_pub(destination_2.clone());
    assert_eq!(result, Some(link_2));
}
