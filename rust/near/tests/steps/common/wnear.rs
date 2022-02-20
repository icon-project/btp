use lazy_static::lazy_static;
use serde_json::json;
use test_helper::types::{Context, Contract, WNear, WNearContract};

lazy_static! {
    pub static ref WNEAR_CONTRACT: Contract<'static, WNear> =
    WNearContract::new("wnercontract", "res/NEP141_CONTRACT.wasm");
}

pub static WNEAR_CONTRACT_IS_DEPLOYED: fn(Context) -> Context =
    |context| WNEAR_CONTRACT.deploy(context);

pub static WNEAR_CONTRACT_IS_INITIALZIED: fn(Context) -> Context = |mut context: Context| {
    context.add_method_params(
        "new",
        json!({
            "owner_id": context.contracts().get("tokenbsh").id(),
            "total_supply": "10000000000000000",
            "metadata": {
                    "spec": "ft-1.0.0",
                    "name" : "WNear",
                    "symbol": "wNear",
                    "icon" : null,
                    "reference": null,
                    "reference_hash": null,
                    "decimals": 24
            },
        }),
    );

    WNEAR_CONTRACT.initialize(context)
};

pub static WNEAR_CONTRACT_IS_DEPLOYED_AND_INITIALZIED: fn(Context) -> Context =
    |mut context: Context| {
        context
            .pipe(WNEAR_CONTRACT_IS_DEPLOYED)
            .pipe(WNEAR_CONTRACT_IS_INITIALZIED)
    };

pub static USER_INVOKES_FT_TRANSFER_CALL_IN_WNEAR: fn(Context) -> Context =
    |context: Context| WNEAR_CONTRACT.ft_transfer_call(context, 300000000000000);

pub static USER_INVOKES_FT_BALANCE_OF_CALL_IN_WNEAR: fn(Context) -> Context =
    |context: Context| WNEAR_CONTRACT.ft_balance_of(context);
