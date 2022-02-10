use crate::invoke_call;
use crate::types::{Context, Contract, NativeCoinBsh, TokenBsh};
use duplicate::duplicate;

#[duplicate(
    contract_type;
    [ NativeCoinBsh ];
    [ TokenBsh ];
)]

impl Contract<'_, contract_type> {
    pub fn register(&self, mut context: Context) -> Context {
        invoke_call!(self, context, "register", method_params);
        context
    }
}
