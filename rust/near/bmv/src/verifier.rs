use libraries::bmv_types::*;
use near_sdk::CryptoHash;

pub trait Verifier {
    fn verify_mtp_proof(&mut self, _receipt_hash: CryptoHash) -> Receipt {
        todo!()
    }
    fn to_message_event(&mut self) -> MessageEvent {
        todo!()
    }
}
