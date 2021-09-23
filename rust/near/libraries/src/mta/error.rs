//! Error types

use thiserror::Error;

#[derive(Clone, Debug, Eq, Error, PartialEq)]
pub enum MtaError {
    /// Serialization error
    #[error("Serialization error: _0")]
    HashSerialize(String),
    /// None error
    #[error("Option.None an error")]
    NoneError,
}
