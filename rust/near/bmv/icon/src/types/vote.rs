use super::PartSetId;
use btp_common::errors::BmvError;
use libraries::{BytesMut, rlp::{self, Decodable, Encodable}};
use near_sdk::{
    base64::{self, URL_SAFE_NO_PAD},
    serde::{de, ser, Deserialize, Serialize},
};
use std::convert::TryFrom;

#[derive(PartialEq, Eq, Debug)]
pub struct Vote {
    timestamp: u64,
    signature: Vec<u8>,
}

#[derive(Default, PartialEq, Eq, Debug)]
pub struct Votes {
    round: u8,
    part_set_id: PartSetId,
    items: Vec<Vote>,
}

impl Decodable for Votes {
    fn decode(rlp: &rlp::Rlp) -> Result<Self, rlp::DecoderError> {
        Ok(Self {
            round: rlp.val_at(0)?,
            part_set_id: rlp.val_at(1)?,
            items: rlp.list_at(2)?,
        })
    }
}

impl Decodable for Vote {
    fn decode(rlp: &rlp::Rlp) -> Result<Self, rlp::DecoderError> {
        // let data = rlp.as_val::<Vec<u8>>()?;
        // let rlp = rlp::Rlp::new(&data);
        Ok(Self {
            timestamp: rlp.val_at(0)?,
            signature: rlp.val_at(1)?,
        })
    }
}

impl TryFrom<String> for Votes {
    type Error = BmvError;
    fn try_from(value: String) -> Result<Self, Self::Error> {
        let decoded = base64::decode_config(value, URL_SAFE_NO_PAD).map_err(|error| {
            BmvError::DecodeFailed {
                message: format!("base64: {}", error),
            }
        })?;
        let rlp = rlp::Rlp::new(&decoded);
        println!("{:?}", rlp.as_raw().rlp_bytes());
        Self::decode(&rlp).map_err(|error| BmvError::DecodeFailed {
            message: format!("rlp: {}", error),
        })
    }
}

#[cfg(test)]
mod tests {
    use crate::types::BlockHeader;

    use super::*;

    #[test]
    fn deserialize_votes() {
        let votes = Votes::try_from("-HMA4oCgR-tt4xG9sf5vZklw5hegFuT_n83KFXxPy7wktLXygVn4TfhLhwXQG6OK1i-4QWom-8316eS01O0Xi76mg-bMkVetvKzbUYWMwB9lmskqAfEvrS3jYFojsDHpT6vbMgkLYcNDpNG2NMWpLR0BVAYA".to_string()).unwrap();
        assert_eq!(
            votes,
            Votes {
                round: 0,
                part_set_id: PartSetId {
                    count: 0,
                    hash: vec![
                        71, 235, 109, 227, 17, 189, 177, 254, 111, 102, 73, 112, 230, 23, 160, 22,
                        228, 255, 159, 205, 202, 21, 124, 79, 203, 188, 36, 180, 181, 242, 129, 89
                    ]
                },
                items: vec![Vote {
                    timestamp: 1636192010032687,
                    signature: vec![
                        106, 38, 251, 205, 245, 233, 228, 180, 212, 237, 23, 139, 190, 166, 131,
                        230, 204, 145, 87, 173, 188, 172, 219, 81, 133, 140, 192, 31, 101, 154,
                        201, 42, 1, 241, 47, 173, 45, 227, 96, 90, 35, 176, 49, 233, 79, 171, 219,
                        50, 9, 11, 97, 195, 67, 164, 209, 182, 52, 197, 169, 45, 29, 1, 84, 6, 0
                    ]
                }]
            }
        )
    }
}
