use super::{BlockProof, BlockUpdate, Nullable, ReceiptProof};
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
    block_proof: Nullable<BlockProof>,
    receipt_proofs: Vec<ReceiptProof>,
}

impl RelayMessage {
    pub fn block_updates(&self) -> &Vec<BlockUpdate> {
        &self.block_updates
    }

    pub fn block_proof(&self) -> &Nullable<BlockProof> {
        &self.block_proof
    }

    pub fn receipt_proofs(&self) -> &Vec<ReceiptProof> {
        &self.receipt_proofs
    }
}

impl Serialize for RelayMessage {
    fn serialize<S>(&self, serializer: S) -> Result<S::Ok, S::Error>
    where
        S: near_sdk::serde::Serializer,
    {
        unimplemented!()
    }
}

impl Decodable for RelayMessage {
    fn decode(rlp: &rlp::Rlp) -> Result<Self, rlp::DecoderError> {
        Ok(Self {
            block_updates: rlp.list_at(0)?,
            block_proof: rlp.val_at(1)?,
            receipt_proofs: rlp.list_at(2)?,
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
    use super::*;

    #[test]
    fn deserialize_relay_message() {
        let relay_message = RelayMessage::try_from("-Qee-QQ8uQFS-QFPuNT40gKCQKOHBdCqJx0eQ5UAtrV5G-C172cGOzwQuED7gVFNsv2g1tINkHhbtViQXPruQ-FnkmYs_jA0tYrJKbvq3BYO9Rqgt12iGPeeEGlPxvT_hNLnhr9K5nUqvKiB6A_Yc0bfl76g7Z5kTlmy_2VEb189fXfCeFj6z4rrO5aUcNdJnHn5dXz4APgAgLhG-ESgMpG4jtB7-p0Ls3k2EYmLp9RerPVrvKAFveS23xQl2UT4AKBhUsXohogqHXqYEfjsfRAHCiSbbzQfM_H7NFRDIoxzKLh1-HMA4gGgtVIyurXEb0cIjT3frYFFTMfDQ48iwgntObJ1ht6v-RL4TfhLhwXQqicsVs-4QfNbBjk7e16wLiu54BjoVhrFrH41obLbpuP8tVCxRbVLHYoBQyGbXYOYQduPi9KL_jc9iJ284R8Z5uKMJYJvntwB-AC5AVD5AU240vjQAoJApIcF0KonLFbPlQC2tXkb4LXvZwY7PBC4QPuBUU2y_aB_WlGDMMOOnc25Zw71Asy9qm5P1ckuxut4n7eyRa8sX6BkLQ2BfmajCXpSPKQyPbQfknSBaVE5Jh3D-Wyd1gODu6DtnmROWbL_ZURvXz19d8J4WPrPius7lpRw10mcefl1fPgAoNBv8fj4bmAFfa9a8IyYHOK_pHPZt7lJi8AF0Cgkad7_gKbloDKRuI7Qe_qdC7N5NhGJi6fUXqz1a7ygBb3ktt8UJdlE-AD4ALh1-HMA4gGgqw1QRCuG0tcqzCpKDqhHQO09T3_OQMhOHAAgt-D4O-_4TfhLhwXQqic7qGy4QZPydQXTosV0Bw1Q3ntf7-FCxRDMSM7FOnPhP591OxiYUlsWMdK0qfbp-WrnfBJKObfEwNm1dqfX98D9xKLtCVsA-AC5AZH5AY65ARL5AQ8CgkClhwXQqic7qGyVALa1eRvgte9nBjs8ELhA-4FRTbL9oItWxVWVSnoGHZaeON6tuc1mKVrq1Wm18UkpjaplJALMoBbHRfvDVLJUkLGrwX2BHDYbXjXpRT-EcCLGG68iaSdKoO2eZE5Zsv9lRG9fPX13wnhY-s-K6zuWlHDXSZx5-XV8-AD4ALg8CAAgcEAMEg8IhMDEBAhUDg0OhQEgYIiMEEEWjMIisaAENjsEAUIjEXghCkEZQEbjMQhEChETlEglUKgIuEb4RKC_bo8qyH3l705OLCmJ1kMQd9iEV0aw7gStxMPmCTOAffgAoL2i-AmVd_b_p3gkSP5glYl9pIn40AEUMIvqwpHIvkahuHX4cwDiAaCp1buI8W7RsDhZAp69_T6gJBjO_aR00EmjIj3-397EXfhN-EuHBdCqJ0rbgLhB459yJ9MK7TDZ2xQ23jTHbdbiwOCTEcmb5U7olxiUgaFV28sh8nuVzXrjJXuGfUnNureSKSOSXSwvp-aQTixLNAH4APgA-QNauQNX-QNUALkBVPkBUbkBTvkBS4IgALkBRfkBQgCVAZQ5KOt2a33MGDNmJIyCIn-oz8dWgwdSZIMHUmSFAukO3QC49hAAAAAAAAABAAAAAAAAAAAAAAAAAAAAAAAAAAAgQAAAAAAAAAAAAAEAAAAAAAAAAAAAAAAAAAAABAAACAAAAAAAAAAAAAAAACAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAgAAAAAAAAAAAAAAAAAAEAAAAAAAAAAAAAAAAAAAAAAAAIAAAAAACAAAAAAIAAAAEIAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAgAAAAAAACAAAAAAAAAAAAAAAAQAAAAAAAAAQAAAAAAAABAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAIAAAAAAAAAAAPgA-ACgvEWAgjiMdu0pUAxvQcurR0OVNFEmP5ZzhNMkpIVwqPr5Afn5AfYAuQHy-QHvo-IQoMOetu_ZC-YK5vgAe7UsNeBouviaGeP9VQgl1JDY7dRguFP4UaBjTIu0WBCx1UEf8MBqzibUyAkFNsmaQ3gnewIx7LIl96CegRcYwno7-6NZanIbuiC4o_93OVbLE-UByERTjAHFv4CAgICAgICAgICAgICAgLkBc_kBcCC5AWz5AWmVAZwHK-9UWr65aj97ov_fi4YftV3q-FSWTWVzc2FnZShzdHIsaW50LGJ5dGVzKbg6YnRwOi8vMHg1MDEucHJhLzB4NUNDMzA3MjY4YTEzOTNBQjlBNzY0QTIwREFDRTg0OEFCODI3NWM0NgL4-7j5-Pe4PmJ0cDovLzB4NThlYjFjLmljb24vY3g5YzA3MmJlZjU0NWFiZWI5NmEzZjdiYTJmZmRmOGI4NjFmYjU1ZGVhuDpidHA6Ly8weDUwMS5wcmEvMHg1Q0MzMDcyNjhhMTM5M0FCOUE3NjRBMjBEQUNFODQ4QUI4Mjc1YzQ2im5hdGl2ZWNvaW4BuG34awC4aPhmqmh4NDUwMmFhZDc5ODZhZDVhODQ4OTU1MTVmYWY3NmU5MGI1YjQ3ODY1NKoweDE1OEEzOTFGMzUwMEMzMjg4QWIyODY1MzcyMmE2NDU5RTc3MjZCMDHPzoNJQ1iJAIlj3YwsXgAA".to_string()).unwrap();
        assert_eq!(
            relay_message,
            RelayMessage {
                block_updates: vec![BlockUpdate::default()],
                block_proof: <Nullable<BlockProof>>::new(None),
                receipt_proofs: vec![]
            }
        )
    }

    #[test]
    fn serialize_relay_message() {}
}
