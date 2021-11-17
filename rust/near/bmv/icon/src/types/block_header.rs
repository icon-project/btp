use super::{BlockResult, Nullable, RlpBytes};
use libraries::rlp::{self, Decodable, Encodable};
use libraries::types::Hash;

#[derive(Default, PartialEq, Eq, Debug, Clone)]
pub struct BlockHeader {
    pub version: u8,
    pub height: u128,
    pub timestamp: u64,
    pub proposer: Vec<u8>,
    pub previous_hash: Hash,
    pub vote_hash: Hash,
    pub next_validator_hash: Hash,
    pub patch_tx_hash: Nullable<Hash>,
    pub tx_hash: Nullable<Hash>,
    pub logs_bloom: Vec<u8>,
    pub block_result: BlockResult,
}

impl BlockHeader {
    pub fn height(&self) -> u128 {
        self.height
    }

    pub fn next_validator_hash(&self) -> &Hash {
        &self.next_validator_hash
    }
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
            next_validator_hash: rlp.val_at(6)?,
            patch_tx_hash: rlp.val_at(7)?,
            tx_hash: rlp.val_at(8)?,
            logs_bloom: rlp.val_at(9)?,
            block_result: rlp.val_at(10)?,
        })
    }
}

impl Encodable for BlockHeader {
    fn rlp_append(&self, stream: &mut rlp::RlpStream) {
        let mut params = rlp::RlpStream::new_list(11);
        params
            .append(&self.version)
            .append(&self.height)
            .append(&self.timestamp)
            .append(&self.proposer)
            .append(&self.previous_hash)
            .append(&self.vote_hash)
            .append(&self.next_validator_hash)
            .append(&self.patch_tx_hash)
            .append(&self.tx_hash)
            .append(&self.logs_bloom)
            .append(&self.block_result);
        stream.append(&params.out());
    }
}

impl From<&BlockHeader> for RlpBytes {
    fn from(block_header: &BlockHeader) -> Self {
        rlp::encode(block_header).to_vec()
    }
}
