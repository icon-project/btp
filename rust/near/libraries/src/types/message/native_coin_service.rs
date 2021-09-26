use crate::types::{TransferRequest, TransferResponse, message::ServiceMessage};

#[derive(Clone)]
pub enum ServiceType {
    RequestCoinTransfer,
    RequestCoinRegister,
    ResponseHandleService,
    UnknownType,
}

pub struct NativeCoinService {
    service_type: ServiceType,
}

impl ServiceMessage for NativeCoinService {
    type ServiceType = ServiceType;

    fn service_type(&self) -> &Self::ServiceType {
       &self.service_type 
    }

    fn set_service_type(&mut self, service_type: &Self::ServiceType) {
        self.service_type.clone_from(&service_type)
    }
}
