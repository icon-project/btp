use super::*;

impl BtpMessageVerifier {
    pub fn update_state(&mut self, state_changes: &mut StateChanges) {
        state_changes.retain(|state_change| {
            match state_change {
                StateChange::SetValidators { validators } => {
                    self.validators.set(validators);
                }
                StateChange::MtaAddBlockHash { block_hash } => {
                    self.mta.add(*block_hash);
                }
            };
            false
        });
    }
}
