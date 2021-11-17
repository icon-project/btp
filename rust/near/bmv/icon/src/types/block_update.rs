use super::{BlockHeader, Validators, Votes};
use libraries::rlp::{self, Decodable};
use std::convert::TryFrom;

#[derive(Default, PartialEq, Eq, Debug, Clone)]
pub struct BlockUpdate {
    block_header: BlockHeader,
    votes: Votes,
    next_validators: Validators,
}

impl BlockUpdate {
    pub fn block_header(&self) -> &BlockHeader {
        &self.block_header
    }

    pub fn votes(&self) -> &Votes {
        &self.votes
    }

    pub fn next_validators(&self) -> &Validators {
        &self.next_validators
    }
}

impl Decodable for BlockUpdate {
    fn decode(rlp: &rlp::Rlp) -> Result<Self, rlp::DecoderError> {
        let data = rlp.as_val::<Vec<u8>>()?;
        let rlp = rlp::Rlp::new(&data);
        Ok(Self {
            block_header: rlp.val_at(0)?,
            votes: rlp.val_at(1)?,
            next_validators: rlp.val_at(2)?,
        })
    }
}
