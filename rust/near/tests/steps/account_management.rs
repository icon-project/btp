use test_helper::actions::create_account;
use test_helper::types::Context;

// * * * * * * * * * * * * * *
// * * * * * * * * * * * * * *
// *   Create Account  * * * *
// * * * * * * * * * * * * * *
// * * * * * * * * * * * * * *

pub static CHARLIES_ACCOUNT_IS_CREATED: fn(Context) -> Context = |mut context: Context| {
    let charlie = create_account(&context);
    context.accounts_mut().add("charlie", charlie);
    context
};

pub static CHUCKS_ACCOUNT_IS_CREATED: fn(Context) -> Context = |mut context: Context| {
    let chuck = create_account(&context);
    context.accounts_mut().add("chuck", chuck);
    context
};

pub static BOBS_ACCOUNT_IS_CREATED: fn(Context) -> Context = |mut context: Context| {
    let bob = create_account(&context);
    context.accounts_mut().add("bob", bob);
    context
};

pub static RELAY_1_ACCOUNT_IS_CREATED: fn(Context) -> Context = |mut context: Context| {
    let relay_1 = create_account(&context);
    context.accounts_mut().add("relay_1", relay_1);
    context
};

// * * * * * * * * * * * * * *
// * * * * * * * * * * * * * *
// * * * *  Set Signer * * * *
// * * * * * * * * * * * * * *
// * * * * * * * * * * * * * *

pub static THE_TRANSACTION_IS_SIGNED_BY_BMC_OWNER: fn(Context) -> Context = |mut context: Context| {
    let signer = context.contracts().get("bmc").as_account().clone();
    context.set_signer(&signer);
    context
};

pub static THE_TRANSACTION_IS_SIGNED_BY_ALICE: fn(Context) -> Context = |mut context: Context| {
    let signer = context.accounts().get("alice").to_owned();
    context.set_signer(&signer);
    context
};

pub static THE_TRANSACTION_IS_SIGNED_BY_BOB: fn(Context) -> Context = |mut context: Context| {
    let signer = context.accounts().get("bob").to_owned();
    context.set_signer(&signer);
    context
};

pub static THE_TRANSACTION_IS_SIGNED_BY_CHUCK: fn(Context) -> Context = |mut context: Context| {
    let signer = context.accounts().get("chuck").to_owned();
    context.set_signer(&signer);
    context
};

pub static THE_TRANSACTION_IS_SIGNED_BY_RELAY_1: fn(Context) -> Context = |mut context: Context| {
    let signer = context.accounts().get("relay_1").to_owned();
    context.set_signer(&signer);
    context
};

pub static RELAY_2_ACCOUNT_IS_CREATED: fn(Context) -> Context = |mut context: Context| {
    let signer = create_account(&context);
    context.accounts_mut().add("relay_2", signer);
    context
};

pub static THE_TRANSACTION_IS_SIGNED_BY_RELAY_2: fn(Context) -> Context = |mut context: Context| {
    let signer = context.accounts().get("relay_2").to_owned();
    context.set_signer(&signer);
    context
};

pub static THE_TRANSACTION_IS_SIGNED_BY_CHARLIE: fn(Context) -> Context = |mut context: Context| {
    let signer = context.accounts().get("charlie").to_owned();
    context.set_signer(&signer);
    context
};
