use super::{BlockHeader, BlockWitness, Nullable};
use libraries::rlp::{self, Decodable, Encodable};
use std::convert::TryFrom;

#[derive(Default, PartialEq, Eq, Debug)]
pub struct BlockProof {
    pub block_header: BlockHeader,
    pub block_witness: Nullable<BlockWitness>,
}

impl BlockProof {
    pub fn block_header(&self) -> &BlockHeader {
        &self.block_header
    }

    pub fn block_witness(&self) -> &Nullable<BlockWitness> {
        &self.block_witness
    }
}

impl Decodable for BlockProof {
    fn decode(rlp: &rlp::Rlp) -> Result<Self, rlp::DecoderError> {
        Ok(Self {
            block_header: rlp.val_at(0)?,
            block_witness: rlp.val_at(1)?,
        })
    }
}

impl TryFrom<&Vec<u8>> for BlockProof {
    type Error = rlp::DecoderError;
    fn try_from(bytes: &Vec<u8>) -> Result<Self, Self::Error> {
        let rlp = rlp::Rlp::new(bytes);
        Self::decode(&rlp)
    }
}
