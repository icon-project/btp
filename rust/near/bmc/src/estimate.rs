use near_sdk::{Balance, Gas};

pub const HANDLE_RELAY_MESSAGE: Gas = Gas(100_000_000_000_000);
pub const HANDLE_RELAY_MESSAGE_BMV_CALLBACK: Gas = Gas(180_000_000_000_000);
pub const SEND_MESSAGE: Gas = Gas(5_000_000_000_000);
pub const BSH_HANDLE_BTP_MESSAGE : Gas = Gas(150_000_000_000_000);
pub const GATHER_FEE: Gas = Gas(1_000_000_000_000);
pub const NO_DEPOSIT: Balance = 0;
pub const BMV_GET_STATUS: Gas = Gas(1_000_000_000_000);
pub const BMC_SET_LINK_BMV_CALLBACK: Gas = Gas(5_000_000_000_000);