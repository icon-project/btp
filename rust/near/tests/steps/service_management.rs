use super::*;
use serde_json::json;
use test_helper::types::Context;

pub static NEWLY_ADDED_REQUEST_SHOULD_BE_IN_BMC_REQUESTS: fn(Context) = |context: Context| {
    let requests = context.method_responses("get_requests");
    assert_eq!(
        requests,
        json!([context.method_params("get_requests").to_string()])
    );
};

pub static SERVICE_ADDED_SHOULD_BE_IN_SERVICES: fn(Context) = |context: Context| {
    let services = context.method_responses("get_services");
    assert_eq!(services, json!(["ServiceName"]));
};

pub static BOB_IS_BSH_CONTRACT_OWNER: fn(Context) -> Context = |mut context: Context| {
    let bsh_signer = context.contracts().get("bsh").to_owned();
    context.accounts_mut().add("bob", &bsh_signer);
    context
};

pub static SERVICE_NAME_AND_SMARTCONTRACT_ADDRESS_PROVIDED_AS_REQUEST_SERVICE_PARAM: fn(
    Context,
)
    -> Context = |mut context: Context| {
    let bob = context.accounts().get("bob").to_owned();
    context.add_method_params(
        "request_service",
        json!({
            "name": "BSH",
            "service": "Service Address"
        }),
    );
    context
};

pub static SERVICE_NAME_AND_INVALID_SMARTCONTRACT_ADDRESS_PROVIDED_AS_REQUEST_SERVICE_PARAM:
    fn(Context) -> Context = |mut context: Context| {
    let bob = context.accounts().get("bob").to_owned();
    context.add_method_params(
        "request_service",
        json!({
            "name": "BSH",
            "service": "INVALID_ADDRESS"
        }),
    );
    context
};

pub static BOB_INVOKES_REQUEST_SERVICE_IN_BMC_FROM_BSH: fn(Context) -> Context =
    |mut context: Context| {
        let signer = context.accounts().get("bob").to_owned();
        context.set_signer(&signer);
        BMC_CONTRACT.request_service(context)
    };

pub static BOB_INVOKES_REQUEST_SERVICE_IN_BMC_FROM_BSH_AGAIN: fn(Context) -> Context =
    |mut context: Context| {
        let signer = context.accounts().get("bob").to_owned();
        context.set_signer(&signer);
        BMC_CONTRACT.request_service(context)
    };

pub static BSH_IS_AN_EXISTING_SERVICE: fn(Context) -> Context = |mut context: Context| {
    BMC_CONTRACT_IS_DEPLOYED(context)
        .pipe(TOKEN_BSH_CONTRACT_IS_DEPLOYED)
        .pipe(BOB_IS_BSH_CONTRACT_OWNER)
        .pipe(SERVICE_NAME_AND_SMARTCONTRACT_ADDRESS_PROVIDED_AS_REQUEST_SERVICE_PARAM)
};

pub static BSH_SERVICE_ALREADY_EXISTS: fn(Context) -> Context = |mut context: Context| {
    BMC_CONTRACT_IS_DEPLOYED(context)
        .pipe(ALICE_IS_BMC_CONTRACT_OWNER)
        .pipe(TOKEN_BSH_CONTRACT_IS_DEPLOYED)
        .pipe(BOB_IS_BSH_CONTRACT_OWNER)
        .pipe(SERVICE_NAME_AND_SMARTCONTRACT_ADDRESS_PROVIDED_AS_REQUEST_SERVICE_PARAM)
        .pipe(BOB_INVOKES_REQUEST_SERVICE_IN_BMC_FROM_BSH)
};

pub static CONTRACTS_ARE_DEPLOYED: fn(Context) -> Context =
    |mut context: Context| BMC_CONTRACT_IS_DEPLOYED(context).pipe(TOKEN_BSH_CONTRACT_IS_DEPLOYED);

pub static CHUCK_IS_NOT_A_BSH_OWNER: fn(Context) -> Context = |context: Context| context;

pub static CHUCK_INVOKES_REQUEST_SERVICE_IN_BMC_FROM_BSH: fn(Context) -> Context =
    |mut context: Context| {
        let signer = context.accounts().get("chuck").to_owned();
        context.set_signer(&signer);
        BMC_CONTRACT.request_service(context)
    };

pub static ALICE_INVOKES_APPROVE_SERVICE_IN_BMC: fn(Context) -> Context = |mut context: Context| {
    let signer = context.accounts().get("alice").to_owned();
    context.set_signer(&signer);
    BMC_CONTRACT.approve_service(context)
};

pub static CHUCK_INVOKES_APPROVE_SERVICE_IN_BMC: fn(Context) -> Context = |mut context: Context| {
    let signer = context.accounts().get("alice").to_owned();
    context.set_signer(&signer);
    BMC_CONTRACT.approve_service(context)
};

pub static BSH_SHOULD_BE_ADDED_IN_BMC_SERVICES: fn(Context) -> Context = |context: Context| {
    let services = context.method_responses("get_services");
    assert_eq!(services, json!(["services_provided"]));
    context
};

pub static BSH_REQUEST_SHOULD_BE_REMOVED_FROM_BMC_REQUESTS: fn(Context) = |context: Context| {
    let requests = context.method_params("get_requests");
    assert_eq!(requests, json!(["Service_required_provided"]));
};

pub static APPROVED_BSH_SERVICE: fn(Context) -> Context = |mut context: Context| {
    BMC_CONTRACT_IS_DEPLOYED(context)
        .pipe(ALICE_IS_BMC_CONTRACT_OWNER)
        .pipe(TOKEN_BSH_CONTRACT_IS_DEPLOYED)
        .pipe(BOB_IS_BSH_CONTRACT_OWNER)
        .pipe(SERVICE_NAME_AND_SMARTCONTRACT_ADDRESS_PROVIDED_AS_REQUEST_SERVICE_PARAM)
        .pipe(BOB_INVOKES_REQUEST_SERVICE_IN_BMC_FROM_BSH)
        .pipe(ALICE_INVOKES_APPROVE_SERVICE_IN_BMC)
};

pub static SERVICE_NAME_PROVIDED_AS_REMOVE_REQUEST_PARAM: fn(Context) -> Context =
    |mut context: Context| {
        context.add_method_params(
            "request_service",
            json!({
                "name": "BSH",
            }),
        );
        context
    };

pub static NONEXISTING_SERVICE_NAME_PROVIDED_AS_REMOVE_REQUEST_PARAM: fn(Context) -> Context =
    |mut context: Context| {
        context.add_method_params(
            "request_service",
            json!({
                "name": "InvalidAddress",
            }),
        );
        context
    };

pub static ALICE_INVOKES_REMOVE_REQUEST: fn(Context) -> Context = |mut context: Context| {
    let signer = context.accounts().get("alice").to_owned();
    context.set_signer(&signer);
    BMC_CONTRACT.approve_service(context)
};

pub static CHUCK_INVOKES_REMOVE_REQUEST: fn(Context) -> Context = |mut context: Context| {
    let signer = context.accounts().get("chuck").to_owned();
    context.set_signer(&signer);
    BMC_CONTRACT.approve_service(context)
};

pub static BSH_SERVICE_REMOVED_FROM_SERVICES: fn(Context) = |context: Context| {
    let services = context.method_responses("get_services");
    assert_eq!(services, json!([]));
};

//TODO: Error Handling
pub static BSH_SHOULD_THROW_REQUESTEXIST_ERROR: fn(Context) -> Context = |context: Context| context;
pub static BSH_SHOULD_THROW_UNAUTHORIZED_ERROR: fn(Context) -> Context = |context: Context| context;
pub static BSH_SHOULD_THROW_ALREADY_EXIST_ERRROR: fn(Context) -> Context = |context: Context| context;
pub static BMC_SHOULD_THROW_INVALIDADDRESS_ERROR: fn(Context) -> Context =
    |context: Context| context;
