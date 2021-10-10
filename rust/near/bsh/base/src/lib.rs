use btp_common::errors::{BtpException, Exception};
use libraries::types::{messages::SerializedMessage, BTPAddress, Requests};
use near_sdk::borsh::{self, BorshDeserialize, BorshSerialize};
use near_sdk::AccountId;
use near_sdk::{
    env,
    json_types::{Base64VecU8, U128, U64},
    log, near_bindgen, require, serde_json, Gas,
};

#[derive(BorshDeserialize, BorshSerialize)]
pub struct BaseService {
    requests: Requests,
    name: String,
    bmc: AccountId,
    serial_no: u128,
}

impl BaseService {
    pub fn new(name: String, bmc: AccountId) -> BaseService {
        BaseService {
            name,
            bmc,
            serial_no: Default::default(),
            requests: Requests::new(),
        }
    }

    pub fn bmc(&self) -> &AccountId {
        &self.bmc
    }

    pub fn name(&self) -> &String {
        &self.name
    }

    pub fn send_service_message(
        &mut self,
        destination_network: String,
        message: SerializedMessage,
    ) {
        self.serial_no
            .clone_from(&self.serial_no.checked_add(1).unwrap());
        self.requests.add(self.serial_no, &message);
    }

    pub fn send_response_message() {}
    pub fn handle_service_message() {}
    pub fn handle_service_requests() {}
    pub fn handle_service_response() {}
    pub fn handle_btp_error() {}
    pub fn handle_fee_gathering() {}
}
