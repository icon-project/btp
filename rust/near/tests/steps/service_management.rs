use super::BMC_CONTRACT;
use serde_json::json;
use test_helper::types::Context;

pub static SERVICE_NAME_AND_SMARTCONTRACT_ADDRESS_PROVIDED_AS_ADD_SERVICE_PARAM: fn(
    Context,
) -> Context = |mut context: Context| {
    let address = context.contracts().get("bmc").account_id();
    context.add_method_params(
        "add_service",
        json!({
            "service": "SERVICENAME",
            "addr" : address
        }),
    );
    context
};

pub static ON_QUERYING_SERVICES_IN_BMC: fn(Context) -> Context =
    |context: Context| BMC_CONTRACT.get_services(context);

pub static SERVICE_ADDED_SHOULD_BE_IN_SERVICES: fn(Context) = |context: Context| {
    let services = context.method_responses("get_services");
    assert_eq!(
        services,
        json!([
            "ServiceName"
        ])
    );
};

pub static ALICE_INVOKES_ADD_SERVICE_IN_BMC: fn(Context) -> Context = |mut context: Context| {
    let signer = context.accounts().get("alice").to_owned();
    context.set_signer(&signer);
    BMC_CONTRACT.add_service(context)
};
