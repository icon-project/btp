use crate::types::{messages::SerializedMessage, messages::BtpMessage, messages::Message, Asset, BTPAddress};
use btp_common::errors::BshError;
use rlp::{self, Decodable, Encodable};
use std::convert::TryFrom;

#[derive(Clone, PartialEq, Eq, Debug)]
pub enum NativeCoinServiceType {
    RequestCoinTransfer {
        source: String,
        destination: String,
        assets: Vec<Asset>,
    },
    RequestCoinRegister,
    ResponseHandleService {
        code: u128,
        message: String,
    },
    UnknownType,
}

impl Default for NativeCoinServiceType {
    fn default() -> Self {
        Self::UnknownType
    }
}

impl NativeCoinServiceMessage {
    pub fn new(service_type: NativeCoinServiceType) -> Self {
        Self { service_type }
    }

    pub fn service_type(&self) -> &NativeCoinServiceType {
        &self.service_type
    }
}

impl Encodable for NativeCoinServiceMessage {
    fn rlp_append(&self, stream: &mut rlp::RlpStream) {
        stream.begin_unbounded_list();
        match self.service_type() {
            &NativeCoinServiceType::RequestCoinTransfer {
                ref source,
                ref destination,
                ref assets,
            } => {
                let mut params = rlp::RlpStream::new_list(3);
                params
                    .append(source)
                    .append(destination)
                    .append_list(assets);
                stream.append::<u128>(&0).append(&params.out());
            }
            _ => (),
        }
        stream.finalize_unbounded_list();
    }
}

impl Decodable for NativeCoinServiceMessage {
    fn decode(rlp: &rlp::Rlp) -> Result<Self, rlp::DecoderError> {
        Ok(Self {
            service_type: NativeCoinServiceType::try_from((rlp.val_at(0)?, &rlp.val_at(1)?))?,
        })
    }
}

impl TryFrom<(u128, &Vec<u8>)> for NativeCoinServiceType {
    type Error = rlp::DecoderError;
    fn try_from((index, payload): (u128, &Vec<u8>)) -> Result<Self, Self::Error> {
        let payload = rlp::Rlp::new(payload as &[u8]);
        match index {
            0 => Ok(Self::RequestCoinTransfer {
                source: payload.val_at(0)?,
                destination: payload.val_at(1)?,
                assets: payload.list_at(2)?,
            }),
            _ => Ok(Self::UnknownType),
        }
    }
}

impl Message for NativeCoinServiceMessage {}

impl From<NativeCoinServiceMessage> for SerializedMessage {
    fn from(message: NativeCoinServiceMessage) -> Self {
        Self::new(rlp::encode(&message).to_vec())
    }
}

impl TryFrom<&Vec<u8>> for NativeCoinServiceMessage {
    type Error = BshError;
    fn try_from(value: &Vec<u8>) -> Result<Self, Self::Error> {
        let rlp = rlp::Rlp::new(value as &[u8]);
        Self::decode(&rlp).map_err(|error| BshError::DecodeFailed {
            message: format!("rlp: {}", error),
        })
    }
}

impl TryFrom<BtpMessage<SerializedMessage>> for BtpMessage<NativeCoinServiceMessage> {
    type Error = BshError;
    fn try_from(value: BtpMessage<SerializedMessage>) -> Result<Self, Self::Error> {
        Ok(Self::new(
            value.source().clone(),
            value.destination().clone(),
            value.service().clone(),
            value.serial_no().clone(),
            value.payload().clone(),
            Some(NativeCoinServiceMessage::try_from(value.payload())?),
        ))
    }
}

#[derive(Clone, PartialEq, Eq, Debug)]
pub struct NativeCoinServiceMessage {
    service_type: NativeCoinServiceType,
}
