use crate::types::{Bmc, Bsh, Context, Contract};
use crate::{invoke_call, invoke_view};
use duplicate::duplicate;

#[duplicate(
    contract_type;
    [ Bsh ];
)]

impl Contract<'_, contract_type> {
    pub fn register(&self, context: Context) -> Context {
        invoke_call!(self, context, "register", method_params).unwrap();
        context
    }

    pub fn get_coin_id(&self, context: Context) -> Context {
        invoke_call!(self, context, "get_coin_id", method_params).unwrap();
        context
    }

    pub fn get_balance_of(&self, context: Context) -> Context {
        invke_call!(self, context, "get_balance_of", method_params).unwrap();
        context
    }

    pub fn get_balance_of_bath(&self, context: Context) -> Context {
        invoke_call!(self, context, "get_balance_of_bath", method_params).unwrap();
        context
    }

    pub fn transfer_native_coin(&self, context: Context) -> Context {
        invoke_call!(self, context, "transfer_native_coin", method_params).unwrap();
        context
    }

    pub fn transfer(&self, context: Context) -> Context {
        invoke_call!(self, context, "transfer", method_params).unwrap();
        context
    }

    pub fn transfer_batch(&self, context: Context) -> Context {
        invoke_call!(self, context, "transfer_batch", method_params).unwrap();
        context
    }

    pub fn reclaim(&self, context: Context) -> Context {
        invoke_call!(self, context, "reclaim", method_params).unwrap();
        context
    }
    pub fn mint(&self, context: Context) -> Context {
        invoke_call!(self, context, "mint", method_params).unwrap();
        context
    }
    pub fn mint(&self, context: Context) -> Context {
        invoke_call!(self, context, "mint", method_params).unwrap();
        context
    }

    pub fn update_bsh_periphery(&self, context: Context) -> Context {
        invoke_call!(self, context, "update_bsh_periphery", method_params).unwrap();
        context
    }

    pub fn set_fee_ratio(&self, context: Context) -> {
        invoke_call!(self, context, "set_fee_ratio", method_params).unwrap();
        context
    }

    pub fn set_fixed_fee(&self, context: Context) -> {
        invoke_call!(self, context, "set_fixed_fee", method_params).unwrap();
        context
    }

    pub fn update_uri(&self, context: Context) -> Context {
        invoke_call!(self, context, "update_uri", method_params).unwrap();
        context
    }

    pub fn get_coin_names(&self, mut context: Context) -> Context {
        invoke_view!(self, context, "get_conin_names");
        context
    }

    pub fn get_balance_of(&sellf,mut context: Context) -> Context {
        invoke_view!(self, context, "get_balance_of")
        context
    }

    pub fn is_valid_coin(&self, mut context: Context) -> Context {
        invoke_view!(self, context, "is_valid_coin");
        context
    }
}
