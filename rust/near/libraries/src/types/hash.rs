use std::ops::Deref;
use near_sdk::borsh::{self, BorshDeserialize, BorshSchema, BorshSerialize};
use near_sdk::serde::{Deserialize, Serialize, de};
use rustc_hex::FromHex;
use near_sdk::base64::{self, URL_SAFE_NO_PAD};
use std::convert::TryFrom;
use crate::rlp::{Decodable, Encodable};

pub trait Hasher {
    fn hash(input: &[u8]) -> [u8; 32];
}

#[derive(
    BorshDeserialize,
    BorshSchema,
    BorshSerialize,
    Clone,
    Debug,
    Default,
    PartialEq,
    Eq,
    PartialOrd,
    Ord,
    Serialize,
    Hash,
    Copy,
)]
#[serde(crate = "near_sdk::serde")]
pub struct Hash([u8; 32]);

impl Hash {
    /// Create Hash from bytes
    pub fn new<H: Hasher>(data: &[u8]) -> Self {
        Self(Self::hash::<H>(data))
    }

    /// Create Hash for any serializable data
    pub fn serialize<S: BorshSerialize + BorshSchema, H: Hasher>(d: &S) -> Result<Self, String> {
        let ser = borsh::try_to_vec_with_schema(d)
            .map_err(|err| format!("{}", err))?;
        Ok(Self(Self::hash::<H>(&ser[..])))
    }

    pub fn from_hash(hash: &[u8]) -> Self {
        let mut slice = [0u8; 32];
        slice.clone_from_slice(&hash);
        Self(slice)
    }

    fn hash<H: Hasher>(input: &[u8]) -> [u8; 32] {
        H::hash(input)
    }
}

impl From<Hash> for [u8; 32] {
    fn from(hash: Hash) -> Self {
        hash.0
    }
}

impl Deref for Hash {
    type Target = [u8; 32];

    fn deref(&self) -> &Self::Target {
        &self.0
    }
}

impl Decodable for Hash {
    fn decode(rlp: &crate::rlp::Rlp) -> Result<Self, crate::rlp::DecoderError> {
        Ok(Self::from_hash(&rlp.as_val::<Vec<u8>>()?))
    }
}

impl Encodable for Hash {
    fn rlp_append(&self, stream: &mut crate::rlp::RlpStream) {
        stream.append_internal::<Vec<u8>>(&(self.deref().to_vec()));
    }
}

impl<'de> Deserialize<'de> for Hash {
    fn deserialize<D>(deserializer: D) -> Result<Self, <D as de::Deserializer<'de>>::Error>
    where
        D: de::Deserializer<'de>,
    {
        <String as Deserialize>::deserialize(deserializer)
            .and_then(|s| Self::try_from(s).map_err(de::Error::custom))
    }
}

impl TryFrom<String> for Hash {
    type Error = String;

    fn try_from(value: String) -> Result<Self, Self::Error> {
        let decoded = match value.starts_with("0x") {
            true => value.strip_prefix("0x").unwrap().from_hex().map_err(|error| {
                format!("Failed to decode hash from Hex: {}", error)
            })?,
            _ => base64::decode_config(value, URL_SAFE_NO_PAD).map_err(|error| {
                format!("Failed to decode hash from Base 64: {}", error)
            })?,
        };

        Ok(Hash::from_hash(&decoded))
    }
}