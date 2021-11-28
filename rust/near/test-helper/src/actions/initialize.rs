use crate::types::{Bmc, Bmv, NativeCoinBsh, TokenBsh, Context, Contract};
use crate::{invoke_call, invoke_view};
use duplicate::duplicate;

#[duplicate(
    contract_type;
    [ Bmc ];
    [ Bmv ];
    [ NativeCoinBsh ];
    [ TokenBsh ];
)]
impl Contract<'_, contract_type> {
    pub fn initialize(&self, mut context: Context) -> Context {
        invoke_call!(self, context, "new", method_params);
        context
    }
}
