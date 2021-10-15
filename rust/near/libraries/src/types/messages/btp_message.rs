use crate::types::{messages::Message, BTPAddress, WrappedI128};
use btp_common::errors::BmcError;
use near_sdk::{
    base64::{self, URL_SAFE_NO_PAD}, // TODO: Confirm
    borsh::{self, maybestd::io, BorshDeserialize, BorshSerialize},
    serde::{de, ser, Deserialize, Serialize},
};
use rlp::{self, Decodable, Encodable};
use std::convert::TryFrom;
use std::vec::IntoIter;

#[derive(Clone, PartialEq, Eq, Debug)]
pub struct BtpMessage<T: Message> {
    source: BTPAddress,
    destination: BTPAddress,
    service: String,
    serial_no: WrappedI128,
    payload: Vec<u8>,
    message: Option<T>,
}

impl<T> BtpMessage<T>
where
    T: Message,
{
    pub fn new(
        source: BTPAddress,
        destination: BTPAddress,
        service: String,
        serial_no: WrappedI128,
        payload: Vec<u8>,
        message: Option<T>,
    ) -> Self {
        Self {
            source,
            destination,
            service,
            serial_no,
            payload,
            message,
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

    pub fn message(&self) -> &Option<T> {
        &self.message
    }
}

pub type SerializedBtpMessages = Vec<BtpMessage<SerializedMessage>>;

#[derive(Clone, BorshDeserialize, BorshSerialize, PartialEq, Eq, Debug)]
pub struct SerializedMessage(Vec<u8>);

impl SerializedMessage {
    pub fn new(data: Vec<u8>) -> Self {
        Self(data)
    }

    pub fn data(&self) -> &Vec<u8> {
        &self.0
    }
}

impl Message for SerializedMessage {}

impl Encodable for BtpMessage<SerializedMessage> {
    fn rlp_append(&self, stream: &mut rlp::RlpStream) {
        stream
        .begin_unbounded_list()
        .append(self.source())
        .append(self.destination())
        .append(self.service())
        .append(self.serial_no())
        .append(self.payload())
        .finalize_unbounded_list()
    }
}

impl Decodable for BtpMessage<SerializedMessage> {
    fn decode(rlp: &rlp::Rlp) -> Result<Self, rlp::DecoderError> {
        Ok(Self {
            source: rlp.val_at::<BTPAddress>(0)?,
            destination: rlp.val_at::<BTPAddress>(1)?,
            service: rlp.val_at::<String>(2)?,
            serial_no: rlp.val_at::<WrappedI128>(3)?,
            payload: rlp.val_at::<Vec<u8>>(4)?,
            message: None,
        })
    }
}

impl From<&BtpMessage<SerializedMessage>> for String {
    fn from(btp_message: &BtpMessage<SerializedMessage>) -> Self {
        let rlp = rlp::encode(btp_message);
        base64::encode_config(rlp, URL_SAFE_NO_PAD)
    }
}

impl From<BtpMessage<SerializedMessage>> for Vec<u8> {
    fn from(btp_message: BtpMessage<SerializedMessage>) -> Self {
        rlp::encode(&btp_message).to_vec()
    }
}

impl TryFrom<String> for BtpMessage<SerializedMessage> {
    type Error = BmcError;
    fn try_from(value: String) -> Result<Self, Self::Error> {
        let decoded = base64::decode_config(value, URL_SAFE_NO_PAD).map_err(|error| {
            BmcError::DecodeFailed {
                message: format!("base64: {}", error),
            }
        })?;
        let rlp = rlp::Rlp::new(&decoded);
        Self::decode(&rlp).map_err(|error| BmcError::DecodeFailed {
            message: format!("rlp: {}", error),
        })
    }
}

impl TryFrom<Vec<u8>> for BtpMessage<SerializedMessage> {
    type Error = String;
    fn try_from(value: Vec<u8>) -> Result<Self, Self::Error> {
        let rlp = rlp::Rlp::new(&value);
        Self::decode(&rlp).map_err(|error| format!("rlp: {}", error))
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
        <String as Deserialize>::deserialize(deserializer)
            .and_then(|s| Self::try_from(s).map_err(de::Error::custom))
    }
}
