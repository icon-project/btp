use super::error::MptError;
use super::util::bytes_to_nibbles;
use crate::{
    rlp::{self, Decodable, Encodable},
    types::{Hash, Hasher},
};
use std::{convert::TryFrom, marker::PhantomData, ops::Range};

#[derive(Copy, Clone, PartialEq, Eq, Debug)]
pub struct MerklePatriciaTree<H> {
    _hasher: PhantomData<H>,
}

impl<H: Hasher> MerklePatriciaTree<H> {
    pub fn verify_proof(
        root: &Hash,
        key: &Vec<u8>,
        proof: &Vec<Vec<u8>>,
    ) -> Result<Vec<u8>, MptError> {
        let mut actual_key = vec![];
        for el in key {
            actual_key.push(el / 16);
            actual_key.push(el % 16);
        }
        Self::prove(root, &actual_key, &proof, 0, 0)
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
                if proof_index + 1 != proof.len() {
                    return Err(MptError::InvalidLength);
                }
                node.val_at::<Vec<u8>>(16)
                    .map_err(|error| MptError::DecodeFailed {
                        message: error.to_string(),
                    })
            } else {
                let new_expected_root = Hash::new::<H>(
                    &node
                        .val_at::<Vec<u8>>(nibbles[key_index] as usize)
                        .map_err(|error| MptError::DecodeFailed {
                            message: error.to_string(),
                        })?,
                );
                Self::prove(
                    &new_expected_root,
                    nibbles,
                    proof,
                    key_index + 1,
                    proof_index + 1,
                )
            }
        } else if node.iter().count() == 2 {
            let path_u8 = node
                .val_at::<Vec<u8>>(0)
                .map_err(|error| MptError::DecodeFailed {
                    message: error.to_string(),
                })?;

            let head = path_u8[0] / 16;

            let mut path = vec![];
            if head % 2 == 1 {
                path.push(path_u8[0] % 16);
            }
            for val in path_u8.into_iter().skip(1) {
                path.push((val >> 4) & 0x0f);
                path.push(val & 0x0f);
            }

            if head >= 2 {
                if proof_index + 1 != proof.len() && key_index + path.len() != nibbles.len() {
                    return Err(MptError::InvalidLength);
                }
                node.val_at::<Vec<u8>>(1)
                    .map_err(|error| MptError::DecodeFailed {
                        message: error.to_string(),
                    })
            } else {
                let new_expected_root =
                    Hash::new::<H>(&node.val_at::<Vec<u8>>(1).map_err(|error| {
                        MptError::DecodeFailed {
                            message: error.to_string(),
                        }
                    })?);
                Self::prove(
                    &new_expected_root,
                    &nibbles,
                    proof,
                    key_index + path.len(),
                    proof_index + 1,
                )
            }
        } else {
            Err(MptError::InvalidLength)
        }
    }
}
