use libraries::rlp::{self, Decodable, Encodable};
use libraries::types::Hash;

#[derive(Default, PartialEq, Eq, Debug, Clone)]
pub struct PartSetId {
    pub count: u64,
    pub hash: Hash,
}

impl Decodable for PartSetId {
    fn decode(rlp: &rlp::Rlp) -> Result<Self, rlp::DecoderError> {
        Ok(Self {
            count: rlp.val_at(0)?,
            hash: rlp.val_at(1)?,
        })
    }
}

impl Encodable for PartSetId {
    fn rlp_append(&self, stream: &mut rlp::RlpStream) {
        stream
            .begin_unbounded_list()
            .append(&self.count)
            .append(&self.hash)
            .finalize_unbounded_list()
    }
}