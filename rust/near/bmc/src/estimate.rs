use near_sdk::{Balance, Gas};

pub const SEND_MESSAGE: Gas = Gas(10_000_000_000_000);
pub const GATHER_FEE: Gas = Gas(1_000_000_000_000);
pub const NO_DEPOSIT: Balance = 0;