use libraries::rlp::{self, Decodable, Encodable};
use std::{convert::TryFrom};
use super::BlockResult;

#[derive(Default, PartialEq, Eq, Debug)]
pub struct BlockHeader {
    pub version: u8,
    pub height: u128,
    pub timestamp: u64,
    pub proposer: Vec<u8>,
    pub previous_hash: Vec<u8>,
    pub vote_hash: Vec<u8>,
    pub next_validator_hash: Vec<u8>,
    pub patch_tx_hash: Vec<u8>,
    pub tx_hash: Vec<u8>,
    pub logs_bloom: Vec<u8>,
    pub block_result: BlockResult
}

impl Decodable for BlockHeader {
    fn decode(rlp: &rlp::Rlp) -> Result<Self, rlp::DecoderError> {
        let data = rlp.as_val::<Vec<u8>>()?;
        let rlp = rlp::Rlp::new(&data);
        Ok(Self {
            version: rlp.val_at(0)?,
            height: rlp.val_at(1)?,
            timestamp: rlp.val_at(2)?,
            proposer: rlp.val_at(3)?,
            previous_hash: rlp.val_at(4)?,
            vote_hash: rlp.val_at(5)?,
            next_validator_hash: rlp.val_at(6).unwrap_or_default(),
            patch_tx_hash: rlp.val_at(7).unwrap_or_default(),
            tx_hash: rlp.val_at(8).unwrap_or_default(),
            logs_bloom: rlp.val_at(9).unwrap_or_default(),
            block_result: rlp.val_at(10).unwrap_or_default()
        })
    }
}