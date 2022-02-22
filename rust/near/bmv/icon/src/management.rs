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
}
