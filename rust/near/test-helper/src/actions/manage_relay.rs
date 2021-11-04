use crate::{invoke_call, invoke_view};
use crate::types::{Bmc, Context, Contract};
use duplicate::duplicate;

#[duplicate(
    contract_type;
    [ Bmc ];
)]

impl Contract<'_, contract_type> {
    pub fn add_relay(&self, context: Context) -> Context {
        invoke_call!(self, context, "add_relay", method_params).unwrap();
        context
    }

    pub fn remove_relay(&self, context: Context) -> Context {
        invoke_call!(self, context, "remove_relay", method_params).unwrap();
        context
    }

    pub fn get_relays(&self, mut context: Context) -> Context {
        invoke_view!(self, context, "get_relays");
        context
    }

}