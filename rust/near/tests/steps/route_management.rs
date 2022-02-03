use super::*;
use super::{BMC_CONTRACT,};
use serde_json::{from_value, json};
use test_helper::types::Context;
use libraries::types::{HashedCollection, HashedValue};

pub static DESTINATION_NETWORK: &str = "0x1.bsc";
pub static DESTINATION_BMC: &str = "0xc294b1A62E82d3f135A8F9b2f9cAEAA23fbD6Ce7";

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

pub static ADDED_ROUTE_SHOULD_BE_PRESENT: fn(Context) = |mut context: Context| {
    let link = context.method_responses("get_routes");

    let result: HashSet<_> = from_value::<Vec<String>>(link)
        .unwrap()
        .into_iter()
        .collect();
    let expected: HashSet<_> = vec!["BTPADDRES_OF_THE_LINK_PROVIDED".to_string()].into_iter().collect();
    assert_eq!(result, expected);
};

pub static REMOVED_ROUTE_SHOULD_NOT_BE_PRESENT: fn(Context) = |mut context: Context| {
    let routes = context.method_responses("get_routes");
    let expected_routes = json!([]);
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
};

pub static ADDED_ROUTES_SHOULD_PRESENT_IN_ROUTES_LIST: fn(Context) =
    |mut context: Context| {
        let routes = context.method_responses("get_routes");
        let expected_routes = json!([{
            "dst": format!{"btp://{}/{}",DESTINATION_NETWORK,DESTINATION_BMC},
            "next": format!{"btp://{}/{}",ICON_NETWORK,ICON_BMC}
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
    };

pub static ALICE_INVOKES_ADD_ROUTE_IN_BMC: fn(Context) -> Context = |context: Context| {
    context
        .pipe(ALICE_IS_THE_SIGNER)
        .pipe(USER_INVOKES_ADD_ROUTE_IN_BMC)
        .pipe(USER_INVOKES_GET_ROUTES_BMC)
};

pub static ALICE_INVOKES_REMOVE_ROUTE_IN_BMC: fn(Context) -> Context = |context: Context| {
    context
        .pipe(ALICE_IS_THE_SIGNER)
        .pipe(USER_INVOKES_REMOVE_ROUTE_IN_BMC)
        .pipe(USER_INVOKES_GET_ROUTES_BMC)
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

pub static DESTINATION_AND_LINK_ADDRESS_ARE_PROVIDED_AS_ADD_ROUTE_PARAM: fn(Context) -> Context =
    |mut context: Context| {
        context.add_method_params(
            "add_route",
            json!({
                "destination":  format!("btp://{}/{}", DESTINATION_NETWORK, DESTINATION_BMC),
                "link":  format!("btp://{}/{}", ICON_NETWORK, ICON_BMC)
            }),
        );
        context
    };

pub static DESTINATION_ADDRESS_IS_PROVIDED_AS_REMOVE_ROUTE_PARAM: fn(Context) -> Context =
    |mut context: Context| {
        context.add_method_params(
            "remove_route",
            json!({
                "destination":format!("btp://{}/{}", DESTINATION_NETWORK, DESTINATION_BMC),
            }),
        );
        context
};

pub static DESTINATION_AND_INVALID_LINK_ADDRESS_ARE_PROVIDED_AS_ADD_ROUTE_PARAM: fn(Context) -> Context =
    |mut context: Context| {
        context.add_method_params(
            "add_route",
            json!({
                "destination":  format!("btp://{}/{}", DESTINATION_NETWORK, DESTINATION_BMC),
                "link":  format!("btp://{}//{}//", ICON_NETWORK, ICON_BMC)
            }),
        );
        context
    };

pub static LINK_AND_INVALID_ROUTE_ADDRESS_ARE_PROVIDED_AS_ADD_ROUTE_PARAM: fn(Context) -> Context =
    |mut context: Context| {
        context.add_method_params(
            "add_route",
            json!({
                "destination":  format!("btp://{}///{}", DESTINATION_NETWORK, DESTINATION_BMC),
                "link":  format!("btp://{}/{}/", ICON_NETWORK, ICON_BMC)
            }),
        );
        context
    };

pub static BMC_SHOULD_THROW_INVALID_LINK_ADDRESS_ERROR: fn(Context) -> Context =
    |mut context: Context| {
        context.pipe(BMC_SHOULD_THROW_INVALIDADDRESS_ERROR)
    };   