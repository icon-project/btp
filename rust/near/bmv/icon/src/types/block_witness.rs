use libraries::rlp::Decodable;
use libraries::types::Hash;

pub type Witness = Hash; // Confirm: Is this byte 32 hash?

#[derive(Clone, Default, PartialEq, Eq, Debug)]
pub struct BlockWitness {
    height: u64,
    witnesses: Vec<Witness>
}

impl BlockWitness {
    pub fn witnesses(&self) -> &Vec<Witness> {
        &self.witnesses
    }

    pub fn height(&self) -> u64 {
        self.height
    }
}

impl Decodable for BlockWitness {
    fn decode(rlp: &libraries::rlp::Rlp) -> Result<Self, libraries::rlp::DecoderError> {
        Ok(
            Self {
                height: rlp.val_at(0)?,
                witnesses: rlp.list_at(1)?
            }
        )
    }
}