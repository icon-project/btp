use crate::types::{message::ServiceMessage, BTPAddress, WrappedI128};

#[derive(Clone, PartialEq, Eq, Debug)]
pub struct BtpMessage<T: ServiceMessage> {
    source: BTPAddress,
    destination: BTPAddress,
    service: String,
    serial_no: WrappedI128,
    payload: Vec<u8>,
    service_message: Option<T>,
}

impl<T> BtpMessage<T>
where
    T: ServiceMessage,
{
    pub fn new(
        source: BTPAddress,
        destination: BTPAddress,
        service: String,
        serial_no: WrappedI128,
        payload: Vec<u8>,
        service_message: Option<T>,
    ) -> Self {
        Self {
            source,
            destination,
            service,
            serial_no,
            payload,
            service_message,
        }
    }
}