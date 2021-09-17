use crate::types::{Context, Contract, Bmc, Bsh};
use duplicate::duplicate;

#[duplicate(
    contract_type;
    [ Bmc ];
    [ Bsh ];
)]
impl Contract<'_, contract_type> {
    pub fn add_owner(&self, context: Context) -> Context {
        context
    }
}
