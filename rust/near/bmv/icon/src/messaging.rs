use super::*;

#[near_bindgen]
impl BtpMessageVerifier {
    pub fn handle_relay_message(
        &mut self,
        relay_message: RelayMessage,
    ) -> PromiseOrValue<Response> {
        self.assert_predecessor_is_bmc();
        self.assert_have_block_updates_or_block_proof(&relay_message);

        let mut state_changes = StateChanges::new();
        let mut last_block_header = BlockHeader::default();

        let outcome = self.process_block_updates(
            relay_message.block_updates(),
            &mut last_block_header,
            &mut state_changes,
        );
        if outcome.is_err() {
            return PromiseOrValue::Value(Response::Failed);
        }

        if relay_message.block_proof().is_some() {
            let outcome =
                self.process_block_proof(relay_message.block_proof().get(), &mut last_block_header);
            if outcome.is_err() {
                return PromiseOrValue::Value(Response::Failed);
            }
        }

        if !relay_message.receipt_proofs().is_empty() {
            let outcome = self.process_receipt_proofs(relay_message.receipt_proofs(), &last_block_header);
            if outcome.is_err() {
                return PromiseOrValue::Value(Response::Failed);
            }
        }
        self.update_state(&mut state_changes);

        PromiseOrValue::Value(Response::Success)
    }
}
