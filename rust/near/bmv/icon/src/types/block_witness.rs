use libraries::rlp::Decodable;
use libraries::types::Hash;

pub type Witness = Hash; // Confirm: Is this byte 32 hash?

#[derive(Default, PartialEq, Eq, Debug)]
pub struct BlockWitness {
    height: u128,
    witnesses: Vec<Witness> // Confirm: Can the list contain null eg [witness, null, witness];
}

impl BlockWitness {
    pub fn witnesses(&self) -> &Vec<Witness> {
        &self.witnesses
    }

    pub fn height(&self) -> u128 {
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