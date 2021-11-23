use super::PartSetId;
use btp_common::errors::BmvError;
use libraries::{
    rlp::{self, Decodable, Encodable},
    BytesMut,
};
use near_sdk::{
    base64::{self, URL_SAFE_NO_PAD},
    serde::{de, ser, Deserialize, Serialize},
};
use std::convert::TryFrom;
use std::slice::Iter;

#[derive(PartialEq, Eq, Debug, Clone)]
pub struct Vote {
    timestamp: u64,
    signature: Vec<u8>,
}

impl Vote {
    pub fn timestamp(&self) -> u64 {
        self.timestamp
    }

    pub fn signature(&self) -> &Vec<u8> {
        &self.signature
    }
}

#[derive(Default, PartialEq, Eq, Debug, Clone)]
pub struct Votes {
    round: u8,
    part_set_id: PartSetId,
    items: Vec<Vote>,
}

impl Votes {
    pub fn round(&self) -> u8 {
        self.round
    }

    pub fn part_set_id(&self) -> &PartSetId {
        &self.part_set_id
    }

    pub fn iter(&self) -> Iter<'_, Vote> {
        self.items.iter()
    }
}

impl Decodable for Votes {
    fn decode(rlp: &rlp::Rlp) -> Result<Self, rlp::DecoderError> {
        let data = rlp.as_val::<Vec<u8>>()?;
        let rlp = rlp::Rlp::new(&data);
        Ok(Self {
            round: rlp.val_at(0)?,
            part_set_id: rlp.val_at(1)?,
            items: rlp.list_at(2)?,
        })
    }
}

impl Decodable for Vote {
    fn decode(rlp: &rlp::Rlp) -> Result<Self, rlp::DecoderError> {
        Ok(Self {
            timestamp: rlp.val_at(0)?,
            signature: rlp.val_at(1)?,
        })
    }
}

impl Encodable for Vote {
    fn rlp_append(&self, stream: &mut rlp::RlpStream) {
        stream
            .begin_unbounded_list()
            .append(&self.timestamp)
            .append(&self.signature)
            .finalize_unbounded_list()
    }
}

impl Encodable for Votes {
    fn rlp_append(&self, stream: &mut rlp::RlpStream) {
        let mut params = rlp::RlpStream::new_list(3);
        params
            .append(&self.round)
            .append(&self.part_set_id)
            .append_list(&self.items);
        stream.append(&params.out());
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
        Self::decode(&rlp).map_err(|error| BmvError::DecodeFailed {
            message: format!("rlp: {}", error),
        })
    }
}

impl From<&Votes> for String {
    fn from(votes: &Votes) -> Self {
        let rlp = rlp::encode(votes);
        base64::encode_config(rlp, URL_SAFE_NO_PAD)
    }
}

#[cfg(test)]
mod tests {
    use libraries::types::Hash;

    use super::*;

    #[test]
    fn deserialize_votes() {
        let votes = Votes::try_from("uQFe-QFbgOKAoEfrbeMRvbH-b2ZJcOYXoBbk_5_NyhV8T8u8JLS18oFZ-QE0-EuHBcyhUmPrkrhBvoKQR_3bcXWLJ8kuzhIC7i3kG39iMVcyzEY6hv2AKARYdOLR96MfBQ_IHw1SIaMg9A4urxR9evI4hShhByOaxwD4S4cFzKFSY-sVuEGxX-iPKhfE-rOB_ME4SkUMK0jVzIUK0w11NhrnUz78-naDIGhivOOPp_qrHrYyARpeHKWLTkbnfqyug21B6j7WAPhLhwXMoVJj7LG4QZYr5iRVPn8xUJ08nvTo8mPpG8JaYFp2o00NeTArl7yAUonzeLDTfxJFf2HuQ4truNvbTtnprgcFixaGXUnLMTkB-EuHBcyhUmPrw7hBlEeTfAGOK9k5S3eCrjH-P5OqZdWQkfKBG8PE-Jma_AkV5bC3o8BbyW7OtEStz9Zs2nLbd2gkeaken3wVggNyWgE".to_string()).unwrap();
        assert_eq!(
            votes,
            Votes {
                round: 0,
                part_set_id: PartSetId {
                    count: 0,
                    hash: Hash::from_hash(&vec![
                        71, 235, 109, 227, 17, 189, 177, 254, 111, 102, 73, 112, 230, 23, 160, 22,
                        228, 255, 159, 205, 202, 21, 124, 79, 203, 188, 36, 180, 181, 242, 129, 89,
                    ]),
                },
                items: vec![
                    Vote {
                        timestamp: 1632368127634322,
                        signature: vec![
                            190, 130, 144, 71, 253, 219, 113, 117, 139, 39, 201, 46, 206, 18, 2,
                            238, 45, 228, 27, 127, 98, 49, 87, 50, 204, 70, 58, 134, 253, 128, 40,
                            4, 88, 116, 226, 209, 247, 163, 31, 5, 15, 200, 31, 13, 82, 33, 163,
                            32, 244, 14, 46, 175, 20, 125, 122, 242, 56, 133, 40, 97, 7, 35, 154,
                            199, 0,
                        ],
                    },
                    Vote {
                        timestamp: 1632368127634197,
                        signature: vec![
                            177, 95, 232, 143, 42, 23, 196, 250, 179, 129, 252, 193, 56, 74, 69,
                            12, 43, 72, 213, 204, 133, 10, 211, 13, 117, 54, 26, 231, 83, 62, 252,
                            250, 118, 131, 32, 104, 98, 188, 227, 143, 167, 250, 171, 30, 182, 50,
                            1, 26, 94, 28, 165, 139, 78, 70, 231, 126, 172, 174, 131, 109, 65, 234,
                            62, 214, 0,
                        ],
                    },
                    Vote {
                        timestamp: 1632368127634609,
                        signature: vec![
                            150, 43, 230, 36, 85, 62, 127, 49, 80, 157, 60, 158, 244, 232, 242, 99,
                            233, 27, 194, 90, 96, 90, 118, 163, 77, 13, 121, 48, 43, 151, 188, 128,
                            82, 137, 243, 120, 176, 211, 127, 18, 69, 127, 97, 238, 67, 139, 107,
                            184, 219, 219, 78, 217, 233, 174, 7, 5, 139, 22, 134, 93, 73, 203, 49,
                            57, 1,
                        ],
                    },
                    Vote {
                        timestamp: 1632368127634371,
                        signature: vec![
                            148, 71, 147, 124, 1, 142, 43, 217, 57, 75, 119, 130, 174, 49, 254, 63,
                            147, 170, 101, 213, 144, 145, 242, 129, 27, 195, 196, 248, 153, 154,
                            252, 9, 21, 229, 176, 183, 163, 192, 91, 201, 110, 206, 180, 68, 173,
                            207, 214, 108, 218, 114, 219, 119, 104, 36, 121, 169, 30, 159, 124, 21,
                            130, 3, 114, 90, 1,
                        ],
                    },
                ],
            }
        )
    }

    #[test]
    fn serialize_votes() {
        let votes = Votes {
            round: 0,
            part_set_id: PartSetId {
                count: 0,
                hash: Hash::from_hash(&vec![
                    71, 235, 109, 227, 17, 189, 177, 254, 111, 102, 73, 112, 230, 23, 160, 22, 228,
                    255, 159, 205, 202, 21, 124, 79, 203, 188, 36, 180, 181, 242, 129, 89,
                ]),
            },
            items: vec![
                Vote {
                    timestamp: 1632368127634322,
                    signature: vec![
                        190, 130, 144, 71, 253, 219, 113, 117, 139, 39, 201, 46, 206, 18, 2, 238,
                        45, 228, 27, 127, 98, 49, 87, 50, 204, 70, 58, 134, 253, 128, 40, 4, 88,
                        116, 226, 209, 247, 163, 31, 5, 15, 200, 31, 13, 82, 33, 163, 32, 244, 14,
                        46, 175, 20, 125, 122, 242, 56, 133, 40, 97, 7, 35, 154, 199, 0,
                    ],
                },
                Vote {
                    timestamp: 1632368127634197,
                    signature: vec![
                        177, 95, 232, 143, 42, 23, 196, 250, 179, 129, 252, 193, 56, 74, 69, 12,
                        43, 72, 213, 204, 133, 10, 211, 13, 117, 54, 26, 231, 83, 62, 252, 250,
                        118, 131, 32, 104, 98, 188, 227, 143, 167, 250, 171, 30, 182, 50, 1, 26,
                        94, 28, 165, 139, 78, 70, 231, 126, 172, 174, 131, 109, 65, 234, 62, 214,
                        0,
                    ],
                },
                Vote {
                    timestamp: 1632368127634609,
                    signature: vec![
                        150, 43, 230, 36, 85, 62, 127, 49, 80, 157, 60, 158, 244, 232, 242, 99,
                        233, 27, 194, 90, 96, 90, 118, 163, 77, 13, 121, 48, 43, 151, 188, 128, 82,
                        137, 243, 120, 176, 211, 127, 18, 69, 127, 97, 238, 67, 139, 107, 184, 219,
                        219, 78, 217, 233, 174, 7, 5, 139, 22, 134, 93, 73, 203, 49, 57, 1,
                    ],
                },
                Vote {
                    timestamp: 1632368127634371,
                    signature: vec![
                        148, 71, 147, 124, 1, 142, 43, 217, 57, 75, 119, 130, 174, 49, 254, 63,
                        147, 170, 101, 213, 144, 145, 242, 129, 27, 195, 196, 248, 153, 154, 252,
                        9, 21, 229, 176, 183, 163, 192, 91, 201, 110, 206, 180, 68, 173, 207, 214,
                        108, 218, 114, 219, 119, 104, 36, 121, 169, 30, 159, 124, 21, 130, 3, 114,
                        90, 1,
                    ],
                },
            ],
        };
        assert_eq!(String::from(&votes), "uQFe-QFbgOKAoEfrbeMRvbH-b2ZJcOYXoBbk_5_NyhV8T8u8JLS18oFZ-QE0-EuHBcyhUmPrkrhBvoKQR_3bcXWLJ8kuzhIC7i3kG39iMVcyzEY6hv2AKARYdOLR96MfBQ_IHw1SIaMg9A4urxR9evI4hShhByOaxwD4S4cFzKFSY-sVuEGxX-iPKhfE-rOB_ME4SkUMK0jVzIUK0w11NhrnUz78-naDIGhivOOPp_qrHrYyARpeHKWLTkbnfqyug21B6j7WAPhLhwXMoVJj7LG4QZYr5iRVPn8xUJ08nvTo8mPpG8JaYFp2o00NeTArl7yAUonzeLDTfxJFf2HuQ4truNvbTtnprgcFixaGXUnLMTkB-EuHBcyhUmPrw7hBlEeTfAGOK9k5S3eCrjH-P5OqZdWQkfKBG8PE-Jma_AkV5bC3o8BbyW7OtEStz9Zs2nLbd2gkeaken3wVggNyWgE".to_string());
    }
}
