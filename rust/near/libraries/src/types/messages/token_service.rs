use crate::types::{messages::BtpMessage, messages::Message, messages::SerializedMessage, Asset};
use btp_common::errors::BshError;
use near_sdk::base64::{self, URL_SAFE_NO_PAD};
use crate::rlp::{self, Decodable, Encodable};
use std::convert::TryFrom;

#[derive(Clone, PartialEq, Eq, Debug)]
pub enum TokenServiceType {
    RequestTokenTransfer {
        sender: String,
        receiver: String,
        assets: Vec<Asset>,
    },
    RequestTokenRegister,
    ResponseHandleService {
        code: u128,
        message: String,
    },
    UnknownType,
    UnhandledType,
}

impl Default for TokenServiceType {
    fn default() -> Self {
        Self::UnknownType
    }
}

impl TokenServiceMessage {
    pub fn new(service_type: TokenServiceType) -> Self {
        Self { service_type }
    }

    pub fn service_type(&self) -> &TokenServiceType {
        &self.service_type
    }
}

impl Encodable for TokenServiceMessage {
    fn rlp_append(&self, stream: &mut rlp::RlpStream) {
        stream.begin_unbounded_list();
        match self.service_type() {
            &TokenServiceType::RequestTokenTransfer {
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
            &TokenServiceType::ResponseHandleService {
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

impl Decodable for TokenServiceMessage {
    fn decode(rlp: &rlp::Rlp) -> Result<Self, rlp::DecoderError> {
        Ok(Self {
            service_type: TokenServiceType::try_from((rlp.val_at::<u128>(0)?, &rlp.val_at(1)?))?,
        })
    }
}

impl TryFrom<(u128, &Vec<u8>)> for TokenServiceType {
    type Error = rlp::DecoderError;
    fn try_from((index, payload): (u128, &Vec<u8>)) -> Result<Self, Self::Error> {
        let payload = rlp::Rlp::new(payload as &[u8]);
        match index {
            0 => Ok(Self::RequestTokenTransfer {
                sender: payload.val_at(0)?,
                receiver: payload.val_at(1)?,
                assets: payload.list_at(2)?,
            }),
            2 => Ok(Self::ResponseHandleService {
                code: payload.val_at(0)?,
                message: payload.val_at(1)?,
            }),
            3 => Ok(Self::UnknownType),
            _ => Ok(Self::UnhandledType),
        }
    }
}

impl Message for TokenServiceMessage {}

impl From<TokenServiceMessage> for SerializedMessage {
    fn from(message: TokenServiceMessage) -> Self {
        Self::new(rlp::encode(&message).to_vec())
    }
}

impl TryFrom<&Vec<u8>> for TokenServiceMessage {
    type Error = BshError;
    fn try_from(value: &Vec<u8>) -> Result<Self, Self::Error> {
        let rlp = rlp::Rlp::new(value as &[u8]);
        Self::decode(&rlp).map_err(|error| BshError::DecodeFailed {
            message: format!("rlp: {}", error),
        })
    }
}

impl TryFrom<SerializedMessage> for TokenServiceMessage {
    type Error = BshError;
    fn try_from(value: SerializedMessage) -> Result<Self, Self::Error> {
        Self::try_from(value.data())
    }
}

impl From<TokenServiceMessage> for Vec<u8> {
    fn from(service_message: TokenServiceMessage) -> Self {
        rlp::encode(&service_message).to_vec()
    }
}

impl TryFrom<BtpMessage<SerializedMessage>> for BtpMessage<TokenServiceMessage> {
    type Error = BshError;
    fn try_from(value: BtpMessage<SerializedMessage>) -> Result<Self, Self::Error> {
        Ok(Self::new(
            value.source().clone(),
            value.destination().clone(),
            value.service().clone(),
            value.serial_no().clone(),
            value.payload().clone(),
            Some(TokenServiceMessage::try_from(value.payload())?),
        ))
    }
}

impl TryFrom<&BtpMessage<TokenServiceMessage>> for BtpMessage<SerializedMessage> {
    type Error = BshError;
    fn try_from(value: &BtpMessage<TokenServiceMessage>) -> Result<Self, Self::Error> {
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

impl TryFrom<String> for TokenServiceMessage {
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

impl From<&TokenServiceMessage> for String {
    fn from(service_message: &TokenServiceMessage) -> Self {
        let rlp = rlp::encode(service_message);
        base64::encode_config(rlp, URL_SAFE_NO_PAD)
    }
}

#[derive(Clone, PartialEq, Eq, Debug)]
pub struct TokenServiceMessage {
    service_type: TokenServiceType,
}

#[cfg(test)]
mod tests {
    use super::{Asset, TokenServiceMessage, TokenServiceType};
    use std::convert::TryFrom;

    #[test]
    fn deserialize_transfer_request_message() {
        let service_message = TokenServiceMessage::try_from(
            "-FoAuFf4VaoweGMyOTRiMUE2MkU4MmQzZjEzNUE4RjliMmY5Y0FFQUEyM2ZiRDZDZjWKMHgxMjM0NTY3ON7JhElDT06DAwqEyYRUUk9OgwSQwMmEUEFSQYMBhEg".to_string(),
        )
        .unwrap();

        assert_eq!(
            service_message,
            TokenServiceMessage {
                service_type: TokenServiceType::RequestTokenTransfer {
                    sender: "0xc294b1A62E82d3f135A8F9b2f9cAEAA23fbD6Cf5"
                        .to_string(),
                    receiver: "0x12345678".to_string(),
                    assets: vec![
                        Asset::new("ICON".to_string(), 199300, 0),
                        Asset::new("TRON".to_string(), 299200, 0),
                        Asset::new("PARA".to_string(), 99400, 0)
                        ]
                },
            },
        );
    }

    #[test]
    fn serialize_transfer_request_message() {
        let service_message = TokenServiceMessage {
            service_type: TokenServiceType::RequestTokenTransfer {
                sender: "0xc294b1A62E82d3f135A8F9b2f9cAEAA23fbD6Cf5"
                    .to_string(),
                receiver: "0x12345678".to_string(),
                assets: vec![
                    Asset::new("ICON".to_string(), 199300, 0),
                    Asset::new("TRON".to_string(), 299200, 0),
                    Asset::new("PARA".to_string(), 99400, 0)
                    ]
            },
        };

        assert_eq!(
            String::from(&service_message),
            "-FoAuFf4VaoweGMyOTRiMUE2MkU4MmQzZjEzNUE4RjliMmY5Y0FFQUEyM2ZiRDZDZjWKMHgxMjM0NTY3ON7JhElDT06DAwqEyYRUUk9OgwSQwMmEUEFSQYMBhEg".to_string(),
        );
    }
}
