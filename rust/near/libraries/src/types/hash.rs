use std::ops::Deref;
use near_sdk::borsh::{self, BorshDeserialize, BorshSchema, BorshSerialize};
use near_sdk::serde::{Deserialize, Serialize};
use tiny_keccak::{Hasher, Sha3};

use crate::rlp::{Decodable, Encodable};

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
    Deserialize,
    Hash,
    Copy,
)]
#[serde(crate = "near_sdk::serde")]
pub struct Hash([u8; 32]);

impl Hash {
    /// Create Hash from bytes
    pub fn new(data: &[u8]) -> Self {
        Self(Self::hash(data))
    }

    /// Create Hash for any serializable data
    pub fn serialize<S: BorshSerialize + BorshSchema>(d: &S) -> Result<Self, String> {
        let ser = borsh::try_to_vec_with_schema(d)
            .map_err(|err| format!("{}", err))?;
        Ok(Self(Self::hash(&ser[..])))
    }

    pub fn from_hash(hash: &[u8]) -> Self {
        let mut slice = [0u8; 32];
        slice.clone_from_slice(&hash);
        Self(slice)
    }

    fn hash(input: &[u8]) -> [u8; 32] {
        let mut output = [0; 32];
        let mut sha3 = Sha3::v256();
        sha3.update(input);
        sha3.finalize(&mut output);
        output
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
        stream.append(&self.deref().to_vec());
    }
}
