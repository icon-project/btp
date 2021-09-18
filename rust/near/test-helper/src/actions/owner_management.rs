use crate::invoke_call;
use crate::types::{Bmc, Bsh, Context, Contract};
use duplicate::duplicate;

#[duplicate(
    contract_type;
    [ Bmc ];
    [ Bsh ];
)]
impl Contract<'_, contract_type> {
    pub fn add_owner(&self, context: Context) -> Context {
        invoke_call!(self, context, "add_owner", method_params);
        context
    }
}
