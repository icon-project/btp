use super::{EventLog, Nullable};
use libraries::rlp::{self, Decodable};
use libraries::types::Hash;

#[derive(Debug)]
pub struct Receipt {
    status: u64,
    to: Nullable<Vec<u8>>,
    cumulative_step_used: Nullable<Vec<u8>>,
    step_used: Nullable<Vec<u8>>,
    step_price: Nullable<Vec<u8>>,
    logs_bloom: Nullable<Vec<u8>>,
    event_logs: Vec<EventLog>,
    score_address: Nullable<Vec<u8>>,
    event_logs_hash: Nullable<Hash>,
}

impl Receipt {
    pub fn event_logs_hash(&self) -> &Nullable<Hash> {
        &self.event_logs_hash
    }

    pub fn event_logs_mut(&mut self) -> &mut Vec<EventLog> {
        &mut self.event_logs
    }

    pub fn event_logs(&self) -> &Vec<EventLog> {
        &self.event_logs
    }
}

impl Decodable for Receipt {
    fn decode(rlp: &rlp::Rlp) -> Result<Self, rlp::DecoderError> {
        Ok(Self {
            status: rlp.val_at(0)?,
            to: rlp.val_at(1)?,
            cumulative_step_used: rlp.val_at(2)?,
            step_used: rlp.val_at(3)?,
            step_price: rlp.val_at(4)?,
            logs_bloom: rlp.val_at(5)?,
            event_logs: rlp.list_at(6)?,
            score_address: rlp.val_at(7)?,
            event_logs_hash: rlp.val_at(8)?,
        })
    }
}
