use super::*;

#[near_bindgen]
impl BtpMessageVerifier {
    pub fn handle_relay_message(
        &mut self,
        bmc: BTPAddress,
        source: BTPAddress,
        rx_seq: u128,
        relay_message: RelayMessage,
    ) -> PromiseOrValue<VerifierResponse> {
        self.assert_predecessor_is_bmc();
        self.assert_bmc_is_valid(&bmc);
        self.assert_source_is_valid(&source);
        self.assert_have_block_updates_or_block_proof(&relay_message);

        let previous_height = self.mta.height();
        let mut state_changes = StateChanges::new();
        let mut last_block_header = BlockHeader::default();
        let mut btp_messages = SerializedBtpMessages::new();

        let outcome = self.process_block_updates(
            relay_message.block_updates(),
            &mut last_block_header,
            &mut state_changes,
        );
        if outcome.is_err() {
            #[cfg(feature = "testable")]
            outcome.clone().unwrap();

            return PromiseOrValue::Value(VerifierResponse::Failed(outcome.unwrap_err()));
        }

        if relay_message.block_proof().is_some() {
            let outcome = self.process_block_proof(
                relay_message
                    .block_proof()
                    .get()
                    .map_err(|message| BmvError::InvalidBlockProof {
                        message: message.to_string(),
                    })
                    .unwrap(),
                &mut last_block_header,
            );
            if outcome.is_err() {
                #[cfg(feature = "testable")]
                outcome.clone().unwrap();

                return PromiseOrValue::Value(VerifierResponse::Failed(outcome.unwrap_err()));
            }
        }

        if !relay_message.receipt_proofs().is_empty() {
            let outcome = self.process_receipt_proofs(
                rx_seq,
                source,
                bmc,
                relay_message.receipt_proofs(),
                &last_block_header,
                &mut btp_messages,
            );
            if outcome.is_err() {
                #[cfg(feature = "testable")]
                outcome.clone().unwrap();

                return PromiseOrValue::Value(VerifierResponse::Failed(outcome.unwrap_err()));
            }
        }

        if !btp_messages.is_empty() {
            state_changes.push(StateChange::SetLastHeight {
                height: last_block_header.height(),
            });
        }

        self.update_state(&mut state_changes);

        PromiseOrValue::Value(VerifierResponse::Success {
            previous_height,
            messages: btp_messages,
            verifier_status: self.status(),
        })
    }
}
