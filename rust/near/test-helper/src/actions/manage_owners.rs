use crate::{invoke_call, invoke_view};
use crate::types::{Bmc, Bsh, Context, Contract};
use duplicate::duplicate;

#[duplicate(
    contract_type;
    [ Bmc ];
    [ Bsh ];
)]
impl Contract<'_, contract_type> {
    pub fn add_owner(&self, context: Context) -> Context {
        invoke_call!(self, context, "add_owner", method_params).unwrap();
        context
    }

    pub fn remove_owner(&self, context: Context) -> Context {
        invoke_call!(self, context, "remove_owner", method_params).unwrap();
        context
    }

    pub fn get_owners(&self, mut context: Context) -> Context {
        invoke_view!(self, context, "get_owners");
        context
    }

}
