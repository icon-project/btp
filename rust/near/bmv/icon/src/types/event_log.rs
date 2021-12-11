use super::Nullable;
use libraries::rlp::{self, Decodable, Encodable};
use libraries::types::{BTPAddress, Message};
use near_sdk::json_types::{Base64VecU8, U128};
use std::str::from_utf8;
use std::str::FromStr;

#[derive(Debug)]
pub struct EventLog {
    address: Nullable<Vec<u8>>,
    indexed: Vec<Vec<u8>>,
    data: Vec<Vec<u8>>,
}

impl Decodable for EventLog {
    fn decode(rlp: &rlp::Rlp) -> Result<Self, rlp::DecoderError> {
        Ok(Self {
            address: rlp.val_at(0)?,
            indexed: rlp.list_at(1)?,
            data: rlp.list_at(2)?,
        })
    }
}

impl EventLog {
    pub fn address(&self) -> &Nullable<Vec<u8>> {
        &self.address
    }

    pub fn is_message(&self) -> bool {
        if let Ok(method_signature) = rlp::decode::<String>(&self.indexed[0]) {
            if method_signature.contains("Message") {
                return true;
            }
            false
        } else {
            false
        }
    }

    pub fn to_message(&self) -> Result<Message, rlp::DecoderError> {
        let next: BTPAddress = rlp::decode(&self.indexed[1])?;
        let sequence: u128 = rlp::decode(&self.indexed[2])?;
        let message = Base64VecU8::from(self.data[0].clone());
        Ok(Message::new(next, sequence.into(), message))
    }
}
