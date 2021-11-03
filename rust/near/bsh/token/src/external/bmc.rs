use libraries::types::messages::SerializedMessage;
use near_sdk::ext_contract;

#[ext_contract(ext_bmc)]
pub trait BtpMessageCenter {
    fn send_service_message(
        serial_no: i128,
        service: String,
        network: String,
        message: SerializedMessage,
    );
}
