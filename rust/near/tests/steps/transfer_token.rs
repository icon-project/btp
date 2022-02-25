use super::*;
use libraries::types::LinkStatus;
use libraries::types::{
    messages::{BtpMessage, TokenServiceMessage, TokenServiceType},
    Account, AccountBalance, AccumulatedAssetFees, Asset, BTPAddress, Math, TransferableAsset,
    WrappedI128, WrappedNativeCoin,
};
use serde_json::{from_value, json, Value};
use std::convert::TryFrom;
use test_helper::types::Context;

pub static TOKEN_BSH_NAME_AND_ACCOUNT_ID_ARE_PROVIDED_AS_ADD_SERVICE_PARAM: fn(Context) -> Context =
    |mut context: Context| {
        context.add_method_params(
            "add_service",
            json!({
                "name":format!("token"),
                "service": context.contracts().get("tokenbsh").id()
            }),
        );
        context
    };

pub static BSH_RECEIVES_BTP_MESSAGE_TO_MINT_AND_TRANSFER_WRAPPED_TOKEN: fn(Context) -> Context =
    |mut context: Context| {
        let destination = BTPAddress::new(
            "btp://0x1.icon/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string(),
        );
        let btp_message = &BtpMessage::new(
            BTPAddress::new("btp://0x1.icon/0x12345678".to_string()),
            BTPAddress::new("btp://1234.iconee/0x12345678".to_string()),
            "token".to_string(),
            WrappedI128::new(1),
            vec![],
            Some(TokenServiceMessage::new(
                TokenServiceType::RequestTokenTransfer {
                    sender: destination.account_id().to_string(),
                    receiver: context.accounts().get("charlie").id().to_string(),
                    assets: vec![TransferableAsset::new("BALN".to_string(), 900, 99)],
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

pub static ALICE_INVOKES_HANDLE_SERVICE_MESSAGE_IN_TOKEN_BSH: fn(Context) -> Context =
    |mut context: Context| {
        context
            .pipe(THE_TRANSACTION_IS_SIGNED_BY_ALICE)
            .pipe(USER_INVOKES_SEND_BTP_MESSAGE_FROM_TOKEN_BSH)
    };

pub static CHARLIE_WITHDRAWS_AMOUNT_TOKEN_BSH: fn(Context) -> Context = |mut context: Context| {
    let mut context = context.pipe(USER_INVOKES_GET_TOKEN_ID_FROM_TOKEN_BSH_CONTRACT);
    context.add_method_params(
        "withdraw",
        json!({
            "token_id": context.method_responses("token_id"),
            "amount": "400",
        }),
    );
    context
        .pipe(THE_TRANSACTION_IS_SIGNED_BY_CHARLIE)
        .pipe(USER_INVOKES_WITHDRAW_IN_TOKEN_BSH)
};

pub static USER_INVOKES_GET_TOKEN_ID_FROM_TOKEN_BSH_CONTRACT: fn(Context) -> Context =
    |mut context: Context| {
        context.add_method_params(
            "token_id",
            json!({
                "token_name": "BALN",
            }),
        );

        context.pipe(USER_INVOKES_GET_COIN_ID_IN_TOKEN_BSH)
    };

pub static CHARLIE_INVOKES_BALANCE_OF_IN_TOKEN_BSH: fn(Context) -> Context =
    |mut context: Context| {
        context.add_method_params(
            "balance_of",
            json!({
                "owner_id": context.accounts().get("charlie").id().to_string(),
                "token_id": context.method_responses("token_id") ,
            }),
        );
        context.pipe(USER_INVOKES_GET_BALANCE_IN_TOKEN_BSH)
    };

pub static CHARLIE_DEPOSITS_TO_WRAPPED_FUNGIBLE_TOKEN: fn(Context) -> Context =
    |mut context: Context| {
        context.add_method_params(
            "ft_transfer_call",
            json!({
                "receiver_id":  context.contracts().get("tokenbsh").id(),
                "amount": "200",
                "memo": null,
                "msg" : "transfer",
            }),
        );

        context
            .pipe(THE_TRANSACTION_IS_SIGNED_BY_CHARLIE)
            .pipe(USER_INVOKES_FT_TRANSFER_CALL_IN_NEP141)
    };

pub static AFTER_WITHDRAW_AMOUNT_SHOULD_BE_PRESENT_IN_TOKEN_BSH_ACCOUNT: fn(Context) =
    |context: Context| {
        let balance = context.method_responses("balance_of");
        assert_eq!(balance, "500");
    };

pub static BOB_INVOKES_FT_TRANSFER_BALCE_OF_CHARLIE: fn(Context) -> Context =
    |mut context: Context| {
        context.add_method_params(
            "ft_balance_of",
            json!({
                "account_id":  context.contracts().get("tokenbsh").id(),
            }),
        );

        context
            .pipe(THE_TRANSACTION_IS_SIGNED_BY_CHARLIE)
            .pipe(USER_INVOKES_FT_BALANCE_OF_CALL_IN_NEP141)
    };

pub static AFTER_DEPOSIT_AMOUNT_SHOULD_BE_PRESENT_IN_CHARLIES_ACCOUNT: fn(Context) =
    |context: Context| {
        let balance = context.method_responses("balance_of");
        assert_eq!(balance, "700");
    };

pub static BALN_TOKEN_IS_REGISTERED_IN_TOKEN_BSH: fn(Context) -> Context = |context: Context| {
    context
        .pipe(TOKEN_BSH_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
        .pipe(TOKEN_BSH_CONTRACT_IS_OWNED_BY_BOB)
        .pipe(NEP141_CONTRACT_IS_DEPLOYED)
        .pipe(REGISTER_BALN_TOKEN_IN_TOKEN_BSH)
};

pub static TOKEN_BSH_SERVICE_IS_ADDED_TO_BMC: fn(Context) -> Context = |context: Context| {
    context
        .pipe(TOKEN_BSH_NAME_AND_ACCOUNT_ID_ARE_PROVIDED_AS_ADD_SERVICE_PARAM)
        .pipe(ALICE_INVOKES_ADD_SERVICE_IN_BMC)
};

pub static TOKEN_BSH_HANDLES_RECIEVED_SERVICE_MESSAGE: fn(Context) -> Context =
    |context: Context| {
        context
            .pipe(BSH_RECEIVES_BTP_MESSAGE_TO_MINT_AND_TRANSFER_WRAPPED_TOKEN)
            .pipe(ALICE_INVOKES_HANDLE_SERVICE_MESSAGE_IN_TOKEN_BSH)
    };

pub static BSH_RECEIVES_RESPONSE_HANDLE_BTP_MESSAGE_TO_TOKEN_BSH: fn(Context) -> Context =
    |mut context: Context| {
        let destination = BTPAddress::new(
            "btp://0x1.icon/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string(),
        );
        let btp_message = &BtpMessage::new(
            BTPAddress::new("btp://0x1.icon/0x12345678".to_string()),
            BTPAddress::new("btp://1234.iconee/0x12345678".to_string()),
            "token".to_string(),
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

pub static BALNCE_SHOULD_BE_PRESENT_IN_CHARLIES_ACCOUNT_AFTER_GETTING_SUCCESS_RESPONSE: fn(
    Context,
) = |context: Context| {
    let balance: Value = from_value(context.method_responses("balance_of")).unwrap();
    assert_eq!(balance, "900");
};

pub static WNEAR_TOKEN_IS_REGISTERED_IN_TOKEN_BSH: fn(Context) -> Context = |context: Context| {
    context
        .pipe(TOKEN_BSH_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
        .pipe(TOKEN_BSH_CONTRACT_IS_OWNED_BY_BOB)
        .pipe(WNEAR_CONTRACT_IS_DEPLOYED_AND_INITIALZIED)
        .pipe(REGISTER_WNEAR_TOKEN_IN_TOKEN_BSH)
};

pub static REGISTER_WNEAR_TOKEN_IN_TOKEN_BSH: fn(Context) -> Context = |mut context: Context| {
    context.add_method_params(
        "register",
        json!({
            "token" : {
                "metadata": {
                    "name": "WNear",
                    "symbol": "wNear",
                    "uri":  context.contracts().get("wnearcontract").id(),
                    "network": "0x1.near",
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
        .pipe(THE_TRANSACTION_IS_SIGNED_BY_TOKEN_BSH_OWNER)
        .pipe(USER_INVOKES_REGISTER_NEW_TOKEN_IN_TOKEN_BSH)
};

pub static USER_INVOKES_GET_TOKEN_ID_FOR_BALN_FROM_TOKEN_BSH: fn(Context) -> Context =
    |mut context: Context| {
        context.add_method_params(
            "token_id",
            json!({
                "token_name": "BALN",
            }),
        );

        context.pipe(USER_INVOKES_GET_COIN_ID_IN_TOKEN_BSH)
    };

pub static BALN_TOKEN_ID_SHOULD_BE_PRESENT_IN_TOKEN_TOKEN_BSH: fn(Context) = |context: Context| {
    let token_id = context.method_responses("token_id");
    let expected = json!([
        88, 203, 62, 48, 96, 56, 91, 171, 129, 22, 47, 67, 162, 209, 102, 192, 143, 53, 200, 229,
        61, 180, 216, 96, 174, 14, 68, 45, 220, 171, 167, 222
    ]);
    assert_eq!(token_id, expected);
};

pub static REGISTER_BALN_TOKEN_IN_TOKEN_BSH: fn(Context) -> Context = |mut context: Context| {
    context.add_method_params(
        "register",
        json!({
            "token" : {
                "metadata": {
                    "name": "BALN",
                    "symbol": "baln",
                    "uri":  context.contracts().get("nep141service").id(),
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
        .pipe(THE_TRANSACTION_IS_SIGNED_BY_TOKEN_BSH_OWNER)
        .pipe(USER_INVOKES_REGISTER_NEW_TOKEN_IN_TOKEN_BSH)
};

pub static USER_INVOKES_GET_TOKEN_ID_FOR_WNEAR_FROM_TOKEN_BSH: fn(Context) -> Context =
    |mut context: Context| {
        context.add_method_params(
            "token_id",
            json!({
                "token_name": "WNear",
            }),
        );

        context.pipe(USER_INVOKES_GET_COIN_ID_IN_TOKEN_BSH)
    };

pub static CHARLIE_INVOKES_FT_TRANSFER_BALCE_OF: fn(Context) -> Context = |mut context: Context| {
    context.add_method_params(
        "ft_balance_of",
        json!({
            "account_id":  context.accounts().get("charlie").id(),
        }),
    );

    context
        .pipe(THE_TRANSACTION_IS_SIGNED_BY_CHARLIE)
        .pipe(USER_INVOKES_FT_BALANCE_OF_CALL_IN_WNEAR)
};

pub static BALANCE_SHOULD_BE_PRESENT_IN_CHARLIES_ACCOUNT_AFTER_DEPOSIT: fn(Context) =
    |context: Context| {
        let balance: Value = from_value(context.method_responses("balance_of")).unwrap();
        assert_eq!(balance, "500");
    };

pub static CHARLIE_DEPOSITS_WNEAR_TO_CHARLIES_TOKEN_BSH_ACCOUNT: fn(Context) -> Context =
    |mut context: Context| {
        context.add_method_params(
            "ft_transfer_call",
            json!({
                "receiver_id":  context.contracts().get("tokenbsh").id(),
                "amount": "500",
                "memo": null,
                "msg" : "transfer",
            }),
        );

        context
            .pipe(THE_TRANSACTION_IS_SIGNED_BY_CHARLIE)
            .pipe(USER_INVOKES_FT_TRANSFER_CALL_IN_WNEAR)
    };

pub static USER_INVOKES_GET_NEAR_TOKEN_ID_FROM_TOKEN_BSH_CONTRACT: fn(Context) -> Context =
    |mut context: Context| {
        context.add_method_params(
            "token_id",
            json!({
                "token_name": "WNear",
            }),
        );

        context.pipe(USER_INVOKES_GET_COIN_ID_IN_TOKEN_BSH)
    };

pub static CHARLIE_TRANSFERS_0_NATIVE_NEAR_TOKENS_TO_CROSS_CHAIN: fn(Context) -> Context =
    |mut context: Context| {
        let mut context = context.pipe(USER_INVOKES_GET_NEAR_TOKEN_ID_FROM_TOKEN_BSH_CONTRACT);

        context.add_method_params(
            "transfer",
            json!({
                "token_id": context.method_responses("token_id"),
                 "destination": BTPAddress::new("btp://0x1.icon/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string()),
                 "amount": "0"
            }),
        );

        context
            .pipe(THE_TRANSACTION_IS_SIGNED_BY_CHARLIE)
            .pipe(USER_INVOKES_TRANSFER_IN_TOKEN_BSH)
    };

pub static CHARLIE_INVOKES_BALN_TOKEN_BALANCE_IN_TOKEN_BSH: fn(Context) -> Context =
    |mut context: Context| {
        context
            .pipe(USER_INVOKES_GET_TOKEN_ID_FROM_TOKEN_BSH_CONTRACT)
            .pipe(CHARLIE_INVOKES_BALANCE_OF_IN_TOKEN_BSH)
    };

pub static CHARLIE_INVOKES_WNEAR_TOKEN_BALANCE_IN_TOKEN_BSH: fn(Context) -> Context =
    |mut context: Context| {
        context
            .pipe(USER_INVOKES_GET_TOKEN_ID_FOR_WNEAR_FROM_TOKEN_BSH)
            .pipe(CHARLIE_INVOKES_BALANCE_OF_IN_TOKEN_BSH)
    };

pub static BALN_TOKEN_METADATA_METADATA_IS_PROVIDED_AS_REGISTER_TOKEN_PARAM_IN_TOKEN_BSH:
    fn(Context) -> Context = |mut context: Context| {
    context.add_method_params(
        "register",
        json!({
            "token" : {
                "metadata": {
                    "name": "BALN",
                    "symbol": "baln",
                    "uri":  context.contracts().get("nep141service").id(),
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
};
