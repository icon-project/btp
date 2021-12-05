#[derive(Clone, Debug, Eq, PartialEq)]
pub enum MptError {
    HashMismatch,
    EncodeFailed,
    DecodeFailed { message: String }
}