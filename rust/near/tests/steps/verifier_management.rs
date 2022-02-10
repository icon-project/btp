use super::*;
use libraries::types::{AccountId, Verifier};
use serde_json::{from_value, json};
use std::convert::TryFrom;
use test_helper::types::Context;

pub static BMC_OWNER_INVOKES_ADD_VERIFIER_IN_BMC: fn(Context) -> Context = |context: Context| {
    context
        .pipe(THE_TRANSACTION_IS_SIGNED_BY_BMC_OWNER)
        .pipe(USER_INVOKES_ADD_VERIFIER_IN_BMC)
};

pub static ICON_BMV_ACCOUNT_ID_AND_ICON_NETWORK_ADDRESS_ARE_PROVIDED_AS_ADD_VERIFIER_PARAM:
    fn(Context) -> Context = |mut context| {
    context.add_method_params(
        "add_verifier",
        json!({
            "network": ICON_NETWORK,
            "verifier": context.contracts().get("bmv").id()
        }),
    );
    context
};

pub static VERIFIER_FOR_ICON_IS_ADDED: fn(Context) -> Context = |context| {
    context
        .pipe(ICON_BMV_ACCOUNT_ID_AND_ICON_NETWORK_ADDRESS_ARE_PROVIDED_AS_ADD_VERIFIER_PARAM)
        .pipe(BMC_OWNER_INVOKES_ADD_VERIFIER_IN_BMC)
};

pub static BMC_SHOULD_THROW_USER_DOES_NOT_EXIST_ERROR_ON_REMOVING_OWNER: fn(Context) =
    |context: Context| {
        let error = context.method_errors("remove_owner");
        assert!(error.to_string().contains("BMCRevertNotExistsOwner"));
    };

pub static ICON_NETWORK_ADDRESS_AND_VERIFIER_ACCOUNT_ID_ARE_PROVIDED_AS_ADD_VERIFIER_PARAM:
    fn(Context) -> Context = |mut context: Context| {
    let address = context.contracts().get("bmv").id().clone();
    context.add_method_params(
        "add_verifier",
        json!({
            "network": "0x1.icon",
            "verifier": address,
        }),
    );
    context
};

pub static ALICE_INVOKES_ADD_VERIFIER_IN_BMC: fn(Context) -> Context = |context: Context| {
    context
        .pipe(THE_TRANSACTION_IS_SIGNED_BY_ALICE)
        .pipe(USER_INVOKES_ADD_VERIFIER_IN_BMC)
};

pub static THE_ADDED_VERIFIER_SHOULD_BE_IN_THE_LIST_OF_VERIFIERS: fn(Context) =
    |context: Context| {
        let context = context.pipe(USER_INVOKES_GET_VERIFIERS_IN_BMC);
        let verifiers = context.method_responses("get_verifiers");
        let result: HashSet<_> = from_value::<Vec<Verifier>>(verifiers)
            .unwrap()
            .into_iter()
            .collect();

        let expected: HashSet<Verifier> = vec![Verifier {
            network: ICON_NETWORK.to_string(),
            verifier: AccountId::try_from(context.contracts().get("bmv").id().to_string()).unwrap(),
        }]
        .into_iter()
        .collect();

        assert_eq!(result, expected);
    };

pub static CHUCK_INVOKES_ADD_VERIFIER_IN_BMC: fn(Context) -> Context = |context: Context| {
    context
        .pipe(THE_TRANSACTION_IS_SIGNED_BY_CHUCK)
        .pipe(USER_INVOKES_ADD_VERIFIER_IN_BMC)
};

pub static BMC_SHOULD_THROW_UNAUTHORIZED_ERROR_ON_ADDING_VERIFIER: fn(Context) =
    |context: Context| {
        let error = context.method_errors("add_verifier");
        assert!(error.to_string().contains("BMCRevertNotExistsPermission"));
    };

pub static USER_SHOULD_GET_THE_EXISITING_LIST_OF_VERIFIERS: fn(Context) = |context: Context| {
    let verifiers = context.method_responses("get_verifiers");
    let result: HashSet<_> = from_value::<Vec<Verifier>>(verifiers)
        .unwrap()
        .into_iter()
        .collect();
    assert!(result.len() > 0);
};

pub static BMC_SHOULD_THROW_VERIFIER_ALREADY_EXISTS_ERROR_ON_ADDING_VERIFIER: fn(Context) =
    |context: Context| {
        let error = context.method_errors("add_verifier");
        assert!(error.to_string().contains("BMCRevertAlreadyExistsBMV"));
    };

    pub static ICON_NETWORK_ADDRESS_IS_PROVIDED_AS_REMOVE_VERIFIER_PARAM: fn(Context) -> Context =
    |mut context: Context| {
        context.add_method_params(
            "remove_verifier",
            json!({
                "network":ICON_NETWORK
            }),
        );
        context
    };

    pub static ALICE_INVOKES_REMOVE_VERIFIER_IN_BMC: fn(Context) -> Context = |mut context: Context| {
        context.
        pipe(THE_TRANSACTION_IS_SIGNED_BY_ALICE)
        .pipe(USER_INVOKES_REMOVE_VERIFIER_IN_BMC)
    };

    pub static BMC_SHOULD_THROW_VERIFIER_DOES_NOT_EXISTS_ERROR_ON_REMOVING_VERIFIER: fn(Context) = |context: Context| {
        let error = context.method_errors("remove_verifier");
        assert!(error.to_string().contains("BMCRevertNotExistBMV"));
        };