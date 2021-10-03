mod native_coin_service;
mod token_service;
pub mod bmc_service;
pub use bmc_service::*;
mod btp_message;
pub use btp_message::*;
mod service_message;
pub use service_message::{ServiceMessage};
// pub mod service_messages {
//     pub mod native_coin_service;
// }