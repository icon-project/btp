use crate::{BTPAddress, BtpMessage, SerializedMessage};
use near_sdk::ext_contract;

#[ext_contract(bsh_contract)]
pub trait BshContract {
    fn handle_btp_message(message: BtpMessage<SerializedMessage>);
    fn handle_btp_error(
        source: BTPAddress,
        service: String,
        serial_no: i128,
        code: u128,
        message: String,
    );
    fn handle_fee_gathering(fee_aggregator: BTPAddress, service: String);
}
