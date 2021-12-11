use super::error::MptError;
use super::util::bytes_to_nibbles;
use crate::{
    rlp::{self, Decodable, Encodable},
    types::{Hash, Hasher},
};
use std::{convert::TryFrom, marker::PhantomData, ops::Range};

#[derive(Copy, Clone, PartialEq, Eq, Debug)]
pub struct MerklePatriciaTree<H> {
    root: Node,
    hash: Hash,
    _hasher: PhantomData<H>,
}

impl<H: Hasher> MerklePatriciaTree<H> {
    pub fn verify_proof(
        root: &Hash,
        key: &Vec<u8>,
        proof: &Vec<Vec<u8>>,
    ) -> Result<Vec<u8>, MptError> {
        let nibbles = bytes_to_nibbles(key, 0, None);
        Self::prove(root, &nibbles, &proof, 0, 0)
    }

    fn prove(
        root: &Hash,
        nibbles: &Vec<u8>,
        proof: &Vec<Vec<u8>>,
        key_index: usize,
        proof_index: usize,
    ) -> Result<Vec<u8>, MptError> {
        let node = &proof[proof_index];
        if key_index == 0 {
            if Hash::new::<H>(node) != *root {
                return Err(MptError::HashMismatch);
            };
        };

        let node = rlp::Rlp::new(&node.as_slice());
        if node.iter().count() == 17 {
            if key_index == nibbles.len() {
                if proof_index + 1 != proof.len(){
                    return Err(MptError::InvalidLength);
                }
                node.val_at::<Vec<u8>>(16).map_err(|error| MptError::DecodeFailed { message: error.to_string() })
            } else {
                let new_expected_root = Hash::new::<H>(&node.val_at::<Vec<u8>>(nibbles[key_index] as usize).map_err(|error| MptError::DecodeFailed { message: error.to_string() })?);
                Self::prove(
                    &new_expected_root,
                    nibbles,
                    proof,
                    key_index + 1,
                    proof_index + 1,
                )
            }
        } else if node.iter().count() == 2 {
            let header = node.val_at::<Vec<u8>>(0).map_err(|error| MptError::DecodeFailed { message: error.to_string() })?;
            let prefix = header[0] & 0xF0;

            let mut nibbles: Vec<u8> = vec![];
            if (prefix & 0x10) != 0 {
                nibbles = bytes_to_nibbles(&header, 1, Some(vec![prefix]));
            }

            if (prefix & 0x20) != 0 { 
                node.val_at::<Vec<u8>>(1).map_err(|error| MptError::DecodeFailed { message: error.to_string() })
            } else {
                let new_expected_root = Hash::new::<H>(&node.val_at::<Vec<u8>>(1).map_err(|error| MptError::DecodeFailed { message: error.to_string() })?);
                Self::prove(
                    &new_expected_root,
                    &nibbles,
                    proof,
                    key_index + nibbles.len(),
                    proof_index + 1,
                )
            }
        } else {
            Err(MptError::InvalidLength)
        }
    }
}

impl<H: Hasher> Decodable for MerklePatriciaTree<H> {
    fn decode(rlp: &rlp::Rlp) -> Result<Self, rlp::DecoderError> {
        Ok(Self {
            root: rlp.as_val()?,
            hash: Hash::new::<H>(rlp.as_raw()),
            _hasher: PhantomData::default(),
        })
    }
}

impl<H: Hasher> TryFrom<&Vec<u8>> for MerklePatriciaTree<H> {
    type Error = MptError;

    fn try_from(value: &Vec<u8>) -> Result<Self, Self::Error> {
        let rlp = rlp::Rlp::new(&value);
        Self::decode(&rlp).map_err(|error| MptError::DecodeFailed {
            message: format!("rlp: {}", error),
        })
    }
}
