use crate::types::{TransferRequest, TransferResponse};

#[derive(Clone)]
pub enum ServiceType {
    RequestCoinTransfer,
    RequestCoinRegister,
    ResponseHandleService,
    UnknownType,
}

pub struct NativeCoinServiceMessage {
    service_type: ServiceType,
}