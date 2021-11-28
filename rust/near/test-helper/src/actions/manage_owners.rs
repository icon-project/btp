use crate::{invoke_call, invoke_view};
use crate::types::{Bmc, NativeCoinBsh, TokenBsh, Context, Contract};
use duplicate::duplicate;

#[duplicate(
    contract_type;
    [ Bmc ];
    [ NativeCoinBsh ];
    [ TokenBsh ];
)]
impl Contract<'_, contract_type> {
    pub fn add_owner(&self, mut context: Context) -> Context {
        invoke_call!(self, context, "add_owner", method_params);
        context
    }

    pub fn remove_owner(&self, mut context: Context) -> Context {
        invoke_call!(self, context, "remove_owner", method_params);
        context
    }

    pub fn get_owners(&self, mut context: Context) -> Context {
        invoke_view!(self, context, "get_owners");
        context
    }

}
