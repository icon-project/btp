use crate::types::{
    messages::BtpMessage, messages::Message, messages::SerializedMessage, BTPAddress, WrappedI128,
};
use rlp::{self, Decodable, Encodable};

#[derive(Default, Debug, PartialEq, Eq, Clone)]
pub struct ErrorMessage {
    code: u32,
    message: String,
}

impl Message for ErrorMessage {}

impl ErrorMessage {
    pub fn new(code: u32, message: String) -> Self {
        Self { code, message }
    }
}

impl From<ErrorMessage> for Vec<u8> {
    fn from(error_message: ErrorMessage) -> Self {
        rlp::encode(&error_message).to_vec()
    }
}

impl From<BtpMessage<ErrorMessage>> for BtpMessage<SerializedMessage> {
    fn from(value: BtpMessage<ErrorMessage>) -> Self {
        Self::new(
            value.source().clone(),
            value.destination().clone(),
            value.service().clone(),
            value.serial_no().clone(),
            value.message().clone().unwrap().into(),
            None,
        )
    }
}

impl Encodable for ErrorMessage {
    fn rlp_append(&self, stream: &mut rlp::RlpStream) {
        stream
            .begin_unbounded_list()
            .append(&self.code)
            .append(&self.message)
            .finalize_unbounded_list();
    }
}
