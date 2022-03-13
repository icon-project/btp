use crate::types::{Bmc, Context, Contract};
use crate::{invoke_call, invoke_view};
use duplicate::duplicate;
use near_primitives::types::Gas;

#[duplicate(
    contract_type;
    [ Bmc ];
)]
impl Contract<'_, contract_type> {
    pub fn handle_relay_message(&self, mut context: Context, gas: Gas) -> Context {
        invoke_call!(
            self,
            context,
            "handle_relay_message",
            method_params,
            None,
            Some(gas)
        );
        context
    }

    pub fn handle_relay_message_bmv_callback_mockable(
        &self,
        mut context: Context,
        gas: Gas,
    ) -> Context {
        invoke_call!(
            self,
            context,
            "handle_relay_message_bmv_callback_mockable",
            method_params,
            None,
            Some(gas)
        );
        context
    }

    pub fn get_message(&self, mut context: Context) -> Context {
        invoke_view!(self, context, "get_message");
        context
    }
}
