use super::{BlockProof, BlockUpdate, Nullable, ReceiptProof};
use btp_common::errors::BmvError;
use libraries::rlp::{self, Decodable};
use near_sdk::{
    base64::{self, URL_SAFE_NO_PAD},
    serde::{de, Deserialize, Serialize},
};
use std::convert::TryFrom;

#[derive(Clone, PartialEq, Eq, Debug)]
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
    #[ignore]
    fn deserialize_relay_message() {
        let relay_message = RelayMessage::try_from("-QE6-QE0uQEx-QEuuLP4sQKCAICHBdnRKezFFpUAj5Ro8w6i2whfP6Ag12JBsIkIbTOgtk28FPIlHJBv9AuorFBEZca38jI3xZrwlrXWFkDWGWqgbzswfVctI7GpCV8QjL5MBwqBdUGNx5TtuzLKccyFmpegMFoBIDUx5nqnIPb7LEOdCK8R5QKfUtYpibFyv-8KsmH4APgAgKbloHtTELa6AzM1SOsM56bIp_typSzWgZ74phF4A-VLBsni-AD4ALh1-HMA4gGgt1V-X3_9HTvjnmf9PSs4m1pUi0OtKPwziTrB-1Ns-6L4TfhLhwXZ0Sn8Afm4QQG-jAXl8Yk_VDXM2CrMn-7FgVgEJXR_uiqerhrcYbhZdYZxfXcFwwX1FbMUSBUTWxl2XgWAgDSdg444gQQTiaEA-AD4AMA=".to_string()).unwrap();
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
