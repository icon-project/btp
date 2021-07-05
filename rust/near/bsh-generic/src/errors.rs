use num_derive::FromPrimitive;
use thiserror::Error;

/// Errors that may be returned by a BSH contract.
#[derive(Clone, Debug, Eq, Error, FromPrimitive, PartialEq)]
pub enum BSHError {
    #[error("The supplied BTP address is invalid")]
    InvalidBtpAddress,
}
