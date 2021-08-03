use near_sdk::borsh::{self, BorshDeserialize, BorshSerialize};
use near_sdk::{ collections::LookupSet, serde::{Serialize, Deserialize},json_types::{I128, Base64VecU8}};

#[derive(Serialize, Deserialize, BorshDeserialize, BorshSerialize)]
pub enum Event {
    Message { _nxt: String, _seq: String, _msg: Base64VecU8 },
    ErrorOnBTPError { _svc: String, _sn: I128, _code: i8, _errMsg: String, _svcErrCode: i8, _svcErrMsg: String }
}

#[derive(BorshDeserialize, BorshSerialize)]
pub struct Events(LookupSet<Event>);

impl Events {
    pub fn new() -> Self {
        Self(LookupSet::new(b"events".to_vec()))
    }

    pub fn add(&mut self, event: Event) {
        self.0.insert(&event);
    }
}