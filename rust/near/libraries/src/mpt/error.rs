#[derive(Clone, Debug, Eq, PartialEq)]
pub enum MptError {
    InvalidLength,
    HashMismatch,
    EncodeFailed,
    DecodeFailed { message: String }
}