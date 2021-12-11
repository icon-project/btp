use libraries::rlp::{self, Decodable, Encodable, encode};
use super::{Proof, Proofs, Nullable};

#[derive(Clone, PartialEq, Eq, Debug)]
pub struct EventProof {
    index: u64,
    proofs: Nullable<Proofs>,
}

impl EventProof {
    pub fn index_serialized(&self) -> Vec<u8> {
        encode(&self.index).to_vec()
    }

    pub fn proofs(&self) -> &Nullable<Proofs> {
        &self.proofs
    }
}

impl Decodable for EventProof {
    fn decode(rlp: &rlp::Rlp) -> Result<Self, rlp::DecoderError> {
        Ok(Self {
            index: rlp.val_at(0)?,
            proofs: rlp.val_at(1)?,
        })
    }
}
