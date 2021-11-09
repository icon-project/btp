use super::BlockHeader;
use btp_common::errors::BmvError;
use libraries::rlp::{self, Decodable, Encodable};
use near_sdk::{
    base64::{self, URL_SAFE_NO_PAD},
    serde::{de, ser, Deserialize, Serialize},
};
use std::convert::TryFrom;

#[derive(Default, PartialEq, Eq, Debug)]
pub struct BlockProof {
    pub block_header: BlockHeader,
}

#[derive(Default, PartialEq, Eq, Debug)]
pub struct NullableBlockProof(Option<BlockProof>);

impl NullableBlockProof {
    pub fn new(block_proof: Option<BlockProof>) -> Self {
        Self(block_proof)
    }
}

impl Decodable for NullableBlockProof {
    fn decode(rlp: &rlp::Rlp) -> Result<Self, rlp::DecoderError> {
        if rlp.is_null() {
            Ok(Self(None))
        } else {
            Ok(Self(Some(rlp.as_val()?)))
        }
    }
}

impl Decodable for BlockProof {
    fn decode(rlp: &rlp::Rlp) -> Result<Self, rlp::DecoderError> {
        Ok(Self {
            block_header: rlp.val_at(0)?,
        })
    }
}

impl TryFrom<&Vec<u8>> for BlockProof {
    type Error = rlp::DecoderError;
    fn try_from(bytes: &Vec<u8>) -> Result<Self, Self::Error> {
        let rlp = rlp::Rlp::new(bytes);
        Self::decode(&rlp)
    }
}

// impl TryFrom<String> for BlockProof {
//     type Error = BmvError;
//     fn try_from(value: String) -> Result<Self, Self::Error> {
//         let decoded = base64::decode_config(value, URL_SAFE_NO_PAD).map_err(|error| {
//             BmvError::DecodeFailed {
//                 message: format!("base64: {}", error),
//             }
//         })?;
//         let rlp = rlp::Rlp::new(&decoded);
//         Self::decode(&rlp).map_err(|error| BmvError::DecodeFailed {
//             message: format!("rlp: {}", error),
//         })
//     }
// }
