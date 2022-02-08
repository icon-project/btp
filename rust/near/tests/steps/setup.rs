use lazy_static::lazy_static;
pub use std::collections::HashSet;
use test_helper::types::{
    Context, Contract, NativeCoinBsh, NativeCoinBshContract,
    TokenBsh, TokenBshContract,
};

lazy_static! {
    pub static ref NATIVE_COIN_BSH_CONTRACT: Contract<'static, NativeCoinBsh> =
        NativeCoinBshContract::new("bsh", "res/BMC_CONTRACT.wasm");
    pub static ref TOKEN_BSH_CONTRACT: Contract<'static, TokenBsh> =
        TokenBshContract::new("bsh", "res/BMC_CONTRACT.wasm");
}

pub static NEW_CONTEXT: fn() -> Context = || Context::new();

pub static NATIVE_COIN_BSH_CONTRACT_IS_DEPLOYED: fn(Context) -> Context =
    |context: Context| NATIVE_COIN_BSH_CONTRACT.deploy(context);

pub static TOKEN_BSH_CONTRACT_IS_DEPLOYED: fn(Context) -> Context =
    |context: Context| TOKEN_BSH_CONTRACT.deploy(context);
