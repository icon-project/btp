use crate::types::{Bmc, NativeCoinBsh, TokenBsh, Context, Contract};
use crate::{invoke_call, invoke_view};
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
