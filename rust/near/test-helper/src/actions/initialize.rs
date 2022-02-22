use crate::invoke_call;
use crate::types::{Bmc, Bmv, Context, Contract, NativeCoinBsh, TokenBsh,Nep141, Nep141Testable, WNear};
use duplicate::duplicate;

#[duplicate(
    contract_type;
    [ Bmc ];
    [ Bmv ];
    [ NativeCoinBsh ];
    [ TokenBsh ];
    [ Nep141 ];
    [ Nep141Testable ];
    [ WNear ];
)]
impl Contract<'_, contract_type> {
    pub fn initialize(&self, mut context: Context) -> Context {
        invoke_call!(self, context, "new", method_params);
        context
    }
}
