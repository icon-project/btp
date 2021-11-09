use super::{NullableBlockProof, BlockUpdate, ReceiptProof};
use btp_common::errors::BmvError;
use libraries::rlp::{self, Decodable, Encodable};
use near_sdk::{
    base64::{self, URL_SAFE_NO_PAD},
    serde::{de, ser, Deserialize, Serialize},
};
use std::convert::TryFrom;

#[derive(PartialEq, Eq, Debug)]
pub struct RelayMessage {
    block_updates: Vec<BlockUpdate>,
    block_proof: NullableBlockProof,
    // receipt_proofs: Vec<ReceiptProof>,
}

impl Decodable for RelayMessage {
    fn decode(rlp: &rlp::Rlp) -> Result<Self, rlp::DecoderError> {
        Ok(Self {
            block_updates: rlp.list_at(0)?,
            block_proof: rlp.val_at(1)?, // receipt_proofs: rlp.list_at(2)?
        })
    }
}

impl TryFrom<String> for RelayMessage {
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

impl<'de> Deserialize<'de> for RelayMessage {
    fn deserialize<D>(deserializer: D) -> Result<Self, <D as de::Deserializer<'de>>::Error>
    where
        D: de::Deserializer<'de>,
    {
        <String as Deserialize>::deserialize(deserializer)
            .and_then(|s| Self::try_from(s).map_err(de::Error::custom))
    }
}

#[cfg(test)]
mod tests {
    use crate::types::BlockHeader;

    use super::*;

    #[test]
    fn deserialize_relay_message() {
        let relay_message = RelayMessage::try_from("-QiR-QiLuQK8-QK5uQGh-QGeAoM0s--HBcyhUgh_-pUABFjYtvZJqeAFlj3JpyZpyJ7VLYWgCsgckk88Z8zEBqyoAnbbOKmMtqniTlVQhH1xa9o1V2GgdCg8AKcmf9dOkkjyPpbfnDlMj4Ci5IVl6-bpjrX7HCygZ3LUBWCxhGBqqeyRzWgy1JVM3Cqk4bkFTVgzb8VkuP34AKA_ut-RKCUO0QIozoLVkJSOZ_ku4IhQmkuNqW9S-sNwFaIBACBwSCwaDwMEQQAwiGw6HxAAQqIwaJxSLxiMwcgRqBwEuND4zqAcfII71tclsceu1RwNxPCAug4TPqsglfPPZOCOps2OYvgAoM-dQ20rcPSnAPatdGrX2FrzGASLRXgYt5VjMROpCBGluIj4hqDvUxVcnaaZizh-sTdbVE0k7VbMLAOZ7BT8JMgUsBiJRKB0OZr3qHXbRE52jRUq0pDB2oLxHZ9AN4unKkIMAjWsbPgAoCFkCcC27_cLaMx1jIp02B0-8VjZCt_H80hRxcOOFmVRoOfDD6P-atzNAdkyAadn_DbGl54gTXcbZlFlwJj7_44muQEQ-QENAOIBoOxziQuOW1z08w82GOfKMNwvwIU-jhcL0hdemkDHFryd-Of4S4cFzKFSJyCEuEGRtMoNFru2so9w-ZHpGOVIQv2Cyu2tdamg2ntu_XhEiA0W7tZy-vBMyMUMtzteas81-7Ttxn-BCWdTchldYbDjAfhLhwXMoVInFo64QTwSF8hRF2KA0lffO-vlYRkxd7cCllK8THzCUnMrUsvYQXX7IiYbAKCufMKTw9fLyVoISSRx05A1-x-iLjlcL38A-EuHBcyhUiccTbhB-sgq-HdCKlG_xc0WYK3YqOUzLOam1dqM6gUHth6zGI48hNDUpq4hCRT2e5DtqBFle3mTnRgfMihdlrn6LMlayAH4ALkCvPkCubkBofkBngKDNLPwhwXMoVInHE2VAO3rG4K5TVSOxEDfVT9SIUTKg_uNoGK5cmONt6kHKjNRJFP6lzOxY-BWhLTWB-b1O93BEL6yoAC4SXP2XXP0vnhv2l3LSUoWXsGIii1LqLVa-beu53EyoGdy1AVgsYRgaqnskc1oMtSVTNwqpOG5BU1YM2_FZLj9-ACg0_FbTAWdeaW7sCZuSaaGhP1diXwRlvRmWg_XQi66y5CiAQAgcEgsGg8DBEEAMIhsOh8QAEKiMGicUi8YjMHIEagcBLjQ-M6gHHyCO9bXJbHHrtUcDcTwgLoOEz6rIJXzz2TgjqbNjmL4AKAEJWiUOIqGxqgZMHe6oWo_zveV2MbmKNSHicQpt6PVlriI-IagKVvZfefWIbvb0uxZNCbRIp1f3x5w0CuM0oCBLai4-lagdDma96h120ROdo0VKtKQwdqC8R2fQDeLpypCDAI1rGz4AKAhZAnAtu_3C2jMdYyKdNgdPvFY2Qrfx_NIUcXDjhZlUaDnww-j_mrczQHZMgGnZ_w2xpeeIE13G2ZRZcCY-_-OJrkBEPkBDQDiAaARm6fRKphJeFlBuv8I_BjO7tZeFP3O5Zk2UJkEPMo_uPjn-EuHBcyhUkWbJbhBnycf-L2-3rcyxYmBlrrPNBA7LkS3NhqEt94FC5BRVWMzNtwwjPHJ_evaWAYI-AcBXaojjWzdIUCK3iCWNdR40gD4S4cFzKFSRYkzuEFqa1GfQNsdIs067r04GUDsu7AQl3K-CRqNPDFjup4eVmHsvt6m6suAQbBf9-MeE4sAu9c6lYrkQlH3F70ZC_27AfhLhwXMoVJFkn-4QdiZLNnRl93it2zXrAy_TAMuhZybF5Nlnr0hs7XpgEktEBZpUEQ96PGakB8rgU9ev1EMeq9XpiXKCLJNlR9DQZgA-AC5Awr5Awe5AaH5AZ4CgzSz8YcFzKFSRZJ_lQDWPExztiPpf2dAfWh69O_P5IalFaDY9mTy31D9Jmb8_A5X8jVca4PzdWX-O6BmGh90NpS-2aAlL30MApf-8LBBxncoFkRgKxsD6B7TGwTvbbF1U2qn4KBnctQFYLGEYGqp7JHNaDLUlUzcKqThuQVNWDNvxWS4_fgAoNamdrLXhLJ757QmelDzFBDJ1pHaYq2js9Nb0LL_GOnTogEAIHBILBoPAwRBADCIbDofEABCojBonFIvGIzByBGoHAS40PjOoBx8gjvW1yWxx67VHA3E8IC6DhM-qyCV889k4I6mzY5i-ACgTKD5cGKxYQLc-jO_biPZjkxtlGCoujMW85ipfjDvB8u4iPiGoJbMORAvie7GK08Gyu8ObS4bfa_mml35c0ZvtBPs8sBBoHQ5mveoddtETnaNFSrSkMHagvEdn0A3i6cqQgwCNaxs-ACgIWQJwLbv9wtozHWMinTYHT7xWNkK38fzSFHFw44WZVGg58MPo_5q3M0B2TIBp2f8NsaXniBNdxtmUWXAmPv_jia5AV75AVsA4gGgm0ISbyuPaw5Pu9QCQXCRGnpBhciDcf3jEAHSBts2eoP5ATT4S4cFzKFSY-uSuEG-gpBH_dtxdYsnyS7OEgLuLeQbf2IxVzLMRjqG_YAoBFh04tH3ox8FD8gfDVIhoyD0Di6vFH168jiFKGEHI5rHAPhLhwXMoVJj6xW4QbFf6I8qF8T6s4H8wThKRQwrSNXMhQrTDXU2GudTPvz6doMgaGK844-n-qsetjIBGl4cpYtORud-rK6DbUHqPtYA-EuHBcyhUmPssbhBlivmJFU-fzFQnTye9OjyY-kbwlpgWnajTQ15MCuXvIBSifN4sNN_EkV_Ye5Di2u429tO2emuBwWLFoZdScsxOQH4S4cFzKFSY-vDuEGUR5N8AY4r2TlLd4KuMf4_k6pl1ZCR8oEbw8T4mZr8CRXlsLejwFvJbs60RK3P1mzactt3aCR5qR6ffBWCA3JaAfgA-ADA".to_string()).unwrap();
        assert_eq!(
            relay_message,
            RelayMessage {
                block_updates: vec![BlockUpdate::default()],
                block_proof: NullableBlockProof::new(None)
            }
        )
    }

    #[test]
    fn serialize_relay_message() {}
}
