use super::PartSetId;

pub static VOTE_TYPE_PRECOMMIT: u8 = 1;

pub struct VoteMessage {
    height: u64,
    round: u64,
    vote_type: u64,
    block_id: Vec<u8>,
    part_set_id: PartSetId,
    timestamp: u64
}