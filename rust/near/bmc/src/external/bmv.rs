use near_sdk::ext_contract;
use crate::{VerifierStatus, VerifierResponse, SerializedMessage};

#[ext_contract(bmv_contract)]
pub trait BmvContract {
    fn get_status() -> VerifierStatus;
    fn handle_relay_message(relay_message: SerializedMessage) -> VerifierResponse;
}
