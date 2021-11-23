#[derive(Clone, Debug, Eq, PartialEq)]
pub enum MptError {
    EncodeFailed,
    DecodeFailed { message: String }
}