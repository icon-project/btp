use super::*;
use libraries::types::LinkStatus;
use libraries::types::{
    messages::{BtpMessage, TokenServiceMessage, TokenServiceType},
    Account, AccountBalance, AccumulatedAssetFees, Asset, BTPAddress, Math, MultiTokenCore, Token,
    WrappedI128, WrappedNativeCoin,
};
use serde_json::{from_value, json, Value};
use std::convert::{TryFrom, TryInto};
use std::str::FromStr;
use test_helper::{types::Context,actions::call};
use workspaces::{Contract as WorkspaceContract, Sandbox};

pub static WRAPPED_ICX_COIN_IS_REGESITERED_IN_NATIVE_COIN_BSH: fn(Context) -> Context =
    |mut context: Context| {
        context
            .pipe(NATIVE_COIN_BSH_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
            .pipe(NEP141_CONTRACT_IS_DEPLOYED)
            .pipe(REGISTER_WRAPPED_ICX_IN_NATIVE_COIN_BSH)
    };

pub static REGISTER_WRAPPED_ICX_IN_NATIVE_COIN_BSH: fn(Context) -> Context =
    |mut context: Context| {

        let account = context.contracts().get("nep141service").id().clone();
        
        context.add_method_params(
            "register",
            json!({
                "coin" : {
                    "metadata": {
                        "name": "WrappedICX",
                        "symbol": "nICX",
                        "uri":  account.to_string(),
                        "network": "0x1.icon",
                        "extras": {
                            "spec": "ft-1.0.0",
                            "icon" : null,
                            "reference": null,
                            "reference_hash": null,
                            "decimals": 24
                        },
                    }
                }
            }),
        );

        context
            .pipe(THE_TRANSACTION_IS_SIGNED_BY_NATIVE_COIN_BSH_OWNER)
            .pipe(USER_INVOKES_REGISTER_NEW_TOKEN_IN_NATIVE_COIN_BSH)
    };

pub static NATIVE_COIN_BSH_SERVICE_IS_ADDED_TO_BMC: fn(Context) -> Context =
    |mut context: Context| {
        context
            .pipe(NATIVE_COIN_BSH_NAME_AND_ACCOUNT_ID_ARE_PROVIDED_AS_ADD_SERVICE_PARAM)
            .pipe(ALICE_INVOKES_ADD_SERVICE_IN_BMC)
    };

pub static NATIVE_COIN_BSH_HANDLES_RECEIVED_SERVICE_MESSAGE: fn(Context) -> Context =
    |mut context: Context| {
        context
            .pipe(BSH_RECEIVES_BTP_MESSAGE_TO_MINT_AND_TRANSFER_WRAPPED_NATIVE_COIN)
            .pipe(ALICE_INVOKES_HANDLE_SERVICE_MESSAGE_IN_NATIVE_COIN_BSH)
    };

pub static BSH_RECEIVES_BTP_MESSAGE_TO_MINT_AND_TRANSFER_WRAPPED_NATIVE_COIN: fn(
    Context,
) -> Context = |mut context: Context| {
    let destination =
        BTPAddress::new("btp://0x1.icon/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string());
    let btp_message = &BtpMessage::new(
        BTPAddress::new("btp://0x1.icon/0x12345678".to_string()),
        BTPAddress::new("btp://1234.iconee/0x12345678".to_string()),
        "nativecoin".to_string(),
        WrappedI128::new(1),
        vec![],
        Some(TokenServiceMessage::new(
            TokenServiceType::RequestTokenTransfer {
                sender: destination.account_id().to_string(),
                receiver: context.accounts().get("charlie").id().to_string(),
                assets: vec![Asset::new("WrappedICX".to_string(), 900, 99)],
            },
        )),
    );

    let serialized_message = BtpMessage::try_from(btp_message).unwrap();

    context.add_method_params(
        "handle_btp_message",
        json!({
            "message": String::from(&serialized_message),
        }),
    );
    context
};

pub static ALICE_INVOKES_HANDLE_SERVICE_MESSAGE_IN_NATIVE_COIN_BSH: fn(Context) -> Context =
    |mut context: Context| {
        context
            .pipe(THE_TRANSACTION_IS_SIGNED_BY_ALICE)
            .pipe(USER_INVOKES_SEND_BTP_MESSAGE_FROM_NATIVE_COIN_BSH)
    };
pub static USER_INVOKES_GET_COIN_ID_FROM_NATIVE_COIN_BSH_FOR_WRAPPED_COIN: fn(Context) -> Context =
    |mut context: Context| {
        context.add_method_params(
            "coin_id",
            json!({
                "coin_name": "WrappedICX",
            }),
        );

        context.pipe(USER_INVOKES_GET_COIN_ID_IN_NATIVE_COIN_BSH)
    };

pub static USER_INVOKES_BALANCE_OF_TOKEN_BSH: fn(Context) -> Context = |mut context: Context| {
    context.add_method_params(
        "balance_of",
        json!({
            "owner_id": context.accounts().get("charlie").id().to_string() ,
            "coin_id": context.method_responses("coin_id") ,
        }),
    );
    context.pipe(USER_INVOKES_GET_BALANCE_IN_NATIVE_COIN_BSH)
};

pub static AMOUNT_SHOULD_BE_PRESENT_IN_TOKEN_BSH_ACCOUNT: fn(Context) = |context: Context| {
    let balance: String = from_value(context.method_responses("balance_of")).unwrap();
    assert_eq!(balance, "900");
};

pub static CHARLIE_WITHDRAWS_AMOUNT_FROM_NATIVE_COIN_BSH: fn(Context) -> Context =
    |mut context: Context| {
        let mut context =
            context.pipe(USER_INVOKES_GET_COIN_ID_FROM_NATIVE_COIN_BSH_FOR_WRAPPED_COIN);

        context.add_method_params(
            "withdraw",
            json!({
                "coin_id": context.method_responses("coin_id"),
                "amount": "400",
            }),
        );
        context
            .pipe(THE_TRANSACTION_IS_SIGNED_BY_CHARLIE)
            .pipe(USER_INVOKES_WITHDRAW_IN_NATIVE_COIN_BSH)
    };

pub static CHARLIE_DEPOSITS_TO_WRAPPED_FUNGIBLE_COIN: fn(Context) -> Context =
    |mut context: Context| {
        context.add_method_params(
            "ft_transfer_call",
            json!({
                "receiver_id":  context.contracts().get("nativecoin").id(),
                "amount": "300",
                "memo": null,
                "msg" : "transfer",
            }),
        );

        context
            .pipe(THE_TRANSACTION_IS_SIGNED_BY_CHARLIE)
            .pipe(USER_INVOKES_FT_TRANSFER_CALL_IN_NEP141)
    };

pub static AMOUNT_SHOULD_BE_PRESENT_IN_TOKEN_BSH_ACCOUNT_ON_DEPOSITING: fn(Context) =
    |context: Context| {
        //let error = context.method_errors("ft_transfer_call");
        //println!("{}",error);
        let balance: String = from_value(context.method_responses("balance_of")).unwrap();
        assert_eq!(balance, "800");
    };

pub static CHARLIES_ACCOUNT_IS_CREATED_AND_AMOUNT_DEPOSITED: fn(Context) -> Context =
    |mut context: Context| {
        context
            .pipe(CHARLIES_ACCOUNT_IS_CREATED)
            .pipe(THE_TRANSACTION_IS_SIGNED_BY_CHARLIE)
            .pipe(USER_INVOKES_DEPOSIT_IN_NATIVE_COIN_BSH)
    };

pub static CHARLIES_TRANSFERS_NATIVE_NEAR_COIN_TO_CROSS_CHAIN: fn(Context) -> Context =
    |mut context: Context| {
        let mut context =
            context.pipe(USER_INVOKES_GET_COIN_ID_FROM_NATIVE_COIN_BSH_FOR_NATIVE_COIN);

        context.add_method_params(
            "transfer",
            json!({
                "coin_id": context.method_responses("coin_id"),
                 "destination": BTPAddress::new(format!("btp://{}/{}", ICON_NETWORK, ICON_BMC)),
                 "amount": "200"
            }),
        );

        context
            .pipe(THE_TRANSACTION_IS_SIGNED_BY_CHARLIE)
            .pipe(USER_INVOKES_TRANSFER_IN_NATIVE_COIN_BSH)
    };

pub static TRANSFERED_AMOUNT_SHOULD_BE_DEDUCTED_FROM_ACCOUNT_ON_TRANSFERING_NATIVE_COIN: fn(
    Context,
) = |mut context: Context| {
    let context = context.pipe(CHARLIE_INVOKES_BALANCE_IN_NATIVE_COIN_BSH);

    let response: Value = from_value(context.method_responses("balance_of")).unwrap();

    assert_eq!(response, "800");

    // let error = context.method_responses("balance_of");
    // println!("{}",error);
};

pub static USER_INVOKES_GET_COIN_ID_FROM_NATIVE_COIN_BSH_FOR_NATIVE_COIN: fn(Context) -> Context =
    |mut context: Context| {
        context.add_method_params(
            "coin_id",
            json!({
                "coin_name": "NEAR",
            }),
        );

        context.pipe(USER_INVOKES_GET_COIN_ID_IN_NATIVE_COIN_BSH)
    };

pub static CHARLIE_INVOKES_BALANCE_IN_NATIVE_COIN_BSH: fn(Context) -> Context =
    |mut context: Context| {
        context.add_method_params(
            "balance_of",
            json!({
                "owner_id": context.accounts().get("charlie").id().to_string() ,
                "coin_id": context.method_responses("coin_id") ,
            }),
        );
        context.pipe(USER_INVOKES_GET_BALANCE_IN_NATIVE_COIN_BSH)
    };

pub static CHARLIE_TRANSFER_WRAPPED_NATIVE_COIN_TO_CROSS_CHAIN: fn(Context) -> Context =
    |mut context: Context| {
        let mut context =
            context.pipe(USER_INVOKES_GET_COIN_ID_FROM_NATIVE_COIN_BSH_FOR_WRAPPED_COIN);

        context.add_method_params(
            "transfer",
            json!({
                "coin_id": context.method_responses("coin_id"),
                 "destination": BTPAddress::new(format!("btp://{}/{}", ICON_NETWORK, ICON_BMC)),
                 "amount": "500"
            }),
        );

        context
            .pipe(THE_TRANSACTION_IS_SIGNED_BY_CHARLIE)
            .pipe(USER_INVOKES_TRANSFER_IN_NATIVE_COIN_BSH)
    };

pub static TRANSFERED_AMOUNT_SHOULD_BE_DEDUCTED_FROM_WRAPPED_TOKEN_BALANCE: fn(Context) =
    |mut context: Context| {
        let context = context.pipe(CHARLIE_INVOKES_BALANCE_IN_NATIVE_COIN_BSH);

        let response: Value = from_value(context.method_responses("balance_of")).unwrap();

        assert_eq!("300", response);
    };

pub static BSH_RECEIVES_RESPONSE_HANDLE_BTP_MESSAGE_TO_NATIVE_COIN: fn(Context) -> Context =
    |mut context: Context| {
        let destination = BTPAddress::new(
            "btp://0x1.icon/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string(),
        );
        let btp_message = &BtpMessage::new(
            BTPAddress::new("btp://0x1.icon/0x12345678".to_string()),
            BTPAddress::new("btp://1234.iconee/0x12345678".to_string()),
            "nativecoin".to_string(),
            WrappedI128::new(1),
            vec![],
            Some(TokenServiceMessage::new(
                TokenServiceType::ResponseHandleService {
                    code: 0,
                    message: "Transfer Success".to_string(),
                },
            )),
        );

        let serialized_message = BtpMessage::try_from(btp_message).unwrap();

        context.add_method_params(
            "handle_btp_message",
            json!({
                "message": String::from(&serialized_message),
            }),
        );
        context
    };

pub static CHARLIE_INVOKES_BALANCE_FROM_NATIVE_COIN_BSH: fn(Context) -> Context =
    |mut context: Context| {
        context
            .pipe(USER_INVOKES_GET_COIN_ID_FROM_NATIVE_COIN_BSH_FOR_WRAPPED_COIN)
            .pipe(CHARLIE_INVOKES_BALANCE_IN_NATIVE_COIN_BSH)
    };

pub static BALANCE_SHOULD_BE_UNLOCKED_AFTER_GETTING_SUCCESS_RESPONSE: fn(Context) =
    |mut context: Context| {
        let balance: Value = from_value(context.method_responses("balance_of")).unwrap();
        assert_eq!(balance, "900");
    };

pub static CHARLIES_TRANSFERS_0_NATIVE_NEAR_COIN_TO_CROSS_CHAIN: fn(Context) -> Context =
    |mut context: Context| {
        let mut context =
            context.pipe(USER_INVOKES_GET_COIN_ID_FROM_NATIVE_COIN_BSH_FOR_NATIVE_COIN);

        context.add_method_params(
            "transfer",
            json!({
                "coin_id": context.method_responses("coin_id"),
                 "destination": BTPAddress::new("btp://0x1.icon/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string()),
                 "amount": "0"
            }),
        );

        context
            .pipe(THE_TRANSACTION_IS_SIGNED_BY_CHARLIE)
            .pipe(USER_INVOKES_TRANSFER_IN_NATIVE_COIN_BSH)
    };

pub static NATIVE_COIN_BSH_SHOULD_THROW_USER_CANNOT_TRANSFER_0_COIN_ERROR_ON_TRANSFERRING_COIN: fn(Context) = |context: Context| {
        let error =context.method_errors("transfer");
        assert!(error.to_string().contains("BSHRevertNotMinimumAmount"));
    };