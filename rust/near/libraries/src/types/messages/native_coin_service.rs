use crate::types::{
    messages::BtpMessage, messages::Message, messages::SerializedMessage, Asset, BTPAddress,
};
use btp_common::errors::BshError;
use near_sdk::base64::{self, URL_SAFE_NO_PAD};
use rlp::{self, Decodable, Encodable};
use std::convert::TryFrom;

#[derive(Clone, PartialEq, Eq, Debug)]
pub enum NativeCoinServiceType {
    RequestCoinTransfer {
        sender: String,
        receiver: String,
        assets: Vec<Asset>,
    },
    RequestCoinRegister,
    ResponseHandleService {
        code: u128,
        message: String,
    },
    UnknownType,
    UnhandledType,
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
                ref sender,
                ref receiver,
                ref assets,
            } => {
                let mut params = rlp::RlpStream::new_list(3);
                params
                    .append::<String>(sender)
                    .append::<String>(receiver)
                    .append_list(assets);
                stream.append::<u128>(&0).append(&params.out());
            }
            &NativeCoinServiceType::ResponseHandleService {
                ref code,
                ref message,
            } => {
                let mut params = rlp::RlpStream::new_list(2);
                params.append::<u128>(code).append::<String>(message);
                stream.append::<u128>(&2).append(&params.out());
            }
            _ => (),
        }
        stream.finalize_unbounded_list();
    }
}

impl Decodable for NativeCoinServiceMessage {
    fn decode(rlp: &rlp::Rlp) -> Result<Self, rlp::DecoderError> {
        Ok(Self {
            service_type: NativeCoinServiceType::try_from((
                rlp.val_at::<u128>(0)?,
                &rlp.val_at(1)?,
            ))?,
        })
    }
}

impl TryFrom<(u128, &Vec<u8>)> for NativeCoinServiceType {
    type Error = rlp::DecoderError;
    fn try_from((index, payload): (u128, &Vec<u8>)) -> Result<Self, Self::Error> {
        let payload = rlp::Rlp::new(payload as &[u8]);
        match index {
            0 => Ok(Self::RequestCoinTransfer {
                sender: payload.val_at(0)?,
                receiver: payload.val_at(1)?,
                assets: payload.list_at(2)?,
            }),
            2 => Ok(Self::ResponseHandleService {
                code: payload.val_at(0)?,
                message: payload.val_at(1)?,
            }),
            3 => Ok(Self::UnknownType),
            _ => Ok(Self::UnhandledType)
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

impl TryFrom<SerializedMessage> for NativeCoinServiceMessage {
    type Error = BshError;
    fn try_from(value: SerializedMessage) -> Result<Self, Self::Error> {
        Self::try_from(value.data())
    }
}

impl From<NativeCoinServiceMessage> for Vec<u8> {
    fn from(service_message: NativeCoinServiceMessage) -> Self {
        rlp::encode(&service_message).to_vec()
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

impl TryFrom<&BtpMessage<NativeCoinServiceMessage>> for BtpMessage<SerializedMessage> {
    type Error = BshError;
    fn try_from(value: &BtpMessage<NativeCoinServiceMessage>) -> Result<Self, Self::Error> {
        Ok(Self::new(
            value.source().clone(),
            value.destination().clone(),
            value.service().clone(),
            value.serial_no().clone(),
            value
                .message()
                .clone()
                .ok_or(BshError::EncodeFailed {
                    message: "Encoding Failed".to_string(),
                })?
                .into(),
            None,
        ))
    }
}

impl TryFrom<String> for NativeCoinServiceMessage {
    type Error = BshError;
    fn try_from(value: String) -> Result<Self, Self::Error> {
        let decoded = base64::decode_config(value, URL_SAFE_NO_PAD).map_err(|error| {
            BshError::DecodeFailed {
                message: format!("base64: {}", error),
            }
        })?;
        let rlp = rlp::Rlp::new(&decoded);
        Self::decode(&rlp).map_err(|error| BshError::DecodeFailed {
            message: format!("rlp: {}", error),
        })
    }
}

impl From<&NativeCoinServiceMessage> for String {
    fn from(service_message: &NativeCoinServiceMessage) -> Self {
        let rlp = rlp::encode(service_message);
        base64::encode_config(rlp, URL_SAFE_NO_PAD)
    }
}

#[derive(Clone, PartialEq, Eq, Debug)]
pub struct NativeCoinServiceMessage {
    service_type: NativeCoinServiceType,
}

#[cfg(test)]
mod tests {
    use super::{BTPAddress, NativeCoinServiceMessage, NativeCoinServiceType};
    use crate::types::{
        messages::{btp_message::BtpMessage, SerializedMessage},
        Asset, WrappedI128,
    };
    use std::convert::TryFrom;

    #[test]
    fn deserialize_transfer_request_message() {
        let service_message = NativeCoinServiceMessage::try_from(
            "-H2AuHr4eLhAODhiZDA1NDQyNjg2YmUwYTVkZjdkYTMzYjZmMTA4OWViZmVhMzc2OWIxOWRiYjI0NzdmZTBjZDZlMGYxMjZlNKpjeDg3ZWQ5MDQ4YjU5NGI5NTE5OWYzMjZmYzc2ZTc2YTlkMzNkZDY2NWLKyYRORUFSggOEYw".to_string(),
        )
        .unwrap();

        assert_eq!(
            service_message,
            NativeCoinServiceMessage {
                service_type: NativeCoinServiceType::RequestCoinTransfer {
                    sender: "88bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4"
                        .to_string(),
                    receiver: "cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string(),
                    assets: vec![Asset::new("NEAR".to_string(), 900, 99)]
                },
            },
        );
    }

    #[test]
    fn serialize_transfer_request_message() {
        let service_message = NativeCoinServiceMessage {
            service_type: NativeCoinServiceType::RequestCoinTransfer {
                sender: "88bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4"
                    .to_string(),
                receiver: "cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string(),
                assets: vec![Asset::new("NEAR".to_string(), 900, 99)],
            },
        };

        assert_eq!(
            String::from(&service_message),
            "-H2AuHr4eLhAODhiZDA1NDQyNjg2YmUwYTVkZjdkYTMzYjZmMTA4OWViZmVhMzc2OWIxOWRiYjI0NzdmZTBjZDZlMGYxMjZlNKpjeDg3ZWQ5MDQ4YjU5NGI5NTE5OWYzMjZmYzc2ZTc2YTlkMzNkZDY2NWLKyYRORUFSggOEYw".to_string(),
        );
    }
}
