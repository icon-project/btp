use crate::types::{
    messages::BtpMessage, messages::SerializedMessage, messages::Message, BTPAddress,
    WrappedI128,
};
use btp_common::errors::BmcError;
use near_sdk::{
    base64::{self, URL_SAFE_NO_PAD}, // TODO: Confirm
    serde::{de, ser, Deserialize, Serialize},
};
use rlp::{self, Decodable, Encodable};
use std::convert::TryFrom;

#[derive(Clone, PartialEq, Eq, Debug)]
pub enum BmcServiceType {
    Init {
        links: Vec<BTPAddress>,
    },
    Link {
        link: BTPAddress,
    },
    Unlink {
        link: BTPAddress,
    },
    FeeGathering {
        fee_aggregator: BTPAddress,
        services: Vec<String>,
    },
    UnknownType,
}

impl Default for BmcServiceType {
    fn default() -> Self {
        Self::UnknownType
    }
}

impl TryFrom<(&String, &Vec<u8>)> for BmcServiceType {
    type Error = rlp::DecoderError;
    fn try_from((label, payload): (&String, &Vec<u8>)) -> Result<Self, Self::Error> {
        let payload = rlp::Rlp::new(payload as &[u8]);
        match label.as_str() {
            "Init" => Ok(Self::Init {
                links: payload.list_at(0)?,
            }),
            "Link" => Ok(Self::Link {
                link: payload.val_at(0)?,
            }),
            "Unlink" => Ok(Self::Unlink {
                link: payload.val_at(0)?,
            }),
            "FeeGathering" => Ok(Self::FeeGathering {
                fee_aggregator: payload.val_at(0)?,
                services: payload.list_at(1)?,
            }),
            _ => Ok(Self::UnknownType),
        }
    }
}

#[derive(Default, Debug, PartialEq, Eq, Clone)]
pub struct BmcServiceMessage {
    service_type: BmcServiceType,
}

impl BmcServiceMessage {
    pub fn new(service_type: BmcServiceType) -> Self {
        Self { service_type }
    }

    pub fn service_type(&self) -> &BmcServiceType {
        &self.service_type
    }
}

impl Decodable for BmcServiceMessage {
    fn decode(rlp: &rlp::Rlp) -> Result<Self, rlp::DecoderError> {
        Ok(Self {
            service_type: BmcServiceType::try_from((&rlp.val_at(0)?, &rlp.val_at(1)?))?,
        })
    }
}

impl Encodable for BmcServiceMessage {
    fn rlp_append(&self, stream: &mut rlp::RlpStream) {
        stream.begin_unbounded_list();
        match self.service_type() {
            &BmcServiceType::Init { ref links } => {
                let mut params = rlp::RlpStream::new_list(1);
                params.append_list(links);
                stream
                    .append::<String>(&"Init".to_string())
                    .append(&params.out());
            }
            &BmcServiceType::Link { ref link } => {
                let mut params = rlp::RlpStream::new_list(1);
                params.append(link);
                stream
                    .append::<String>(&"Link".to_string())
                    .append(&params.out());
            }
            &BmcServiceType::Unlink { ref link } => {
                let mut params = rlp::RlpStream::new_list(1);
                params.append(link);
                stream
                    .append::<String>(&"Unlink".to_string())
                    .append(&params.out());
            }
            &BmcServiceType::FeeGathering {
                ref services,
                ref fee_aggregator,
            } => {
                let mut params = rlp::RlpStream::new_list(2);
                params.append(fee_aggregator);
                params.begin_unbounded_list();
                services.iter().for_each(|service| {
                    params.append(service);
                });
                params.finalize_unbounded_list();
                stream
                    .append::<String>(&"FeeGathering".to_string())
                    .append(&params.out());
            }
            _ => (),
        }
        stream.finalize_unbounded_list()
    }
}

impl Encodable for BtpMessage<BmcServiceMessage> {
    fn rlp_append(&self, stream: &mut rlp::RlpStream) {
        stream
            .begin_unbounded_list()
            .append(self.source())
            .append(self.destination())
            .append(self.service())
            .append(self.serial_no())
            .append(self.payload())
            .finalize_unbounded_list()
    }
}

impl<'de> Deserialize<'de> for BmcServiceMessage {
    fn deserialize<D>(deserializer: D) -> Result<Self, <D as de::Deserializer<'de>>::Error>
    where
        D: de::Deserializer<'de>,
    {
        <String as Deserialize>::deserialize(deserializer)
            .and_then(|s| Self::try_from(s).map_err(de::Error::custom))
    }
}

impl Serialize for BmcServiceMessage {
    fn serialize<S>(&self, serializer: S) -> Result<S::Ok, <S as ser::Serializer>::Error>
    where
        S: ser::Serializer,
    {
        serializer.serialize_str(&String::from(self))
    }
}

impl TryFrom<String> for BmcServiceMessage {
    type Error = BmcError;
    fn try_from(value: String) -> Result<Self, Self::Error> {
        let decoded = base64::decode_config(value, URL_SAFE_NO_PAD).map_err(|error| {
            BmcError::DecodeFailed {
                message: format!("base64: {}", error),
            }
        })?;
        let rlp = rlp::Rlp::new(&decoded);
        Self::decode(&rlp).map_err(|error| BmcError::DecodeFailed {
            message: format!("rlp: {}", error),
        })
    }
}

impl TryFrom<&Vec<u8>> for BmcServiceMessage {
    type Error = BmcError;
    fn try_from(value: &Vec<u8>) -> Result<Self, Self::Error> {
        let rlp = rlp::Rlp::new(value as &[u8]);
        Self::decode(&rlp).map_err(|error| BmcError::DecodeFailed {
            message: format!("rlp: {}", error),
        })
    }
}

impl From<BmcServiceMessage> for Vec<u8> {
    fn from(service_message: BmcServiceMessage) -> Self {
        rlp::encode(&service_message).to_vec()
    }
}

impl From<&BmcServiceMessage> for String {
    fn from(service_message: &BmcServiceMessage) -> Self {
        let rlp = rlp::encode(service_message);
        base64::encode_config(rlp, URL_SAFE_NO_PAD)
    }
}

impl From<&BtpMessage<BmcServiceMessage>> for String {
    fn from(btp_message: &BtpMessage<BmcServiceMessage>) -> Self {
        let rlp = rlp::encode(btp_message);
        base64::encode_config(rlp, URL_SAFE_NO_PAD)
    }
}

impl Message for BmcServiceMessage {}

impl Decodable for BtpMessage<BmcServiceMessage> {
    fn decode(rlp: &rlp::Rlp) -> Result<Self, rlp::DecoderError> {
        let service = rlp.val_at::<String>(2)?;
        let service_message = match service.as_str() {
            "bmc" => Some(
                BmcServiceMessage::try_from(&rlp.val_at::<Vec<u8>>(4)?)
                    .map_err(|_| rlp::DecoderError::Custom("BTPAddress Decode Error"))?,
            ),
            _ => None,
        };
        Ok(Self::new(
            rlp.val_at::<BTPAddress>(0)?,
            rlp.val_at::<BTPAddress>(1)?,
            service,
            rlp.val_at::<WrappedI128>(3)?,
            rlp.val_at::<Vec<u8>>(4)?,
            service_message,
        ))
    }
}

impl TryFrom<String> for BtpMessage<BmcServiceMessage> {
    type Error = BmcError;
    fn try_from(value: String) -> Result<Self, Self::Error> {
        let decoded = base64::decode_config(value, URL_SAFE_NO_PAD).map_err(|error| {
            BmcError::DecodeFailed {
                message: format!("base64: {}", error),
            }
        })?;
        let rlp = rlp::Rlp::new(&decoded);
        Self::decode(&rlp).map_err(|error| BmcError::DecodeFailed {
            message: format!("rlp: {}", error),
        })
    }
}

impl TryFrom<&BtpMessage<SerializedMessage>> for BtpMessage<BmcServiceMessage> {
    type Error = BmcError;
    fn try_from(value: &BtpMessage<SerializedMessage>) -> Result<Self, Self::Error> {
        Ok(Self::new(
            value.source().clone(),
            value.destination().clone(),
            value.service().clone(),
            value.serial_no().clone(),
            value.payload().clone(),
            Some(BmcServiceMessage::try_from(value.payload())?),
        ))
    }
}

impl TryFrom<&BtpMessage<BmcServiceMessage>> for BtpMessage<SerializedMessage> {
    type Error = BmcError;
    fn try_from(value: &BtpMessage<BmcServiceMessage>) -> Result<Self, Self::Error> {
        Ok(Self::new(
            value.source().clone(),
            value.destination().clone(),
            value.service().clone(),
            value.serial_no().clone(),
            value.message().clone().ok_or(BmcError::EncodeFailed { message: "Encoding Failed".to_string()})?.into(),
            None,
        ))
    }
}

#[cfg(test)]
mod tests {
    use super::{BTPAddress, BmcServiceMessage, BmcServiceType};
    use crate::types::{WrappedI128, messages::{SerializedMessage, btp_message::BtpMessage}};
    use std::convert::TryFrom;

    #[test]
    fn deserialize_init_bmc_message() {
        let service_message = BmcServiceMessage::try_from(
            "-H-ESW5pdLh4-Hb4dLg4YnRwOi8vMHg1LnByYS9jeDg3ZWQ5MDQ4YjU5NGI5NTE5OWYzMjZmYzc2ZTc2YTlkMzNkZDY2NWK4OGJ0cDovLzB4NS5wcmEvY3g4N2VkOTA0OGI1OTRiOTUxOTlmMzI2ZmM3NmU3NmE5ZDMzZGQ2NjVi".to_string(),
        )
        .unwrap();
        assert_eq!(
            service_message,
            BmcServiceMessage {
                service_type: BmcServiceType::Init {
                    links: vec![
                        BTPAddress::new(
                            "btp://0x5.pra/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string()
                        ),
                        BTPAddress::new(
                            "btp://0x5.pra/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string()
                        )
                    ]
                },
            },
        );
    }

    #[test]
    fn deserialize_link_bmc_message() {
        let service_message = BmcServiceMessage::try_from("-EOETGlua7g8-Dq4OGJ0cDovLzB4NS5wcmEvY3g4N2VkOTA0OGI1OTRiOTUxOTlmMzI2ZmM3NmU3NmE5ZDMzZGQ2NjVi".to_string()).unwrap();
        assert_eq!(
            service_message,
            BmcServiceMessage {
                service_type: BmcServiceType::Link {
                    link: BTPAddress::new(
                        "btp://0x5.pra/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string()
                    ),
                },
            }
        );
    }

    #[test]
    fn deserialize_unlink_bmc_message() {
        let service_message = BmcServiceMessage::try_from("-EWGVW5saW5ruDz4Org4YnRwOi8vMHg1LnByYS9jeDg3ZWQ5MDQ4YjU5NGI5NTE5OWYzMjZmYzc2ZTc2YTlkMzNkZDY2NWI".to_string()).unwrap();
        assert_eq!(
            service_message,
            BmcServiceMessage {
                service_type: BmcServiceType::Unlink {
                    link: BTPAddress::new(
                        "btp://0x5.pra/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string()
                    ),
                },
            }
        );
    }

    #[test]
    fn deserialize_fee_gathering_bmc_message() {
        let service_message = BmcServiceMessage::try_from("-F2MRmVlR2F0aGVyaW5nuE74TLg4YnRwOi8vMHg1LnByYS9jeDg3ZWQ5MDQ4YjU5NGI5NTE5OWYzMjZmYzc2ZTc2YTlkMzNkZDY2NWLRik5hdGl2ZUNvaW6FVG9rZW4".to_string()).unwrap();
        assert_eq!(
            service_message,
            BmcServiceMessage {
                service_type: BmcServiceType::FeeGathering {
                    fee_aggregator: BTPAddress::new(
                        "btp://0x5.pra/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string()
                    ),
                    services: vec!["NativeCoin".to_string(), "Token".to_string()]
                },
            },
        );
    }

    #[test]
    fn serialize_init_bmc_message() {
        let service_message = BmcServiceMessage {
            service_type: BmcServiceType::Init {
                links: vec![
                    BTPAddress::new(
                        "btp://0x5.pra/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string(),
                    ),
                    BTPAddress::new(
                        "btp://0x5.pra/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string(),
                    ),
                ],
            },
        };
        assert_eq!(String::from(&service_message), "-H-ESW5pdLh4-Hb4dLg4YnRwOi8vMHg1LnByYS9jeDg3ZWQ5MDQ4YjU5NGI5NTE5OWYzMjZmYzc2ZTc2YTlkMzNkZDY2NWK4OGJ0cDovLzB4NS5wcmEvY3g4N2VkOTA0OGI1OTRiOTUxOTlmMzI2ZmM3NmU3NmE5ZDMzZGQ2NjVi".to_string());
    }

    #[test]
    fn serialize_link_bmc_message() {
        let service_message = BmcServiceMessage {
            service_type: BmcServiceType::Link {
                link: BTPAddress::new(
                    "btp://0x5.pra/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string(),
                ),
            },
        };
        assert_eq!(String::from(&service_message), "-EOETGlua7g8-Dq4OGJ0cDovLzB4NS5wcmEvY3g4N2VkOTA0OGI1OTRiOTUxOTlmMzI2ZmM3NmU3NmE5ZDMzZGQ2NjVi".to_string());
    }

    #[test]
    fn serialize_unlink_bmc_message() {
        let service_message = BmcServiceMessage {
            service_type: BmcServiceType::Unlink {
                link: BTPAddress::new(
                    "btp://0x5.pra/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string(),
                ),
            },
        };
        assert_eq!(String::from(&service_message), "-EWGVW5saW5ruDz4Org4YnRwOi8vMHg1LnByYS9jeDg3ZWQ5MDQ4YjU5NGI5NTE5OWYzMjZmYzc2ZTc2YTlkMzNkZDY2NWI".to_string());
    }

    #[test]
    fn serialize_fee_gathering_bmc_message() {
        let service_message = BmcServiceMessage {
            service_type: BmcServiceType::FeeGathering {
                fee_aggregator: BTPAddress::new(
                    "btp://0x5.pra/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string(),
                ),
                services: vec!["NativeCoin".to_string(), "Token".to_string()],
            },
        };
        assert_eq!(String::from(&service_message), "-F2MRmVlR2F0aGVyaW5nuE74TLg4YnRwOi8vMHg1LnByYS9jeDg3ZWQ5MDQ4YjU5NGI5NTE5OWYzMjZmYzc2ZTc2YTlkMzNkZDY2NWLRik5hdGl2ZUNvaW6FVG9rZW4".to_string());
    }

    #[test]
    fn deserialize_btp_message() {
        let bmc_service_message = BmcServiceMessage::new(BmcServiceType::Init {
            links: vec![
                BTPAddress::new(
                    "btp://0x1.pra/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string(),
                ),
                BTPAddress::new(
                    "btp://0x5.pra/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string(),
                ),
            ],
        });
        let btp_message = BtpMessage::try_from("-MKcYnRwOi8vMTIzNC5pY29uZWUvMHgxMjM0NTY3OJxidHA6Ly8xMjM0Lmljb25lZS8weDEyMzQ1Njc4g2JtYwG4gfh_hEluaXS4ePh2-HS4OGJ0cDovLzB4MS5wcmEvY3g4N2VkOTA0OGI1OTRiOTUxOTlmMzI2ZmM3NmU3NmE5ZDMzZGQ2NjViuDhidHA6Ly8weDUucHJhL2N4ODdlZDkwNDhiNTk0Yjk1MTk5ZjMyNmZjNzZlNzZhOWQzM2RkNjY1Yg".to_string()).unwrap();
        assert_eq!(
            btp_message,
            BtpMessage::new(
                BTPAddress::new("btp://1234.iconee/0x12345678".to_string()),
                BTPAddress::new("btp://1234.iconee/0x12345678".to_string()),
                "bmc".to_string(),
                WrappedI128::new(1),
                <Vec<u8>>::from(bmc_service_message.clone()),
                Some(BmcServiceMessage::new(BmcServiceType::Init {
                    links: vec![
                        BTPAddress::new(
                            "btp://0x1.pra/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string()
                        ),
                        BTPAddress::new(
                            "btp://0x5.pra/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string()
                        )
                    ]
                }))
            )
        );
    }

    #[test]
    fn deserialize_btp_message_sn_negative_i8() {
        let bmc_service_message = BmcServiceMessage::new(BmcServiceType::Init {
            links: vec![
                BTPAddress::new(
                    "btp://0x1.pra/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string(),
                ),
                BTPAddress::new(
                    "btp://0x5.pra/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string(),
                ),
            ],
        });
        let btp_message = BtpMessage::try_from("-MOcYnRwOi8vMTIzNC5pY29uZWUvMHgxMjM0NTY3OJxidHA6Ly8xMjM0Lmljb25lZS8weDEyMzQ1Njc4g2JtY4GAuIH4f4RJbml0uHj4dvh0uDhidHA6Ly8weDEucHJhL2N4ODdlZDkwNDhiNTk0Yjk1MTk5ZjMyNmZjNzZlNzZhOWQzM2RkNjY1Yrg4YnRwOi8vMHg1LnByYS9jeDg3ZWQ5MDQ4YjU5NGI5NTE5OWYzMjZmYzc2ZTc2YTlkMzNkZDY2NWI".to_string()).unwrap();
        assert_eq!(
            btp_message,
            BtpMessage::new(
                BTPAddress::new("btp://1234.iconee/0x12345678".to_string()),
                BTPAddress::new("btp://1234.iconee/0x12345678".to_string()),
                "bmc".to_string(),
                WrappedI128::new(-128),
                <Vec<u8>>::from(bmc_service_message.clone()),
                Some(BmcServiceMessage::new(BmcServiceType::Init {
                    links: vec![
                        BTPAddress::new(
                            "btp://0x1.pra/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string()
                        ),
                        BTPAddress::new(
                            "btp://0x5.pra/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string()
                        )
                    ]
                }))
            )
        );
    }

    #[test]
    fn deserialize_btp_message_sn_negative_i16() {
        let bmc_service_message = BmcServiceMessage::new(BmcServiceType::Init {
            links: vec![
                BTPAddress::new(
                    "btp://0x1.pra/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string(),
                ),
                BTPAddress::new(
                    "btp://0x5.pra/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string(),
                ),
            ],
        });
        let btp_message = BtpMessage::try_from("-MScYnRwOi8vMTIzNC5pY29uZWUvMHgxMjM0NTY3OJxidHA6Ly8xMjM0Lmljb25lZS8weDEyMzQ1Njc4g2JtY4KAALiB-H-ESW5pdLh4-Hb4dLg4YnRwOi8vMHgxLnByYS9jeDg3ZWQ5MDQ4YjU5NGI5NTE5OWYzMjZmYzc2ZTc2YTlkMzNkZDY2NWK4OGJ0cDovLzB4NS5wcmEvY3g4N2VkOTA0OGI1OTRiOTUxOTlmMzI2ZmM3NmU3NmE5ZDMzZGQ2NjVi".to_string()).unwrap();
        assert_eq!(
            btp_message,
            BtpMessage::new(
                BTPAddress::new("btp://1234.iconee/0x12345678".to_string()),
                BTPAddress::new("btp://1234.iconee/0x12345678".to_string()),
                "bmc".to_string(),
                WrappedI128::new(-32768),
                <Vec<u8>>::from(bmc_service_message.clone()),
                Some(BmcServiceMessage::new(BmcServiceType::Init {
                    links: vec![
                        BTPAddress::new(
                            "btp://0x1.pra/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string()
                        ),
                        BTPAddress::new(
                            "btp://0x5.pra/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string()
                        )
                    ]
                }))
            )
        );
    }

    #[test]
    fn deserialize_btp_message_sn_negative_i32() {
        let bmc_service_message = BmcServiceMessage::new(BmcServiceType::Init {
            links: vec![
                BTPAddress::new(
                    "btp://0x1.pra/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string(),
                ),
                BTPAddress::new(
                    "btp://0x5.pra/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string(),
                ),
            ],
        });
        let btp_message = BtpMessage::try_from("-MacYnRwOi8vMTIzNC5pY29uZWUvMHgxMjM0NTY3OJxidHA6Ly8xMjM0Lmljb25lZS8weDEyMzQ1Njc4g2JtY4SAAAAAuIH4f4RJbml0uHj4dvh0uDhidHA6Ly8weDEucHJhL2N4ODdlZDkwNDhiNTk0Yjk1MTk5ZjMyNmZjNzZlNzZhOWQzM2RkNjY1Yrg4YnRwOi8vMHg1LnByYS9jeDg3ZWQ5MDQ4YjU5NGI5NTE5OWYzMjZmYzc2ZTc2YTlkMzNkZDY2NWI".to_string()).unwrap();
        assert_eq!(
            btp_message,
            BtpMessage::new(
                BTPAddress::new("btp://1234.iconee/0x12345678".to_string()),
                BTPAddress::new("btp://1234.iconee/0x12345678".to_string()),
                "bmc".to_string(),
                WrappedI128::new(-2147483648),
                <Vec<u8>>::from(bmc_service_message.clone()),
                Some(BmcServiceMessage::new(BmcServiceType::Init {
                    links: vec![
                        BTPAddress::new(
                            "btp://0x1.pra/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string()
                        ),
                        BTPAddress::new(
                            "btp://0x5.pra/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string()
                        )
                    ]
                }))
            )
        );
    }

    #[test]
    fn deserialize_btp_message_sn_negative_i64() {
        let bmc_service_message = BmcServiceMessage::new(BmcServiceType::Init {
            links: vec![
                BTPAddress::new(
                    "btp://0x1.pra/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string(),
                ),
                BTPAddress::new(
                    "btp://0x5.pra/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string(),
                ),
            ],
        });
        let btp_message = BtpMessage::try_from("-MqcYnRwOi8vMTIzNC5pY29uZWUvMHgxMjM0NTY3OJxidHA6Ly8xMjM0Lmljb25lZS8weDEyMzQ1Njc4g2JtY4iAAAAAAAAAALiB-H-ESW5pdLh4-Hb4dLg4YnRwOi8vMHgxLnByYS9jeDg3ZWQ5MDQ4YjU5NGI5NTE5OWYzMjZmYzc2ZTc2YTlkMzNkZDY2NWK4OGJ0cDovLzB4NS5wcmEvY3g4N2VkOTA0OGI1OTRiOTUxOTlmMzI2ZmM3NmU3NmE5ZDMzZGQ2NjVi".to_string()).unwrap();
        assert_eq!(
            btp_message,
            BtpMessage::new(
                BTPAddress::new("btp://1234.iconee/0x12345678".to_string()),
                BTPAddress::new("btp://1234.iconee/0x12345678".to_string()),
                "bmc".to_string(),
                WrappedI128::new(-9223372036854775808),
                <Vec<u8>>::from(bmc_service_message.clone()),
                Some(BmcServiceMessage::new(BmcServiceType::Init {
                    links: vec![
                        BTPAddress::new(
                            "btp://0x1.pra/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string()
                        ),
                        BTPAddress::new(
                            "btp://0x5.pra/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string()
                        )
                    ]
                }))
            )
        );
    }

    #[test]
    fn deserialize_btp_message_sn_negative_i128() {
        let bmc_service_message = BmcServiceMessage::new(BmcServiceType::Init {
            links: vec![
                BTPAddress::new(
                    "btp://0x1.pra/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string(),
                ),
                BTPAddress::new(
                    "btp://0x5.pra/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string(),
                ),
            ],
        });
        let btp_message = BtpMessage::try_from("-NKcYnRwOi8vMTIzNC5pY29uZWUvMHgxMjM0NTY3OJxidHA6Ly8xMjM0Lmljb25lZS8weDEyMzQ1Njc4g2JtY5CAAAAAAAAAAAAAAAAAAAAAuIH4f4RJbml0uHj4dvh0uDhidHA6Ly8weDEucHJhL2N4ODdlZDkwNDhiNTk0Yjk1MTk5ZjMyNmZjNzZlNzZhOWQzM2RkNjY1Yrg4YnRwOi8vMHg1LnByYS9jeDg3ZWQ5MDQ4YjU5NGI5NTE5OWYzMjZmYzc2ZTc2YTlkMzNkZDY2NWI".to_string()).unwrap();
        assert_eq!(
            btp_message,
            BtpMessage::new(
                BTPAddress::new("btp://1234.iconee/0x12345678".to_string()),
                BTPAddress::new("btp://1234.iconee/0x12345678".to_string()),
                "bmc".to_string(),
                WrappedI128::new(-170141183460469231731687303715884105728),
                <Vec<u8>>::from(bmc_service_message.clone()),
                Some(BmcServiceMessage::new(BmcServiceType::Init {
                    links: vec![
                        BTPAddress::new(
                            "btp://0x1.pra/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string()
                        ),
                        BTPAddress::new(
                            "btp://0x5.pra/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string()
                        )
                    ]
                }))
            )
        );
    }

    #[test]
    fn serialize_btp_message() {
        let bmc_service_message = BmcServiceMessage::new(BmcServiceType::Init {
            links: vec![
                BTPAddress::new(
                    "btp://0x1.pra/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string(),
                ),
                BTPAddress::new(
                    "btp://0x5.pra/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string(),
                ),
            ],
        });
        let btp_message = BtpMessage::new(
            BTPAddress::new("btp://1234.iconee/0x12345678".to_string()),
            BTPAddress::new("btp://1234.iconee/0x12345678".to_string()),
            "bmc".to_string(),
            WrappedI128::new(1),
            <Vec<u8>>::from(bmc_service_message.clone()),
            Some(bmc_service_message),
        );
        assert_eq!(String::from(&btp_message), "-MKcYnRwOi8vMTIzNC5pY29uZWUvMHgxMjM0NTY3OJxidHA6Ly8xMjM0Lmljb25lZS8weDEyMzQ1Njc4g2JtYwG4gfh_hEluaXS4ePh2-HS4OGJ0cDovLzB4MS5wcmEvY3g4N2VkOTA0OGI1OTRiOTUxOTlmMzI2ZmM3NmU3NmE5ZDMzZGQ2NjViuDhidHA6Ly8weDUucHJhL2N4ODdlZDkwNDhiNTk0Yjk1MTk5ZjMyNmZjNzZlNzZhOWQzM2RkNjY1Yg".to_string());
    }

    #[test]
    fn serialize_serialized_btp_message() {
        let bmc_service_message = BmcServiceMessage::new(BmcServiceType::Init {
            links: vec![
                BTPAddress::new(
                    "btp://0x1.pra/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string(),
                ),
                BTPAddress::new(
                    "btp://0x5.pra/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string(),
                ),
            ],
        });
        let btp_message = BtpMessage::new(
            BTPAddress::new("btp://1234.iconee/0x12345678".to_string()),
            BTPAddress::new("btp://1234.iconee/0x12345678".to_string()),
            "bmc".to_string(),
            WrappedI128::new(1),
            vec![],
            Some(bmc_service_message),
        );
        assert_eq!(String::from(&<BtpMessage<SerializedMessage>>::try_from(&btp_message).unwrap()), "-MKcYnRwOi8vMTIzNC5pY29uZWUvMHgxMjM0NTY3OJxidHA6Ly8xMjM0Lmljb25lZS8weDEyMzQ1Njc4g2JtYwG4gfh_hEluaXS4ePh2-HS4OGJ0cDovLzB4MS5wcmEvY3g4N2VkOTA0OGI1OTRiOTUxOTlmMzI2ZmM3NmU3NmE5ZDMzZGQ2NjViuDhidHA6Ly8weDUucHJhL2N4ODdlZDkwNDhiNTk0Yjk1MTk5ZjMyNmZjNzZlNzZhOWQzM2RkNjY1Yg".to_string());
    }

    #[test]
    fn serialize_btp_message_sn_negative_i8() {
        let bmc_service_message = BmcServiceMessage::new(BmcServiceType::Init {
            links: vec![
                BTPAddress::new(
                    "btp://0x1.pra/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string(),
                ),
                BTPAddress::new(
                    "btp://0x5.pra/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string(),
                ),
            ],
        });
        let btp_message = BtpMessage::new(
            BTPAddress::new("btp://1234.iconee/0x12345678".to_string()),
            BTPAddress::new("btp://1234.iconee/0x12345678".to_string()),
            "bmc".to_string(),
            WrappedI128::new(-128),
            <Vec<u8>>::from(bmc_service_message.clone()),
            Some(bmc_service_message),
        );
        assert_eq!(String::from(&btp_message), "-MOcYnRwOi8vMTIzNC5pY29uZWUvMHgxMjM0NTY3OJxidHA6Ly8xMjM0Lmljb25lZS8weDEyMzQ1Njc4g2JtY4GAuIH4f4RJbml0uHj4dvh0uDhidHA6Ly8weDEucHJhL2N4ODdlZDkwNDhiNTk0Yjk1MTk5ZjMyNmZjNzZlNzZhOWQzM2RkNjY1Yrg4YnRwOi8vMHg1LnByYS9jeDg3ZWQ5MDQ4YjU5NGI5NTE5OWYzMjZmYzc2ZTc2YTlkMzNkZDY2NWI".to_string());
    }

    #[test]
    fn serialize_btp_message_sn_negative_i16() {
        let bmc_service_message = BmcServiceMessage::new(BmcServiceType::Init {
            links: vec![
                BTPAddress::new(
                    "btp://0x1.pra/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string(),
                ),
                BTPAddress::new(
                    "btp://0x5.pra/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string(),
                ),
            ],
        });
        let btp_message = BtpMessage::new(
            BTPAddress::new("btp://1234.iconee/0x12345678".to_string()),
            BTPAddress::new("btp://1234.iconee/0x12345678".to_string()),
            "bmc".to_string(),
            WrappedI128::new(-32768),
            <Vec<u8>>::from(bmc_service_message.clone()),
            Some(bmc_service_message),
        );
        assert_eq!(String::from(&btp_message), "-MScYnRwOi8vMTIzNC5pY29uZWUvMHgxMjM0NTY3OJxidHA6Ly8xMjM0Lmljb25lZS8weDEyMzQ1Njc4g2JtY4KAALiB-H-ESW5pdLh4-Hb4dLg4YnRwOi8vMHgxLnByYS9jeDg3ZWQ5MDQ4YjU5NGI5NTE5OWYzMjZmYzc2ZTc2YTlkMzNkZDY2NWK4OGJ0cDovLzB4NS5wcmEvY3g4N2VkOTA0OGI1OTRiOTUxOTlmMzI2ZmM3NmU3NmE5ZDMzZGQ2NjVi".to_string());
    }

    #[test]
    fn serialize_btp_message_sn_negative_i32() {
        let bmc_service_message = BmcServiceMessage::new(BmcServiceType::Init {
            links: vec![
                BTPAddress::new(
                    "btp://0x1.pra/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string(),
                ),
                BTPAddress::new(
                    "btp://0x5.pra/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string(),
                ),
            ],
        });
        let btp_message = BtpMessage::new(
            BTPAddress::new("btp://1234.iconee/0x12345678".to_string()),
            BTPAddress::new("btp://1234.iconee/0x12345678".to_string()),
            "bmc".to_string(),
            WrappedI128::new(-2147483648),
            <Vec<u8>>::from(bmc_service_message.clone()),
            Some(bmc_service_message),
        );
        assert_eq!(String::from(&btp_message), "-MacYnRwOi8vMTIzNC5pY29uZWUvMHgxMjM0NTY3OJxidHA6Ly8xMjM0Lmljb25lZS8weDEyMzQ1Njc4g2JtY4SAAAAAuIH4f4RJbml0uHj4dvh0uDhidHA6Ly8weDEucHJhL2N4ODdlZDkwNDhiNTk0Yjk1MTk5ZjMyNmZjNzZlNzZhOWQzM2RkNjY1Yrg4YnRwOi8vMHg1LnByYS9jeDg3ZWQ5MDQ4YjU5NGI5NTE5OWYzMjZmYzc2ZTc2YTlkMzNkZDY2NWI".to_string());
    }

    #[test]
    fn serialize_btp_message_sn_negative_i64() {
        let bmc_service_message = BmcServiceMessage::new(BmcServiceType::Init {
            links: vec![
                BTPAddress::new(
                    "btp://0x1.pra/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string(),
                ),
                BTPAddress::new(
                    "btp://0x5.pra/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string(),
                ),
            ],
        });
        let btp_message = BtpMessage::new(
            BTPAddress::new("btp://1234.iconee/0x12345678".to_string()),
            BTPAddress::new("btp://1234.iconee/0x12345678".to_string()),
            "bmc".to_string(),
            WrappedI128::new(-9223372036854775808),
            <Vec<u8>>::from(bmc_service_message.clone()),
            Some(bmc_service_message),
        );
        assert_eq!(String::from(&btp_message), "-MqcYnRwOi8vMTIzNC5pY29uZWUvMHgxMjM0NTY3OJxidHA6Ly8xMjM0Lmljb25lZS8weDEyMzQ1Njc4g2JtY4iAAAAAAAAAALiB-H-ESW5pdLh4-Hb4dLg4YnRwOi8vMHgxLnByYS9jeDg3ZWQ5MDQ4YjU5NGI5NTE5OWYzMjZmYzc2ZTc2YTlkMzNkZDY2NWK4OGJ0cDovLzB4NS5wcmEvY3g4N2VkOTA0OGI1OTRiOTUxOTlmMzI2ZmM3NmU3NmE5ZDMzZGQ2NjVi".to_string());
    }

    #[test]
    fn serialize_btp_message_sn_negative_i128() {
        let bmc_service_message = BmcServiceMessage::new(BmcServiceType::Init {
            links: vec![
                BTPAddress::new(
                    "btp://0x1.pra/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string(),
                ),
                BTPAddress::new(
                    "btp://0x5.pra/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string(),
                ),
            ],
        });
        let btp_message = BtpMessage::new(
            BTPAddress::new("btp://1234.iconee/0x12345678".to_string()),
            BTPAddress::new("btp://1234.iconee/0x12345678".to_string()),
            "bmc".to_string(),
            WrappedI128::new(-170141183460469231731687303715884105728),
            <Vec<u8>>::from(bmc_service_message.clone()),
            Some(bmc_service_message),
        );
        assert_eq!(String::from(&btp_message), "-NKcYnRwOi8vMTIzNC5pY29uZWUvMHgxMjM0NTY3OJxidHA6Ly8xMjM0Lmljb25lZS8weDEyMzQ1Njc4g2JtY5CAAAAAAAAAAAAAAAAAAAAAuIH4f4RJbml0uHj4dvh0uDhidHA6Ly8weDEucHJhL2N4ODdlZDkwNDhiNTk0Yjk1MTk5ZjMyNmZjNzZlNzZhOWQzM2RkNjY1Yrg4YnRwOi8vMHg1LnByYS9jeDg3ZWQ5MDQ4YjU5NGI5NTE5OWYzMjZmYzc2ZTc2YTlkMzNkZDY2NWI".to_string());
    }
}
