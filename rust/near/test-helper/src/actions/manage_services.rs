use crate::types::{Bmc, Context, Contract};
use crate::{invoke_call, invoke_view};
use duplicate::duplicate;

#[duplicate(
    contract_type;
    [ Bmc ];
)]

impl Contract<'_, contract_type> {
    pub fn request_service(&self, mut context: Context) -> Context {
        invoke_call!(self, context, "request_service", method_params);
        context
    }

    pub fn approve_service(&self, mut context: Context) -> Context {
        invoke_call!(self, context, "approve_service", method_params);
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

    pub fn get_requests(&self, mut context: Context) -> Context {
        invoke_view!(self, context, "get_requests");
        context
    }
}
