use super::{EventLog, Nullable, Proofs, Sha256};
use libraries::mpt::Prove;
use libraries::rlp::{self, encode, Decodable};
use std::ops::Deref;

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

impl Prove<Sha256> for EventProof {
    type Output = EventLog;

    fn index_ref(&self) -> u64 {
        self.index
    }

    fn mpt_proofs(&self) -> Result<&Vec<Vec<u8>>, String> {
        if let Ok(proof) = self.proofs.get() {
            Ok(proof.deref())
        } else {
            Err("Invalid Event Proof".to_string())
        }
    }
}
