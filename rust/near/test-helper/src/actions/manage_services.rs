use crate::{invoke_call, invoke_view};
use crate::types::{Bmc,Bsh, Context, Contract};
use duplicate::duplicate;

#[duplicate(
    contract_type;
    [ Bmc ];
    [ Bsh ];
)]

impl Contract<'_, contract_type> {
    pub fn request_service(&self, context: Context) -> Context {
        invoke_call!(self, context, "request_service", method_params).unwrap();
        context
    }

    pub fn approve_service(&self, context: Context) -> Context {
        invoke_call!(self, context, "approve_service", method_params).unwrap();
        context
    }
    
    pub fn remove_service(&self, context: Context) -> Context{
        invoke_call!(self, context, "remove_service",method_params).unwrap();
        context
    }

    pub fn get_services(&self, mut context: Context) -> Context {
        invoke_view!(self, context, "get_services");
        context
    }

    pub fn get_requests(&self, mut context: Context) -> Context{
        invoke_view!(self, context, "get_requests");
        context
    }

}