use super::*;

#[near_bindgen]
impl BtpMessageVerifier {
    pub fn handle_relay_message(&mut self) {
        self.assert_predecessor_is_bmc();
    }
}