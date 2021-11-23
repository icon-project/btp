pub type Proof = Vec<u8>;
use libraries::rlp::{self, Decodable, Encodable};

#[derive(PartialEq, Eq, Debug)]
pub struct EventProof {
    index: u64,
    proofs: Vec<Proof>,
}

impl Decodable for EventProof {
    fn decode(rlp: &rlp::Rlp) -> Result<Self, rlp::DecoderError> {
        Ok(Self {
            index: rlp.val_at(0)?,
            proofs: rlp.list_at(1)?,
        })
    }
}
