use crate::{invoke_call, invoke_view};
use crate::types::{Bmc, Context, Contract};
use duplicate::duplicate;

#[duplicate(
    contract_type;
    [ Bmc ];
)]

impl Contract<'_, contract_type> {
    pub fn add_route(&self, context: Context) -> Context {
        invoke_call!(self, context, "add_route", method_params).unwrap();
        context
    }

    pub fn remove_route(&self, context: Context) -> Context {
        invoke_call!(self, context, "remove_route", method_params).unwrap();
        context
    }

    pub fn get_routes(&self, mut context: Context) -> Context {
        invoke_view!(self, context, "get_routes");
        context
    }
    
}