use libraries::types::Request;
use libraries::types::WrappedI128;

use super::*;

#[near_bindgen]
impl NativeCoinService {
    // * * * * * * * * * * * * * * * * *
    // * * * * * * * * * * * * * * * * *
    // * * * * * *  Messaging  * * * * *
    // * * * * * * * * * * * * * * * * *
    // * * * * * * * * * * * * * * * * *

    pub fn handle_btp_message(&mut self, message: BtpMessage<SerializedMessage>) {
        self.assert_predecessor_is_bmc();
        self.assert_valid_service(message.service());
        let outcome = self.handle_service_message(message.clone().try_into());

        #[cfg(feature = "testable")]
        outcome.clone().unwrap();

        if outcome.is_err() {
            let error = outcome.unwrap_err();
            self.send_response(
                message.serial_no(),
                message.source(),
                NativeCoinServiceMessage::new(NativeCoinServiceType::ResponseHandleService {
                    code: 1,
                    message: format!("{}", error),
                }),
            );
        } else {
            match outcome.unwrap() {
                Some(service_message) => {
                    self.send_response(message.serial_no(), message.source(), service_message);
                }
                None => (),
            }
        }
    }

    pub fn handle_btp_error(
        &mut self,
        source: BTPAddress,
        service: String,
        serial_no: i128,
        code: u128,
        message: String,
    ) {
        self.assert_predecessor_is_bmc();
        self.assert_valid_service(&service);
        self.handle_response(&WrappedI128::new(serial_no), 1, &format!("[BTPError] source: {}, code: {} message: {}", source, code, message)).unwrap();
    }

    pub fn handle_fee_gathering(&mut self, fee_aggregator: BTPAddress, service: String) {
        self.assert_predecessor_is_bmc();
        self.assert_valid_service(&service);
        self.transfer_fees(&fee_aggregator);
    }

    #[cfg(feature = "testable")]
    pub fn last_request(&self) -> Option<Request> {
        self.requests().get(self.serial_no())
    }
}

impl NativeCoinService {
    fn handle_service_message(
        &mut self,
        message: Result<BtpMessage<NativeCoinServiceMessage>, BshError>,
    ) -> Result<Option<NativeCoinServiceMessage>, BshError> {
        let btp_message = message.clone()?;

        if let Some(service_message) = btp_message.message() {
            match service_message.service_type() {
                NativeCoinServiceType::RequestCoinTransfer {
                    sender: _,
                    ref receiver,
                    ref assets,
                } => self.handle_coin_transfer(btp_message.source(), receiver, assets),

                NativeCoinServiceType::ResponseHandleService {
                    ref code,
                    ref message,
                } => self.handle_response(btp_message.serial_no(), *code, &message),

                NativeCoinServiceType::UnknownType => {
                    log!(
                        "Unknown Response: from {} for serial_no {}",
                        btp_message.source(),
                        btp_message.serial_no().get()
                    );
                    Ok(None)
                }

                _ => Ok(Some(NativeCoinServiceMessage::new(
                    NativeCoinServiceType::UnknownType,
                ))),
            }
        } else {
            Err(BshError::Unknown)
        }
    }

    pub fn send_request(
        &mut self,
        sender_id: AccountId,
        destination: BTPAddress,
        assets: Vec<Asset>,
    ) {
        let serial_no = self.serial_no.checked_add(1).unwrap();
        let message = NativeCoinServiceMessage::new(NativeCoinServiceType::RequestCoinTransfer {
            sender: sender_id.clone().into(),
            receiver: destination.account_id().into(),
            assets: assets.clone(),
        });

        self.requests_mut().add(
            serial_no,
            &Request::new(
                sender_id.clone().into(),
                destination.account_id().into(),
                assets,
            ),
        );
        self.send_service_message(destination.network_address().unwrap(), message.into());
    }

    pub fn send_response(
        &mut self,
        serial_no: &WrappedI128,
        destination: &BTPAddress,
        service_message: NativeCoinServiceMessage,
    ) {
        self.send_service_response(
            serial_no,
            destination.network_address().unwrap(),
            service_message.into(),
        );
    }

    fn handle_response(
        &mut self,
        serial_no: &WrappedI128,
        code: u128,
        _message: &str,
    ) -> Result<Option<NativeCoinServiceMessage>, BshError> {
        if let Some(request) = self.requests().get(*serial_no.get()) {
            let sender_id = AccountId::try_from(request.sender().to_owned()).map_err(|error| {
                BshError::InvalidAddress {
                    message: error.to_string(),
                }
            })?;
            if code == 0 {
                self.finalize_external_transfer(&sender_id, request.assets());
            } else if code == 1 {
                self.rollback_external_transfer(&sender_id, request.assets());
            }
            self.requests_mut().remove(*serial_no.get());
        }
        Ok(None)
    }

    pub fn send_service_message(
        &mut self,
        destination_network: String,
        message: SerializedMessage,
    ) {
        self.serial_no
            .clone_from(&self.serial_no.checked_add(1).unwrap());
        // TODO
    }

    pub fn send_service_response(
        &mut self,
        serial_no: &WrappedI128,
        destination_network: String,
        message: SerializedMessage,
    ) {
        // TODO
    }
}
