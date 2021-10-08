use super::{BMC_CONTRACT, BSH_CONTRACT,BMV_CONTRACT};
use serde_json::{json,from_value};
use test_helper::types::Context;
use super::*;

pub static CHCUCK_IS_NOT_REGISTERED_RELAYER: fn(Context) -> Context = |context: Context| context;

pub static SOURCE_AND_ENCODED_MESSAGE_IS_PROVIDED_AS_HANDLE_RELAY_MESSAGE_PARAM: fn(Context) -> Context = |mut context: Context| {
    context.add_method_params(
        "handle_relay_message",
        json!({
            "source": "BTPADDRESS",
            "message": "ENCODED_MESSAGE"
        }),
    );
    context
};

pub static CHUCK_INVOKES_HANDLE_RELAY_MESSAGE: fn(Context) -> Context = |mut context: Context| {
    let signer = context.accounts().get("chuck").to_owned();
    context.set_signer(&signer);
    BMC_CONTRACT.hadnle_relay_message(context)
};

pub static RELAYER_INVOKES_HANDLE_RELAY_MESSAGE: fn(Context) -> Context = |mut context: Context| {
    let signer = context.accounts().get("relayer").to_owned();
    context.set_signer(&signer);
    BMC_CONTRACT.hadnle_relay_message(context)
};

pub static HANDLE_RELAY_MESSSAGE_IS_INVOKED_WITH_INVALID_RELAY: fn(Context) -> Context = | mut context: Context| {
    CHCUCK_IS_NOT_REGISTERED_RELAYER(context)
    .pipe(SOURCE_AND_ENCODED_MESSAGE_IS_PROVIDED_AS_HANDLE_RELAY_MESSAGE_PARAM)
    .pipe(CHUCK_INVOKES_HANDLE_RELAY_MESSAGE)
};

pub static BMC_SHOULD_SEND_SERVICE_MESSAGE_TO_BSH: fn(Context) -> Context = |context: Context| context;
pub static BMC_INITIALIZES_LINK: fn(Context) -> Context = | context: Context | context; 
 
pub static HANDLE_RELAY_MESSAGE_INVOKED_WITH_EXISTING_RELAY: fn(Context) -> Context = | mut context: Context | {
    ADD_RELAYS_INVOKED_BY_BMC_OWNER(context)
    .pipe(SOURCE_AND_ENCODED_MESSAGE_IS_PROVIDED_AS_HANDLE_RELAY_MESSAGE_PARAM)
    .pipe(RELAYER_INVOKES_HANDLE_RELAY_MESSAGE)
};