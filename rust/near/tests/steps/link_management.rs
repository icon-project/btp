use super::{BMC_CONTRACT, BMV_CONTRACT};
use serde_json::{json,from_value};
use test_helper::types::Context;
use super::*;

pub static ADD_LINK_INVOKED_WITH_EXISTING_ADDRESS_BY_BMC_OWNER: fn(Context) -> Context = | mut context: Context | {
    VERIFIER_NETWORKADDRESS_AND_VERIFIER_ADDRESS_PROVIDED_AS_ADD_VERIFIER_PARAM(context)
    .pipe(ALICE_INVOKES_ADD_VERIFIER_IN_BMC)
    .pipe(LINK_ADDRESS_PROVIDED_AS_ADD_LINK_PARAM)
    .pipe(ALICE_INVOKES_ADD_LINK_IN_BMC)
    .pipe(LINK_ADDRESS_PROVIDED_AS_ADD_LINK_PARAM)
    .pipe(ALICE_INVOKES_ADD_LINK_IN_BMC)
};


pub static ADD_LINK_INVOKED_BY_BMC_OWNER: fn(Context) -> Context = |mut context: Context| {

    //provide the link to be added
    //link provided shoud be in verifier
    VERIFIER_NETWORKADDRESS_AND_VERIFIER_ADDRESS_PROVIDED_AS_ADD_VERIFIER_PARAM(context)
    .pipe(ALICE_INVOKES_ADD_VERIFIER_IN_BMC)
    .pipe(LINK_ADDRESS_PROVIDED_AS_ADD_LINK_PARAM)
    .pipe(ALICE_INVOKES_ADD_LINK_IN_BMC)
};

pub static REMOVE_LINK_INVKED_BY_BMC_OWNER: fn(Context) -> Context = | mut context: Context | {
    VERIFIER_NETWORKADDRESS_AND_VERIFIER_ADDRESS_PROVIDED_AS_ADD_VERIFIER_PARAM(context)
    .pipe(ALICE_INVOKES_ADD_VERIFIER_IN_BMC)
    .pipe(LINK_ADDRESS_PROVIDED_AS_ADD_LINK_PARAM)
    .pipe(ALICE_INVOKES_ADD_LINK_IN_BMC)
    .pipe(LINK_ADDRESS_PROVIDED_AS_REMOVE_LINK_PARAM)
    .pipe(ALICE_INVOKES_REMOVE_LINK_IN_BMC)
};

pub static REMOVE_LINK_INVKED_BY_NON_BMC_OWNER: fn(Context) -> Context = | mut context: Context | {
    VERIFIER_NETWORKADDRESS_AND_VERIFIER_ADDRESS_PROVIDED_AS_ADD_VERIFIER_PARAM(context)
    .pipe(ALICE_INVOKES_ADD_VERIFIER_IN_BMC)
    .pipe(LINK_ADDRESS_PROVIDED_AS_ADD_LINK_PARAM)
    .pipe(ALICE_INVOKES_ADD_LINK_IN_BMC)
    .pipe(LINK_ADDRESS_PROVIDED_AS_REMOVE_LINK_PARAM)
    .pipe(CHUCK_INVOKES_REMOVE_LINK_IN_BMC)
};

pub static REMOVE_NON_EXISTING_LINK_BY_BMC_OWNER: fn(Context) -> Context = | mut context: Context | {
    VERIFIER_NETWORKADDRESS_AND_VERIFIER_ADDRESS_PROVIDED_AS_ADD_VERIFIER_PARAM(context)
    .pipe(ALICE_INVOKES_ADD_VERIFIER_IN_BMC)
    .pipe(LINK_ADDRESS_PROVIDED_AS_ADD_LINK_PARAM)
    .pipe(ALICE_INVOKES_ADD_LINK_IN_BMC)
    .pipe(NON_EXISTING_LINK_ADDRESS_PROVIDED_AS_REMOVE_LINK_PARAM)
    .pipe(ALICE_INVOKES_REMOVE_LINK_IN_BMC)

};

pub static LINK_ADDRESS_PROVIDED_AS_ADD_LINK_PARAM: fn(Context) -> Context = |mut context: Context| {
    context.add_method_params(
        "add_link",
        json!({
            "link": "BTPADDRESS"
        }),
    );
    context
};

pub static NON_EXISTING_LINK_ADDRESS_PROVIDED_AS_REMOVE_LINK_PARAM: fn(Context) -> Context = |mut context: Context| {
    context.add_method_params(
        "remove_link",
        json!({
            "link": "NONEXISTING_BTPADDRESS"
        }),
    );
    context
};

pub static LINK_ADDRESS_PROVIDED_AS_REMOVE_LINK_PARAM: fn(Context) -> Context = |mut context: Context| {
    context.add_method_params(
        "remove_link",
        json!({
            "link": "BTPADDRESS"
        }),
    );
    context
};

pub static INVALID_LINK_ADDRESS_PROVIDED_AS_ADD_LINK_PARAM: fn(Context) -> Context = |mut context: Context| {
    context.add_method_params(
        "add_link",
        json!({
            "link": "INVALIDVERIFERADDRESS"
        }),
    );
    context
};



pub static ADD_LINK_WITH_INVALID_ADDRESS_INVOKED_BY_BMC_OWNER: fn(Context) -> Context = |mut context: Context| {

    //provide the link to be added
    //link provided shoud be in verifier
    VERIFIER_NETWORKADDRESS_AND_VERIFIER_ADDRESS_PROVIDED_AS_ADD_VERIFIER_PARAM(context)
    .pipe(ALICE_INVOKES_ADD_VERIFIER_IN_BMC)
    .pipe(INVALID_LINK_ADDRESS_PROVIDED_AS_ADD_LINK_PARAM)
    .pipe(ALICE_INVOKES_ADD_LINK_IN_BMC)
};

pub static ADD_LINK_INVOKED_BY_NON_BMC_OWNER: fn(Context) -> Context = |mut context: Context| {

    //provide the link to be added
    //link provided shoud be in verifier
    VERIFIER_NETWORKADDRESS_AND_VERIFIER_ADDRESS_PROVIDED_AS_ADD_VERIFIER_PARAM(context)
    .pipe(ALICE_INVOKES_ADD_VERIFIER_IN_BMC)
    .pipe(LINK_ADDRESS_PROVIDED_AS_ADD_LINK_PARAM)
    .pipe(CHUCK_INVOKES_ADD_LINK_IN_BMC)
};

pub static LINKS_ARE_QURIED_IN_BMC: fn(Context) -> Context = |context: Context| BMC_CONTRACT.get_links(context);

pub static ADDED_LINK_SHOULD_BE_IN_LIST: fn(Context) = |mut context: Context| {

    let link = context.method_responses("get_links");

    let result: HashSet<_> = from_value::<Vec<String>>(link)
        .unwrap()
        .into_iter()
        .collect();
    let expected: HashSet<_> = vec![
       "BTPADDRES_OF_THE_LINK_PROVIDED".to_string(),
    ]
    .into_iter()
    .collect();
    assert_eq!(result, expected);
};


pub static REMOVED_LINK_SHOULD_NOT_BE_PRESENT: fn(Context) = |mut context: Context| {

    let link = context.method_responses("get_links");

    let result: HashSet<_> = from_value::<Vec<String>>(link)
        .unwrap()
        .into_iter()
        .collect();
    let expected: HashSet<_> = vec![
       "BTPADDRES_OF_THE_LINK_PROVIDED".to_string(),
    ]
    .into_iter()
    .collect();
    assert_ne!(result, expected);
};



pub static ALICE_INVOKES_ADD_LINK_IN_BMC: fn(Context) -> Context = |mut context: Context| {
    let signer = context.accounts().get("alice").to_owned();
    context.set_signer(&signer);
    BMC_CONTRACT.add_link(context)
};


pub static ALICE_INVOKES_REMOVE_LINK_IN_BMC: fn(Context) -> Context = |mut context: Context| {
    let signer = context.accounts().get("alice").to_owned();
    context.set_signer(&signer);
    BMC_CONTRACT.remove_link(context)
};

pub static CHUCK_INVOKES_ADD_LINK_IN_BMC: fn(Context) -> Context = |mut context: Context| {
    let signer = context.accounts().get("chucks").to_owned();
    context.set_signer(&signer);
    BMC_CONTRACT.add_link(context)
};

pub static CHUCK_INVOKES_REMOVE_LINK_IN_BMC: fn(Context) -> Context = |mut context: Context| {
    let signer = context.accounts().get("chucks").to_owned();
    context.set_signer(&signer);
    BMC_CONTRACT.remove_link(context)
};