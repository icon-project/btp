use crate::types::{
    messages::BtpMessage, messages::Message, messages::SerializedMessage, BTPAddress, WrappedI128,
};
use rlp::{self, Decodable, Encodable};
use std::convert::TryFrom;

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

impl Decodable for ErrorMessage {
    fn decode(rlp: &rlp::Rlp) -> Result<Self, rlp::DecoderError> {
        Ok(Self {
            code: rlp.val_at(0)?,
            message: rlp.val_at(1)?,
        })
    }
}

impl TryFrom<&Vec<u8>> for ErrorMessage {
    type Error = String;
    fn try_from(value: &Vec<u8>) -> Result<Self, Self::Error> {
        let rlp = rlp::Rlp::new(value as &[u8]);
        Self::decode(&rlp).map_err(|error| format!("rlp: {}", error))
    }
}

impl TryFrom<BtpMessage<SerializedMessage>> for BtpMessage<ErrorMessage> {
    type Error = String;
    fn try_from(value: BtpMessage<SerializedMessage>) -> Result<Self, Self::Error> {
        Ok(Self::new(
            value.source().clone(),
            value.destination().clone(),
            value.service().clone(),
            value.serial_no().clone(),
            value.payload().clone(),
            Some(ErrorMessage::try_from(value.payload())?),
        ))
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
