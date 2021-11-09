use crate::mta::error::MtaError;
use byteorder::{BigEndian, ByteOrder};
use near_sdk::borsh::{self, BorshDeserialize, BorshSchema, BorshSerialize};
use near_sdk::serde::{Deserialize, Serialize};
use crate::rlp::{self, encode_list};
use safe_transmute::transmute_vec;
use tiny_keccak::{Hasher, Sha3};

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
pub struct Hash(pub [u8; 32]);

impl Hash {
    /// Create Hash from bytes
    pub fn new(data: &[u8]) -> Self {
        Self(Self::hash(data))
    }

    /// Create Hash for any serializable data
    pub fn serialize<S: BorshSerialize + BorshSchema>(d: &S) -> Result<Self, MtaError> {
        let ser = borsh::try_to_vec_with_schema(d)
            .map_err(|err| MtaError::HashSerialize(format!("{}", err)))?;
        Ok(Self(Self::hash(&ser[..])))
    }

    fn hash(input: &[u8]) -> [u8; 32] {
        let mut output = [0; 32];
        let mut sha3 = Sha3::v256();
        sha3.update(input);
        sha3.finalize(&mut output);
        output
    }

    pub fn sha3_fips_256(input: &[u8]) -> Vec<u32> {
        let mut output = [0; 32];
        let mut sha3 = Sha3::v256();
        sha3.update(input);
        sha3.finalize(&mut output);
        transmute_vec(output.to_vec()).expect("Failed to transform the vector")
    }

    pub fn ec_recover_public_key(message_hash: &[u8], vote_signature: &[u8]) -> Vec<u8> {
        let r = &vote_signature[0..32];
        let s = &vote_signature[32..64];
        let v = &vote_signature[64..65];
        let prefix: Vec<u8> = transmute_vec([0u8; 31].to_vec()).unwrap();
        let message_hash: Vec<u8> = transmute_vec(message_hash.to_vec()).unwrap();
        let input = encode_list(&[
            BigEndian::read_u128(&message_hash),
            BigEndian::read_u128(&prefix),
            BigEndian::read_u128(v),
            BigEndian::read_u128(r),
            BigEndian::read_u128(s),
        ])
        .to_vec();
        let mut output = [0; 32];
        let mut sha3 = Sha3::v256();
        sha3.update(&input);
        sha3.finalize(&mut output);
        transmute_vec(output.to_vec()).expect("Failed to transform the vector")
    }
}
