use libraries::bmv_types::*;
use merkle_tree_accumulator::hash::Hash;

pub trait Verifier {
    fn verify_mtp_proof(&mut self, _receipt_hash: Hash) -> Receipt {
        todo!()
    }
    fn to_message_event(&mut self) -> MessageEvent {
        todo!()
    }
}
