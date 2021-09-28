use super::*;
use super::{BMC_CONTRACT, BMV_CONTRACT, BSH_CONTRACT};
use serde_json::{from_value, json};
use test_helper::types::Context;

pub static ADD_ROUTE_INVOKED_BY_BMC_OWNER: fn(Context) -> Context = |mut context: Context| {
    DESTINATION_AND_LINK_ADDRESS_PROVIDED_AS_ADD_ROUTE_PARAM(context)
        .pipe(ALICE_INVOKES_ADD_ROUTE_IN_BMC)
};
pub static ADD_ROUTE_INVOKED_BY_NON_BMC_OWNER: fn(Context) -> Context = |mut context: Context| {
    DESTINATION_AND_LINK_ADDRESS_PROVIDED_AS_ADD_ROUTE_PARAM(context)
        .pipe(CHUCK_INVOKES_ADD_ROUTE_IN_BMC)
};

pub static REMOVE_ROUTE_INVOKED_BY_BMC_OWNER: fn(Context) -> Context = |mut context: Context| {
    DESTINATION_AND_LINK_ADDRESS_PROVIDED_AS_REMOVE_ROUTE_PARAM(context)
        .pipe(ALICE_INVOKES_REMOVE_ROUTE_IN_BMC)
};

pub static REMOVE_NON_EXISTING_ROUTE_INVOKED_BY_BMC_OWNER: fn(Context) -> Context =
    |mut context: Context| {
        DESTINATION_AND_LINK_ADDRESS_PROVIDED_AS_REMOVE_ROUTE_PARAM(context)
            .pipe(ALICE_INVOKES_REMOVE_ROUTE_IN_BMC)
            .pipe(DESTINATION_AND_LINK_ADDRESS_PROVIDED_AS_REMOVE_ROUTE_PARAM)
            .pipe(ALICE_INVOKES_REMOVE_ROUTE_IN_BMC)
    };

pub static REMOVE_ROUTE_INVOKED_BY_NON_BMC_OWNER: fn(Context) -> Context =
    |mut context: Context| {
        DESTINATION_AND_LINK_ADDRESS_PROVIDED_AS_REMOVE_ROUTE_PARAM(context)
            .pipe(CHUCK_INVOKES_REMOVE_ROUTE_IN_BMC)
    };

pub static ADD_EXISTING_ROUTE_INVOKED_BY_BMC_OWNER: fn(Context) -> Context =
    |mut context: Context| {
        DESTINATION_AND_LINK_ADDRESS_PROVIDED_AS_ADD_ROUTE_PARAM(context)
            .pipe(ALICE_INVOKES_ADD_ROUTE_IN_BMC)
            .pipe(DESTINATION_AND_LINK_ADDRESS_PROVIDED_AS_ADD_ROUTE_PARAM)
            .pipe(ALICE_INVOKES_ADD_ROUTE_IN_BMC)
    };

pub static ROUTES_ARE_QURIED_IN_BMC: fn(Context) -> Context =
    |mut context: Context| BMC_CONTRACT.get_routes(context);

pub static ADDED_ROUTE_SHOULD_BE_PRESENT: fn(Context) -> Context = |mut context: Context| {
    let link = context.method_responses("get_routes");

    let result: HashSet<_> = from_value::<Vec<String>>(link)
        .unwrap()
        .into_iter()
        .collect();
    let expected: HashSet<_> = vec!["BTPADDRES_OF_THE_LINK_PROVIDED"].into_iter().collect();
    assert_eq!(result, expected)
};

pub static REMOVED_ROUTE_SHOULD_NOT_BE_PRESENT: fn(Context) -> Context = |mut context: Context| {
    let link = context.method_responses("get_routes");

    let result: HashSet<_> = from_value::<Vec<String>>(link)
        .unwrap()
        .into_iter()
        .collect();
    let expected: HashSet<_> = vec!["BTPADDRES_OF_THE_LINK_PROVIDED"].into_iter().collect();
    assert_ne!(result, expected)
};

pub static ALICE_INVOKES_ADD_ROUTE_IN_BMC: fn(Context) -> Context = |mut context: Context| {
    let signer = context.accounts().get("alice").to_owned();
    context.set_signer(&signer);
    BMC_CONTRACT.add_route(context)
};

pub static DESTINATION_AND_LINK_ADDRESS_PROVIDED_AS_ADD_ROUTE_PARAM: fn(Context) -> Context =
    |mut context: Context| {
        context.add_method_params(
            "add_link",
            json!({
                "destination": "BTPADDRESS",
                "link": "BTPADDRESS"
            }),
        );
        context
    };

pub static DESTINATION_AND_LINK_ADDRESS_PROVIDED_AS_REMOVE_ROUTE_PARAM: fn(Context) -> Context =
    |mut context: Context| {
        context.add_method_params(
            "add_link",
            json!({
                "destination": "BTPADDRESS",
                "link": "BTPADDRESS"
            }),
        );
        context
    };

pub static ALICE_INVOKES_REMOVE_ROUTE_IN_BMC: fn(Context) -> Context = |mut context: Context| {
    let signer = context.accounts().get("alice").to_owned();
    context.set_signer(&signer);
    BMC_CONTRACT.remove_route(context)
};

pub static CHUCK_INVOKES_ADD_ROUTE_IN_BMC: fn(Context) -> Context = |mut context: Context| {
    let signer = context.accounts().get("chucks").to_owned();
    context.set_signer(&signer);
    BMC_CONTRACT.add_route(context)
};

pub static CHUCK_INVOKES_REMOVE_ROUTE_IN_BMC: fn(Context) -> Context = |mut context: Context| {
    let signer = context.accounts().get("chucks").to_owned();
    context.set_signer(&signer);
    BMC_CONTRACT.remove_route(context)
};
