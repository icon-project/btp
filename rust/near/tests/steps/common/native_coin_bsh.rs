use lazy_static::lazy_static;
use serde_json::json;
use test_helper::types::{Context, Contract, NativeCoinBsh, NativeCoinBshContract};

lazy_static! {
    pub static ref NATIVE_COIN_BSH_CONTRACT: Contract<'static, NativeCoinBsh> =
        NativeCoinBshContract::new("nativecoin", "res/NATIVE_COIN_BSH_CONTRACT.wasm");
}

pub static NATIVE_COIN_BSH_CONTRACT_IS_DEPLOYED: fn(Context) -> Context =
    |context| NATIVE_COIN_BSH_CONTRACT.deploy(context);

pub static NATIVE_COIN_BSH_CONTRACT_IS_INITIALZIED: fn(Context) -> Context =
    |mut context: Context| {
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
                        "uri": null,
                        "network": super::NEAR_NETWORK,
                        "extras": null,
                    }
                },
                "fee_numerator":"1000"
            }),
        );

        NATIVE_COIN_BSH_CONTRACT.initialize(context)
    };

pub static NATIVE_COIN_BSH_CONTRACT_IS_DEPLOYED_AND_INITIALIZED: fn(Context) -> Context =
    |context: Context| {
        context
            .pipe(NATIVE_COIN_BSH_CONTRACT_IS_DEPLOYED)
            .pipe(NATIVE_COIN_BSH_CONTRACT_IS_INITIALZIED)
    };

pub static USER_INVOKES_SET_FEE_RATIO_IN_NATIVE_COIN_BSH: fn(Context) -> Context =
    |context: Context| NATIVE_COIN_BSH_CONTRACT.set_fee_ratio(context);

pub static USER_INVOKES_REGISTER_NEW_TOKEN_IN_NATIVE_COIN_BSH: fn(Context) -> Context =
    |context: Context| NATIVE_COIN_BSH_CONTRACT.register(context);

pub static USER_INVOKES_GET_ACCUMLATED_FEES_IN_NATIVE_COIN_BSH: fn(Context) -> Context =
    |context: Context| NATIVE_COIN_BSH_CONTRACT.accumulated_fees(context);

pub static USER_INVOKES_GET_COIN_ID_IN_NATIVE_COIN_BSH: fn(Context) -> Context = 
    |context: Context| NATIVE_COIN_BSH_CONTRACT.coin_id(context);

    pub static USER_INVOKES_GET_COIN_ID_IN_NATIVE_COIN_BSH_CONTRACT: fn(Context) -> Context = 
    |context: Context| NATIVE_COIN_BSH_CONTRACT.coin_id_error(context);

pub static USER_INVOKES_GET_COINS_IN_NATIVE_COIN_BSH: fn(Context) -> Context =
    |context: Context| NATIVE_COIN_BSH_CONTRACT.coins(context);

pub static USER_HANDLES_FEE_GATHERING_NATIVE_COIN_BSH: fn(Context) -> Context =
    |context: Context| NATIVE_COIN_BSH_CONTRACT.handle_fee_gathering(context);

pub static USER_INVOKES_SEND_BTP_MESSAGE_FROM_NATIVE_COIN_BSH: fn(Context) -> Context =
    |context: Context| NATIVE_COIN_BSH_CONTRACT.handle_btp_message(context);

pub static USER_INVOKES_CALCULATE_TOKEN_TRANFER_FEE_IN_NATIVE_COIN_BSH: fn(Context) -> Context =
    |context: Context| NATIVE_COIN_BSH_CONTRACT.calculate_token_transfer_fee(context);

pub static USER_INVOKES_GET_BALANCE_IN_NATIVE_COIN_BSH: fn(Context) -> Context =
    |context: Context| NATIVE_COIN_BSH_CONTRACT.get_balance_of(context);

pub static USER_INVOKES_GET_LOCKED_BALANCE_IN_NATIVE_COIN_BSH: fn(Context) -> Context =
    |context: Context| NATIVE_COIN_BSH_CONTRACT.locked_balance_of(context);

pub static USER_INVOKES_GET_OWNERS_IN_NATIVE_COIN_BSH: fn(Context) -> Context =
    |context: Context| NATIVE_COIN_BSH_CONTRACT.get_owners(context);

pub static USER_INVOKES_ADD_OWNER_IN_NATIVE_COIN_BSH: fn(Context) -> Context =
    |context: Context| NATIVE_COIN_BSH_CONTRACT.add_owner(context);

pub static USER_INVOKES_REMOVE_OWNER_IN_NATIVE_COIN_BSH: fn(Context) -> Context =
    |context: Context| NATIVE_COIN_BSH_CONTRACT.remove_owner(context);

pub static USER_INVOKES_TRANSFER_FEES_TO_FEE_AGGREGATOR_IN_NATIVE_COIN_BSH: fn(Context) -> Context =
    |context: Context| NATIVE_COIN_BSH_CONTRACT.transfer_fees(context);

pub static USER_INVOKES_REGISTER_NEW_COIN_IN_NATIVE_COIN_BSH: fn(Context) -> Context =
    |context: Context| NATIVE_COIN_BSH_CONTRACT.register(context);