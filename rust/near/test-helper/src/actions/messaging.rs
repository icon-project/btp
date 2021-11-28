use crate::{invoke_call, invoke_view};
use crate::types::{Bmc, Context, Contract};
use duplicate::duplicate;

#[duplicate(
    contract_type;
    [ Bmc ];
)]
impl Contract<'_, contract_type> {
    pub fn handle_relay_message(&self, mut context: Context) -> Context {
        invoke_call!(self, context, "handle_relay_message", method_params);
        context
    }
}