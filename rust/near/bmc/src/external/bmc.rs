use near_sdk::{ ext_contract };
use crate::BTPAddress;
use crate::{BtpMessage, SerializedMessage};

#[ext_contract(bmc_contract)]
pub trait BmcContract {
    fn emit_message(link: BTPAddress, btp_message: BtpMessage<SerializedMessage>);
    fn emit_error();
}