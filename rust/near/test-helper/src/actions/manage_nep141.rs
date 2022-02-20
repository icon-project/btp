use crate::{invoke_call, invoke_view};
use crate::types::{Contract,Nep141,Context, WNear};
use duplicate::duplicate;

#[duplicate(
    contract_type;
    [ Nep141 ];
    [ WNear ];

)]
impl Contract<'_, contract_type> {
    pub fn ft_transfer_call(&self, mut context: Context, gas: u64) -> Context {
        invoke_call!(
            self,
            context,
            "ft_transfer_call",
            method_params,
            Some(1),
            Some(gas)
        );
        context
    }

   pub fn ft_balance_of(&self, mut context: Context) -> Context {
       invoke_view!(self,context,"ft_balance_of",method_params);
       context
   }

   pub fn mint(&self, mut context: Context) -> Context {
    invoke_call!(self,context,"mint",method_params);
    context
    }

    pub fn burn(&self, mut context: Context) -> Context {
        invoke_call!(self,context,"burn",method_params);
        context
        }
}
