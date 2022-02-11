use bmc::BtpMessageCenter;
use near_sdk::{
    json_types::Base64VecU8,
    serde::Deserialize,
    serde_json::{self, from_value, json},
    testing_env, AccountId, VMContext,
};
use std::{collections::HashSet, convert::TryFrom};
pub mod accounts;
use accounts::*;
use libraries::types::{
    messages::BmcServiceMessage, messages::BmcServiceType, messages::BtpMessage,
    messages::ErrorMessage, messages::SerializedBtpMessages, messages::SerializedMessage,
    messages::TokenServiceMessage, messages::TokenServiceType, Account, Address, BTPAddress,
    HashedCollection, WrappedI128,
};
use std::convert::TryInto;

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
fn decode() {
    let message: SerializedMessage = serde_json::from_str("\"-Qee-QQ8uQFS-QFPuNT40gKCQKOHBdCqJx0eQ5UAtrV5G-C172cGOzwQuED7gVFNsv2g1tINkHhbtViQXPruQ-FnkmYs_jA0tYrJKbvq3BYO9Rqgt12iGPeeEGlPxvT_hNLnhr9K5nUqvKiB6A_Yc0bfl76g7Z5kTlmy_2VEb189fXfCeFj6z4rrO5aUcNdJnHn5dXz4APgAgLhG-ESgMpG4jtB7-p0Ls3k2EYmLp9RerPVrvKAFveS23xQl2UT4AKBhUsXohogqHXqYEfjsfRAHCiSbbzQfM_H7NFRDIoxzKLh1-HMA4gGgtVIyurXEb0cIjT3frYFFTMfDQ48iwgntObJ1ht6v-RL4TfhLhwXQqicsVs-4QfNbBjk7e16wLiu54BjoVhrFrH41obLbpuP8tVCxRbVLHYoBQyGbXYOYQduPi9KL_jc9iJ284R8Z5uKMJYJvntwB-AC5AVD5AU240vjQAoJApIcF0KonLFbPlQC2tXkb4LXvZwY7PBC4QPuBUU2y_aB_WlGDMMOOnc25Zw71Asy9qm5P1ckuxut4n7eyRa8sX6BkLQ2BfmajCXpSPKQyPbQfknSBaVE5Jh3D-Wyd1gODu6DtnmROWbL_ZURvXz19d8J4WPrPius7lpRw10mcefl1fPgAoNBv8fj4bmAFfa9a8IyYHOK_pHPZt7lJi8AF0Cgkad7_gKbloDKRuI7Qe_qdC7N5NhGJi6fUXqz1a7ygBb3ktt8UJdlE-AD4ALh1-HMA4gGgqw1QRCuG0tcqzCpKDqhHQO09T3_OQMhOHAAgt-D4O-_4TfhLhwXQqic7qGy4QZPydQXTosV0Bw1Q3ntf7-FCxRDMSM7FOnPhP591OxiYUlsWMdK0qfbp-WrnfBJKObfEwNm1dqfX98D9xKLtCVsA-AC5AZH5AY65ARL5AQ8CgkClhwXQqic7qGyVALa1eRvgte9nBjs8ELhA-4FRTbL9oItWxVWVSnoGHZaeON6tuc1mKVrq1Wm18UkpjaplJALMoBbHRfvDVLJUkLGrwX2BHDYbXjXpRT-EcCLGG68iaSdKoO2eZE5Zsv9lRG9fPX13wnhY-s-K6zuWlHDXSZx5-XV8-AD4ALg8CAAgcEAMEg8IhMDEBAhUDg0OhQEgYIiMEEEWjMIisaAENjsEAUIjEXghCkEZQEbjMQhEChETlEglUKgIuEb4RKC_bo8qyH3l705OLCmJ1kMQd9iEV0aw7gStxMPmCTOAffgAoL2i-AmVd_b_p3gkSP5glYl9pIn40AEUMIvqwpHIvkahuHX4cwDiAaCp1buI8W7RsDhZAp69_T6gJBjO_aR00EmjIj3-397EXfhN-EuHBdCqJ0rbgLhB459yJ9MK7TDZ2xQ23jTHbdbiwOCTEcmb5U7olxiUgaFV28sh8nuVzXrjJXuGfUnNureSKSOSXSwvp-aQTixLNAH4APgA-QNauQNX-QNUALkBVPkBUbkBTvkBS4IgALkBRfkBQgCVAZQ5KOt2a33MGDNmJIyCIn-oz8dWgwdSZIMHUmSFAukO3QC49hAAAAAAAAABAAAAAAAAAAAAAAAAAAAAAAAAAAAgQAAAAAAAAAAAAAEAAAAAAAAAAAAAAAAAAAAABAAACAAAAAAAAAAAAAAAACAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAgAAAAAAAAAAAAAAAAAAEAAAAAAAAAAAAAAAAAAAAAAAAIAAAAAACAAAAAAIAAAAEIAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAgAAAAAAACAAAAAAAAAAAAAAAAQAAAAAAAAAQAAAAAAAABAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAIAAAAAAAAAAAPgA-ACgvEWAgjiMdu0pUAxvQcurR0OVNFEmP5ZzhNMkpIVwqPr5Afn5AfYAuQHy-QHvo-IQoMOetu_ZC-YK5vgAe7UsNeBouviaGeP9VQgl1JDY7dRguFP4UaBjTIu0WBCx1UEf8MBqzibUyAkFNsmaQ3gnewIx7LIl96CegRcYwno7-6NZanIbuiC4o_93OVbLE-UByERTjAHFv4CAgICAgICAgICAgICAgLkBc_kBcCC5AWz5AWmVAZwHK-9UWr65aj97ov_fi4YftV3q-FSWTWVzc2FnZShzdHIsaW50LGJ5dGVzKbg6YnRwOi8vMHg1MDEucHJhLzB4NUNDMzA3MjY4YTEzOTNBQjlBNzY0QTIwREFDRTg0OEFCODI3NWM0NgL4-7j5-Pe4PmJ0cDovLzB4NThlYjFjLmljb24vY3g5YzA3MmJlZjU0NWFiZWI5NmEzZjdiYTJmZmRmOGI4NjFmYjU1ZGVhuDpidHA6Ly8weDUwMS5wcmEvMHg1Q0MzMDcyNjhhMTM5M0FCOUE3NjRBMjBEQUNFODQ4QUI4Mjc1YzQ2im5hdGl2ZWNvaW4BuG34awC4aPhmqmh4NDUwMmFhZDc5ODZhZDVhODQ4OTU1MTVmYWY3NmU5MGI1YjQ3ODY1NKoweDE1OEEzOTFGMzUwMEMzMjg4QWIyODY1MzcyMmE2NDU5RTc3MjZCMDHPzoNJQ1iJAIlj3YwsXgAA\"").unwrap();
}

#[test]
fn handle_serialized_btp_messages_service_message() {
    let context = |v: AccountId| (get_context(vec![], false, v));
    testing_env!(context(alice()));
    let mut contract = BtpMessageCenter::new("0x1.near".into(), 1500);
}

#[test]
#[cfg(feature = "testable")]
fn handle_internal_service_message_init() {
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
        BTPAddress::new(
            "btp://0x1.near/88bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4"
                .to_string(),
        ),
        "bmc".to_string(),
        WrappedI128::new(1),
        <Vec<u8>>::from(bmc_service_message.clone()),
        None,
    );

    contract.handle_btp_messages(link.clone(), vec![btp_message]);
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
fn handle_internal_service_message_link() {
    let context = |v: AccountId| (get_context(vec![], false, v));
    testing_env!(context(alice()));
    let mut contract = BtpMessageCenter::new("0x1.near".into(), 1500);
    let context = |v: AccountId| (get_context(vec![], false, v));
    testing_env!(context(alice()));
    let mut contract = BtpMessageCenter::new("0x1.near".into(), 1500);
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
        BTPAddress::new(
            "btp://0x1.near/88bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4"
                .to_string(),
        ),
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
        BTPAddress::new(
            "btp://0x1.near/88bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4"
                .to_string(),
        ),
        "bmc".to_string(),
        WrappedI128::new(1),
        <Vec<u8>>::from(bmc_service_message_2.clone()),
        None,
    );

    contract.handle_btp_messages(link.clone(), vec![btp_message_1, btp_message_2]);
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
fn handle_internal_service_message_unlink() {
    let context = |v: AccountId| (get_context(vec![], false, v));
    testing_env!(context(alice()));
    let mut contract = BtpMessageCenter::new("0x1.near".into(), 1500);
    let context = |v: AccountId| (get_context(vec![], false, v));
    testing_env!(context(alice()));
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
        BTPAddress::new(
            "btp://0x1.near/88bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4"
                .to_string(),
        ),
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
        BTPAddress::new(
            "btp://0x1.near/88bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4"
                .to_string(),
        ),
        "bmc".to_string(),
        WrappedI128::new(1),
        <Vec<u8>>::from(bmc_service_message_2.clone()),
        None,
    );

    contract.handle_btp_messages(link.clone(), vec![btp_message_1, btp_message_2]);
    let reachables = contract.get_reachable_link(link.clone());
    let mut expected = HashedCollection::new();
    expected.add(BTPAddress::new(
        "btp://0x1.pra/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string(),
    ));
    assert_eq!(reachables, expected);
}

#[test]
fn deserialize_serialized_btp_messages_from_json() {
    let btp_message = json!(["-QEUuDlidHA6Ly8weDEuaWNvbi9jeDg3ZWQ5MDQ4YjU5NGI5NTE5OWYzMjZmYzc2ZTc2YTlkMzNkZDY2NWK4TmJ0cDovLzB4NS5wcmEvODhiZDA1NDQyNjg2YmUwYTVkZjdkYTMzYjZmMTA4OWViZmVhMzc2OWIxOWRiYjI0NzdmZTBjZDZlMGYxMjZlNINibWOBgLiB-H-ESW5pdLh4-Hb4dLg4YnRwOi8vMHgxLnByYS9jeDg3ZWQ5MDQ4YjU5NGI5NTE5OWYzMjZmYzc2ZTc2YTlkMzNkZDY2NWK4OGJ0cDovLzB4NS5wcmEvY3g4N2VkOTA0OGI1OTRiOTUxOTlmMzI2ZmM3NmU3NmE5ZDMzZGQ2NjVi"]);
    let serialized_btp_messages: SerializedBtpMessages = from_value(btp_message).unwrap();
    // TODO: Add;
}

#[test]
#[cfg(feature = "testable")]
fn handle_route_message() {
    let btp_message = json!(["-QETuDlidHA6Ly8weDEuaWNvbi9jeDg3ZWQ5MDQ4YjU5NGI5NTE5OWYzMjZmYzc2ZTc2YTlkMzNkZDY2NWK4TmJ0cDovLzB4MS5wcmEvODhiZDA1NDQyNjg2YmUwYTVkZjdkYTMzYjZmMTA4OWViZmVhMzc2OWIxOWRiYjI0NzdmZTBjZDZlMGYxMjZlNINibWMKuIH4f4RJbml0uHj4dvh0uDhidHA6Ly8weDEucHJhL2N4ODdlZDkwNDhiNTk0Yjk1MTk5ZjMyNmZjNzZlNzZhOWQzM2RkNjY1Yrg4YnRwOi8vMHg1LnByYS9jeDg3ZWQ5MDQ4YjU5NGI5NTE5OWYzMjZmYzc2ZTc2YTlkMzNkZDY2NWI"]);
    let serialized_btp_messages: SerializedBtpMessages = from_value(btp_message).unwrap();
    let context = |v: AccountId| (get_context(vec![], false, v));
    testing_env!(context(alice()));
    let mut contract = BtpMessageCenter::new("0x1.near".into(), 1500);
    let link =
        BTPAddress::new("btp://0x2.icon/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string());
    contract.add_verifier(link.network_address().unwrap(), verifier());
    contract.add_link(link.clone());
    contract.handle_btp_messages(link.clone(), serialized_btp_messages);
    let btp_message: BtpMessage<ErrorMessage> = contract.get_message().unwrap().try_into().unwrap();
    let error_message = ErrorMessage::new(21, "BMCRevertUnreachable at btp://0x1.pra/88bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4".to_string());
    assert_eq!(
        btp_message,
        BtpMessage::new(
            BTPAddress::new(
                "btp://0x1.near/88bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4"
                    .to_string()
            ),
            BTPAddress::new(
                "btp://0x1.icon/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string()
            ),
            "bmc".to_string(),
            WrappedI128::new(-10),
            error_message.clone().into(),
            Some(error_message)
        )
    );
}

#[test]
#[cfg(feature = "testable")]
fn handle_external_service_message_existing_service() {
    let context = |v: AccountId| (get_context(vec![], false, v));
    let link =
        BTPAddress::new("btp://0x1.icon/cx87ed9048b594b95199f326fc76e76a9d33dd675b".to_string());
    let destination =
        BTPAddress::new("btp://0x1.near/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string());
    let btp_message = BtpMessage::new(
        link.clone(),
        destination.clone(),
        "nativecoin".to_string(),
        WrappedI128::new(1),
        vec![],
        Some(TokenServiceMessage::new(
            TokenServiceType::RequestTokenTransfer {
                sender: chuck().to_string(),
                receiver: destination.account_id().to_string(),
                assets: vec![],
            },
        )),
    );
    testing_env!(context(alice()));
    let mut contract = BtpMessageCenter::new("0x1.near".into(), 1500);
    contract.add_verifier(link.network_address().unwrap(), verifier());
    contract.add_link(link.clone());

    contract.add_service(
        "nativecoin".to_string(),
        "nativecoin.near".parse::<AccountId>().unwrap(),
    );

    contract.handle_service_message_testable(link.clone(), <BtpMessage<SerializedMessage>>::try_from(&btp_message).unwrap());
}

#[test]
#[cfg(feature = "testable")]
#[should_panic(expected = "BMCRevertNotExistBSH")]
fn handle_external_service_message_non_existing_service() {
    let context = |v: AccountId| (get_context(vec![], false, v));
    let link =
        BTPAddress::new("btp://0x1.icon/cx87ed9048b594b95199f326fc76e76a9d33dd675b".to_string());
    let destination =
        BTPAddress::new("btp://0x1.near/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string());
    let btp_message = BtpMessage::new(
        link.clone(),
        destination.clone(),
        "nativecoin".to_string(),
        WrappedI128::new(1),
        vec![],
        Some(TokenServiceMessage::new(
            TokenServiceType::RequestTokenTransfer {
                sender: chuck().to_string(),
                receiver: destination.account_id().to_string(),
                assets: vec![],
            },
        )),
    );
    testing_env!(context(alice()));
    let mut contract = BtpMessageCenter::new("0x1.near".into(), 1500);
    contract.add_verifier(link.network_address().unwrap(), verifier());
    contract.add_link(link.clone());
    contract.handle_service_message_testable(link.clone(), <BtpMessage<SerializedMessage>>::try_from(&btp_message).unwrap());
}