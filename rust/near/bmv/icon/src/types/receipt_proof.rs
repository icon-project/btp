use super::{MptProof, Proof};
use libraries::rlp::{self, Decodable, Encodable};

#[derive(PartialEq, Eq, Debug)]
pub struct ReceiptProof {
    index: u128,
    proof: Proof,
    event_proofs: Vec<MptProof>,
}

impl Decodable for ReceiptProof {
    fn decode(rlp: &rlp::Rlp) -> Result<Self, rlp::DecoderError> {
        let data = rlp.as_val::<Vec<u8>>()?;
        let rlp = rlp::Rlp::new(&data);
        Ok(
            Self {
                index: rlp.val_at(0)?,
                proof: rlp.val_at(1)?,
                event_proofs: rlp.list_at(2)?
            }
        )
    }
}