use crate::types::{message::ServiceMessage, BTPAddress, WrappedI128};
use btp_common::errors::BMCError;
use near_sdk::{
    base64::{self, URL_SAFE_NO_PAD}, // TODO: Confirm
    borsh::{self, maybestd::io, BorshDeserialize, BorshSerialize},
    serde::{de, ser, Deserialize, Serialize},
};
use rlp::{self, Decodable, Encodable};
use std::convert::TryFrom;
use std::vec::IntoIter;

#[derive(Clone, PartialEq, Eq, Debug)]
pub struct BtpMessage<T: ServiceMessage> {
    source: BTPAddress,
    destination: BTPAddress,
    service: String,
    serial_no: WrappedI128,
    payload: Vec<u8>,
    service_message: Option<T>,
}

impl<T> BtpMessage<T>
where
    T: ServiceMessage,
{
    pub fn new(
        source: BTPAddress,
        destination: BTPAddress,
        service: String,
        serial_no: WrappedI128,
        payload: Vec<u8>,
        service_message: Option<T>,
    ) -> Self {
        Self {
            source,
            destination,
            service,
            serial_no,
            payload,
            service_message,
        }
    }

    pub fn source(&self) -> &BTPAddress {
        &self.source
    }

    pub fn destination(&self) -> &BTPAddress {
        &self.destination
    }

    pub fn service(&self) -> &String {
        &self.service
    }

    pub fn serial_no(&self) -> &WrappedI128 {
        &self.serial_no
    }

    pub fn payload(&self) -> &Vec<u8> {
        &self.payload
    }

    pub fn service_message(&self) -> &Option<T> {
        &self.service_message
    }
}

pub type SerializedBtpMessages = Vec<BtpMessage<SerializedMessage>>;

pub struct SerializedMessage {}

impl ServiceMessage for SerializedMessage {}

impl Decodable for BtpMessage<SerializedMessage> {
    fn decode(rlp: &rlp::Rlp) -> Result<Self, rlp::DecoderError> {
        Ok(Self {
            source: rlp.val_at::<BTPAddress>(0)?,
            destination: rlp.val_at::<BTPAddress>(1)?,
            service: rlp.val_at::<String>(2)?,
            serial_no: rlp.val_at::<WrappedI128>(3)?,
            payload: rlp.val_at::<Vec<u8>>(4)?,
            service_message: None,
        })
    }
}

impl From<&BtpMessage<SerializedMessage>> for String {
    fn from(btp_message: &BtpMessage<SerializedMessage>) -> Self {
        base64::encode_config(btp_message.payload.clone(), URL_SAFE_NO_PAD)
    }
}

impl TryFrom<Vec<u8>> for BtpMessage<SerializedMessage> {
    type Error = BMCError;
    fn try_from(value: Vec<u8>) -> Result<Self, Self::Error> {
        let rlp = rlp::Rlp::new(&value);
        Self::decode(&rlp).map_err(|error| BMCError::DecodeFailed {
            message: format!("rlp: {}", error),
        })
    }
}

impl Serialize for BtpMessage<SerializedMessage> {
    fn serialize<S>(&self, serializer: S) -> Result<S::Ok, <S as ser::Serializer>::Error>
    where
        S: ser::Serializer,
    {
        serializer.serialize_str(&String::from(self))
    }
}

impl<'de> Deserialize<'de> for BtpMessage<SerializedMessage> {
    fn deserialize<D>(deserializer: D) -> Result<Self, <D as de::Deserializer<'de>>::Error>
    where
        D: de::Deserializer<'de>,
    {
        <Vec<u8> as Deserialize>::deserialize(deserializer)
            .and_then(|s| Self::try_from(s).map_err(de::Error::custom))
    }
}
