use crate::types::{Bmc, Context, Contract};
use crate::{invoke_call, invoke_view};
use duplicate::duplicate;

#[duplicate(
    contract_type;
    [ Bmc ];
)]

impl Contract<'_, contract_type> {
    pub fn add_relay(&self, mut context: Context) -> Context {
        invoke_call!(self, context, "add_relay", method_params);
        context
    }
    pub fn add_relays(&self, mut context: Context) -> Context {
        invoke_call!(self, context, "add_relays", method_params);
        context
    }

    pub fn remove_relay(&self, mut context: Context) -> Context {
        invoke_call!(self, context, "remove_relay", method_params);
        context
    }

    pub fn get_relays(&self, mut context: Context) -> Context {
        invoke_view!(self, context, "get_relays", method_params);
        context
    }
}
