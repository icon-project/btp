use super::{EventProof, Nullable, Proofs};
use libraries::rlp::{self, Decodable, Encodable, encode};

#[derive(Clone, PartialEq, Eq, Debug)]
pub struct ReceiptProof {
    index: u128,
    proofs: Nullable<Proofs>,
    event_proofs: Vec<EventProof>,
}

impl ReceiptProof {
    pub fn index_serialized(&self) -> Vec<u8> {
        encode(&self.index).to_vec()
    }
    
    pub fn index(&self) -> u128 {
        self.index
    }

    pub fn proofs(&self) -> &Nullable<Proofs> {
        &self.proofs
    }

    pub fn event_proofs(&self) -> &Vec<EventProof> {
        &self.event_proofs
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
