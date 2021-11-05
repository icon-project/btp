use libraries::rlp::{self, Decodable};

#[derive(Default, PartialEq, Eq, Debug)]
pub struct PartSetId {
    pub count: u64,
    pub hash: Vec<u8>
}

impl Decodable for PartSetId {
    fn decode(rlp: &rlp::Rlp) -> Result<Self, rlp::DecoderError> {
        Ok(Self {
            count: rlp.val_at(0)?,
            hash: rlp.val_at(1)?,
        })
    }
}