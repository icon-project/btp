use super::HashedCollection;
pub use btp_common::btp_address::{validate_btp_address, Address};
use near_sdk::{
    borsh::{self, maybestd::io, BorshDeserialize, BorshSerialize},
    serde::{de, Deserialize, Serialize},
    serde_json::Value,
    AccountId,
};
use rlp::{self, Decodable, Encodable};
use std::convert::TryFrom;
use std::fmt::{self, Error, Formatter};
use std::iter::FromIterator;
use std::str;

pub trait Account {
    fn account_id(&self) -> AccountId;
}

#[derive(Default, BorshSerialize, Serialize, Debug, Eq, PartialEq, PartialOrd, Hash, Clone)]
#[serde(crate = "near_sdk::serde")]
pub struct BTPAddress(String);

impl BTPAddress {
    pub fn new(string: String) -> Self {
        Self(string)
    }

    pub fn to_string(&self) -> String {
        self.0.to_owned()
    }
}
impl Address for BTPAddress {
    fn as_ref(&self) -> &String {
        &self.0
    }
}
impl Account for BTPAddress {
    fn account_id(&self) -> AccountId {
        self.contract_address()
            .unwrap()
            .parse::<AccountId>()
            .unwrap()
    }
}

impl<'de> Deserialize<'de> for BTPAddress {
    fn deserialize<D>(deserializer: D) -> Result<Self, <D as de::Deserializer<'de>>::Error>
    where
        D: de::Deserializer<'de>,
    {
        <String as Deserialize>::deserialize(deserializer)
            .and_then(|s| Self::try_from(s).map_err(de::Error::custom))
    }
}

impl BorshDeserialize for BTPAddress {
    fn deserialize(buf: &mut &[u8]) -> io::Result<Self> {
        <String as BorshDeserialize>::deserialize(buf).and_then(|s| {
            Self::try_from(s).map_err(|e| io::Error::new(io::ErrorKind::InvalidData, e))
        })
    }
}

impl TryFrom<String> for BTPAddress {
    type Error = String;
    fn try_from(value: String) -> Result<Self, Self::Error> {
        validate_btp_address(value.as_str())?;
        Ok(Self(value))
    }
}

impl From<BTPAddress> for Value {
    fn from(value: BTPAddress) -> Value {
        value.to_string().into()
    }
}

impl std::str::FromStr for BTPAddress {
    type Err = String;
    fn from_str(value: &str) -> Result<Self, Self::Err> {
        validate_btp_address(value)?;
        Ok(Self(value.to_string()))
    }
}

impl Decodable for BTPAddress {
    fn decode(rlp: &rlp::Rlp) -> Result<Self, rlp::DecoderError> {
        Ok(Self::try_from(rlp.as_val::<String>()?)
            .map_err(|_| rlp::DecoderError::Custom("BTPAddress Decode Error"))?)
    }
}

impl Encodable for BTPAddress {
    fn rlp_append(&self, stream: &mut rlp::RlpStream) {
        stream.append_internal(&self.to_string());
    }
}

impl FromIterator<BTPAddress> for HashedCollection<BTPAddress> {
    fn from_iter<I: IntoIterator<Item = BTPAddress>>(iter: I) -> Self {
        let mut c = HashedCollection::new();
        for i in iter {
            c.add(i);
        }
        c
    }
}

impl fmt::Display for BTPAddress {
    fn fmt(&self, f: &mut Formatter<'_>) -> Result<(), Error> {
        write!(f, "{}", self.0)
    }
}
