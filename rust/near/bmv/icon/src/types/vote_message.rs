use super::{PartSetId, RlpBytes, Nullable};
use libraries::rlp::{self, Encodable};
use std::convert::AsRef;
use libraries::types::Hash;
use hex::{decode, encode};

pub static VOTE_TYPE_PRECOMMIT: u8 = 1;

#[derive(PartialEq, Eq, Debug, Clone)]
pub struct VoteMessage {
    height: u64,
    round: u8,
    vote_type: u8,
    block_id: Hash,
    part_set_id: Nullable<PartSetId>,
    timestamp: u64,
}

impl VoteMessage {
    pub fn new(
        height: u64,
        round: u8,
        vote_type: u8,
        block_id: Hash,
        part_set_id: PartSetId,
    ) -> Self {
        Self {
            height,
            round,
            vote_type,
            block_id,
            part_set_id: Nullable::new(Some(part_set_id)),
            timestamp: Default::default(),
        }
    }

    pub fn timestamp_mut(&mut self) -> &mut u64 {
        &mut self.timestamp
    }
}

impl Encodable for VoteMessage {
    fn rlp_append(&self, stream: &mut libraries::rlp::RlpStream) {
        stream
            .begin_list(6)
            .append(&self.height)
            .append(&self.round)
            .append(&self.vote_type)
            .append(&self.block_id)
            .append(&self.part_set_id)
            .append(&self.timestamp);
    }
}

impl AsRef<VoteMessage> for VoteMessage {
    fn as_ref(&self) -> &Self {
        &self
    }
}

impl From<&VoteMessage> for RlpBytes {
    fn from(vote_message: &VoteMessage) -> Self {
        rlp::encode(vote_message).to_vec()
    }
}
