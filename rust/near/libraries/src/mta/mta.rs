//! Merkle Tree Accumulator

use super::{hash::Hash, utils::RlpItem};
use near_sdk::borsh::{self, BorshDeserialize, BorshSerialize};
use near_sdk::serde::{Deserialize, Serialize};

/// Struct for MTA
#[derive(BorshDeserialize, BorshSerialize, Clone, Debug, Default, Deserialize, Serialize)]
#[serde(crate = "near_sdk::serde")]
pub struct MerkleTreeAccumulator {
    /// Height
    pub height: u128,
    /// Roots
    pub roots: Vec<Hash>,
    /// Offset
    pub offset: usize,
    /// Roots size
    pub roots_size: usize,
    /// Cache size
    pub cache_size: usize,
    /// Cache
    pub cache: Vec<Hash>,
    /// Is a newer witness allowed?
    pub newer_witness_allowed: bool,
}

impl MerkleTreeAccumulator {
    pub fn new(height: u128) -> Self {
        Self{
            height,
            ..Default::default()
        }
    }
    /// Initialize MTA from a serialized type
    pub fn init_from_serialized(&mut self, rlp_bytes: &[u8]) {
        let mut unpacked: Vec<RlpItem> = vec![];
        let mut serialized_roots: Vec<RlpItem> = vec![];

        if !rlp_bytes.is_empty() {
            let rlp_item = RlpItem::to_rlp_item(rlp_bytes);
            unpacked.push(rlp_item);
        }
        if !unpacked.is_empty() {
            self.height = unpacked[0].len as u128;
        }
        if unpacked.len() > 1 {
            serialized_roots.push(unpacked[1].clone());
            for root in &serialized_roots {
                let hash = Hash::new(&RlpItem::try_to_vec(root).expect("Failed to convert to vec"));
                self.roots.push(hash);
            }
        }
        if unpacked.len() > 2 {
            self.offset = unpacked[2].len;
        }
        if unpacked.len() > 3 {
            self.roots_size = unpacked[3].len;
        }
        if unpacked.len() > 4 {
            self.cache_size = unpacked[4].len;
        }
        if unpacked.len() > 5 {
            serialized_roots.clear();
            serialized_roots.push(unpacked[5].clone());
            for root in &serialized_roots {
                let hash = Hash::new(&RlpItem::try_to_vec(root).expect("Failed to convert to vec"));
                self.cache.push(hash);
            }
        }
        if unpacked.len() > 6 {
            self.newer_witness_allowed = unpacked[6].len != 0;
        }
        if self.height == 0 && self.offset == 0 {
            self.height = self.offset as u128;
        }
    }

    /// Set offset
    pub fn set_offset(&mut self, offset: usize) {
        self.offset = offset;
        if self.height == 0 && self.offset > 0 {
            self.height = self.offset as u128;
        }
    }

    /// Get root
    pub fn get_root(&self, idx: usize) -> Result<&Hash, &str> {
        if idx >= self.roots.len() {
            return Err("RevertInvalidBlockWitness: root index is out of range");
        }
        Ok(&self.roots[idx])
    }

    /// Check if the MTA includes a cache
    pub fn includes_cache(&self, hash: &Hash) -> bool {
        if hash.0.is_empty() {
            return false;
        }
        for entry in &self.cache {
            if entry == hash {
                return true;
            }
        }
        false
    }

    /// Set cache
    pub fn set_cache(&mut self, hash: &Hash) {
        if self.cache_size > 0 {
            self.cache.push(*hash);
        }
        if self.cache.len() > self.cache_size {
            let mut new_cache: Vec<Hash> = Vec::with_capacity(self.cache_size);
            for i in 0..self.cache_size {
                new_cache[i] = self.cache[i + self.cache.len() - self.cache_size];
            }
            self.cache.clear();
            self.cache = new_cache;
        }
    }

    /// Add
    pub fn add(&mut self, mut hash: Hash) {
        self.set_cache(&hash);
        if self.height == 0 || self.roots.is_empty() {
            self.roots.push(hash);
        } else {
            let mut root = Hash::default();
            for i in 0..self.clone().roots.len() {
                if self.roots[i].0.is_empty() {
                    root = hash;
                    self.roots[i] = root;
                    break;
                } else if self.roots_size > 0 && self.roots_size <= i + 1 {
                    root = hash;
                    self.roots[i] = root;
                    self.offset += 2_usize.pow(i as u32);
                    break;
                } else {
                    let index = self
                        .roots
                        .iter()
                        .position(|x| *x == hash)
                        .expect("Error in lookup");
                    hash = Hash::default();
                    let _ = self.roots.remove(index);
                }
            }
            if root.0.is_empty() {
                self.roots.push(hash);
            }
        }
        self.height += 1;
    }

    /// Get root index by height
    pub fn get_root_index_by_height(&self, height: u128) -> Result<usize, &str> {
        let mut idx = (height - 1) as usize - self.offset;
        let mut root_idx = 0;
        let mut i = self.roots.len();
        let mut bit_flag: usize;
        while i > 0 {
            i -= 1;
            if self.roots[i].0.is_empty() {
                continue;
            }
            bit_flag = 1 << i;
            if idx < bit_flag {
                root_idx = i;
                break;
            }
            idx -= bit_flag;
        }
        Ok(root_idx)
    }

    /// Verify
    pub fn verify(
        &mut self,
        proofs: &[Hash],
        leaf: &Hash,
        height: u128,
        at: u128,
    ) -> Result<(), &str> {
        let root: Hash;
        let root_idx: usize;
        if self.height == at {
            root = *self
                .get_root(proofs.len())
                .expect("Failed to retrieve root");
            self.verify_internal(proofs, &root, leaf)
                .expect("Failed to verify");
        } else if self.height < at {
            if !self.newer_witness_allowed {
                return Err("RevertInvalidBlockWitness: newer witness not allowed.");
            }
            if self.height < height {
                return Err("RevertInvalidBlockWitness: given witness for newer node.");
            }
            root_idx = self
                .get_root_index_by_height(height)
                .expect("Failed to retrieve root index by height");
            root = *self.get_root(root_idx).expect("Failed to get root");
            let mut slice_roots: Vec<Hash> = Vec::with_capacity(root_idx);
            slice_roots[..root_idx].clone_from_slice(&proofs[..root_idx]);
            self.verify_internal(slice_roots.as_slice(), &root, leaf)
                .expect("Failed to verify");
        } else if (self.height - height - 1) < self.cache_size as u128 && !self.includes_cache(leaf)
        {
            return Err("RevertInvalidBlockWitness: invalid old witness");
        }
        Ok(())
    }

    /// Convert MTA to bytes
    pub fn to_bytes(&self) -> Vec<u8> {
        Self::try_to_vec(self).expect("Failed to convert Merkle Tree Accumulator to bytes.")
    }

    fn verify_internal(
        &mut self,
        witnesses: &[Hash],
        root: &Hash,
        leaf: &Hash,
    ) -> Result<(), &str> {
        let mut hash = *leaf;
        for witness in witnesses {
            hash = Hash::serialize(witness).expect("Failed to serialize");
        }
        if &hash != root {
            return Err("RevertInvalidBlockWitness: invalid witness");
        }
        Ok(())
    }
}
