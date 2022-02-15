use super::Network;
use crate::rlp::{self, Decodable, Encodable};
use near_sdk::borsh::{self, BorshDeserialize, BorshSerialize};
use near_sdk::serde::{Deserialize, Serialize};

#[derive(Clone, Debug, PartialEq, Eq, BorshDeserialize, BorshSerialize)]
pub struct TransferableAsset {
    name: String,
    amount: u128,
    fees: u128,
}

impl TransferableAsset {
    pub fn new(name: String, amount: u128, fees: u128) -> Self {
        Self { name, amount, fees }
    }

    pub fn name(&self) -> &String {
        &self.name
    }

    pub fn amount(&self) -> u128 {
        self.amount
    }

    pub fn fees(&self) -> u128 {
        self.fees
    }
}

impl Encodable for TransferableAsset {
    fn rlp_append(&self, stream: &mut rlp::RlpStream) {
        stream
            .begin_list(3)
            .append(&self.name)
            .append(&self.amount)
            .append(&self.fees);
    }
}

impl Decodable for TransferableAsset {
    fn decode(rlp: &rlp::Rlp) -> Result<Self, rlp::DecoderError> {
        Ok(Self::new(
            rlp.val_at::<String>(0)?,
            rlp.val_at::<u128>(1)?,
            rlp.val_at::<u128>(2).unwrap_or_default(),
        ))
    }
}

#[derive(Debug, PartialEq, Eq, Serialize, Deserialize)]
pub struct AccumulatedAssetFees {
    pub name: String,
    pub network: Network,
    pub accumulated_fees: u128,
}
