use super::*;
use super::{BMC_CONTRACT, BMV_CONTRACT, BSH_CONTRACT};
use serde_json::{from_value, json};
use test_helper::types::Context;

pub static VERIFIER_NETWORKADDRESS_AND_VERIFIER_ADDRESS_PROVIDED_AS_ADD_VERIFIER_PARAM:
    fn(Context) -> Context = |mut context: Context| {
    let address = context.contracts().get("bmv").account_id();
    context.add_method_params(
        "add_verifier",
        json!({
            "net": "NETWORK_ADDRESS",
            "addr": address
        }),
    );
    context
};

pub static VERIFIER_NETWORKADDRESS_AND_VERIFIER_ADDRESS_PROVIDED_AS_ADD_VERIFEIR_PARAM_AGAIN:
    fn(Context) -> Context = |mut context: Context| {
    let address = context.contracts().get("bmv").account_id();
    context.add_method_params(
        "add_verifier",
        json!({
            "net": "NETWORK_ADDRESS",
            "addr": address
        }),
    );
    context
};

pub static VERIFIER_NETWORKADDRESS_AND_VERIFIER_INVALID_ADDRESS_PROVIDED_AS_ADD_VERIFEIR_PARAM:
    fn(Context) -> Context = |mut context: Context| {
    let address = context.contracts().get("bmv").to_owned();
    context.add_method_params(
        "add_verifier",
        json!({
            "net": "NETWORK_ADDRESS",
            "addr": "invalidaddress"
        }),
    );
    context
};

pub static VERIFER_ADDRESS_PROVIDED_AS_REMOVE_VERIFIER_PARAM: fn(Context) -> Context =
    |mut context: Context| {
        context.add_method_params(
            "remove_verifier",
            json!({
                "net":"NETWORK_ADDRESS"
            }),
        );
        context
    };

pub static VERIFER_ADDRESS_PROVIDED_AS_REMOVE_VERIFIER_PARAM_AGAIN: fn(Context) -> Context =
    |mut context: Context| {
        context.add_method_params(
            "remove_verifier",
            json!({
                "net":"NETWORK_ADDRESS"
            }),
        );
        context
    };

pub static ALICE_INVOKES_ADD_VERIFIER_IN_BMC: fn(Context) -> Context = |mut context: Context| {
    let signer = context.accounts().get("alice").to_owned();
    context.set_signer(&signer);
    BMC_CONTRACT.add_verifier(context)
};
pub static CHUCK_INVOKES_ADD_VERIFIER_IN_BMC: fn(Context) -> Context = |mut context: Context| {
    let signer = context.accounts().get("chucks").to_owned();
    context.set_signer(&signer);
    BMC_CONTRACT.add_verifier(context)
};

pub static VERIFIER_IS_ADDED_BY_NON_BMC_OWNER: fn(Context) -> Context = |mut context: Context| {
    CHUCKS_ACCOUNT_IS_CREATED(context)
        .pipe(CHUCK_IS_NOT_A_BMC_OWNER)
        .pipe(VERIFIER_NETWORKADDRESS_AND_VERIFIER_ADDRESS_PROVIDED_AS_ADD_VERIFIER_PARAM)
        .pipe(CHUCK_INVOKES_ADD_VERIFIER_IN_BMC)
};
pub static EXISTING_VERIFIER_IS_ADDED_AGAIN_BY_BMC_OWNER: fn(Context) -> Context =
    |mut context: Context| {
        ALICE_IS_BMC_CONTRACT_OWNER(context)
            .pipe(VERIFIER_NETWORKADDRESS_AND_VERIFIER_ADDRESS_PROVIDED_AS_ADD_VERIFIER_PARAM)
            .pipe(ALICE_INVOKES_ADD_VERIFIER_IN_BMC)
            .pipe(VERIFER_ADDRESS_PROVIDED_AS_REMOVE_VERIFIER_PARAM_AGAIN)
            .pipe(ALICE_INVOKES_ADD_VERIFIER_IN_BMC)
    };

pub static VERIFIER_IS_ADDED_BY_BMC_CONTRACT_OWNER: fn(Context) -> Context =
    |mut context: Context| {
        ALICE_IS_BMC_CONTRACT_OWNER(context)
            .pipe(VERIFIER_NETWORKADDRESS_AND_VERIFIER_ADDRESS_PROVIDED_AS_ADD_VERIFIER_PARAM)
            .pipe(ALICE_INVOKES_ADD_VERIFIER_IN_BMC)
    };

pub static VERIFIERS_IN_BMC_ARE_QUERIED: fn(Context) -> Context =
    |context: Context| BMC_CONTRACT.get_verifiers(context);

pub static ADDED_VERIFIER_SHOULD_BE_IN_LIST_OF_VERIFIERS: fn(Context) = |context: Context| {
    let verifiers = context.method_responses("get_verifiers");
    let result: HashSet<_> = from_value::<Vec<String>>(verifiers)
        .unwrap()
        .into_iter()
        .collect();
    let expected: HashSet<_> = vec!["address".to_string()].into_iter().collect();
    assert_eq!(result, expected);
};

pub static REMOVE_VERIFER_INOKED_BY_BMC_OWNER: fn(Context) -> Context = |mut context: Context| {
    ALICE_IS_BMC_CONTRACT_OWNER(context)
        .pipe(VERIFIER_NETWORKADDRESS_AND_VERIFIER_ADDRESS_PROVIDED_AS_ADD_VERIFIER_PARAM)
        .pipe(ALICE_INVOKES_ADD_VERIFIER_IN_BMC)
        .pipe(VERIFER_ADDRESS_PROVIDED_AS_REMOVE_VERIFIER_PARAM)
        .pipe(ALICE_INVOKES_REMOVE_VERIFIER_IN_BMC)
};

pub static REMOVE_VERIFER_INOKED_BY_NON_BMC_OWNER: fn(Context) -> Context =
    |mut context: Context| {
        ALICE_IS_BMC_CONTRACT_OWNER(context)
            .pipe(VERIFIER_NETWORKADDRESS_AND_VERIFIER_ADDRESS_PROVIDED_AS_ADD_VERIFIER_PARAM)
            .pipe(ALICE_INVOKES_ADD_VERIFIER_IN_BMC)
            .pipe(VERIFER_ADDRESS_PROVIDED_AS_REMOVE_VERIFIER_PARAM)
            .pipe(CHUCK_IS_NOT_A_BMC_OWNER)
            .pipe(CHUCK_INVOKES_REMOVE_VERIFIER_IN_BMC)
    };

pub static ALICE_INVOKES_REMOVE_VERIFIER_IN_BMC: fn(Context) -> Context = |mut context: Context| {
    let signer = context.accounts().get("alice").to_owned();
    context.set_signer(&signer);
    BMC_CONTRACT.remove_verifier(context)
};

pub static CHUCK_INVOKES_REMOVE_VERIFIER_IN_BMC: fn(Context) -> Context = |mut context: Context| {
    let signer = context.accounts().get("chuck").to_owned();
    context.set_signer(&signer);
    BMC_CONTRACT.remove_verifier(context)
};

pub static VERIFIER_DELETED_SHOULD_NOT_BE_IN_LIST_OF_VERIFIERS: fn(Context) =
    |mut context: Context| {
        let verifiers = context.method_responses("get_verifiers");
        let result: HashSet<_> = from_value::<Vec<String>>(verifiers)
            .unwrap()
            .into_iter()
            .collect();
        let expected: HashSet<_> = vec!["address".to_string()].into_iter().collect();
        assert_ne!(result, expected);
    };

pub static ADD_VERIFIER_INVOKED_WITH_INVALID_VERIFIER_ADDRESS: fn(Context) -> Context = |mut context: Context| {
    unimplemented!();
    context
};