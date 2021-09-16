use test_helper::{types::{Context}, actions::*};
use super::BMC_CONTRACT;

pub static USER_INVOKES_ADD_OWNER: fn(Context) -> Context = |context: Context| BMC_CONTRACT.add_owner(context);
pub static USER_ADDS_EXISTING_OWNER: fn(Context) -> Context = |context: Context| BMC_CONTRACT.add_owner(context);
pub static USER_ADDS_NON_EXISTING_OWNER: fn(Context) -> Context = |context: Context| BMC_CONTRACT.add_owner(context);


