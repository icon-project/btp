use libraries::types::WrappedI128;

use super::*;

#[near_bindgen]
impl BtpMessageCenter {
    // * * * * * * * * * * * * * * * * *
    // * * * * * * * * * * * * * * * * *
    // * * * *    Messaging    * * * * *
    // * * * * * * * * * * * * * * * * *
    // * * * * * * * * * * * * * * * * *

    pub fn handle_relay_message(&self, source: BTPAddress, message: Base64VecU8) {
        unimplemented!()
    }

    #[private]
    // Invoked by BMV as callback
    // ****** Require Strict Vulnerability check here **********
    // Though #[private] will let only if the predecessor is this smart contract account
    // An additional check can be added if the caller is registered BMV if any vulnerability found
    pub fn handle_serialized_btp_messages(
        &mut self,
        source: BTPAddress,
        #[allow(unused_mut)] mut messages: SerializedBtpMessages,
    ) {
        messages
            .retain(|message| self.handle_btp_error_message(&source, message, BmcError::ErrorDrop));
        messages.retain(|message| self.handle_service_message(&source, message));
        messages.retain(|message| self.handle_route_message(&source, message));
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

    pub fn send_service_message(
        &mut self,
        serial_no: i128,
        service: String,
        network: String,
        message: SerializedMessage,
    ) {
        //TODO
        self.assert_sender_is_authorized_service(&service);

        if let Some(destination) = self.resolve_route(&BTPAddress::new(format!(
            "btp://{}/{}",
            network, 0000000000000000
        ))) {
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
    }

    #[private]
    pub fn emit_message(&mut self, link: BTPAddress, btp_message: BtpMessage<SerializedMessage>) {
        if let Some(link_property) = self.links.get(&link).as_mut() {
            link_property.tx_seq_mut().checked_add(1).unwrap();
            self.links.set(&link, &link_property);
            emit_message!(self, event, link_property.tx_seq(), link, btp_message);
        }
    }
}

impl BtpMessageCenter {
    pub fn propogate_internal(&mut self, service_message: BmcServiceMessage) {}

    fn send_internal_service_message(&mut self, service_message: BmcServiceMessage) {}

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
        return if self.btp_address.network_address() == message.destination().network_address() {
            self.increment_link_rx_seq(source);
            let outcome = match message.service().as_str() {
                SERVICE => self.handle_internal_service_message(source, message.clone().try_into()),
                _ => self.handle_external_service_message(source, message),
            };

            if outcome.is_err() {
                panic!("{}", outcome.unwrap_err()); // TODO
            }

            false
        } else {
            true
        };
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
                _ => todo!(),
            }
        } else {
            unimplemented!() // TODO
        }
    }

    fn handle_external_service_message(
        &self,
        source: &BTPAddress,
        message: &BtpMessage<SerializedMessage>,
    ) -> Result<(), BmcError> {
        todo!() // TODO
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
