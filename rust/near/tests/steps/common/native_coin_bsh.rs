use lazy_static::lazy_static;
use serde_json::json;
use test_helper::types::{Context, Contract, NativeCoinBsh, NativeCoinBshContract};

lazy_static! {
    pub static ref NATIVE_COIN_BSH_CONTRACT: Contract<'static, NativeCoinBsh> =
        NativeCoinBshContract::new("nativecoin", "res/NATIVE_COIN_BSH_CONTRACT.wasm");
}

pub static NATIVE_COIN_BSH_CONTRACT_IS_DEPLOYED: fn(Context) -> Context =
    |context| NATIVE_COIN_BSH_CONTRACT.deploy(context);

pub static NATIVE_COIN_BSH_CONTRACT_IS_INITIALZIED: fn(Context) -> Context = |mut context: Context| {
    context.add_method_params(
        "new",
        json!({
            "service_name": "nativecoin",
            "bmc": context.contracts().get("bmc").id(),
            "network": super::NEAR_NETWORK,
            "native_coin": {
                "metadata": {
                    "name": "NEAR",
                    "symbol": "NEAR",
                    "fee_numerator": "1000",
                    "denominator": "10000000000000000000",
                    "network": super::NEAR_NETWORK
                }
            },
        }),
    );

    NATIVE_COIN_BSH_CONTRACT.initialize(context)
};

pub static NATIVE_COIN_BSH_CONTRACT_IS_DEPLOYED_AND_INITIALIZED: fn(Context) -> Context = |context: Context| {
    context
        .pipe(NATIVE_COIN_BSH_CONTRACT_IS_DEPLOYED)
        .pipe(NATIVE_COIN_BSH_CONTRACT_IS_INITIALZIED)
};
