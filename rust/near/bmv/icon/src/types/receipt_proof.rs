use super::{EventProof, Proof, Proofs, Nullable};
use libraries::rlp::{self, Decodable, Encodable};

#[derive(Clone, PartialEq, Eq, Debug)]
pub struct ReceiptProof {
    index: u128,
    proofs: Nullable<Proofs>,
    event_proofs: Vec<EventProof>,
}

impl ReceiptProof {
    pub fn index(&self) -> u128 {
        self.index
    }
    
    pub fn proofs(&self) -> &Nullable<Proofs> {
        &self.proofs
    }
}

impl Decodable for ReceiptProof {
    fn decode(rlp: &rlp::Rlp) -> Result<Self, rlp::DecoderError> {
        let data = rlp.as_val::<Vec<u8>>()?;
        let rlp = rlp::Rlp::new(&data);
        Ok(Self {
            index: rlp.val_at(0)?,
            proofs: rlp.val_at(1)?,
            event_proofs: rlp.list_at(2)?,
        })
    }
}
