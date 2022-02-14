use super::*;
use serde_json::{from_value, json};
use test_helper::types::Context;

pub static BMC_CONTRACT_IS_OWNED_BY_ALICE: fn(Context) -> Context = |mut context: Context| {
    let bmc_signer = context.contracts().get("bmc").as_account().clone();
    context.accounts_mut().add("alice", bmc_signer);
    context
};

pub static BOBS_ACCOUNT_ID_IS_PROVIDED_AS_ADD_OWNER_PARAM: fn(Context) -> Context =
    |mut context: Context| {
        let bob = context.accounts().get("bob").to_owned();
        context.add_method_params(
            "add_owner",
            json!({
                "account": bob.id()
            }),
        );
        context
    };

pub static BMC_OWNER_INVOKES_ADD_OWNER_IN_BMC: fn(Context) -> Context =
    |context: Context| -> Context {
        context
            .pipe(THE_TRANSACTION_IS_SIGNED_BY_BMC_OWNER)
            .pipe(USER_INVOKES_ADD_OWNER_IN_BMC)
    };

pub static CHARLIE_INVOKES_ADD_OWNER_IN_BMC: fn(Context) -> Context =
    |context: Context| -> Context {
        context
            .pipe(THE_TRANSACTION_IS_SIGNED_BY_CHARLIE)
            .pipe(USER_INVOKES_ADD_OWNER_IN_BMC)
    };

pub static BMC_CONTRACT_IS_NOT_OWNED_BY_CHUCK: fn(Context) -> Context = |context: Context| context;

pub static CHARLIES_ACCOUNT_ID_IS_PROVIDED_AS_REMOVE_OWNER_PARAM: fn(Context) -> Context =
    |mut context: Context| {
        let charlie = context.accounts().get("charlie").to_owned();
        context.add_method_params(
            "remove_owner",
            json!({
                "account": charlie.id()
            }),
        );
        context
    };

pub static ALICE_INVOKES_REMOVE_OWNER_IN_BMC: fn(Context) -> Context = |context: Context| {
    context
        .pipe(THE_TRANSACTION_IS_SIGNED_BY_ALICE)
        .pipe(USER_INVOKES_REMOVE_OWNER_IN_BMC)
};

pub static CHARLIES_ACCOUNT_ID_IS_PROVIDED_AS_ADD_OWNER_PARAM: fn(Context) -> Context =
    |mut context: Context| {
        let charlie = context.accounts().get("charlie").to_owned();
        context.add_method_params(
            "add_owner",
            json!({
                "account": charlie.id()
            }),
        );
        context
    };

pub static ALICE_INVOKES_ADD_OWNER_IN_BMC: fn(Context) -> Context = |context: Context| {
    context
        .pipe(THE_TRANSACTION_IS_SIGNED_BY_ALICE)
        .pipe(USER_INVOKES_ADD_OWNER_IN_BMC)
};

pub static CHARLIE_IS_AN_EXISITNG_OWNER_IN_BMC: fn(Context) -> Context = |context: Context| {
    context
        .pipe(CHARLIES_ACCOUNT_IS_CREATED)
        .pipe(CHARLIES_ACCOUNT_ID_IS_PROVIDED_AS_ADD_OWNER_PARAM)
        .pipe(BMC_OWNER_INVOKES_ADD_OWNER_IN_BMC)
};

pub static CHARLIES_ACCOUNT_ID_SHOULD_BE_IN_THE_LIST_OF_BMC_OWNERS: fn(Context) =
    |context: Context| {
        let context = context.pipe(USER_INVOKES_GET_OWNERS_IN_BMC);

        let owners = context.method_responses("get_owners");
        let result: HashSet<_> = from_value::<Vec<String>>(owners)
            .unwrap()
            .into_iter()
            .collect();
        let expected: HashSet<_> = vec![
            context.accounts().get("alice").id().to_string(),
            context.accounts().get("charlie").id().to_string(),
        ]
        .into_iter()
        .collect();
        assert_eq!(result, expected);
    };

pub static BOBS_ACCOUNT_ID_SHOULD_BE_IN_THE_LIST_OF_BMC_OWNERS: fn(Context) = |context: Context| {
    let context = context.pipe(USER_INVOKES_GET_OWNERS_IN_BMC);
    let owners = context.method_responses("get_owners");

    let result: HashSet<_> = from_value::<Vec<String>>(owners)
        .unwrap()
        .into_iter()
        .collect();
    let expected = context.accounts().get("bob").id().to_string();
    assert_eq!(result.contains(&expected), true);
};

pub static CHARLIES_ACCOUNT_ID_SHOULD_NOT_BE_IN_THE_LIST_OF_BMC_OWNERS: fn(Context) =
    |context: Context| {
        let context = context.pipe(USER_INVOKES_GET_OWNERS_IN_BMC);
        let owners = context.method_responses("get_owners");
        assert_eq!(owners, json!([context.accounts().get("alice").id()]));
    };

pub static CHUCK_INVOKES_ADD_OWNER_IN_BMC: fn(Context) -> Context = |context: Context| {
    context
        .pipe(THE_TRANSACTION_IS_SIGNED_BY_CHUCK)
        .pipe(USER_INVOKES_ADD_OWNER_IN_BMC)
};

pub static BMC_SHOULD_THROW_UNAUTHORISED_ERROR_ON_ADDING_OWNERS: fn(Context) =
    |context: Context| {
        let error = context.method_errors("add_owner");
        assert!(error.to_string().contains("BMCRevertNotExistsPermission"));
    };

pub static ALICES_ACCOUNT_ID_IS_PROVIDED_AS_REMOVE_OWNER_PARAM: fn(Context) -> Context =
    |mut context: Context| {
        let alice = context.accounts().get("alice").to_owned();
        context.add_method_params(
            "remove_owner",
            json!({
                "account": alice.id()
            }),
        );
        context
    };

pub static BMC_SHOULD_THROW_LAST_OWNER_ERROR_ON_REMOVING_OWNERS: fn(Context) =
    |context: Context| {
        let error = context.method_errors("remove_owner");
        assert!(error.to_string().contains("BMCRevertLastOwner"));
    };

pub static BMC_SHOULD_THROW_OWNER_ALREADY_EXISTS_ERROR_ON_ADDING_OWNERS: fn(Context) =
    |context: Context| {
        let error = context.method_errors("add_owner");
        assert!(error.to_string().contains("BMCRevertAlreadyExistsOwner"));
    };

pub static CHARLIE_INVOKES_REMOVE_OWNER_IN_BMC: fn(Context) -> Context = |context: Context| {
    context
        .pipe(THE_TRANSACTION_IS_SIGNED_BY_CHARLIE)
        .pipe(USER_INVOKES_REMOVE_OWNER_IN_BMC)
};

pub static ALICES_ACCOUNT_ID_SHOULD_NOT_BE_IN_THE_LIST_OF_BMC_OWNERS: fn(Context) =
    |context: Context| {
        let context = context.pipe(USER_INVOKES_GET_OWNERS_IN_BMC);
        let owners = context.method_responses("get_owners");
        assert_eq!(owners, json!([context.accounts().get("charlie").id()]));
    };

pub static BMC_SHOULD_THROW_OWNER_DOES_NOT_EXIST_ON_REMOVING_OWNERS: fn(Context) =
    |context: Context| {
        let error = context.method_errors("remove_owner");
        assert!(error.to_string().contains("BMCRevertNotExistsOwner"));
    };

pub static CHARLIES_ACCOUNT_IS_CREATED_AND_ACCOUNT_ID_IS_PROVIDED_AS_ADD_OWNER_PARAM:
    fn(Context) -> Context = |context: Context| {
    context
        .pipe(CHARLIES_ACCOUNT_IS_CREATED)
        .pipe(CHARLIES_ACCOUNT_ID_IS_PROVIDED_AS_ADD_OWNER_PARAM)
};

pub static ALICES_ACCOUNT_ID_IS_PROVIDED_AS_ADD_OWNER_PARAM: fn(Context) -> Context =
    |mut context: Context| {
        let alice = context.accounts().get("alice").to_owned();
        context.add_method_params(
            "add_owner",
            json!({
                "account": alice.id()
            }),
        );
        context
    };

pub static CHARLIE_INVOKES_ADD_OWNER_IN_NATIVE_COIN_BSH: fn(Context) -> Context =
    |context: Context| -> Context {
        context
            .pipe(THE_TRANSACTION_IS_SIGNED_BY_CHARLIE)
            .pipe(USER_INVOKES_ADD_OWNER_IN_NATIVE_COIN_BSH)
    };

pub static ALICE_ACCOUNT_ID_SHOULD_BE_IN_THE_LIST_OF_NATIVE_COIN_OWNERS: fn(Context) =
    |context: Context| {
        let context = context.pipe(USER_INVOKES_GET_OWNERS_IN_NATIVE_COIN_BSH);
        let owners = context.method_responses("get_owners");

        let result: HashSet<_> = from_value::<Vec<String>>(owners)
            .unwrap()
            .into_iter()
            .collect();
        let expected = context.accounts().get("alice").id().to_string();
        assert_eq!(result.contains(&expected), true);
    };

pub static CHARLIE_IS_AN_EXISTING_OWNER_IN_NATIVE_COIN_BSH: fn(Context) -> Context =
    |context: Context| {
        context
            .pipe(CHARLIES_ACCOUNT_IS_CREATED)
            .pipe(CHARLIES_ACCOUNT_ID_IS_PROVIDED_AS_ADD_OWNER_PARAM)
            .pipe(NATIVE_COIN_BSH_OWNER_INVOKES_ADD_OWNER_IN_NATIVE_COIN_BSH)
    };

pub static NATIVE_COIN_BSH_OWNER_INVOKES_ADD_OWNER_IN_NATIVE_COIN_BSH: fn(Context) -> Context =
    |context: Context| -> Context {
        context
            .pipe(THE_TRANSACTION_IS_SIGNED_BY_NATIVE_COIN_BSH_OWNER)
            .pipe(USER_INVOKES_ADD_OWNER_IN_NATIVE_COIN_BSH)
    };

pub static BOB_INVOKES_REMOVE_OWNER_IN_NATIVE_COIN_BSH: fn(Context) -> Context =
    |context: Context| -> Context {
        context
            .pipe(THE_TRANSACTION_IS_SIGNED_BY_BOB)
            .pipe(USER_INVOKES_REMOVE_OWNER_IN_NATIVE_COIN_BSH)
    };

pub static CHARLIES_ACCOUNT_ID_SHOULD_NOT_BE_IN_THE_LIST_OF_NATIVE_COIN_OWNERS: fn(Context) =
    |context: Context| {
        let context = context.pipe(USER_INVOKES_GET_OWNERS_IN_NATIVE_COIN_BSH);
        let owners = context.method_responses("get_owners");
        assert_eq!(owners, json!([context.accounts().get("bob").id()]));
    };
