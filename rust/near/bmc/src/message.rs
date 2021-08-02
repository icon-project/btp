//! Messaging Module

use super::*;

#[near_bindgen]
impl BTPMessageCenter {
    #[private]
    pub fn emit_event(&mut self, message: Vec<u8>, next: String, sequence: String){
        self.events.add(Event::Message{ _msg: Base64VecU8(message), _nxt: next, _seq: sequence })
    }
    #[private]
    pub fn emit_error(&mut self, _msg: Vec<u8>, _nxt: String, _seq: String){
        //self.events.add(Event::ErrorOnBTPError{})
    }
}
