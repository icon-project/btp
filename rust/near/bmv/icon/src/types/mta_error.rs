use btp_common::errors::BmvError;
use libraries::mta::error::MtaError;

pub trait ToBmvError {
    fn to_bmv_error(&self) -> BmvError;
}

impl ToBmvError for MtaError {
    fn to_bmv_error(&self) -> BmvError {
        match self {
            MtaError::InvalidWitnessOld { message } => BmvError::InvalidWitnessOld { message },
            MtaError::InvalidWitnessNewer { message } => BmvError::InvalidWitnessNewer { message },
            _ => BmvError::Unknown
        }
    }
}