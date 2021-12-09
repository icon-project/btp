use btp_common::errors::BmvError;
use libraries::mta::error::MtaError;

pub trait ToBmvError {
    fn to_bmv_error(&self) -> BmvError;
}

impl ToBmvError for MtaError {
    fn to_bmv_error(&self) -> BmvError {
        match self {
            MtaError::InvalidWitnessOld { message } => BmvError::InvalidWitnessOld { message: message.to_string() },
            MtaError::InvalidWitnessNewer { message } => BmvError::InvalidWitnessNewer { message: message.to_string() },
            _ => BmvError::Unknown { message: "MtaError".to_string() }
        }
    }
}