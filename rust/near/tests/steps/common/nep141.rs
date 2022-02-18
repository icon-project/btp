use lazy_static::lazy_static;
use serde_json::json;
use test_helper::types::{Context, Contract, Nep141, Nep141Contract};

lazy_static! {
    pub static ref NEP141_CONTRACT: Contract<'static, Nep141> =
        Nep141Contract::new("nep141service", "res/NEP141_CONTRACT.wasm");
}

pub static NEP141_CONTRACT_IS_DEPLOYED: fn(Context) -> Context =
    |context| NEP141_CONTRACT.deploy(context);

pub static NEP141_CONTRACT_IS_INITIALZIED: fn(Context) -> Context = |mut context: Context| {
    context.add_method_params(
        "new",
        json!({
            "owner_id": context.contracts().get("nativecoin").id(),
            "total_supply": "0",
            "metadata": {
                    "spec": "ft-1.0.0",
                    "name" : "WrappedICX",
                    "symbol": "nICX",
                    "icon" : null,
                    "reference": null,
                    "reference_hash": null,
                    "decimals": 0
            },
        }),
    );

    NEP141_CONTRACT.initialize(context)
};

pub static NEP141_CONTRACT_IS_DEPLOYED_AND_INITIALZIED: fn(Context) -> Context =
    |mut context: Context| {
        context
            .pipe(NEP141_CONTRACT_IS_DEPLOYED)
            .pipe(NEP141_CONTRACT_IS_INITIALZIED)
    };

pub static USER_INVOKES_FT_TRANSFER_CALL_IN_NEP141: fn(Context) -> Context =
    |context: Context| NEP141_CONTRACT.ft_transfer_call(context, 300000000000000);

pub static USER_INVOKES_FT_BALANCE_OF_CALL_IN_NEP141: fn(Context) -> Context =
    |context: Context| NEP141_CONTRACT.ft_balance_of(context);

    pub static USER_INVOKES_MINT_IN_NEP141: fn(Context) -> Context =
    |context: Context| NEP141_CONTRACT.mint(context);

    pub static USER_INVOKES_BURN_IN_NEP141: fn(Context) -> Context =
    |context: Context| NEP141_CONTRACT.burn(context);

    pub static NEP141_IS_INITIALZIED : fn(Context) -> Context = |mut context: Context| {
        context.add_method_params(
            "new",
            json!({
                "owner_id": context.accounts().get("charlie").id().to_string(),
                "total_supply": "0",
                "metadata": {
                        "spec": "ft-1.0.0",
                        "name" : "WrappedICX",
                        "symbol": "nICX",
                        "icon" : null,
                        "reference": null,
                        "reference_hash": null,
                        "decimals": 0
                },
            }),
        );
    
        NEP141_CONTRACT.initialize(context)
    };

    pub static NEP141_IS_DEPLOYED_AND_INITIALZIED: fn(Context) -> Context =
    |mut context: Context| {
        context
            .pipe(NEP141_CONTRACT_IS_DEPLOYED)
            .pipe(NEP141_IS_INITIALZIED)
    };