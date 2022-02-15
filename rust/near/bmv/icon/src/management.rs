use super::*;

#[near_bindgen]
impl BtpMessageVerifier {
    pub fn get_status(&self) -> PromiseOrValue<VerifierStatus> {
        PromiseOrValue::Value(self.status())
    }
}

impl BtpMessageVerifier {

    pub fn status(&self) -> VerifierStatus {
        VerifierStatus::new(self.mta.height(), self.mta.offset(), self.last_height)
    }

    pub fn update_state(&mut self, state_changes: &mut StateChanges) {
        state_changes.retain(|state_change| {
            match state_change {
                StateChange::SetValidators { validators } => {
                    self.validators.set(validators);
                }
                StateChange::MtaAddBlockHash { block_hash } => {
                    // self.mta.add(*block_hash);
                }
                StateChange::SetLastHeight { height} => {
                    self.last_height.clone_from(height);
                }
            };
            false
        });
    }
}
