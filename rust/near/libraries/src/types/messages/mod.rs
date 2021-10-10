mod native_coin_service;
pub use native_coin_service::*;
mod token_service;
pub mod bmc_service;
pub use bmc_service::*;
mod btp_message;
pub use btp_message::*;
mod error_message;
pub use error_message::*;
pub trait Message {}
// pub mod service_messages {
//     pub mod native_coin_service;
// }