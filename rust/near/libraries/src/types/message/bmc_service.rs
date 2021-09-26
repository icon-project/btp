use crate::types::{message::ServiceMessage, BTPAddress};
use btp_common::errors::BMCError;
use near_sdk::{
    base64::{self, URL_SAFE},
    serde::{de, Deserialize, Serialize},
};
use rlp::{self, Decodable};
use std::convert::TryFrom;

#[derive(Clone, PartialEq, Eq, Debug)]
pub enum ServiceType {
    Init { links: Vec<BTPAddress> },
    Link,
    Unlink,
    FeeGathering,
    Sack,
    UnknownType,
}

impl Default for ServiceType {
    fn default() -> Self {
        Self::UnknownType
    }
}

impl TryFrom<(&String, &[u8])> for ServiceType {
    type Error = rlp::DecoderError;
    fn try_from((label, payload): (&String, &[u8])) -> Result<Self, Self::Error> {
        let payload = rlp::Rlp::new(payload);
        match label.as_str() {
            "Init" => Ok(Self::Init {
                links: payload.as_list()?,
            }),
            "Link" => Ok(Self::Link),
            "Unlink" => Ok(Self::Unlink),
            "FeeGathering" => Ok(Self::FeeGathering),
            "Sack" => Ok(Self::Sack),
            _ => Ok(Self::UnknownType),
        }
    }
}

#[derive(Default, Debug, PartialEq, Eq)]
pub struct BmcService {
    service_type: ServiceType,
}

impl ServiceMessage for BmcService {
    type ServiceType = ServiceType;

    fn service_type(&self) -> &Self::ServiceType {
        &self.service_type
    }

    fn set_service_type(&mut self, service_type: &Self::ServiceType) {
        self.service_type.clone_from(&service_type)
    }
}

impl Decodable for BmcService {
    fn decode(rlp: &rlp::Rlp) -> Result<Self, rlp::DecoderError> {
        Ok(Self {
            service_type: ServiceType::try_from((&rlp.val_at::<String>(0)?, rlp.at(1)?.as_raw()))?,
        })
    }
}

impl<'de> Deserialize<'de> for BmcService {
    fn deserialize<D>(deserializer: D) -> Result<Self, <D as de::Deserializer<'de>>::Error>
    where
        D: de::Deserializer<'de>,
    {
        <String as Deserialize>::deserialize(deserializer)
            .and_then(|s| Self::try_from(s).map_err(de::Error::custom))
    }
}

impl TryFrom<String> for BmcService {
    type Error = BMCError;
    fn try_from(value: String) -> Result<Self, Self::Error> {
        let decoded = base64::decode_config(value, URL_SAFE).map_err(|error| BMCError::DecodeFailed {
            message: format!("base64: {}", error),
        })?;
        let rlp = rlp::Rlp::new(&decoded);
        Self::decode(&rlp).map_err(|error| BMCError::DecodeFailed {
            message: format!("rlp: {}", error),
        })
    }
}

#[cfg(test)]
mod tests {
    use super::{BmcService, ServiceType, BTPAddress};
    use std::convert::TryFrom;

    #[test]
    fn deserialize_bmc_message_service_type() {
        let bmc_service = BmcService::try_from("x4RJbml0gcA".to_string()).unwrap();
        assert_eq!(
            BmcService {
                service_type: ServiceType::Init {
                    links: Default::default()
                },
            },
            bmc_service
        );
    }

    #[test]
    fn deserialize_bmc_message_payload() {
        let bmc_service = BmcService::try_from(
            "-HuESW5pdPh0uDhidHA6Ly8weDUucHJhL2N4ODdlZDkwNDhiNTk0Yjk1MTk5ZjMyNmZjNzZlNzZhOWQzM2RkNjY1Yrg4YnRwOi8vMHg1LnByYS9jeDg3ZWQ5MDQ4YjU5NGI5NTE5OWYzMjZmYzc2ZTc2YTlkMzNkZDY2NWI".to_string(),
        )
        .unwrap();
        assert_eq!(
            bmc_service,
            BmcService {
                service_type: ServiceType::Init {
                    links: vec![BTPAddress::new("btp://0x5.pra/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string()), BTPAddress::new("btp://0x5.pra/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string())]
                },
            },
        );
    }
}
