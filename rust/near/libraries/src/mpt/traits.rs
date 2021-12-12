use super::{error::MptError, MerklePatriciaTree};
use crate::rlp::{self, encode, Decodable};
use crate::types::{Hash, Hasher};

pub trait Prove<T>
where
    T: Hasher,
{
    type Output: Decodable;

    fn index_ref(&self) -> u64;

    fn mpt_proofs(&self) -> Result<&Vec<Vec<u8>>, String>;

    fn prove(&self, root: &Hash) -> Result<Self::Output, MptError> {
        let key = encode(&self.index_ref()).to_vec();
        let serialized = <MerklePatriciaTree<T>>::verify_proof(
            root,
            &key,
            self.mpt_proofs()
                .map_err(|message| MptError::DecodeFailed { message })?,
        )?;
        
        rlp::decode::<Self::Output>(&serialized).map_err(|error| MptError::DecodeFailed {
            message: format!("rlp error: {}", error),
        })
    }
}
