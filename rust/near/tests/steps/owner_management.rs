use super::{BMC_CONTRACT, BSH_CONTRACT};
use test_helper::types::{Context, Signer};


pub static BMC_CONTRACT_OWNER_INVOKES_ADD_OWNER: fn(Context) -> Context = |mut context: Context| {
    let bmc_signer = context.contracts.get("bmc").to_owned();
    context.set_signer(&bmc_signer);
    BMC_CONTRACT.add_owner(context)
};

pub static NEW_OWNER_IS_PROVIDED: fn(Context) -> Context = |mut context: Context| {
    let new_owner = Signer::default();
    //context.add_method_params("add_owner", value);
    context
};

pub static USER_ADDS_EXISTING_OWNER: fn(Context) -> Context =
    |context: Context| BMC_CONTRACT.add_owner(context);
pub static USER_ADDS_NON_EXISTING_OWNER: fn(Context) -> Context =
    |context: Context| BMC_CONTRACT.add_owner(context);
