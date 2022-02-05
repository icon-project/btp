use super::*;
use serde_json::{from_value, json};
use test_helper::types::Context;

pub static BMC_CONTRACT_IS_OWNED_BY_ALICE: fn(Context) -> Context = |mut context: Context| {
    let bmc_signer = context.contracts().get("bmc").to_owned();
    context.accounts_mut().add("alice", &bmc_signer);
    context
};

pub static BOBS_ACCOUNT_ID_IS_PROVIDED_AS_ADD_OWNER_PARAM: fn(Context) -> Context =
    |mut context: Context| {
        let bob = context.accounts().get("bob").to_owned();
        context.add_method_params(
            "add_owner",
            json!({
                "account": bob.account_id()
            }),
        );
        context
    };

pub static BMC_OWNER_INVOKES_ADD_OWNER_IN_BMC: fn(Context) -> Context =
    |context: Context| -> Context {
        context
            .pipe(TRANSACTION_IS_SIGNED_BY_BMC_OWNER)
            .pipe(USER_INVOKES_ADD_OWNER_IN_BMC)
    };

pub static CHARLIE_INVOKES_ADD_OWNER_IN_BMC: fn(Context) -> Context =
    |context: Context| -> Context {
        context
            .pipe(TRANSACTION_IS_SIGNED_BY_CHARLIE)
            .pipe(USER_INVOKES_ADD_OWNER_IN_BMC)
    };

pub static BMC_CONTRACT_IS_NOT_OWNED_BY_CHUCK: fn(Context) -> Context = |context: Context| context;

pub static CHARLIES_ACCOUNT_ID_IS_PROVIDED_AS_REMOVE_OWNER_PARAM: fn(Context) -> Context =
    |mut context: Context| {
        let charlie = context.accounts().get("charlie").to_owned();
        context.add_method_params(
            "remove_owner",
            json!({
                "account": charlie.account_id()
            }),
        );
        context
    };

pub static ALICE_INVOKES_REMOVE_OWNER_IN_BMC: fn(Context) -> Context = |context: Context| {
    context
        .pipe(TRANSACTION_IS_SIGNED_BY_ALICE)
        .pipe(USER_INVOKES_REMOVE_OWNER_IN_BMC)
};

pub static CHARLIES_ACCOUNT_ID_IS_PROVIDED_AS_ADD_OWNER_PARAM: fn(Context) -> Context =
    |mut context: Context| {
        let charlie = context.accounts().get("charlie").to_owned();
        context.add_method_params(
            "add_owner",
            json!({
                "account": charlie.account_id()
            }),
        );
        context
    };

pub static ALICE_INVOKES_ADD_OWNER_IN_BMC: fn(Context) -> Context = |context: Context| {
    context
        .pipe(TRANSACTION_IS_SIGNED_BY_ALICE)
        .pipe(USER_INVOKES_ADD_OWNER_IN_BMC)
};

pub static CHARLIE_IS_AN_EXISITNG_OWNER_IN_BMC: fn(Context) -> Context = |context: Context| {
    context
        .pipe(CHARLIES_ACCOUNT_IS_CREATED)
        .pipe(CHARLIES_ACCOUNT_ID_IS_PROVIDED_AS_ADD_OWNER_PARAM)
        .pipe(BMC_OWNER_INVOKES_ADD_OWNER_IN_BMC)
};

pub static CHARLIES_ACCOUNT_ID_SHOULD_BE_IN_OWNERS_LIST: fn(Context) = |context: Context| {
    let context = context.pipe(USER_INVOKES_GET_OWNERS_IN_BMC);

    let owners = context.method_responses("get_owners");
    let result: HashSet<_> = from_value::<Vec<String>>(owners)
        .unwrap()
        .into_iter()
        .collect();
    let expected: HashSet<_> = vec![
        context.accounts().get("alice").account_id().to_string(),
        context.accounts().get("charlie").account_id().to_string(),
    ]
    .into_iter()
    .collect();
    assert_eq!(result, expected);
};

pub static BOBS_ACCOUNT_ID_SHOULD_BE_IN_BMC_OWNERS_LIST: fn(Context) = |context: Context| {
    let context = context.pipe(USER_INVOKES_GET_OWNERS_IN_BMC);
    let owners = context.method_responses("get_owners");

    let result: HashSet<_> = from_value::<Vec<String>>(owners)
        .unwrap()
        .into_iter()
        .collect();
    let expected = context.accounts().get("bob").account_id().to_string();
    assert_eq!(result.contains(&expected), true);
};

pub static CHARLIES_ACCOUNT_ID_SHOULD_NOT_BE_IN_BMC_OWNERS_LIST: fn(Context) =
    |context: Context| {
        let context = context.pipe(USER_INVOKES_GET_OWNERS_IN_BMC);
        let owners = context.method_responses("get_owners");
        assert_eq!(
            owners,
            json!([context.accounts().get("alice").account_id()])
        );
    };
