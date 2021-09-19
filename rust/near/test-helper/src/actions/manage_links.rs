use crate::types::{Bmc, Context, Contract};
use crate::{invoke_call, invoke_view};
use duplicate::duplicate;

#[duplicate(
    contract_type;
    [ Bmc ];
)]

impl Contract<'_, contract_type> {
    pub fn add_link(&self, context: Context) -> Context {
        invoke_call!(self, context, "add_link", method_params);
        context
    }

    pub fn remove_link(&self, context: Context) -> Context {
        invoke_call!(self, context, "remove_link", method_params);
        context
    }

    pub fn get_links(&self, mut context: Context) -> Context {
        invoke_view!(self, context, "get_links");
        context
    }
    pub fn set_link(&self, mut context: Context) -> Context {
        invoke_call!(self, context, "set_link", method_params);
        context
    }
}
