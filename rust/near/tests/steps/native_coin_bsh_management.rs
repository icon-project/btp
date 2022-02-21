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

pub static COIN_REGISTERED_SHOULD_BE_PRESENT: fn(Context) = |context: Context| {
    let context = context.pipe(USER_INVOKES_GET_COINS_IN_NATIVE_COIN_BSH);
    let coins = context.method_responses("coins");

    let expected = json!([
        {"name": "NEAR", "network": "0x1.near", "symbol": "NEAR"},
        {"name": "WrappedICX", "network": "0x1.icon", "symbol": "nICX"}
    ]);

    assert_eq!(coins, expected);
};

pub static NEW_WRAPPED_COIN_IS_REGISTERED_IN_NATIVE_COIN_BSH: fn(Context) -> Context =
    |mut context: Context| {
        (context)
            .pipe(NEW_COIN_NAME_IS_PROVIDED_AS_REGISTER_WARPPED_COIN_PARAM)
            .pipe(THE_TRANSACTION_IS_SIGNED_BY_BOB)
            .pipe(USER_INVOKES_REGISTER_NEW_COIN_IN_NATIVE_COIN_BSH)
    };

pub static NEW_COIN_NAME_IS_PROVIDED_AS_REGISTER_WARPPED_COIN_PARAM: fn(Context) -> Context =
    |mut context: Context| {
        context.add_method_params(
            "register",
            json!({
                "coin": {
                    "metadata": {
                        "name": "WrappedICX",
                        "symbol": "wicx",
                        "uri": null,
                        "network": "0x1.icon",
                        "extras" : null
                    }
                },
            }),
        );
        context
    };

pub static REGSITERED_COIN_IDS_ARE_QUERIED: fn(Context) = |context: Context| {
    let coin_id = context.method_responses("coin_id");

    let expected = json!([
        169, 251, 132, 88, 218, 49, 26, 238, 101, 25, 214, 174, 76, 238, 27, 82, 101, 204, 224,
        247, 162, 64, 40, 41, 146, 253, 223, 28, 217, 19, 87, 150
    ]);

    assert_eq!(coin_id, expected);
};

pub static COIN_NAME_IS_PROVIDED_AS_GET_COIN_ID_PARAM: fn(Context) -> Context =
    |mut context: Context| {
        context.add_method_params("coin_id", json!({ "coin_name":"NEAR" }));

        context
    };

pub static UNREGISTERED_COIN_NAME_IS_PROVIDED_AS_GET_COIN_ID_PARAM: fn(Context) -> Context =
    |mut context: Context| {
        context.add_method_params("coin_id", json!({ "coin_name":"BTC" }));

        context
    };

pub static NATIVE_COIN_BSH_SHOULD_THROW_INVALID_COIN_ERROR_ON_GETTING_COIN_ID: fn(Context) =
    |context: Context| {
        let error = context.method_errors("coin_id");

        assert!(error.to_string().contains("BSHRevertNotExistsToken:"));
    };

pub static NATIVE_COIN_BSH_SHOULD_THROW_UNAUTHORIZED_ERROR_ON_REGSITERING_NEW_COIN: fn(Context) =
    |context: Context| {
        let error = context.method_errors("register");

        assert!(error.contains("BSHRevertNotExistsPermission"));
    };

pub static CHARLIE_INVOKES_REGISTER_NEW_WRAPPED_COIN_IN_NATIVE_COIN_BSH: fn(Context) -> Context =
    |mut context: Context| {
        (context)
            .pipe(THE_TRANSACTION_IS_SIGNED_BY_CHARLIE)
            .pipe(USER_INVOKES_REGISTER_NEW_COIN_IN_NATIVE_COIN_BSH)
    };

pub static NATIVE_COIN_BSH_SHOULD_THROW_ALREADY_EXISTING_ERROR_ON_REGISTERING_COIN: fn(Context) =
    |context: Context| {
        let error = context.method_errors("register");
        assert!(error.to_string().contains("BSHRevertAlreadyExistsToken"));
    };

pub static BOB_INVOKES_REGISTER_NEW_WRAPPED_COIN_IN_NATIVE_COIN_BSH: fn(Context) -> Context =
    |mut context: Context| {
        (context)
            .pipe(THE_TRANSACTION_IS_SIGNED_BY_BOB)
            .pipe(USER_INVOKES_REGISTER_NEW_COIN_IN_NATIVE_COIN_BSH)
    };

pub static CHARLIES_ACCOUNT_ID_IS_PROVIDED_AS_ADD_NATIVE_COIN_BSH_OWNER_PARAM: fn(
    Context,
) -> Context = |mut context: Context| {
    let charlie = context.accounts().get("charlie").to_owned();
    context.add_method_params(
        "add_owner",
        json!({
            "account": charlie.id()
        }),
    );
    context
};

pub static CHUCK_INVOKES_REGISTER_NEW_WRAPPED_COIN_IN_NATIVE_COIN_BSH: fn(Context) -> Context =
    |mut context: Context| {
        (context)
            .pipe(THE_TRANSACTION_IS_SIGNED_BY_CHUCK)
            .pipe(USER_INVOKES_REGISTER_NEW_COIN_IN_NATIVE_COIN_BSH)
    };

pub static NATIVE_COIN_BSH_SHOULD_THROW_UNAUTHORIZED_ERROR_ON_REGISTERING_COIN: fn(Context) =
    |context: Context| {
        let error = context.method_errors("register");

        assert!(error.to_string().contains("BSHRevertNotExistsPermission"));
    };

    pub static EXISTING_COIN_IS_PROVIDED_AS_REGISTER_COIN_PARAM: fn(Context) -> Context =
    |mut context: Context| {
        context.add_method_params(
            "register",
            json!({
                "coin": {
                    "metadata": {
                        "name": "NEAR",
                        "symbol": "NEAR",
                        "uri": null,
                        "network": super::NEAR_NETWORK,
                        "extras": null,
                    }
                },
            }),
        );
        context
    };

    pub static REGSITERED_COIN_SHOULD_BE_PRESENT: fn(Context) = |context: Context| {
        let coins = context.method_responses("coins");

        let expetced = json!([{"name":"NEAR","network":"0x1.near","symbol":"NEAR"}]);
    
        assert_eq!(expetced, coins);
    
       
    };


    pub static WRAPPED_ICX_PROVIDED_AS_REGSITER_PARAM_IN_NATIVE_COIN_BSH: fn(Context) -> Context =
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
    };
