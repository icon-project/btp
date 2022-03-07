use crate::{BTPAddress, BtpMessage, SerializedMessage};
use near_sdk::ext_contract;

#[ext_contract(bmc_contract)]
pub trait BmcContract {
    fn emit_message(link: BTPAddress, btp_message: BtpMessage<SerializedMessage>);
    fn emit_error();
    fn set_link_bmv_callback(
        link: BTPAddress,
        block_interval: u64,
        max_aggregation: u64,
        delay_limit: u64,
        #[callback] verifier_status: VerifierStatus,
    );
    fn handle_relay_message_bmv_callback(source: BTPAddress, #[callback] verifier_response: VerifierResponse, relay: AccountId);
    fn handle_external_service_message_callback(source: BTPAddress, btp_message: BtpMessage<SerializedMessage>);
}
