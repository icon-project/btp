pub use btp_common::btp_address::{Address, validate_btp_address};
use near_sdk::{
    borsh::{maybestd::io, self, BorshDeserialize, BorshSerialize},
    serde::{de, Deserialize, Serialize},
    AccountId,
};
use std::convert::TryFrom;

pub trait Account {
    fn account_id(&self) -> AccountId;
}

#[derive(Default, BorshSerialize, Serialize, Debug, Eq, PartialEq, PartialOrd, Hash, Clone)]
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
        self
            .contract_address()
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

impl std::str::FromStr for BTPAddress {
    type Err = String;
    fn from_str(value: &str) -> Result<Self, Self::Err> {
        validate_btp_address(value)?;
        Ok(Self(value.to_string()))
    }
}
