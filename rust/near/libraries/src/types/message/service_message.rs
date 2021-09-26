pub trait ServiceMessage {
    type ServiceType;
    fn service_type(&self) -> &Self::ServiceType;
    fn set_service_type(&mut self, service_type: &Self::ServiceType);
}