use crate::types::{TransferRequest, TransferResponse, message::ServiceMessage};
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
pub struct TokenService {
    service_type: ServiceType,
    action: ServiceAction
}

impl ServiceMessage for TokenService {
    type ServiceType = ServiceType;

    fn service_type(&self) -> &Self::ServiceType {
       &self.service_type 
    }

    fn set_service_type(&mut self, service_type: &Self::ServiceType) {
        self.service_type.clone_from(&service_type)
    }
}
