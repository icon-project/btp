use super::{EventProof, Nullable, Proofs, Receipt, Sha256};
use libraries::mpt::Prove;
use libraries::rlp::{self, Decodable};
use std::ops::Deref;

#[derive(Clone, PartialEq, Eq, Debug)]
pub struct ReceiptProof {
    index: u64,
    proofs: Nullable<Proofs>,
    event_proofs: Vec<EventProof>,
}

impl ReceiptProof {
    pub fn index(&self) -> u64 {
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

impl Prove<Sha256> for ReceiptProof {
    type Output = Receipt;

    fn index_ref(&self) -> u64 {
        self.index
    }

    fn mpt_proofs(&self) -> Result<&Vec<Vec<u8>>, String> {
        if let Ok(proof) = self.proofs.get() {
            Ok(proof.deref())
        } else {
            Err("Invalid Receipt Proof".to_string())
        }
    }
}
