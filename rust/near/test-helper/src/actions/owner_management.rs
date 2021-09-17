use crate::types::{Context, Contract, Bmc, Bsh};
use duplicate::duplicate;
use crate::invoke_call;

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
