use super::*;
use serde_json::{from_value, json};
use test_helper::types::Context;

crate::user_call!(ADD_OWNER_IN_BMC; BMC_OWNER, ALICE, CHUCK, CHARLIE);
crate::user_call!(REMOVE_OWNER_IN_BMC; BMC_OWNER, ALICE, CHUCK, CHARLIE);
crate::user_call!(ADD_OWNER_IN_NATIVE_COIN_BSH; NATIVE_COIN_BSH_OWNER, ALICE, BOB, CHUCK, CHARLIE);
crate::user_call!(REMOVE_OWNER_IN_NATIVE_COIN_BSH; NATIVE_COIN_BSH_OWNER, ALICE, BOB, CHUCK, CHARLIE);
crate::user_call!(ADD_OWNER_IN_TOKEN_BSH; TOKEN_BSH_OWNER, ALICE, BOB, CHUCK, CHARLIE);
crate::user_call!(REMOVE_OWNER_IN_TOKEN_BSH; TOKEN_BSH_OWNER, ALICE, BOB, CHUCK, CHARLIE);

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

pub static BMC_SHOULD_THROW_UNAUTHORISED_ERROR_ON_REMOVING_OWNER: fn(Context) =
    |context: Context| {
        let error = context.method_errors("remove_owner");
        assert!(error.contains("BMCRevertNotExistsPermission"));
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

pub static BMC_SHOULD_THROW_UNAUTHORISED_ERROR_ON_ADDING_OWNER: fn(Context) = |context: Context| {
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

pub static BMC_SHOULD_THROW_LAST_OWNER_ERROR_ON_REMOVING_OWNER: fn(Context) = |context: Context| {
    let error = context.method_errors("remove_owner");
    assert!(error.to_string().contains("BMCRevertLastOwner"));
};

pub static BMC_SHOULD_THROW_OWNER_ALREADY_EXISTS_ERROR_ON_ADDING_OWNER: fn(Context) =
    |context: Context| {
        let error = context.method_errors("add_owner");
        assert!(error.to_string().contains("BMCRevertAlreadyExistsOwner"));
    };

pub static ALICES_ACCOUNT_ID_SHOULD_NOT_BE_IN_THE_LIST_OF_BMC_OWNERS: fn(Context) =
    |context: Context| {
        let context = context.pipe(USER_INVOKES_GET_OWNERS_IN_BMC);
        let owners = context.method_responses("get_owners");
        assert_eq!(owners, json!([context.accounts().get("charlie").id()]));
    };

pub static BMC_SHOULD_THROW_OWNER_DOES_NOT_EXIST_ERROR_ON_REMOVING_OWNER: fn(Context) =
    |context: Context| {
        let error = context.method_errors("remove_owner");
        println!("{:?}", error);
        assert!(error.to_string().contains("BMCRevertNotExistsOwner"));
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

pub static ALICES_ACCOUNT_ID_SHOULD_BE_ADDED_TO_THE_LIST_OF_NATIVE_COIN_BSH_OWNERS: fn(Context) =
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

pub static ALICES_ACCOUNT_ID_SHOULD_BE_ADDED_TO_THE_LIST_OF_TOKEN_BSH_OWNERS: fn(Context) =
    |context: Context| {
        let context = context.pipe(USER_INVOKES_GET_OWNERS_IN_TOKEN_BSH);
        let owners = context.method_responses("get_owners");

        let result: HashSet<_> = from_value::<Vec<String>>(owners)
            .unwrap()
            .into_iter()
            .collect();
        let expected = context.accounts().get("alice").id().to_string();
        assert_eq!(result.contains(&expected), true);
    };

pub static CHARLIES_ACCOUNT_ID_SHOULD_BE_ADDED_TO_THE_LIST_OF_NATIVE_COIN_BSH_OWNERS: fn(Context) =
    |context: Context| {
        let context = context.pipe(USER_INVOKES_GET_OWNERS_IN_NATIVE_COIN_BSH);
        let owners = context.method_responses("get_owners");

        let result: HashSet<_> = from_value::<Vec<String>>(owners)
            .unwrap()
            .into_iter()
            .collect();
        let expected = context.accounts().get("charlie").id().to_string();
        assert_eq!(result.contains(&expected), true);
    };

pub static CHARLIES_ACCOUNT_ID_SHOULD_BE_ADDED_TO_THE_LIST_OF_TOKEN_BSH_OWNERS: fn(Context) =
    |context: Context| {
        let context = context.pipe(USER_INVOKES_GET_OWNERS_IN_TOKEN_BSH);
        let owners = context.method_responses("get_owners");

        let result: HashSet<_> = from_value::<Vec<String>>(owners)
            .unwrap()
            .into_iter()
            .collect();
        let expected = context.accounts().get("charlie").id().to_string();
        assert_eq!(result.contains(&expected), true);
    };

pub static CHARLIE_IS_AN_EXISTING_OWNER_IN_NATIVE_COIN_BSH: fn(Context) -> Context =
    |context: Context| {
        context
            .pipe(CHARLIES_ACCOUNT_IS_CREATED)
            .pipe(CHARLIES_ACCOUNT_ID_IS_PROVIDED_AS_ADD_OWNER_PARAM)
            .pipe(NATIVE_COIN_BSH_OWNER_INVOKES_ADD_OWNER_IN_NATIVE_COIN_BSH)
    };

pub static CHARLIE_WAS_AN_OWNER_IN_NATIVE_COIN_BSH: fn(Context) -> Context = |context: Context| {
    context
        .pipe(CHARLIES_ACCOUNT_IS_CREATED)
        .pipe(CHARLIES_ACCOUNT_ID_IS_PROVIDED_AS_ADD_OWNER_PARAM)
        .pipe(NATIVE_COIN_BSH_OWNER_INVOKES_ADD_OWNER_IN_NATIVE_COIN_BSH)
        .pipe(CHARLIES_ACCOUNT_ID_IS_PROVIDED_AS_REMOVE_OWNER_PARAM)
        .pipe(NATIVE_COIN_BSH_OWNER_INVOKES_REMOVE_OWNER_IN_NATIVE_COIN_BSH)
};

pub static CHARLIES_ACCOUNT_ID_SHOULD_NOT_BE_IN_THE_LIST_OF_NATIVE_COIN_BSH_OWNERS: fn(Context) =
    |context: Context| {
        let context = context.pipe(USER_INVOKES_GET_OWNERS_IN_NATIVE_COIN_BSH);
        let owners = context.method_responses("get_owners");
        assert_eq!(owners, json!([context.accounts().get("bob").id()]));
    };

pub static BOBS_ACCOUNT_ID_IS_PROVIDED_AS_REMOVE_OWNER_PARAM: fn(Context) -> Context =
    |mut context: Context| {
        let bob = context.accounts().get("bob").to_owned();
        context.add_method_params(
            "remove_owner",
            json!({
                "account": bob.id()
            }),
        );
        context
    };

pub static BOBS_ACCOUNT_ID_SHOULD_NOT_BE_IN_THE_LIST_OF_NATIVE_COIN_BSH_OWNERS: fn(Context) =
    |context: Context| {
        let context = context.pipe(USER_INVOKES_GET_OWNERS_IN_NATIVE_COIN_BSH);
        let owners = context.method_responses("get_owners");
        assert_eq!(owners, json!([context.accounts().get("charlie").id()]));
    };

pub static NATIVE_COIN_BSH_SHOULD_THROW_UNAUTHORISED_ERROR_ON_ADDING_OWNER: fn(Context) =
    |context: Context| {
        let error = context.method_errors("add_owner");
        assert!(error.contains("BSHRevertNotExistsPermission"));
    };

pub static TOKEN_BSH_SHOULD_THROW_UNAUTHORISED_ERROR_ON_ADDING_OWNER: fn(Context) =
    |context: Context| {
        let error = context.method_errors("add_owner");
        assert!(error.contains("BSHRevertNotExistsPermission"));
    };

pub static NATIVE_COIN_BSH_SHOULD_THROW_UNAUTHORISED_ERROR_ON_REMOVING_OWNER: fn(Context) =
    |context: Context| {
        let error = context.method_errors("remove_owner");
        assert!(error.contains("BSHRevertNotExistsPermission"));
    };

pub static TOKEN_BSH_SHOULD_THROW_UNAUTHORISED_ERROR_ON_REMOVING_OWNER: fn(Context) =
    |context: Context| {
        let error = context.method_errors("remove_owner");
        assert!(error.contains("BSHRevertNotExistsPermission"));
    };

pub static NATIVE_COIN_BSH_SHOULD_THROW_OWNER_DOES_NOT_EXIST_ERROR_ON_REMOVING_OWNER: fn(Context) =
    |context: Context| {
        let error = context.method_errors("remove_owner");
        assert!(error.contains("BSHRevertNotExistsOwner"));
    };

pub static TOKEN_BSH_SHOULD_THROW_OWNER_DOES_NOT_EXIST_ERROR_ON_REMOVING_OWNER: fn(Context) =
    |context: Context| {
        let error = context.method_errors("remove_owner");
        assert!(error.contains("BSHRevertNotExistsOwner"));
    };

pub static NATIVE_COIN_BSH_SHOULD_THROW_OWNER_ALREADY_EXIST_ERROR_ON_REMOVING_OWNER: fn(Context) =
    |context: Context| {
        let error = context.method_errors("add_owner");
        assert!(error.contains("BSHRevertAlreadyExistsOwner"));
    };

pub static TOKEN_BSH_SHOULD_THROW_OWNER_ALREADY_EXIST_ERROR_ON_REMOVING_OWNER: fn(Context) =
    |context: Context| {
        let error = context.method_errors("add_owner");
        assert!(error.contains("BSHRevertAlreadyExistsOwner"));
    };

pub static NATIVE_COIN_BSH_SHOULD_THROW_LAST_OWNER_ERROR_ON_REMOVING_OWNER: fn(Context) =
    |context: Context| {
        let error = context.method_errors("remove_owner");
        assert!(error.to_string().contains("BSHRevertLastOwner"));
    };

pub static TOKEN_BSH_SHOULD_THROW_LAST_OWNER_ERROR_ON_REMOVING_OWNER: fn(Context) =
    |context: Context| {
        let error = context.method_errors("remove_owner");
        assert!(error.to_string().contains("BSHRevertLastOwner"));
    };

pub static CHARLIE_IS_AN_EXISTING_OWNER_IN_TOKEN_BSH: fn(Context) -> Context =
    |context: Context| {
        context
            .pipe(CHARLIES_ACCOUNT_IS_CREATED)
            .pipe(CHARLIES_ACCOUNT_ID_IS_PROVIDED_AS_ADD_OWNER_PARAM)
            .pipe(TOKEN_BSH_OWNER_INVOKES_ADD_OWNER_IN_TOKEN_BSH)
    };

pub static CHARLIE_WAS_AN_OWNER_IN_TOKEN_BSH: fn(Context) -> Context = |context: Context| {
    context
        .pipe(CHARLIES_ACCOUNT_IS_CREATED)
        .pipe(CHARLIES_ACCOUNT_ID_IS_PROVIDED_AS_ADD_OWNER_PARAM)
        .pipe(TOKEN_BSH_OWNER_INVOKES_ADD_OWNER_IN_TOKEN_BSH)
        .pipe(CHARLIES_ACCOUNT_ID_IS_PROVIDED_AS_REMOVE_OWNER_PARAM)
        .pipe(TOKEN_BSH_OWNER_INVOKES_REMOVE_OWNER_IN_TOKEN_BSH)
};

pub static CHARLIES_ACCOUNT_ID_SHOULD_NOT_BE_IN_THE_LIST_OF_TOKEN_BSH_OWNERS: fn(Context) =
    |context: Context| {
        let context = context.pipe(USER_INVOKES_GET_OWNERS_IN_TOKEN_BSH);
        let owners = context.method_responses("get_owners");
        assert_eq!(owners, json!([context.accounts().get("bob").id()]));
    };

pub static BOBS_ACCOUNT_ID_SHOULD_NOT_BE_IN_THE_LIST_OF_TOKEN_BSH_OWNERS: fn(Context) =
    |context: Context| {
        let context = context.pipe(USER_INVOKES_GET_OWNERS_IN_TOKEN_BSH);
        let owners = context.method_responses("get_owners");
        assert_eq!(owners, json!([context.accounts().get("charlie").id()]));
    };

pub static NEP141_IS_OWNED_BY_ALICE: fn(Context) -> Context = |mut context: Context| {
    let nep141_signer = context.contracts().get("nep141").as_account().clone();
    context.accounts_mut().add("alice", nep141_signer);
    context
};
