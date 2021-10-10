use super::{Network, TokenId, TokenName};
use near_sdk::serde::{Deserialize, Serialize};
use rlp::{self, Decodable, Encodable};

#[derive(Clone, Debug, PartialEq, Eq)]
pub struct Asset {
    token_id: TokenId,
    amount: u128,
    fees: u128,
}

impl Asset {
    pub fn new(token_id: TokenId, amount: u128, fees: u128) -> Self {
        Self { token_id, amount, fees }
    }

    pub fn token_id(&self) -> &TokenId {
        &self.token_id
    }

    pub fn amount(&self) -> u128 {
        self.amount
    }
}

impl Encodable for Asset {
    fn rlp_append(&self, stream: &mut rlp::RlpStream) {
        stream
            .append(&self.token_id)
            .append(&self.amount)
            .append(&self.fees);
    }
}

impl Decodable for Asset {
    fn decode(rlp: &rlp::Rlp) -> Result<Self, rlp::DecoderError> {
        Ok(Self::new(
            rlp.val_at::<TokenId>(0)?,
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
