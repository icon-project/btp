use std::ops::Deref;

use super::{EventProof, Proof};
use libraries::rlp::{self, Decodable, Encodable};

#[derive(PartialEq, Eq, Debug)]
pub struct ReceiptProof {
    index: u128,
    proofs: Proofs, // Confirm regarding this why vec of proofs and which is root
    event_proofs: Vec<EventProof>,
}

impl ReceiptProof {
    pub fn proofs(&self) -> &Proofs {
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

#[derive(PartialEq, Eq, Debug)]
pub struct Proofs(Vec<Proof>);

impl Deref for Proofs {
    type Target = Vec<Proof>;
    fn deref(&self) -> &Self::Target {
        &self.0
    }
}

impl Decodable for Proofs {
    fn decode(rlp: &rlp::Rlp) -> Result<Self, rlp::DecoderError> {
        let data = rlp.as_val::<Vec<u8>>()?;
        let rlp = rlp::Rlp::new(&data);
        Ok(Self(rlp.as_list()?))
    }
}
