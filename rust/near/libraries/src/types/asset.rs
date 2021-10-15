use super::{Network, TokenName};
use near_sdk::serde::{Deserialize, Serialize};
use rlp::{self, Decodable, Encodable};

#[derive(Clone, Debug, PartialEq, Eq)]
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

#[derive(Serialize, Deserialize)]
pub struct AccumulatedAssetFees {
    pub name: TokenName,
    pub network: Network,
    pub accumulated_fees: u128,
}
