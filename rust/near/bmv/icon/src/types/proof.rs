use libraries::rlp::{self, Decodable, Encodable};
use std::ops::Deref;

pub type Proof = Vec<u8>;

#[derive(Clone, PartialEq, Eq, Debug)]
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