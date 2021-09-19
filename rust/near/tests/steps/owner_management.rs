use super::{BMC_CONTRACT, BSH_CONTRACT};
use serde_json::json;
use test_helper::types::Context;

// * * * * * * * * * * * * * *
// * * * * * * * * * * * * * *
// *   Common Steps  * * * * *
// * * * * * * * * * * * * * *
// * * * * * * * * * * * * * *

pub static CHARLIES_ACCOUNT_ID_SHOULD_BE_IN_OWNERS_LIST: fn(Context) = |context: Context| {
    let owners = context.method_responses("get_owners");
    assert_eq!(
        owners,
        json!([
            context.accounts().get("alice").account_id(),
            context.accounts().get("charlie").account_id()
        ])
    );
};

pub static CHARLIES_ACCOUNT_ID_SHOULD_NOT_BE_IN_OWNERS_LIST: fn(Context) = |context: Context| {
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

pub static ON_QUERYING_OWNERS_IN_BMC: fn(Context) -> Context =
    |context: Context| BMC_CONTRACT.get_owners(context);

pub static CHARLIE_IS_AN_EXISITNG_OWNER_IN_BMC: fn(Context) -> Context = |mut context: Context| {
    let alice = context.accounts().get("alice").to_owned();
    context.set_signer(&alice);
    BMC_CONTRACT.add_owner(context)
};

// * * * * * * * * * * * * * *
// * * * * * * * * * * * * * *
// *   BMC Add Owner * * * * *
// * * * * * * * * * * * * * *
// * * * * * * * * * * * * * *

pub static CHARLIES_ACCOUNT_ID_IS_PROVIDED_AS_ADD_OWNER_PARAM: fn(Context) -> Context =
    |mut context: Context| {
        let charlie = context.accounts().get("charlie").to_owned();
        context.add_method_params(
            "add_owner",
            json!({
                "account_id": charlie.account_id()
            }),
        );
        context
    };

pub static ALICE_INVOKES_ADD_OWNER_IN_BMC: fn(Context) -> Context = |mut context: Context| {
    let bmc_signer = context.accounts().get("alice").to_owned();
    context.set_signer(&bmc_signer);
    BMC_CONTRACT.add_owner(context)
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
                "account_id": charlie.account_id()
            }),
        );
        context
    };

pub static ALICE_INVOKES_REMOVE_OWNER_IN_BMC: fn(Context) -> Context = |mut context: Context| {
    let bmc_signer = context.accounts().get("alice").to_owned();
    context.set_signer(&bmc_signer);
    BMC_CONTRACT.add_owner(context)
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

