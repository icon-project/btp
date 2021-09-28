use crate::types::{TransferRequest, TransferResponse};
use near_sdk::{
    borsh::{maybestd::io, self, BorshDeserialize, BorshSerialize},
    serde::{de, Deserialize, Serialize},
};
use std::convert::TryFrom;

#[derive(Clone)]
pub enum ServiceType {
    RequestTokenTransfer,
    RequestTokenRegister,
    ResponseHandleService,
    UnknownType,
}

pub enum ServiceAction {
    Request(TransferRequest),
    Response(TransferResponse),
}
pub struct TokenServiceMessage {
    service_type: ServiceType,
    action: ServiceAction
}