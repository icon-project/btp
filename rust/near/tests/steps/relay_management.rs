use super::*;
use serde_json::{from_value, json};
use test_helper::types::Context;

pub static ICON_LINK_ADDRESS_AND_RELAY_1_IS_PROVIDED_AS_ADD_RELAY_PARAM: fn(Context) -> Context =
    |mut context: Context| {
        context.add_method_params(
            "add_relay",
            json!({
                "link": format!("btp://{}/{}", ICON_NETWORK, ICON_BMC),
                "relay": context.accounts().get("relay_1").account_id()
            }),
        );

        context
    };

pub static BMC_OWNER_INVOKES_ADD_RELAY_IN_BMC: fn(Context) -> Context = |context: Context| {
    context
        .pipe(TRANSACTION_IS_SIGNED_BY_BMC_OWNER)
        .pipe(USER_INVOKES_ADD_RELAY_IN_BMC)
};

pub static RELAY_1_IS_REGISTERED: fn(Context) -> Context = |context: Context| -> Context {
    context
        .pipe(RELAY_1_ACCOUNT_IS_CREATED)
        .pipe(ICON_LINK_ADDRESS_AND_RELAY_1_IS_PROVIDED_AS_ADD_RELAY_PARAM)
        .pipe(BMC_OWNER_INVOKES_ADD_RELAY_IN_BMC)
};

pub static ICON_LINK_ADDRESS_AND_RELAY_1_ACCOUNT_ID_AND_RELAY_2_ACCOUNT_ID_ARE_PROVIDED_AS_ADD_RELAYS_PARAM: fn(Context) -> Context =
    |mut context: Context| {
        context.add_method_params(
            "add_relays",
            json!({
                "link": format!("btp://{}/{}", ICON_NETWORK, ICON_BMC),
                "relays": [
                    context.accounts().get("relay_1").account_id(),
                    context.accounts().get("relay_2").account_id()
                ]
            }),
        );

        context
    };

pub static ALICE_INVOKES_ADD_RELAY_IN_BMC: fn(Context) -> Context = |context: Context| {
    context
        .pipe(TRANSACTION_IS_SIGNED_BY_ALICE)
        .pipe(USER_INVOKES_ADD_RELAY_IN_BMC)
};

pub static ADDED_RELAYS_SHOULD_BE_IN_BMC_RELAY_LIST: fn(Context) = |context: Context| {
    let context = context
        .pipe(ICON_LINK_ADDRESS_IS_PROVIDED_AS_GET_RELAYS_PARAM)
        .pipe(USER_INVOKES_GET_RELAYS_IN_BMC);

    let relays = context.method_responses("get_relays");
    let result: HashSet<_> = from_value::<Vec<String>>(relays)
        .unwrap()
        .into_iter()
        .collect();
    let expected: HashSet<_> = vec![context.accounts().get("relay_1").account_id().to_string()]
        .into_iter()
        .collect();

    assert_eq!(result, expected);
};

pub static ICON_LINK_ADDRESS_IS_PROVIDED_AS_GET_RELAYS_PARAM: fn(Context) -> Context =
    |mut context: Context| {
        context.add_method_params(
            "get_relays",
            json!({
                "link": format!("btp://{}/{}", ICON_NETWORK, ICON_BMC),
            }),
        );

        context
    };
