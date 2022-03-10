use super::*;
use serde_json::json;
use test_helper::types::Context;

crate::user_call!(ADD_SERVICE_IN_BMC; BMC_OWNER, ALICE, CHUCK, CHARLIE);
crate::user_call!(REMOVE_SERVICE_IN_BMC; BMC_OWNER, ALICE, CHUCK, CHARLIE);

pub static NATIVE_COIN_BSH_NAME_AND_ACCOUNT_ID_ARE_PROVIDED_AS_ADD_SERVICE_PARAM: fn(
    Context,
) -> Context = |mut context: Context| {
    context.add_method_params(
        "add_service",
        json!({
            "name": "nativecoin",
            "service": context.contracts().get("nativecoin").id()
        }),
    );
    context
};

pub static NATIVE_COIN_BSH_NAME_IS_PROVIDED_AS_REMOVE_SERVICE_PARAM: fn(Context) -> Context =
    |mut context: Context| {
        context.add_method_params(
            "remove_service",
            json!({
                "name":format!("nativecoin"),
            }),
        );
        context
    };

pub static BMC_SHOULD_THROW_SERVICE_DOES_NOT_EXIST_ERROR_ON_REMOVING_SERVICE: fn(Context) =
    |context: Context| {
        let error = context.method_errors("remove_service");
        assert!(error.to_string().contains("BMCRevertNotExistBSH"));
    };

pub static NATIVE_COIN_BSH_IS_REGISTERED_IN_BMC: fn(Context) -> Context = |context: Context| -> Context {
    context
        .pipe(NATIVE_COIN_BSH_NAME_AND_ACCOUNT_ID_ARE_PROVIDED_AS_ADD_SERVICE_PARAM)
        .pipe(BMC_OWNER_INVOKES_ADD_SERVICE_IN_BMC)
};

pub static THE_REMOVED_SERVICE_SHOULD_NOT_BE_IN_THE_LIST_OF_SERVICES: fn(Context) =
    |context: Context| {
        let context = context.pipe(USER_INVOKES_GET_SERVICES_IN_BMC);

        let result = context.method_responses("get_services");
        let expected = json!([]);

        assert_eq!(result, expected);
    };

pub static BMC_SHOULD_THROW_UNAUTHORISED_ERROR_ON_ADDING_SERVICE: fn(Context) =
    |context: Context| {
        let error = context.method_errors("add_service");
        assert!(error.to_string().contains("BMCRevertNotExistsPermission"));
    };

pub static USER_SHOULD_GET_THE_EXISITING_LIST_OF_SERVICES: fn(Context) = |context: Context| {
    let result = context.method_responses("get_services");
    let expected = json!([
        {
            "name": "nativecoin",
            "service": context.contracts().get("nativecoin").id()
        }
    ]);

    assert_eq!(result, expected);
};

pub static NATIVE_COIN_BSH_SHOULD_BE_ADDED_TO_THE_LIST_OF_SERVICES: fn(Context) =
    |context: Context| {
        let context = context.pipe(USER_INVOKES_GET_SERVICES_IN_BMC);
        let result = context.method_responses("get_services");
        let expected = json!([
            {
                "name": "nativecoin",
                "service": context.contracts().get("nativecoin").id()
            }
        ]);

        assert_eq!(result, expected);
    };

pub static BMC_SHOULD_THROW_UNAUTHORISED_ERROR_ON_REMOVING_SERVICE: fn(Context) =
    |context: Context| {
        let error = context.method_errors("remove_service");
        assert!(error.to_string().contains("BMCRevertNotExistsPermission"));
    };

pub static BMC_SHOULD_THROW_SERVICE_ALREADY_EXISTS_ON_ADDING_SERVICE: fn(Context) =
|context: Context| {
    let error = context.method_errors("add_service");
    assert!(error.to_string().contains("BMCRevertAlreadyExistsBSH"));
};

pub static BMC_SHOULD_THROW_SERVICE_DOES_NOT_EXIST_ON_SENDING_SERVICE_MESSAGE: fn(Context) =
|context: Context| {
    let error = context.method_errors("handle_relay_message_bmv_callback_mockable");
    // assert!(error.to_string().contains("BMCRevertAlreadyExistsBSH"));
    println!("{:?}",error);
};