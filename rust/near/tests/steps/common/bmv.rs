use lazy_static::lazy_static;
use serde_json::json;
use test_helper::types::{Bmv, BmvContract, Context, Contract};

pub static ICON_NETWORK: &str = "0x1.icon";
pub static ICON_BMC: &str = "0xc294b1A62E82d3f135A8F9b2f9cAEAA23fbD6Cf5";

lazy_static! {
    pub static ref BMV_CONTRACT: Contract<'static, Bmv> =
        BmvContract::new("bmv", "res/BMV_ICON_CONTRACT.wasm");
}

pub static BMV_CONTRACT_IS_DEPLOYED: fn(Context) -> Context =
    |context| BMV_CONTRACT.deploy(context);

pub static BMV_CONTRACT_INITIALZIED: fn(Context) -> Context = |mut context| {
    context.add_method_params(
        "new",
        json!({
            "bmc": context.contracts().get("bmc").id(),
            "validators": [
                hex::decode("b6b5791be0b5ef67063b3c10b840fb81514db2fd").unwrap()
            ],
            "network": ICON_NETWORK,
            "offset": "1846537"
        }),
    );

    BMV_CONTRACT.initialize(context)
};

pub static BMV_CONTRACT_IS_DEPLOYED_AND_INITIALIZED: fn(Context) -> Context = |context: Context| {
    context
        .pipe(BMV_CONTRACT_IS_DEPLOYED)
        .pipe(BMV_CONTRACT_INITIALZIED)
};
