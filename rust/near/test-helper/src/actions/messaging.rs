use crate::{invoke_call};
use crate::types::{Bmc, Context, Contract};
use duplicate::duplicate;
use near_primitives::types::Gas;

#[duplicate(
    contract_type;
    [ Bmc ];
)]
impl Contract<'_, contract_type> {
    pub fn handle_relay_message(&self, mut context: Context, gas: Gas) -> Context {
        invoke_call!(self, context, "handle_relay_message", method_params, None, Some(gas));
        context
    }
}