use super::{Network, TokenName};
use near_sdk::serde::{Deserialize, Serialize};
use crate::rlp::{self, Decodable, Encodable};
use near_sdk::borsh::{self, BorshDeserialize, BorshSerialize};

#[derive(Clone, Debug, PartialEq, Eq, BorshDeserialize, BorshSerialize)]
pub struct Asset {
    token: TokenName,
    amount: u128,
    fees: u128,
}

impl Asset {
    pub fn new(token: TokenName, amount: u128, fees: u128) -> Self {
        Self {
            token,
            amount,
            fees,
        }
    }

    pub fn token(&self) -> &TokenName {
        &self.token
    }

    pub fn amount(&self) -> u128 {
        self.amount
    }

    pub fn fees(&self) -> u128 {
        self.fees
    }
}

impl Encodable for Asset {
    fn rlp_append(&self, stream: &mut rlp::RlpStream) {
        stream
            .begin_list(3)
            .append(&self.token)
            .append(&self.amount)
            .append(&self.fees);
    }
}

impl Decodable for Asset {
    fn decode(rlp: &rlp::Rlp) -> Result<Self, rlp::DecoderError> {
        Ok(Self::new(
            rlp.val_at::<TokenName>(0)?,
            rlp.val_at::<u128>(1)?,
            rlp.val_at::<u128>(2).unwrap_or_default(),
        ))
    }
}

#[derive(Debug, PartialEq, Eq, Serialize, Deserialize)]
pub struct AccumulatedAssetFees {
    pub name: TokenName,
    pub network: Network,
    pub accumulated_fees: u128,
}
