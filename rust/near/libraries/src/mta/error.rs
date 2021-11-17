#[derive(Clone, Debug, Eq, PartialEq)]
pub enum MtaError {
    HashSerialize(String),
    NoneError,
    InvalidWitnessOld { message: &'static str },
    InvalidWitnessNewer { message: &'static str },
}
