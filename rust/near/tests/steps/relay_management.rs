use super::{BMC_CONTRACT, BMV_CONTRACT};
use serde_json::{json,from_value};
use test_helper::types::Context;
use super::*;

//Common step in add_relays

pub static LINK_AND_RELAY_PROVIDED_AS_ADD_RELAY_PARAM: fn(Context) -> Context = |mut context: Context| {
    context.add_method_params(
        "add_realy",
        json!({
            "link": "BTPADDRESS",
            "relay": "relayaddress"
        }),
    );
    context
};

pub static INVALID_LINK_AND_RELAY_PROVIDED_AS_ADD_RELAY_PARAM: fn(Context) -> Context = |mut context: Context| {
    context.add_method_params(
        "add_realy",
        json!({
            "link": "INVALIDADDRESS",
            "relay": "relayaddress"
        }),
    );
    context
};

pub static LINK_AND_RELAYS_PROVIDED_AS_ADD_RELAY_PARAM: fn(Context) -> Context = |mut context: Context| {
    context.add_method_params(
        "add_realy",
        json!({
            "link": "BTPADDRESS",
            "relay": "VECTOR(relays)"
        }),
    );
    context
};


pub static ALICE_INVOKES_ADD_RELAY_IN_BMC: fn(Context) -> Context = |mut context: Context| {
    let signer = context.accounts().get("alice").to_owned();
    context.set_signer(&signer);
    BMC_CONTRACT.add_relay(context)
};

pub static ALICE_INVOKES_ADD_RELAYS_IN_BMC: fn(Context) -> Context = |mut context: Context| {
    let signer = context.accounts().get("alice").to_owned();
    context.set_signer(&signer);
    BMC_CONTRACT.add_relays(context)
};

pub static ALICE_INVOKES_REMOVE_RELAYS_IN_BMC: fn(Context) -> Context = |mut context: Context| {
    let signer = context.accounts().get("alice").to_owned();
    context.set_signer(&signer);
    BMC_CONTRACT.remove_relay(context)
};

pub static RELAYS_ARE_QURIED_IN_BMC: fn(Context) -> Context =
    |context: Context| BMC_CONTRACT.get_relays(context);

pub static ADD_RELAY_INVOKED_BY_BMC_OWNER: fn(Context) -> Context = |mut context: Context| {
        //bmc owner adds link 
        // relay provided to the link added
        //bmc owner invokes add_relay realy will be added to the link
        ADD_LINK_INVOKED_BY_BMC_OWNER(context)
        .pipe(LINK_AND_RELAY_PROVIDED_AS_ADD_RELAY_PARAM)
        .pipe(ALICE_INVOKES_ADD_RELAYS_IN_BMC)
        
 };


 pub static ADD_RELAYS_INVOKED_BY_BMC_OWNER: fn(Context) -> Context = |mut context: Context| {
    //bmc owner adds link 
    // relay provided to the link added
    //bmc owner invokes add_relay realy will be added to the link
    ADD_LINK_INVOKED_BY_BMC_OWNER(context)
    .pipe(LINK_AND_RELAYS_PROVIDED_AS_ADD_RELAY_PARAM)
    .pipe(ALICE_INVOKES_ADD_RELAYS_IN_BMC)
    
};

pub static CHUCK_INVOKES_ADD_RELAY_IN_BMC: fn(Context) -> Context = |mut context: Context| {
    let signer = context.accounts().get("chucks").to_owned();
    context.set_signer(&signer);
    BMC_CONTRACT.add_relay(context)
};

pub static CHUCK_INVOKES_ADD_RELAYS_IN_BMC: fn(Context) -> Context = |mut context: Context| {
    let signer = context.accounts().get("chucks").to_owned();
    context.set_signer(&signer);
    BMC_CONTRACT.add_relays(context)
};

pub static CHUCK_INVOKES_REMOVE_RELAYS_IN_BMC: fn(Context) -> Context = |mut context: Context| {
    let signer = context.accounts().get("chucks").to_owned();
    context.set_signer(&signer);
    BMC_CONTRACT.add_relays(context)
};
pub static ADD_RELAY_INVOKED_BY_NON_BMC_OWNER: fn(Context) -> Context = |mut context: Context| {
    ADD_LINK_INVOKED_BY_BMC_OWNER(context)
    .pipe(LINK_AND_RELAY_PROVIDED_AS_ADD_RELAY_PARAM)
    .pipe(CHUCK_IS_NOT_A_BMC_OWNER)
    .pipe(CHUCK_INVOKES_ADD_RELAY_IN_BMC)
};

pub static ADD_RELAYS_INVOKED_BY_NON_BMC_OWNER: fn(Context) -> Context = |mut context: Context| {
    ADD_LINK_INVOKED_BY_BMC_OWNER(context)
    .pipe(LINK_AND_RELAYS_PROVIDED_AS_ADD_RELAY_PARAM)
    .pipe(CHUCK_IS_NOT_A_BMC_OWNER)
    .pipe(CHUCK_INVOKES_ADD_RELAYS_IN_BMC)
};

pub static ADD_RELAY_WITH_NON_EXISTING_LINK_INVOKED_BY_BMC_OWNER: fn(Context) -> Context = |mut context: Context| {
    //bmc owner adds link 
    // relay provided to the link added
    //bmc owner invokes add_relay realy will be added to the link
    ADD_LINK_INVOKED_BY_BMC_OWNER(context)
    .pipe(INVALID_LINK_AND_RELAY_PROVIDED_AS_ADD_RELAY_PARAM)
    .pipe(ALICE_INVOKES_ADD_RELAYS_IN_BMC)
    
};


pub static LINK_AND_RELAY_PROVIDED_AS_REMOVE_RELAY_PARAM: fn(Context) -> Context = |mut context: Context| {
    context.add_method_params(
        "remove_realy",
        json!({
            "link": "BTPADDRESS",
            "relay": "relayaddress"
        }),
    );
    context
};

pub static NONEXISTING_LINK_AND_RELAY_PROVIDED_AS_REMOVE_RELAY_PARAM: fn(Context) -> Context = |mut context: Context| {
    context.add_method_params(
        "remove_realy",
        json!({
            "link": "NONEXISTING_LINK",
            "relay": "relayaddress"
        }),
    );
    context
};


pub static REMOVE_RELAY_INVOKED_BY_BMC_OWNER: fn(Context) -> Context = |mut context: Context| {
    //bmc owner adds link 
    // relay provided to the link added
    //bmc owner invokes add_relay realy will be added to the link
    ADD_LINK_INVOKED_BY_BMC_OWNER(context)
    .pipe(LINK_AND_RELAY_PROVIDED_AS_REMOVE_RELAY_PARAM)
    .pipe(ALICE_INVOKES_REMOVE_RELAYS_IN_BMC)
    
};

pub static REMOVE_RELAYS_INVOKED_BY_NON_BMC_OWNER: fn(Context) -> Context = |mut context: Context| {
    ADD_LINK_INVOKED_BY_BMC_OWNER(context)
    .pipe(LINK_AND_RELAY_PROVIDED_AS_REMOVE_RELAY_PARAM)
    .pipe(CHUCK_IS_NOT_A_BMC_OWNER)
    .pipe(CHUCK_INVOKES_REMOVE_RELAYS_IN_BMC)
};


pub static REMOVE_NON_EXISTING_RELAY_INVOKED_BY_BMC_OWNER: fn(Context) -> Context = |mut context: Context| {
    ADD_LINK_INVOKED_BY_BMC_OWNER(context)
    .pipe(NONEXISTING_LINK_AND_RELAY_PROVIDED_AS_REMOVE_RELAY_PARAM)
    .pipe(CHUCK_IS_NOT_A_BMC_OWNER)
    .pipe(CHUCK_INVOKES_REMOVE_RELAYS_IN_BMC)
};


pub static ADDED_RELAYS_SHOULD_BE_IN_LIST: fn(Context) = |mut context: Context| {
    let relay = context.method_responses("get_relays");

    let result: HashSet<_> = from_value::<Vec<String>>(relay)
        .unwrap()
        .into_iter()
        .collect();
    let expected: HashSet<_> = vec![
       "to_be_given".to_string(),
    ]
    .into_iter()
    .collect();
    assert_eq!(result, expected);
};




pub static DELETED_RELAY_SHOULD_NOT_BE_IN_LIST: fn(Context) = |mut context: Context| {
    let relay = context.method_responses("get_relays");

    let result: HashSet<_> = from_value::<Vec<String>>(relay)
        .unwrap()
        .into_iter()
        .collect();
    let expected: HashSet<_> = vec![
       "to_be_given".to_string(),
    ]
    .into_iter()
    .collect();
    assert_eq!(result, expected);
};

