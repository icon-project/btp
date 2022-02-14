use super::*;
use libraries::types::LinkStatus;
use serde_json::{from_value, json};
use test_helper::types::Context;

pub static COIN_REGISTERED_SHOULD_BE_PRESENT: fn(Context) = |context: Context| {
    let context = context
    .pipe(USER_INVOKES_GET_COINS_IN_NATIVE_COIN_BSH);
    let coins = context.method_responses("coins");

    let expected = json!([
        {"name": "NEAR", "network": "0x1.near", "symbol": "NEAR"},
        {"name": "coin_1", "network": "0x1.near", "symbol": "coin_1"}
    ]);
    
    assert_eq!(coins,expected);
};

pub static NEW_WRAPPED_COIN_IS_REGISTERED_IN_NATIVE_COIN_BSH: fn(Context) -> Context = |mut context: Context| {
    (context)
        .pipe(NEW_COIN_NAME_IS_PROVIDED_AS_REGISTER_WARPPED_COIN_PARAM)
        .pipe(USER_INVOKES_REGISTER_NEW_COIN_IN_NATIVE_COIN_BSH)
};

pub static NEW_COIN_NAME_IS_PROVIDED_AS_REGISTER_WARPPED_COIN_PARAM: fn(Context) -> Context =
|mut context: Context| {
    context.add_method_params(
        "register",
        json!({
            "token": {
                "metadata": {
                    "name": "coin_1",
                    "symbol": "coin_1",
                    "uri": null,
                    "network": NEAR_NETWORK,
                    "extras" : null
                }
            },
        }),
    );
    context
};

pub static REGSITERED_COIN_IDS_ARE_QUERIED: fn(Context) = |context: Context| {
    let coin_id = context.method_responses("coin_id");

    let expected  = json!([169, 251, 132, 88, 218, 49, 26, 238, 101, 25, 214, 174, 76, 238, 27, 82, 101, 204, 224, 247, 162, 64, 40, 41, 146, 253, 223, 28, 217, 19, 87, 150
    ]);
    
    assert_eq!(coin_id,expected);
};

pub static COIN_NAME_IS_PROVIDED_AS_GET_COIN_ID_PARAM: fn(Context) -> Context =
|mut context: Context| {
    context.add_method_params(
        "coin_id",
        json!({ "coin_name":"NEAR" }),
    );

    context
};

pub static UNREGISTERED_COIN_NAME_IS_PROVIDED_AS_GET_COIN_ID_PARAM: fn(Context) -> Context =
|mut context: Context| {
    context.add_method_params(
        "coin_id",
        json!({ "coin_name":"BTC" }),
    );

    context
};

pub static NATIVE_COIN_BSH_SHOULD_THROW_INVALID_COIN_ERROR_ON_GETTING_COIN_ID: fn(Context) =
|context: Context| {
    let error = context.method_errors("coin_id");

    //  assert!(error.to_string().contains("BSHRevertNotExistsPermission"));
println!("{:?}",error);  
};

pub static NATIVE_COIN_BSH_SHOULD_THROW_UNAUTHORIZED_ERROR_ON_REGSITERING_NEW_COIN: fn(Context) =
    |context: Context| {
        let error = context.method_errors("register");

        assert!(error.to_string().contains("BSHRevertNotExistsPermission"));
    };

    pub static CHARLIE_INVOKES_REGISTER_NEW_WRAPPED_COIN_IN_NATIVE_COIN_BSH: fn(Context) -> Context = |mut context: Context| {
        (context)
            .pipe(THE_TRANSACTION_IS_SIGNED_BY_CHARLIE)
            .pipe(USER_INVOKES_REGISTER_NEW_COIN_IN_NATIVE_COIN_BSH)
    };

    pub static NATIVE_COIN_BSH_SHOULD_THROW_ALREADY_EXISTING_ERROR_ON_REGISTERING_COIN: fn(Context) =
    |context: Context| {
        let error = context.method_errors("register");

       assert!(error.to_string().contains("BSHRevertNotExistsPermission"));
        
    };

    pub static BOB_INVOKES_REGISTER_NEW_WRAPPED_COIN_IN_NATIVE_COIN_BSH: fn(Context) -> Context = |mut context: Context| {
        (context)
            .pipe(THE_TRANSACTION_IS_SIGNED_BY_BOB)
            .pipe(USER_INVOKES_REGISTER_NEW_COIN_IN_NATIVE_COIN_BSH)
    };