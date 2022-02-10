use crate::types::{Bmc, Context, Contract};
use crate::{invoke_call, invoke_view};
use duplicate::duplicate;

#[duplicate(
    contract_type;
    [ Bmc ];
)]

impl Contract<'_, contract_type> {
    pub fn add_service(&self, mut context: Context) -> Context {
        invoke_call!(self, context, "add_service", method_params);
        context
    }

    pub fn remove_service(&self, mut context: Context) -> Context {
        invoke_call!(self, context, "remove_service", method_params);
        context
    }
    pub fn get_services(&self, mut context: Context) -> Context {
        invoke_view!(self, context, "get_services");
        context
    }
}
