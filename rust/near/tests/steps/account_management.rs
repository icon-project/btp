use std::str::FromStr;

use test_helper::actions::create_account;
use test_helper::types::{Context, Signer, SecretKey};
use workspaces::{InMemorySigner, AccountId};

// * * * * * * * * * * * * * *
// * * * * * * * * * * * * * *
// *   Create Account  * * * *
// * * * * * * * * * * * * * *
// * * * * * * * * * * * * * *

pub static CHARLIE_ACCOUNT_IS_CREATED: fn(Context) -> Context = |mut context: Context| {
    let charlie = Signer::default();
    create_account(&charlie);
    context.accounts_mut().add("charlie", &charlie);
    context
};

pub static CHUCKS_ACCOUNT_IS_CREATED: fn(Context) -> Context = |mut context: Context| {
    let chuck = Signer::default();
    create_account(&chuck);
    context.accounts_mut().add("chuck", &chuck);
    context
};

pub static BOB_ACCOUNT_IS_CREATED: fn(Context) -> Context = |mut context: Context| {
    let bob = Signer::default();
    create_account(&bob);
    context.accounts_mut().add("bob", &bob);
    context
};

pub static RELAY_1_ACCOUNT_IS_CREATED: fn(Context) -> Context = |mut context: Context| {
    let signer = Signer::new(InMemorySigner::from_secret_key(AccountId::from_str("69c003c3b80ed12ea02f5c67c9e8167f0ce3b2e8020a0f43b1029c4d787b0d21").unwrap(), SecretKey::from_str("22yx6AjQgG1jGuAmPuEwLnVKFnuq5LU23dbU3JBZodKxrJ8dmmqpDZKtRSfiU4F8UQmv1RiZSrjWhQMQC3ye7M1J").unwrap()));
    create_account(&signer);
    context.accounts_mut().add("relay_1", &signer);
    context
};

// * * * * * * * * * * * * * *
// * * * * * * * * * * * * * *
// * * * *  Set Signer * * * *
// * * * * * * * * * * * * * *
// * * * * * * * * * * * * * *

pub static BMC_OWNER_IS_THE_SIGNER: fn(Context) -> Context = |mut context: Context| {
    let signer = context.contracts().get("bmc").to_owned();
    context.set_signer(&signer);
    context
};

pub static ALICE_IS_THE_SIGNER: fn(Context) -> Context = |mut context: Context| {
    let signer = context.accounts().get("alice").to_owned();
    context.set_signer(&signer);
    context
};

pub static BOB_IS_THE_SIGNER: fn(Context) -> Context = |mut context: Context| {
    let signer = context.accounts().get("bob").to_owned();
    context.set_signer(&signer);
    context
};

pub static CHUCK_IS_THE_SIGNER: fn(Context) -> Context = |mut context: Context| {
    let signer = context.accounts().get("chuck").to_owned();
    context.set_signer(&signer);
    context
};

pub static RELAY_1_IS_THE_SIGNER: fn(Context) -> Context = |mut context: Context| {
    let signer = context.accounts().get("relay_1").to_owned();
    context.set_signer(&signer);
    context
};

