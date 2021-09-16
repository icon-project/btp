use test_helper::{types::Context, actions::*};

pub static USER_INVOKES_ADD_OWNER: fn(Context) = add_owner;
pub static USER_ADDS_EXISTING_OWNER: fn(Context) = add_owner;
pub static USER_ADDS_NON_EXISTING_OWNER: fn(Context) = add_owner;


