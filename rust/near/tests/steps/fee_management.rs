use super::*;
use libraries::types::{HashedCollection, HashedValue};
use serde_json::{from_value, json, Value};
use test_helper::types::Context;

pub static FEE_NUMERATOR_IS_PROVIDED_AS_SET_FEE_RATIO_PARAM: fn(Context) -> Context =
    |mut context: Context| {
        context.add_method_params(
            "set_fee_ratio",
            json!({
                "fee_numerator":"10000"
            }),
        );
        context
    };

pub static AMOUNT_USED_IS_PROVIDED_AS_CALCULATE_TOKEN_TRANSFER_FEE_PARAM: fn(Context) -> Context =
    |mut context: Context| {
        context.add_method_params(
            "calculate_coin_transfer_fee",
            json!({
                "amount":"1000",
            }),
        );
        context
    };

pub static AMOUNT_USED_IS_PROVIDED_AS_CALCULATE_TOKEN_TRANSFER_FEE_PARAM_TOKEN_BSH: fn(
    Context,
) -> Context = |mut context: Context| {
    context.add_method_params(
        "calculate_token_transfer_fee",
        json!({
            "amount":"1000",
        }),
    );
    context
};

pub static FEE_RATIO_SHOULD_BE_UPDATED_IN_NATIVE_COIN_BSH: fn(Context) = |context: Context| {
    let context = context
        .pipe(AMOUNT_USED_IS_PROVIDED_AS_CALCULATE_TOKEN_TRANSFER_FEE_PARAM)
        .pipe(USER_INVOKES_CALCULATE_TOKEN_TRANFER_FEE_IN_NATIVE_COIN_BSH);
    let result: u128 = from_value(context.method_responses("calculate_coin_transfer_fee")).unwrap();
    assert_eq!(result, 1000);
};

pub static CHUCK_INVOKES_SET_FEE_RATIO_IN_NATIVE_COIN_BSH: fn(Context) -> Context =
    |mut context: Context| {
        context
            .pipe(THE_TRANSACTION_IS_SIGNED_BY_CHUCK)
            .pipe(USER_INVOKES_SET_FEE_RATIO_IN_NATIVE_COIN_BSH)
    };

pub static NATIVE_COIN_BSH_SHOULD_THROW_UNAUTHORIZED_ERROR_ON_SETTING_FEE_RATIO: fn(Context) =
    |context: Context| {
        let error = context.method_errors("set_fee_ratio");

        assert!(error.to_string().contains("BSHRevertNotExistsPermission"));
    };

pub static TOKEN_BSH_SHOULD_THROW_INVALID_NUMERATOR_ERROR_ON_SETTING_FEE_RATIO: fn(Context) =
    |context: Context| {
        let error = context.method_errors("set_fee_ratio");

        assert!(error.to_string().contains("BSHRevertInvalidSetting"));
    };

pub static FEE_NUMERATOR_GREATER_THAN_FEE_DENOMINATOR_IS_PROVIDED_AS_SET_FEE_RATIO_PARAM:
    fn(Context) -> Context = |mut context: Context| {
    context.add_method_params(
        "set_fee_ratio",
        json!({
            "fee_numerator":"100000000000000000000"
        }),
    );
    context
};

pub static BOB_INVOKES_SET_FEE_RATIO_IN_NATIVE_COIN_BSH: fn(Context) -> Context =
    |mut context: Context| {
        context
            .pipe(THE_TRANSACTION_IS_SIGNED_BY_BOB)
            .pipe(USER_INVOKES_SET_FEE_RATIO_IN_NATIVE_COIN_BSH)
    };

pub static NATIVE_COIN_BSH_SHOULD_THROW_INVALID_NUMERATOR_ERROR_ON_SETTING_FEE_RATIO: fn(Context) =
    |context: Context| {
        let error = context.method_errors("set_fee_ratio");

        assert!(error.to_string().contains("BSHRevertInvalidSetting"));
    };

pub static CHARLIE_INVOKES_REGISTER_NEW_WRAPPED_TOKEN_IN_BSH: fn(Context) -> Context =
    |mut context: Context| {
        (context)
            .pipe(THE_TRANSACTION_IS_SIGNED_BY_CHARLIE)
            .pipe(USER_INVOKES_REGISTER_IN_NATIVE_COIN_BSH)
    };

pub static NEW_TOKEN_NAME_IS_PROVIDED_AS_REGISTER_WARPPED_TOKEN_PARAM: fn(Context) -> Context =
    |mut context: Context| {
        context.add_method_params(
            "register",
            json!({
                "token": {
                    "metadata": {
                        "name": "token_1",
                        "symbol": "token_1",
                        "uri": null,
                        "network": NEAR_NETWORK,
                        "extras" : null
                    }
                },
            }),
        );
        context
    };

pub static BOB_INVOKES_ADD_OWNER_IN_NATIVE_COIN_BSH: fn(Context) -> Context =
    |context: Context| -> Context {
        context
            .pipe(THE_TRANSACTION_IS_SIGNED_BY_BOB)
            .pipe(USER_INVOKES_ADD_OWNER_IN_NATIVE_COIN_BSH)
    };

pub static CHUCK_INVOKES_REGISTER_NEW_WRAPPED_TOKEN_IN_NATIVE_COIN_BSH: fn(Context) -> Context =
    |mut context: Context| {
        (context)
            .pipe(THE_TRANSACTION_IS_SIGNED_BY_CHUCK)
            .pipe(USER_INVOKES_REGISTER_IN_NATIVE_COIN_BSH)
    };
pub static NATIVE_COIN_BSH_SHOULD_THROW_UNAUTHORIZED_ERROR_ON_REGISTERING_TOKEN: fn(Context) =
    |context: Context| {
        let error = context.method_errors("register");

        assert!(error.to_string().contains("BSHRevertNotExistsPermission"));
    };

pub static BOB_INVOKES_REGISTER_NEW_WRAPPED_TOKEN_IN_NATIVE_COIN_BSH: fn(Context) -> Context =
    |mut context: Context| {
        (context)
            .pipe(THE_TRANSACTION_IS_SIGNED_BY_BOB)
            .pipe(USER_INVOKES_REGISTER_IN_NATIVE_COIN_BSH)
    };

pub static CHARLIE_INVOKES_SET_FEE_RATIO_IN_NATIVE_COIN_BSH: fn(Context) -> Context =
    |mut context: Context| {
        context
            .pipe(THE_TRANSACTION_IS_SIGNED_BY_CHARLIE)
            .pipe(USER_INVOKES_SET_FEE_RATIO_IN_NATIVE_COIN_BSH)
    };

pub static BOB_INVOKES_SET_FEE_RATIO_IN_TOKEN_BSH: fn(Context) -> Context =
    |mut context: Context| {
        context
            .pipe(THE_TRANSACTION_IS_SIGNED_BY_BOB)
            .pipe(USER_INVOKES_SET_FEE_RATIO_IN_TOKEN_BSH)
    };

pub static FEE_RATIO_SHOULD_BE_UPDATED_IN_TOKEN_BSH: fn(Context) = |context: Context| {
    let context = context
        .pipe(AMOUNT_USED_IS_PROVIDED_AS_CALCULATE_TOKEN_TRANSFER_FEE_PARAM_TOKEN_BSH)
        .pipe(USER_INVOKES_CALCULATE_TOKEN_TRANFER_FEE_IN_TOKEN_BSH);
    let result: u128 =
        from_value(context.method_responses("calculate_token_transfer_fee")).unwrap();
    assert_eq!(result, 1000);
};

pub static CHUCK_INVOKES_SET_FEE_RATIO_IN_NATIVE_COIN_TOKEN_BSH: fn(Context) -> Context =
    |mut context: Context| {
        context
            .pipe(THE_TRANSACTION_IS_SIGNED_BY_CHUCK)
            .pipe(USER_INVOKES_SET_FEE_RATIO_IN_TOKEN_BSH)
    };

pub static TOKEN_BSH_SHOULD_THROW_UNAUTHORIZED_ERROR_ON_SETTING_FEE_RATIO: fn(Context) =
    |context: Context| {
        let error = context.method_errors("set_fee_ratio");
        assert!(error.to_string().contains("BSHRevertNotExistsPermission"));
    };

pub static TOKEN_NAME_IS_PROVIDED_AS_GET_TOKEN_ID_PARAM: fn(Context) -> Context =
    |mut context: Context| {
        context.add_method_params(
            "token_id",
            json!({
                "token_name": "WNear"
            }),
        );
        context
    };

pub static INVALID_TOKEN_NAME_IS_PROVIDED_AS_GET_TOKEN_ID_PARAM: fn(Context) -> Context =
    |mut context: Context| {
        context.add_method_params(
            "token_id",
            json!({
                "token_name": "ICON"
            }),
        );
        context
    };

pub static USER_INVOKES_TOKEN_ID_IN_TOKEN_BSH: fn(Context) -> Context = |mut context: Context| {
    context
        .pipe(TOKEN_NAME_IS_PROVIDED_AS_GET_TOKEN_ID_PARAM)
        .pipe(USER_INVOKES_GET_COIN_ID_IN_TOKEN_BSH)
};

pub static USER_INVOKES_TOKEN_ID_FOR_UNREGISTERED_TOKEN: fn(Context) -> Context =
    |mut context: Context| {
        context
            .pipe(INVALID_TOKEN_NAME_IS_PROVIDED_AS_GET_TOKEN_ID_PARAM)
            .pipe(USER_INVOKES_GET_COIN_ID_ERRORS_IN_TOKEN_BSH)
    };

pub static BOB_INVOKES_GET_TOKEN_ID_FROM_TOKEN_BSH: fn(Context) -> Context =
    |mut context: Context| {
        context
            .pipe(THE_TRANSACTION_IS_SIGNED_BY_BOB)
            .pipe(USER_INVOKES_GET_COIN_ID_IN_TOKEN_BSH)
    };

pub static TOKEN_ID_SHOULD_BE_PRESENT_FOR_THE_REGISTERED_TOKEN: fn(Context) = |context: Context| {
    let token_id = context.method_responses("token_id");
    let expected = json!([
        155, 207, 249, 185, 9, 163, 13, 226, 228, 184, 223, 189, 88, 74, 124, 209, 91, 69, 218,
        156, 181, 253, 214, 0, 228, 209, 0, 29, 170, 135, 179, 55
    ]);
    assert_eq!(token_id, expected);
};

pub static TOKEN_BSH_SHOULD_THROUGH_ERROR_ON_GETTING_TOKEN_ID: fn(Context) = |context: Context| {
    let error = context.method_errors("token_id");

    assert!(error.to_string().contains("BSHRevertNotExistsToken"));
};

pub static CHUCK_INVOKES_REGISTER_NEW_WRAPPED_TOKEN_IN_TOKEN_BSH: fn(Context) -> Context =
    |mut context: Context| {
        (context)
            .pipe(THE_TRANSACTION_IS_SIGNED_BY_CHUCK)
            .pipe(USER_INVOKES_REGISTER_NEW_TOKEN_IN_TOKEN_BSH)
    };

pub static TOKEN_BSH_SHOULD_THROW_UNAUTHORIZED_ERROR_ON_REGISTERING_TOKEN: fn(Context) =
    |context: Context| {
        let error = context.method_errors("register");

        assert!(error.to_string().contains("BSHRevertNotExistsPermission"));
    };

pub static CHARLIE_INVOKES_SET_FEE_RATIO_IN_TOKEN_BSH: fn(Context) -> Context =
    |mut context: Context| {
        context
            .pipe(THE_TRANSACTION_IS_SIGNED_BY_CHARLIE)
            .pipe(USER_INVOKES_SET_FEE_RATIO_IN_TOKEN_BSH)
    };

pub static BOB_INVOKES_REGISTER_NEW_TOKEN_IN_TOKEN_BSH: fn(Context) -> Context =
    |mut context: Context| {
        context
            .pipe(THE_TRANSACTION_IS_SIGNED_BY_BOB)
            .pipe(USER_INVOKES_REGISTER_NEW_TOKEN_IN_TOKEN_BSH)
    };

pub static TOKEN_BSH_SHOULD_THROW_TOKEN_ALREADY_EXISTS_ERROR_ON_REGISTERING_TOKEN: fn(Context) =
    |context: Context| {
        let error = context.method_errors("register");
        assert!(error.to_string().contains("BSHRevertAlreadyExistsToken"));
    };
