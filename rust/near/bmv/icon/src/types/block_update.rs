use super::{BlockHeader, Validators, Votes};
use libraries::rlp::{self, Decodable};
use std::convert::TryFrom;

#[derive(Default, PartialEq, Eq, Debug)]
pub struct BlockUpdate {
    block_header: BlockHeader,
    // votes: Votes,
    // next_validators: Validators,
}

impl Decodable for BlockUpdate {
    fn decode(rlp: &rlp::Rlp) -> Result<Self, rlp::DecoderError> {
        let data = rlp.as_val::<Vec<u8>>()?;
        let rlp = rlp::Rlp::new(&data);
        Ok(Self {
            block_header: rlp.val_at(0)?,
            // votes: rlp.val_at(1)?,
            // next_validators: rlp.val_at(2)?,
        })
    }
}

// impl TryFrom<&Vec<u8>> for Vec<BlockUpdate> {
//     type Error = rlp::DecoderError;
//     fn try_from(bytes: &Vec<u8>) -> Result<Self, Self::Error> {
//         let rlp = rlp::Rlp::new(bytes);
//         Self::decode(&rlp)
//     }
// }
