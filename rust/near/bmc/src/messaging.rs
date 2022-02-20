use libraries::types::WrappedI128;

use super::*;

#[near_bindgen]
impl BtpMessageCenter {
    // * * * * * * * * * * * * * * * * *
    // * * * * * * * * * * * * * * * * *
    // * * * *    Messaging    * * * * *
    // * * * * * * * * * * * * * * * * *
    // * * * * * * * * * * * * * * * * *

    pub fn handle_relay_message(&mut self, source: BTPAddress, message: SerializedMessage) {
        self.assert_link_exists(&source);
        self.assert_relay_is_registered(&source);
        let verifier = self.bmv.get(&source.network_address().unwrap()).unwrap();

        let link = self.links.get(&source).unwrap();

        bmv_contract::handle_relay_message(
            self.btp_address.clone(),
            source.clone(),
            link.rx_seq(),
            message,
            verifier.clone(),
            estimate::NO_DEPOSIT,
            estimate::HANDLE_RELAY_MESSAGE,
        )
        .then(bmc_contract::handle_relay_message_bmv_callback(
            source,
            env::current_account_id(),
            estimate::NO_DEPOSIT,
            estimate::HANDLE_RELAY_MESSAGE_BMV_CALLBACK,
        ));
    }

    #[private]
    pub fn handle_relay_message_bmv_callback(
        &mut self,
        source: BTPAddress,
        #[callback] verifier_response: VerifierResponse,
    ) {
        match verifier_response {
            VerifierResponse::Success {
                previous_height,
                verifier_status,
                messages,
            } => {
                if let Some(mut link) = self.links.get(&source) {
                    let relay = match link
                        .rotate_relay(verifier_status.last_height(), !messages.is_empty())
                    {
                        Some(relay) => {
                            self.assert_relay_is_valid(relay);
                            relay.clone()
                        }
                        None => env::predecessor_account_id(),
                    };

                    let mut relay_status = match link.relays().status(&relay) {
                        Some(status) => status,
                        None => RelayStatus::default(),
                    };
                    relay_status
                        .block_count_mut()
                        .add(verifier_status.mta_height())
                        .unwrap()
                        .sub(previous_height)
                        .unwrap();
                    relay_status
                        .message_count_mut()
                        .add(messages.len().try_into().unwrap())
                        .unwrap();
                    link.relays_mut().set_status(&relay, &relay_status);
                    self.links.set(&source, &link);
                    self.handle_btp_messages(source, messages)
                }
            }
            VerifierResponse::Failed(code) => (env::panic_str(format!("{}", code).as_str())),
        };
    }

    #[cfg(feature = "mockable")]
    #[private]
    pub fn handle_relay_message_bmv_callback_mockable(
        &mut self,
        source: BTPAddress,
        verifier_response: VerifierResponse,
    ) {
        match verifier_response {
            VerifierResponse::Success {
                previous_height,
                verifier_status,
                messages,
            } => {
                if let Some(mut link) = self.links.get(&source) {
                    let relay = match link
                        .rotate_relay(verifier_status.last_height(), !messages.is_empty())
                    {
                        Some(relay) => {
                            self.assert_relay_is_valid(relay);
                            relay.clone()
                        }
                        None => env::predecessor_account_id(),
                    };

                    let mut relay_status = match link.relays().status(&relay) {
                        Some(status) => status,
                        None => RelayStatus::default(),
                    };
                    relay_status
                        .block_count_mut()
                        .add(verifier_status.mta_height())
                        .unwrap()
                        .sub(previous_height)
                        .unwrap();
                    relay_status
                        .message_count_mut()
                        .add(messages.len().try_into().unwrap())
                        .unwrap();
                    link.relays_mut().set_status(&relay, &relay_status);
                    self.links.set(&source, &link);
                    self.handle_btp_messages(source, messages)
                }
            }
            VerifierResponse::Failed(code) => (env::panic_str(format!("{}", code).as_str())),
        };
    }

    #[cfg(feature = "testable")]
    pub fn get_message(&self) -> Result<BtpMessage<SerializedMessage>, String> {
        self.event.get_message()
    }

    #[cfg(feature = "testable")]
    pub fn send_message(
        &mut self,
        previous: &BTPAddress,
        destination: &BTPAddress,
        message: BtpMessage<SerializedMessage>,
    ) {
        if let Some(next) = self.resolve_route(destination) {
            self.emit_message(next, message);
        } else {
            self.send_error(
                previous,
                &BtpException::Bmc(BmcError::Unreachable {
                    destination: destination.to_string(),
                }),
                message,
            );
        }
    }

    #[cfg(not(feature = "testable"))]
    fn send_message(
        &mut self,
        source: &BTPAddress,
        destination: &BTPAddress,
        message: BtpMessage<SerializedMessage>,
    ) {
        if let Some(next) = self.resolve_route(destination) {
            bmc_contract::emit_message(
                next,
                message,
                env::current_account_id(),
                estimate::NO_DEPOSIT,
                estimate::SEND_MESSAGE,
            );
        } else {
            self.send_error(
                source,
                &BtpException::Bmc(BmcError::Unreachable {
                    destination: destination.to_string(),
                }),
                message,
            );
        }
    }

    pub fn send_service_message(
        &mut self,
        serial_no: i128,
        service: String,
        network: String,
        message: SerializedMessage,
    ) {
        self.assert_sender_is_authorized_service(&service);
        let destination = self
            .resolve_route(&BTPAddress::new(format!(
                "btp://{}/{}",
                network, 0000000000000000
            )))
            .expect(format!("{}", BmcError::LinkNotExist).as_str());
        let message = BtpMessage::new(
            self.btp_address.clone(),
            destination.clone(),
            service,
            WrappedI128::new(serial_no),
            message.data().clone(),
            None,
        );
        self.send_message(&self.btp_address.clone(), &destination, message);
    }

    #[private]
    pub fn emit_message(
        &mut self,
        link: BTPAddress,
        btp_message: BtpMessage<SerializedMessage>,
    ) -> Vec<u8> {
        if let Some(link_property) = self.links.get(&link).as_mut() {
            link_property.tx_seq_mut().add(1).unwrap();
            self.links.set(&link, &link_property);
            emit_message!(
                self,
                event,
                link_property.tx_seq(),
                link,
                btp_message.clone()
            );
        }
        env::keccak256(&<Vec<u8>>::from(btp_message))
    }

    #[private]
    pub fn handle_external_service_message_callback(
        &mut self,
        source: BTPAddress,
        message: BtpMessage<SerializedMessage>,
    ) {
        match env::promise_result(0) {
            PromiseResult::Failed => {
                self.send_error(&source, &BtpException::Bsh(BshError::Unknown), message)
            }
            _ => (),
        }
    }
}

impl BtpMessageCenter {
    pub fn handle_btp_messages(
        &mut self,
        source: BTPAddress,
        #[allow(unused_mut)] mut messages: SerializedBtpMessages,
    ) {
        messages
            .retain(|message| self.handle_btp_error_message(&source, message, BmcError::ErrorDrop));
        messages.retain(|message| self.handle_service_message(&source, message));
        messages.retain(|message| self.handle_route_message(&source, message));
    }

    pub fn propogate_internal(&mut self, service_message: BmcServiceMessage) {
        self.links
            .to_vec()
            .iter()
            .for_each(|link| self.send_internal_service_message(link, &service_message));
    }

    pub fn send_internal_service_message(
        &mut self,
        destination: &BTPAddress,
        service_message: &BmcServiceMessage,
    ) {
        let btp_message = <BtpMessage<BmcServiceMessage>>::new(
            self.btp_address.clone(),
            destination.clone(),
            SERVICE.to_string(),
            WrappedI128::new(0),
            vec![],
            Some(service_message.clone()),
        );
        self.send_message(&self.btp_address.clone(), destination, btp_message.into())
    }

    fn handle_btp_error_message(
        &self,
        source: &BTPAddress,
        message: &BtpMessage<SerializedMessage>,
        error: BmcError,
    ) -> bool {
        true
    }

    fn handle_service_message(
        &mut self,
        source: &BTPAddress,
        message: &BtpMessage<SerializedMessage>,
    ) -> bool {
        if self.btp_address.network_address() == message.destination().network_address() {
            self.increment_link_rx_seq(source);
            let outcome = match message.service().as_str() {
                SERVICE => self.handle_internal_service_message(source, message.clone().try_into()),
                _ => self.handle_external_service_message(source, message),
            };

            if outcome.is_err() {
                match outcome.as_ref().unwrap_err() {
                    BmcError::ServiceNotExist => self.send_error(source, &BtpException::Bmc(BmcError::ServiceNotExist), message.to_owned()),
                    _ => panic!("{}", outcome.unwrap_err())
                }
            }
            false
        } else {
            true
        }
    }

    #[cfg(feature = "testable")]
    pub fn handle_service_message_testable(
        &mut self,
        source: BTPAddress,
        message: BtpMessage<SerializedMessage>,
    ) {
        self.handle_service_message(&source, &message);
    }

    fn handle_internal_service_message(
        &mut self,
        source: &BTPAddress,
        message: Result<BtpMessage<BmcServiceMessage>, BmcError>,
    ) -> Result<(), BmcError> {
        if let Some(service_message) = message?.message() {
            match service_message.service_type() {
                BmcServiceType::Init { links } => self.handle_init(source, &links),
                BmcServiceType::Link { link } => self.handle_link(source, &link),
                BmcServiceType::Unlink { link } => self.handle_unlink(source, &link),
                BmcServiceType::FeeGathering {
                    fee_aggregator,
                    services,
                } => self.handle_fee_gathering(source, &fee_aggregator, &services),
                _ => Err(BmcError::InternalEventHandleNotExists),
            }
        } else {
            unimplemented!()
        }
    }

    fn handle_external_service_message(
        &self,
        source: &BTPAddress,
        message: &BtpMessage<SerializedMessage>,
    ) -> Result<(), BmcError> {
        self.ensure_service_exists(message.service())?;
        let serivce_account_id = self.services.get(message.service()).unwrap();

        if message.serial_no().get().to_owned() >= 0 {
            bsh_contract::handle_btp_message(
                message.to_owned(),
                serivce_account_id.to_owned(),
                estimate::NO_DEPOSIT,
                estimate::SEND_MESSAGE,
            )
            .then(bmc_contract::handle_external_service_message_callback(
                source.to_owned(),
                message.to_owned(),
                env::current_account_id(),
                estimate::NO_DEPOSIT,
                estimate::SEND_MESSAGE,
            ));
        } else {
            // Handle Error
        }
        Ok(())
    }

    fn handle_route_message(
        &mut self,
        source: &BTPAddress,
        message: &BtpMessage<SerializedMessage>,
    ) -> bool {
        self.increment_link_rx_seq(source);
        self.send_message(source, &message.destination(), message.to_owned());
        false
    }

    fn send_error(
        &mut self,
        source: &BTPAddress,
        exception: &dyn Exception,
        message: BtpMessage<SerializedMessage>,
    ) {
        self.send_message(
            source,
            source,
            BtpMessage::new(
                self.btp_address.to_owned(),
                message.source().to_owned(),
                message.service().to_owned(),
                message.serial_no().negate(),
                vec![],
                Some(ErrorMessage::new(exception.code(), exception.message())),
            )
            .into(),
        );
    }
}
