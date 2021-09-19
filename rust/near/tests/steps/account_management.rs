use test_helper::types::{Context, Signer};
use test_helper::actions::create_account;

// * * * * * * * * * * * * * *
// * * * * * * * * * * * * * *
// *   Create Account  * * * *
// * * * * * * * * * * * * * *
// * * * * * * * * * * * * * *

pub static CHARLIES_ACCOUNT_IS_CREATED: fn(Context) -> Context = |mut context: Context| {
    let charlie = Signer::default();
    create_account(&charlie);
    context.accounts_mut().add("charlie", &charlie);
    context
};
