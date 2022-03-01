use super::*;
use libraries::types::{HashedCollection, HashedValue};
use serde_json::json;
use test_helper::types::Context;

crate::user_call!(ADD_ROUTE_IN_BMC; BMC_OWNER, ALICE, CHUCK, CHARLIE);
crate::user_call!(REMOVE_ROUTE_IN_BMC; BMC_OWNER, ALICE, CHUCK, CHARLIE);

pub static BSC_LINK_ADDRESS_AS_DESTINATION_AND_ICON_LINK_ADDRESS_AS_LINK_ARE_PROVIDED_AS_ADD_ROUTE_PARAM: fn(Context) -> Context =
    |mut context: Context| {
        context.add_method_params(
            "add_route",
            json!({
                "destination":  format!("btp://{}/{}", BSC_NETWORK, BSC_BMC),
                "link":  format!("btp://{}/{}", ICON_NETWORK, ICON_BMC)
            }),
        );
        context
    };

pub static BSC_LINK_ADDRESS_IS_PROVIDED_AS_REMOVE_ROUTE_PARAM: fn(Context) -> Context =
    |mut context: Context| {
        context.add_method_params(
            "remove_route",
            json!({
                "destination":format!("btp://{}/{}", BSC_NETWORK, BSC_BMC),
            }),
        );
        context
    };

pub static THE_REMOVED_ROUTE_SHOULD_NOT_BE_IN_THE_LIST_OF_ROUTES: fn(Context) =
    |context: Context| {
        let context = context.pipe(USER_INVOKES_GET_ROUTES_IN_BMC);

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

pub static ROUTE_TO_BSC_IS_PRESENT_IN_BMC: fn(Context) -> Context = |context: Context| -> Context {
    context.pipe(ICON_LINK_IS_PRESENT_IN_BMC)
        .pipe(BSC_LINK_ADDRESS_AS_DESTINATION_AND_ICON_LINK_ADDRESS_AS_LINK_ARE_PROVIDED_AS_ADD_ROUTE_PARAM)
        .pipe(USER_INVOKES_ADD_ROUTE_IN_BMC)
};

pub static BSC_LINK_ADDRESS_SHOULD_BE_ADDED_TO_THE_LIST_OF_ROUTES: fn(Context) =
    |context: Context| {
        let context = context.pipe(USER_INVOKES_GET_ROUTES_IN_BMC);
        let routes = context.method_responses("get_routes");
        let result = routes
            .as_array()
            .unwrap()
            .to_owned()
            .into_iter()
            .collect::<HashedCollection<HashedValue>>();
        let expected_routes = json!([{
            "dst":  format!("btp://{}/{}", BSC_NETWORK, BSC_BMC),
            "next":  format!("btp://{}/{}", ICON_NETWORK, ICON_BMC)
        }]);
        let expected = expected_routes
            .as_array()
            .unwrap()
            .to_owned()
            .into_iter()
            .collect::<HashedCollection<HashedValue>>();

        assert_eq!(result, expected);
    };

pub static USER_SHOULD_GET_THE_EXISITING_LIST_OF_ROUTES: fn(Context) = |context: Context| {
    let routes = context.method_responses("get_routes");
    let result = routes
        .as_array()
        .unwrap()
        .to_owned()
        .into_iter()
        .collect::<HashedCollection<HashedValue>>();
    assert!(result.0.len() > 0);
};

pub static BSC_LINK_ADDRESS_AS_DESTINATION_AND_INVALID_BTP_ADDRESS_AS_LINK_ARE_PROVIDED_AS_ADD_ROUTE_PARAM: fn(
    Context,
) -> Context = |mut context: Context| {
    context.add_method_params(
        "add_route",
        json!({
            "destination":  format!("btp://{}/{}", BSC_NETWORK, BSC_BMC),
            "link":  format!("http://0x1.icon/{}", ICON_BMC)
        }),
    );
    context
};

pub static BMC_SHOULD_THROW_INVALID_BTP_ADDRESS_ERROR_ON_ADDING_ROUTE: fn(Context) =
    |context: Context| {
        let error = context.method_errors("add_route");
        assert!(error.to_string().contains("InvalidBtpAddress"));
    };

pub static INVALID_BTP_ADDRESS_AS_DESTINATION_ADDRESS_AND_ICON_LINK_ADDRESS_AS_LINK_ARE_PROVIDED_AS_ADD_ROUTE_PARAM:
    fn(Context) -> Context = |mut context: Context| {
    context.add_method_params(
        "add_route",
        json!({
            "destination":  format!("http://{}/{}", BSC_NETWORK, BSC_BMC),
            "link":  format!("btp://{}/{}/", ICON_NETWORK, ICON_BMC)
        }),
    );
    context
};

pub static BMC_SHOULD_THROW_ROUTE_DOES_NOT_EXIST_ERROR_ON_REMOVING_ROUTE: fn(Context) =
    |context: Context| {
        let error = context.method_errors("remove_route");
        assert!(error.to_string().contains("BMCRevertNotExistsRoute"));
    };

pub static BMC_SHOULD_THROW_ROUTE_ALREADY_EXIST_ERROR_ON_ADDING_ROUTE: fn(Context) =
    |context: Context| {
        let error = context.method_errors("add_route");
        assert!(error.to_string().contains("BMCRevertAlreadyExistsRoute"));
    };

pub static BMC_SHOULD_THROW_UNAUTHORISED_ERROR_ON_ADDING_ROUTE: fn(Context) = |context: Context| {
    let error = context.method_errors("add_route");
    assert!(error.to_string().contains("BMCRevertNotExistsPermission"));
};

pub static BMC_SHOULD_THROW_UNAUTHORISED_ERROR_ON_REMOVING_ROUTE: fn(Context) =
    |context: Context| {
        let error = context.method_errors("remove_route");
        assert!(error.to_string().contains("BMCRevertNotExistsPermission"));
    };
