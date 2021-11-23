use super::*;

impl BtpMessageVerifier {
    pub fn process_receipt_proofs(
        &mut self,
        receipt_proofs: &Vec<ReceiptProof>,
        last_block_header: &BlockHeader,
        btp_messages: &mut SerializedBtpMessages,
    ) -> Result<(), BmvError> {
        receipt_proofs
            .iter()
            .map(|receipt_proof| -> Result<(), BmvError> { Ok(()) })
            .collect()
    }
}
