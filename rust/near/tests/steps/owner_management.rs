use super::*;
use super::{BMC_CONTRACT};
use serde_json::{from_value, json};
use test_helper::types::Context;

// * * * * * * * * * * * * * *
// * * * * * * * * * * * * * *
// *   Common Steps  * * * * *
// * * * * * * * * * * * * * *
// * * * * * * * * * * * * * *

pub static CHARLIES_ACCOUNT_ID_SHOULD_BE_IN_OWNERS_LIST: fn(Context) = |context: Context| {
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

pub static CHARLIES_ACCOUNT_ID_SHOULD_BE_IN_BMC_OWNERS_LIST: fn(Context) = |context: Context| {
    let owners = context.method_responses("get_owners");

    let result: HashSet<_> = from_value::<Vec<String>>(owners)
        .unwrap()
        .into_iter()
        .collect();
    let expected = context.accounts().get("charlie").account_id().to_string();
    assert_eq!(result.contains(&expected), true);
};

pub static CHARLIES_ACCOUNT_ID_SHOULD_NOT_BE_IN_BMC_OWNERS_LIST: fn(Context) = |context: Context| {
    let owners = context.method_responses("get_owners");
    assert_eq!(
        owners,
        json!([context.accounts().get("alice").account_id()])
    );
};

// * * * * * * * * * * * * * *
// * * * * * * * * * * * * * *
// *   BMC Common Steps  * * *
// * * * * * * * * * * * * * *
// * * * * * * * * * * * * * *

pub static ALICE_IS_BMC_CONTRACT_OWNER: fn(Context) -> Context = |mut context: Context| {
    let bmc_signer = context.contracts().get("bmc").to_owned();
    context.accounts_mut().add("alice", &bmc_signer);
    context
};

pub static CHUCK_IS_NOT_A_BMC_OWNER: fn(Context) -> Context = |context: Context| {
    context.pipe(CHUCKS_ACCOUNT_IS_CREATED)
};

pub static OWNERS_IN_BMC_ARE_QUERIED: fn(Context) -> Context =
    |context: Context| BMC_CONTRACT.get_owners(context);

pub static CHARLIE_IS_AN_EXISITNG_OWNER_IN_BMC: fn(Context) -> Context = |mut context: Context| {
    context.pipe(BMC_OWNER_IS_THE_SIGNER)
        .pipe(CHARLIES_ACCOUNT_IS_CREATED_AND_PASSED_AS_ADD_OWNER_PARAM)
        .pipe(USER_INVOKES_ADD_OWNER_IN_BMC)
        .pipe(USER_INVOKES_GET_OWNER_IN_BMC)
};

pub static BMC_SHOULD_THROW_UNAUTHORIZED_ERROR: fn(Context) = |context: Context| {
    let error = context.method_errors("add_owner");
    assert!(error.to_string().contains("BMCRevertNotExistsPermission"));
};

pub static BMC_SHOULD_THROW_LASTOWNER_ERROR: fn(Context) -> Context = |context: Context| context;
pub static BMC_SHOULD_THROW_NOTEXIST_ERROR: fn(Context) -> Context = |context: Context| context;
pub static BMC_SHOULD_THROW_ALREADY_EXIST_ERROR: fn(Context) -> Context = |context: Context| context;

// * * * * * * * * * * * * * *
// * * * * * * * * * * * * * *
// *   BMC Add Owner * * * * *
// * * * * * * * * * * * * * *
// * * * * * * * * * * * * * *

pub static CHARLIES_ACCOUNT_IS_CREATED_AND_PASSED_AS_ADD_OWNER_PARAM: fn(Context) -> Context =
    |context: Context| {
        context
            .pipe(CHARLIES_ACCOUNT_IS_CREATED)
            .pipe(CHARLIES_ACCOUNT_ID_IS_PROVIDED_AS_ADD_OWNER_PARAM)
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

pub static BOBS_ACCOUNT_IS_CREATED_AND_PASSED_AS_ADD_OWNER_PARAM: fn(Context) -> Context =
    |context: Context| {
        context
            .pipe(BOBS_ACCOUNT_IS_CREATED)
            .pipe(BOBS_ACCOUNT_ID_IS_PROVIDED_AS_ADD_OWNER_PARAM)
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

pub static ALICE_INVOKES_ADD_OWNER_IN_BMC: fn(Context) -> Context = |mut context: Context| {
    context.pipe(ALICE_IS_THE_SIGNER)
        .pipe(USER_INVOKES_ADD_OWNER_IN_BMC)
        .pipe(USER_INVOKES_GET_OWNER_IN_BMC)
        
};

pub static BOB_INVOKES_ADD_OWNER_IN_BMC: fn(Context) -> Context = |mut context: Context| {
    context.pipe(BOB_IS_THE_SIGNER)
        .pipe(USER_INVOKES_ADD_OWNER_IN_BMC)
        .pipe(USER_INVOKES_GET_OWNER_IN_BMC)
        
};

pub static CHUCK_INVOKES_ADD_OWNER_IN_BMC: fn(Context) -> Context = |mut context: Context| {
    context.pipe(CHUCK_IS_THE_SIGNER) 
        .pipe(USER_INVOKES_ADD_OWNER_IN_BMC) 
        .pipe(USER_INVOKES_GET_OWNER_IN_BMC)
};

// * * * * * * * * * * * * * *
// * * * * * * * * * * * * * *
// *   BMC Remove Owner  * * *
// * * * * * * * * * * * * * *
// * * * * * * * * * * * * * *

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

pub static ALICE_ACCOUNT_ID_IS_PROVIDED_AS_REMOVE_OWNER_PARAM: fn(Context) -> Context =
    |mut context: Context| {
        let alice = context.accounts().get("alice").to_owned();
        context.add_method_params(
            "remove_owner",
            json!({
                "account_id": alice.account_id()
            }),
        );
        context
    };

pub static ALICE_INVOKES_REMOVE_OWNER_IN_BMC: fn(Context) -> Context = |mut context: Context| {
    context.pipe(ALICE_IS_THE_SIGNER)
        .pipe(USER_INVOKES_REMOVE_OWNER_IN_BMC)
        .pipe(USER_INVOKES_GET_OWNER_IN_BMC)
};

pub static CHUCK_INVOKES_REMOVE_OWNER_IN_BMC: fn(Context) -> Context = |mut context: Context| {
    context.pipe(CHUCK_IS_THE_SIGNER) 
        .pipe(USER_INVOKES_REMOVE_OWNER_IN_BMC) 
        .pipe(USER_INVOKES_GET_OWNER_IN_BMC)
};


// * * * * * * * * * * * * * *
// * * * * * * * * * * * * * *
// *   BSH Common Steps  * * *
// * * * * * * * * * * * * * *
// * * * * * * * * * * * * * *

// * * * * * * * * * * * * * *
// * * * * * * * * * * * * * *
// *   BSH Add Owner * * * * *
// * * * * * * * * * * * * * *
// * * * * * * * * * * * * * *

// * * * * * * * * * * * * * *
// * * * * * * * * * * * * * *
// *   BSH Remove Owner  * * *
// * * * * * * * * * * * * * *
// * * * * * * * * * * * * * *
