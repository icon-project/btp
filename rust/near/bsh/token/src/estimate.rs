use near_sdk::{Balance, Gas};

pub const GAS_FOR_FT_TRANSFER: Gas = Gas(5_000_000_000_000);
pub const GAS_FOR_RESOLVE_TRANSFER: Gas = Gas(5_000_000_000_000);
pub const GAS_FOR_MT_TRANSFER_CALL: Gas = Gas(25_000_000_000_000);
pub const GAS_FOR_SEND_SERVICE_MESSAGE: Gas = Gas(25_000_000_000_000);
pub const NO_DEPOSIT: Balance = 0;