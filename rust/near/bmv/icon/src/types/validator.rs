use super::RlpBytes;
use btp_common::errors::BmvError;
use hex;
use libraries::rlp::{self, Decodable, Encodable};
use near_sdk::base64::{self, URL_SAFE_NO_PAD};
use near_sdk::borsh::{self, BorshDeserialize, BorshSerialize};
use near_sdk::serde::{de, Deserialize, Serialize};
use near_sdk::serde_json::from_str;
use std::convert::TryFrom;

#[derive(Default, PartialEq, Eq, Debug, BorshDeserialize, BorshSerialize, Clone, Serialize)]
#[serde(crate = "near_sdk::serde")]
pub struct Validators(Vec<Validator>);

impl Validators {
    pub fn new() -> Self {
        Self(Vec::new())
    }

    pub fn set(&mut self, validators: &Vec<Validator>) {
        self.0.clone_from(validators)
    }

    pub fn is_empty(&self) -> bool {
        self.0.is_empty()
    }

    pub fn contains(&self, validator: &Validator) -> bool {
        self.0.contains(validator)
    }

    pub fn len(&self) -> usize {
        self.0.len()
    }

    pub fn get(&self) -> &Vec<Validator> {
        &self.0
    }
}

impl AsRef<Validators> for Validators {
    fn as_ref(&self) -> &Self {
        &self
    }
}

impl From<&Vec<Validator>> for Validators {
    fn from(list: &Vec<Validator>) -> Self {
        let mut validators = Self::new();
        validators.set(list);
        validators
    }
}

impl Decodable for Validators {
    fn decode(rlp: &rlp::Rlp) -> Result<Self, rlp::DecoderError> {
        Ok(Self::from(&rlp.as_list::<Validator>()?))
    }
}

impl Encodable for Validators {
    fn rlp_append(&self, stream: &mut rlp::RlpStream) {
        stream.begin_list(self.get().len());
        self.get().iter().for_each(|validator| {
            stream.append(validator);
        });
    }
}

impl From<&Validators> for RlpBytes {
    fn from(validators: &Validators) -> Self {
        rlp::encode(validators).to_vec()
    }
}

impl<'de> Deserialize<'de> for Validators {
    fn deserialize<D>(deserializer: D) -> Result<Self, <D as de::Deserializer<'de>>::Error>
    where
        D: de::Deserializer<'de>,
    {
        <String as Deserialize>::deserialize(deserializer)
            .and_then(|s| Self::try_from(s).map_err(de::Error::custom))
    }
}

impl TryFrom<String> for Validators {
    type Error = BmvError;

    fn try_from(value: String) -> Result<Self, Self::Error> {
        let decoded = match value.starts_with("0x") {
            true => hex::decode(value.strip_prefix("0x").unwrap()).map_err(|error| {
                BmvError::DecodeFailed {
                    message: format!("Hex Decode: {}", error),
                }
            })?,
            _ => base64::decode_config(value, URL_SAFE_NO_PAD).map_err(|error| {
                BmvError::DecodeFailed {
                    message: format!("base64: {}", error),
                }
            })?,
        };
        let rlp = rlp::Rlp::new(&decoded);
        Self::decode(&rlp).map_err(|error| BmvError::DecodeFailed {
            message: format!("rlp: {}", error),
        })
    }
}

pub type Validator = Vec<u8>;

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn rlp_encoded_validators() {
        let validators = Validators::from(&vec![hex::decode(
            "00b6b5791be0b5ef67063b3c10b840fb81514db2fd",
        )
        .unwrap()]);
        let rlp_bytes = RlpBytes::from(&validators);
    }

    #[test]
    fn rlp_decode_validators() {
        let validators =
            Validators::try_from("0xd69500b6b5791be0b5ef67063b3c10b840fb81514db2fd".to_string())
                .unwrap();
        println!("{:?}", validators);
    }
}
