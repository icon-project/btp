use lazy_static::lazy_static;
pub use std::collections::HashSet;
use test_helper::types::{
    Context, Contract, NativeCoinBsh, NativeCoinBshContract, TokenBsh, TokenBshContract,
};

pub static NEW_CONTEXT: fn() -> Context = || Context::new();
