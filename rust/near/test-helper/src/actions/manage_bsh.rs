use crate::types::{Context, Contract, NativeCoinBsh, TokenBsh};
use crate::{invoke_call, invoke_view};
use duplicate::duplicate;

#[duplicate(
    contract_type;
    [ NativeCoinBsh ];
    [ TokenBsh ];
)]

impl Contract<'_, contract_type> {
    pub fn set_fee_ratio(&self, mut context: Context) -> Context {
        invoke_call!(self, context, "set_fee_ratio", method_params);
        context
    }

    pub fn register(&self, mut context: Context) -> Context {
        invoke_call!(
            self,
            context,
            "register",
            method_params,
            Some(7_000_000_000_000_000_000_000_000),
            Some(300000000000000)
        );
        context
    }

    pub fn tokens(&self, mut context: Context) -> Context {
        invoke_view!(self, context, "tokens", method_params);
        context
    }

    pub fn tokens_id(&self, mut context: Context) -> Context {
        invoke_call!(self, context, "token_id", method_params);
        context
    }

    pub fn token_id(&self, mut context: Context) -> Context {
        invoke_view!(self, context, "token_id", method_params);
        context
    }

    pub fn coins(&self, mut context: Context) -> Context {
        invoke_view!(self, context, "coins", method_params);
        context
    }
    pub fn coin_id(&self, mut context: Context) -> Context {
        invoke_view!(self, context, "coin_id", method_params);
        context
    }

    pub fn coin_id_error(&self, mut context: Context) -> Context {
        invoke_call!(self, context, "coin_id", method_params);
        context
    }

    pub fn accumulated_fees(&self, mut context: Context) -> Context {
        invoke_call!(self, context, "accumulated_fees", method_params);
        context
    }
    pub fn handle_fee_gathering(&self, mut context: Context) -> Context {
        invoke_call!(self, context, "handle_fee_gathering", method_params);
        context
    }

    pub fn handle_btp_message(&self, mut context: Context, gas: u64) -> Context {
        invoke_call!(
            self,
            context,
            "handle_btp_message",
            method_params,
            None,
            Some(gas)
        );
        context
    }

    pub fn calculate_token_transfer_fee(&self, mut context: Context) -> Context {
        invoke_view!(self, context, "calculate_token_transfer_fee", method_params);
        context
    }
    pub fn locked_balance_of(&self, mut context: Context) -> Context {
        invoke_view!(self, context, "locked_balance_of", method_params);
        context
    }
    pub fn transfer_fees(&self, mut context: Context) -> Context {
        invoke_call!(self, context, "transfer_fees", method_params);
        context
    }

    pub fn withdraw(&self, mut context: Context, gas: u64) -> Context {
        invoke_call!(self, context, "withdraw", method_params, Some(1), Some(gas));
        context
    }

    pub fn deposit(&self, mut context: Context, gas: u64) -> Context {
        invoke_call!(
            self,
            context,
            "deposit",
            method_params,
            Some(1000),
            Some(gas)
        );
        context
    }

    pub fn transfer(&self, mut context: Context, gas: u64) -> Context {
        invoke_call!(self, context, "transfer", method_params, None, Some(gas));
        context
    }

    pub fn balance_of(&self, mut context: Context) -> Context {
        invoke_view!(self, context, "balance_of", method_params);
        context
    }
}
