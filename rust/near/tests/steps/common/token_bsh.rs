use lazy_static::lazy_static;
use serde_json::json;
use test_helper::types::{Context, Contract, TokenBsh, TokenBshContract};
use crate::steps::{NEAR_NETWORK};

lazy_static! {
    pub static ref TOKEN_BSH_CONTRACT: Contract<'static, TokenBsh> =
    TokenBshContract::new("tokenbsh", "res/TOKEN_BSH_CONTRACT.wasm");
}

pub static TOKEN_BSH_CONTRACT_IS_DEPLOYED: fn(Context) -> Context =
    |context| TOKEN_BSH_CONTRACT.deploy(context);

pub static TOKEN_BSH_CONTRACT_IS_INITIALZIED: fn(Context) -> Context = |mut context: Context| {
    context.add_method_params(
        "new",
        json!({
            "service_name": "token",
            "bmc": context.contracts().get("bmc").id(),
            "network": NEAR_NETWORK,
            "fee_numerator":"1000"
        }),
    );

    TOKEN_BSH_CONTRACT.initialize(context)
};

pub static TOKEN_BSH_CONTRACT_IS_DEPLOYED_AND_INITIALIZED: fn(Context) -> Context =
    |context: Context| {
        context
            .pipe(TOKEN_BSH_CONTRACT_IS_DEPLOYED)
            .pipe(TOKEN_BSH_CONTRACT_IS_INITIALZIED)
    };

    pub static USER_INVOKES_SET_FEE_RATIO_IN_TOKEN_BSH: fn(Context) -> Context =
    |context: Context| TOKEN_BSH_CONTRACT.set_fee_ratio(context);

pub static USER_INVOKES_REGISTER_NEW_TOKEN_IN_TOKEN_BSH: fn(Context) -> Context =
    |context: Context| TOKEN_BSH_CONTRACT.register(context);

pub static USER_INVOKES_GET_ACCUMLATED_FEES_IN_TOKEN_BSH: fn(Context) -> Context =
    |context: Context| TOKEN_BSH_CONTRACT.accumulated_fees(context);

pub static USER_INVOKES_GET_TOKEN_ID_IN_TOKEN_BSH: fn(Context) -> Context =
    |context: Context| TOKEN_BSH_CONTRACT.token_id(context);

    pub static USER_INVOKES_GET_COIN_ID_ERRORS_IN_TOKEN_BSH: fn(Context) -> Context =
    |context: Context| TOKEN_BSH_CONTRACT.tokens_id(context);

pub static USER_INVOKES_GET_COINS_IN_TOKEN_BSH: fn(Context) -> Context =
    |context: Context| TOKEN_BSH_CONTRACT.tokens(context);

pub static USER_HANDLES_FEE_GATHERING_TOKEN_BSH: fn(Context) -> Context =
    |context: Context| TOKEN_BSH_CONTRACT.handle_fee_gathering(context);

    pub static USER_INVOKES_SEND_BTP_MESSAGE_FROM_TOKEN_BSH: fn(Context) -> Context =
    |context: Context| TOKEN_BSH_CONTRACT.handle_btp_message(context, 300000000000000);

pub static USER_INVOKES_CALCULATE_TOKEN_TRANFER_FEE_IN_TOKEN_BSH: fn(Context) -> Context =
    |context: Context| TOKEN_BSH_CONTRACT.calculate_token_transfer_fee(context);

pub static USER_INVOKES_GET_BALANCE_IN_TOKEN_BSH: fn(Context) -> Context =
    |context: Context| TOKEN_BSH_CONTRACT.balance_of(context);

pub static USER_INVOKES_GET_LOCKED_BALANCE_IN_TOKEN_BSH: fn(Context) -> Context =
    |context: Context| TOKEN_BSH_CONTRACT.locked_balance_of(context);

pub static USER_INVOKES_GET_OWNERS_IN_TOKEN_BSH: fn(Context) -> Context =
    |context: Context| TOKEN_BSH_CONTRACT.get_owners(context);

pub static USER_INVOKES_ADD_OWNER_IN_TOKEN_BSH: fn(Context) -> Context =
    |context: Context| TOKEN_BSH_CONTRACT.add_owner(context);

pub static USER_INVOKES_REMOVE_OWNER_IN_TOKEN_BSH: fn(Context) -> Context =
    |context: Context| TOKEN_BSH_CONTRACT.remove_owner(context);

pub static USER_INVOKES_TRANSFER_FEES_TO_FEE_AGGREGATOR_IN_TOKEN_BSH: fn(Context) -> Context =
    |context: Context| TOKEN_BSH_CONTRACT.transfer_fees(context);



    pub static USER_INVOKES_WITHDRAW_IN_TOKEN_BSH: fn(Context) -> Context =
    |context: Context| TOKEN_BSH_CONTRACT.withdraw(context, 300000000000000);
    
pub static USER_INVOKES_TRANSFER_IN_TOKEN_BSH: fn(Context) -> Context =
    |context: Context| TOKEN_BSH_CONTRACT.transfer(context, 300000000000000);
