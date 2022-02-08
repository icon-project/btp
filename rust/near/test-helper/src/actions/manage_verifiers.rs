use crate::types::{Bmc, Context, Contract};
use crate::{invoke_call, invoke_view};
use duplicate::duplicate;
use near_primitives::types::Gas;

#[duplicate(
    contract_type;
    [ Bmc ];
)]

impl Contract<'_, contract_type> {
    pub fn add_verifier(&self, mut context: Context, gas: Gas) -> Context
    {
        invoke_call!(self, context, "add_verifier", method_params, None, Some(gas));
        context
    }

    pub fn remove_verifier(&self, mut context: Context, gas: Gas) -> Context {
        invoke_call!(self, context, "remove_verifier", method_params, None, Some(gas));
        context
    }

    pub fn get_verifiers(&self, mut context: Context) -> Context {
        invoke_view!(self, context, "get_verifiers");
        context
    }
}
