//! BTP Message Center

use btp_common::errors::{BmcError, BtpException, Exception};
use libraries::{
    emit_message,
    types::{
        messages::{
            BmcServiceMessage, BmcServiceType, BtpMessage, ErrorMessage, SerializedBtpMessages,
            SerializedMessage,
        },
        Address, BTPAddress, BmcEvent, Bmv, Bsh, Connection, Connections, HashedCollection, Links,
        Owners, Routes,
    },
};

use near_sdk::borsh::{self, BorshDeserialize, BorshSerialize};
use near_sdk::serde_json::{to_value, Value};
use near_sdk::AccountId;
use near_sdk::{
    env,
    json_types::{Base64VecU8, U128, U64},
    log, near_bindgen, require, serde_json, Gas, PanicOnDefault,
};
use std::convert::TryInto;

mod estimate;
mod external;
use external::*;

const INTERNAL_SERVICE: &str = "bmc";

#[near_bindgen]
#[derive(BorshDeserialize, BorshSerialize, PanicOnDefault)]
pub struct BtpMessageCenter {
    btp_address: BTPAddress,
    owners: Owners,
    bsh: Bsh,
    bmv: Bmv,
    links: Links,
    routes: Routes,
    connections: Connections,
    event: BmcEvent,
}

#[near_bindgen]
impl BtpMessageCenter {
    #[init]
    pub fn new(network: String) -> Self {
        let mut owners = Owners::new();
        let bsh = Bsh::new();
        let bmv = Bmv::new();
        let links = Links::new();
        let routes = Routes::new();
        let connections = Connections::new();
        let btp_address =
            BTPAddress::new(format!("btp://{}/{}", network, env::current_account_id()));
        let event = BmcEvent::new();
        owners.add(&env::current_account_id());
        Self {
            btp_address,
            owners,
            bsh,
            bmv,
            links,
            routes,
            connections,
            event,
        }
    }

    // * * * * * * * * * * * * * * * * *
    // * * * * * * * * * * * * * * * * *
    // * * * * Internal Validations  * *
    // * * * * * * * * * * * * * * * * *
    // * * * * * * * * * * * * * * * * *

    /// Check whether signer account id is an owner
    fn assert_have_permission(&self) {
        require!(
            self.owners.contains(&env::signer_account_id()),
            format!("{}", BmcError::PermissionNotExist)
        );
    }

    fn assert_link_exists(&self, link: &BTPAddress) {
        require!(
            self.links.contains(link),
            format!("{}", BmcError::LinkNotExist)
        );
    }

    fn assert_link_does_not_exists(&self, link: &BTPAddress) {
        require!(
            !self.links.contains(link),
            format!("{}", BmcError::LinkExist)
        );
    }

    fn assert_owner_exists(&self, account: &AccountId) {
        require!(
            self.owners.contains(&account),
            format!("{}", BmcError::OwnerNotExist)
        );
    }

    fn assert_owner_does_not_exists(&self, account: &AccountId) {
        require!(
            !self.owners.contains(account),
            format!("{}", BmcError::OwnerExist)
        );
    }

    fn assert_owner_is_not_last_owner(&self) {
        assert!(self.owners.len() > 1, "{}", BmcError::LastOwner);
    }

    fn assert_request_exists(&self, name: &str) {
        require!(
            self.bsh.requests.contains(name),
            format!("{}", BmcError::RequestNotExist)
        );
    }

    fn assert_request_does_not_exists(&self, name: &str) {
        require!(
            !self.bsh.requests.contains(name),
            format!("{}", BmcError::RequestExist)
        );
    }

    fn assert_route_exists(&self, destination: &BTPAddress) {
        require!(
            self.routes.contains(destination),
            format!("{}", BmcError::RouteNotExist)
        );
    }

    fn assert_route_does_not_exists(&self, destination: &BTPAddress) {
        require!(
            !self.routes.contains(destination),
            format!("{}", BmcError::RouteExist)
        );
    }

    fn assert_sender_is_authorized_service(&self, service: &str) {
        require!(
            self.bsh.services.get(service) == Some(&env::signer_account_id()),
            format!("{}", BmcError::PermissionNotExist)
        );
    }

    fn assert_service_exists(&self, name: &str) {
        require!(
            self.bsh.services.contains(name),
            format!("{}", BmcError::ServiceNotExist)
        );
    }

    fn assert_service_does_not_exists(&self, name: &str) {
        require!(
            !self.bsh.services.contains(name),
            format!("{}", BmcError::ServiceExist)
        );
    }

    fn assert_verifier_exists(&self, network: &str) {
        require!(
            self.bmv.contains(network),
            format!("{}", BmcError::VerifierNotExist)
        );
    }

    fn assert_verifier_does_not_exists(&self, network: &str) {
        require!(
            !self.bmv.contains(network),
            format!("{}", BmcError::VerifierExist)
        );
    }

    // * * * * * * * * * * * * * * * * *
    // * * * * * * * * * * * * * * * * *
    // * * * * Interval Services * * * *
    // * * * * * * * * * * * * * * * * *
    // * * * * * * * * * * * * * * * * *

    fn handle_init(
        &mut self,
        source: &BTPAddress,
        links: &Vec<BTPAddress>,
    ) -> Result<(), BmcError> {
        if let Some(mut link) = self.links.get(source) {
            for source_link in links.iter() {
                // Add to Reachable list of the link
                link.reachable_mut().insert(source_link.to_owned());

                // Add to the connections for quickily quering for routing
                self.connections.add(
                    &Connection::LinkReachable(
                        source_link
                            .network_address()
                            .map_err(|error| BmcError::InvalidAddress { description: error })?,
                    ),
                    source,
                )
            }
            self.links.set(source, &link);
            Ok(())
        } else {
            Err(BmcError::LinkNotExist)
        }
    }

    fn handle_link(
        &mut self,
        source: &BTPAddress,
        source_link: &BTPAddress,
    ) -> Result<(), BmcError> {
        if let Some(mut link) = self.links.get(source) {
            if !link.reachable().contains(source_link) {
                link.reachable_mut().insert(source_link.to_owned());

                // Add to the connections for quickily quering for routing
                self.connections.add(
                    &Connection::LinkReachable(
                        source_link
                            .network_address()
                            .map_err(|error| BmcError::InvalidAddress { description: error })?,
                    ),
                    source,
                );
            }

            self.links.set(source, &link);
            Ok(())
        } else {
            Err(BmcError::LinkNotExist)
        }
    }

    fn handle_unlink(
        &mut self,
        source: &BTPAddress,
        source_link: &BTPAddress,
    ) -> Result<(), BmcError> {
        if let Some(mut link) = self.links.get(source) {
            if link.reachable().contains(source_link) {
                link.reachable_mut().remove(source_link);

                // Remove from the connections for quickily quering for routing
                self.connections.remove(
                    &Connection::LinkReachable(
                        source_link
                            .network_address()
                            .map_err(|error| BmcError::InvalidAddress { description: error })?,
                    ),
                    source,
                );
            }

            self.links.set(source, &link);
            Ok(())
        } else {
            Err(BmcError::LinkNotExist)
        }
    }

    fn handle_fee_gathering(
        &self,
        source: &BTPAddress,
        fee_aggregator: &BTPAddress,
        services: &Vec<String>,
    ) -> Result<(), BmcError> {
        if source.network_address() != fee_aggregator.network_address() {
            return Err(BmcError::FeeAggregatorNotAllowed {
                source: source.to_string(),
            });
        }

        services.iter().for_each(|service| {
            //TODO: Handle Services that are not available
            #[allow(unused_variables)]
            if let Some(account_id) = self.bsh.services.get(service) {
                #[cfg(not(feature = "testable"))]
                bsh_contract::gather_fees(
                    fee_aggregator.clone(),
                    service.clone(),
                    account_id.clone(),
                    0,
                    Gas::from(estimate::GATHER_FEE),
                );
            }
        });
        Ok(())
    }

    // * * * * * * * * * * * * * * * * *
    // * * * * * * * * * * * * * * * * *
    // * * * * Owner Management  * * * *
    // * * * * * * * * * * * * * * * * *
    // * * * * * * * * * * * * * * * * *

    /// Add another owner
    /// Caller must be an owner of BTP network
    pub fn add_owner(&mut self, account: AccountId) {
        self.assert_have_permission();
        self.assert_owner_does_not_exists(&account);
        self.owners.add(&account);
    }

    /// Remove an existing owner
    /// Caller must be an owner of BTP network
    pub fn remove_owner(&mut self, account: AccountId) {
        self.assert_have_permission();
        self.assert_owner_exists(&account);
        self.assert_owner_is_not_last_owner();
        self.owners.remove(&account)
    }

    /// Get account ids of registered owners
    /// Caller can be ANY
    pub fn get_owners(&self) -> Vec<AccountId> {
        self.owners.to_vec()
    }

    // * * * * * * * * * * * * * * * * *
    // * * * * * * * * * * * * * * * * *
    // * * * * Service Management  * * *
    // * * * * * * * * * * * * * * * * *
    // * * * * * * * * * * * * * * * * *

    pub fn approve_service(&mut self, name: String, approve: bool) {
        self.assert_have_permission();
        self.assert_service_does_not_exists(&name);
        self.assert_request_exists(&name);
        if let Some(service) = self.bsh.requests.get(&name) {
            if approve {
                self.bsh.services.add(&name, &service);
            }
            self.bsh.requests.remove(&name);
        };
    }

    /// Register the smart contract for the service
    /// Caller must be an operator of BTP network
    pub fn request_service(&mut self, name: String, service: AccountId) {
        self.assert_request_does_not_exists(&name);
        self.assert_service_does_not_exists(&name);
        self.bsh.requests.add(&name, &service);
    }

    /// De-register the service from BSH
    /// Caller must be an operator of BTP network    
    pub fn remove_service(&mut self, name: String) {
        self.assert_have_permission();
        self.assert_service_exists(&name);
        self.bsh.services.remove(&name);
    }

    pub fn get_requests(&self) -> String {
        to_value(self.bsh.requests.to_vec()).unwrap().to_string()
    }

    /// Get registered services
    /// Returns an array of services
    pub fn get_services(&self) -> String {
        to_value(self.bsh.services.to_vec()).unwrap().to_string()
    }

    // * * * * * * * * * * * * * * * * *
    // * * * * * * * * * * * * * * * * *
    // * * * * Verifier Management * * *
    // * * * * * * * * * * * * * * * * *
    // * * * * * * * * * * * * * * * * *

    /// Register BMV for the network
    /// Caller must be an operator of BTP network
    pub fn add_verifier(&mut self, network: String, verifier: AccountId) {
        self.assert_have_permission();
        self.assert_verifier_does_not_exists(&network);
        self.bmv.add(&network, &verifier);
    }

    /// De-register BMV for the network
    /// Caller must be an operator of BTP network
    pub fn remove_verifier(&mut self, network: String) {
        self.assert_have_permission();
        self.assert_verifier_exists(&network);
        self.bmv.remove(&network)
    }

    /// Get registered verifiers
    /// Returns an array of verifiers
    pub fn get_verifiers(&self) -> String {
        to_value(self.bmv.to_vec()).unwrap().to_string()
    }

    // * * * * * * * * * * * * * * * * *
    // * * * * * * * * * * * * * * * * *
    // * * * * Link Management * * * * *
    // * * * * * * * * * * * * * * * * *
    // * * * * * * * * * * * * * * * * *

    pub fn add_link(&mut self, link: BTPAddress) {
        self.assert_have_permission();
        self.assert_verifier_exists(&link.network_address().unwrap());
        self.assert_link_does_not_exists(&link);
        self.links.add(&link);
    }

    pub fn remove_link(&mut self, link: BTPAddress) {
        self.assert_have_permission();
        self.assert_link_exists(&link);
        self.links.remove(&link);
    }

    #[cfg(feature = "testable")]
    pub fn get_reachable_link(&self, link: BTPAddress) -> HashedCollection<BTPAddress> {
        if let Some(link) = self.links.get(&link) {
            return link
                .reachable()
                .to_owned()
                .into_iter()
                .collect::<HashedCollection<BTPAddress>>();
        }
        HashedCollection::new()
    }

    pub fn get_links(&self) -> serde_json::Value {
        self.links.to_vec().into()
    }

    pub fn set_link(
        &mut self,
        link: BTPAddress,
        block_interval: u64,
        max_aggregation: u64,
        delay_limit: u64,
    ) {
        self.assert_have_permission();
        self.assert_link_exists(&link);
        require!(
            max_aggregation >= 1 && delay_limit >= 1,
            format!("{}", BmcError::InvalidParam)
        );
        unimplemented!();
        if let Some(link_property) = self.links.get(&link).as_mut() {
            unimplemented!();
            self.links.set(&link, &link_property);
        }
    }

    pub fn get_status(&self, link: BTPAddress) {
        self.assert_link_exists(&link);
        unimplemented!();
    }

    fn increment_link_rx_seq(&mut self, link: &BTPAddress) {
        if let Some(link_property) = self.links.get(link).as_mut() {
            link_property.rx_seq_mut().checked_add(1).unwrap();
            self.links.set(&link, &link_property);
        }
    }

    // * * * * * * * * * * * * * * * * *
    // * * * * * * * * * * * * * * * * *
    // * * * * Route Management  * * * *
    // * * * * * * * * * * * * * * * * *
    // * * * * * * * * * * * * * * * * *

    pub fn add_route(&mut self, destination: BTPAddress, link: BTPAddress) {
        self.assert_have_permission();
        self.assert_route_does_not_exists(&destination);
        self.assert_link_exists(&link);
        self.routes.add(&destination, &link);
        self.connections.add(
            &Connection::Route(destination.network_address().unwrap()),
            &link,
        );
    }

    pub fn remove_route(&mut self, destination: BTPAddress) {
        self.assert_have_permission();
        self.assert_route_exists(&destination);
        let link = self.routes.get(&destination).unwrap_or_default();
        self.routes.remove(&destination);
        self.connections.remove(
            &Connection::Route(destination.network_address().unwrap()),
            &link,
        )
    }

    pub fn get_routes(&self) -> Value {
        self.routes.to_vec().into()
    }

    fn resolve_route(&self, destination: &BTPAddress) -> Option<BTPAddress> {
        //TODO: Revisit
        // Check if part of links
        if self.links.contains(destination) {
            return Some(destination.clone());
        }

        // Check if part of routes
        if self
            .connections
            .contains(&Connection::Route(destination.network_address().unwrap()))
        {
            return self
                .connections
                .get(&Connection::Route(destination.network_address().unwrap()));
        }

        // Check if part of link reachable
        if self.connections.contains(&Connection::LinkReachable(
            destination.network_address().unwrap(),
        )) {
            return self.connections.get(&Connection::LinkReachable(
                destination.network_address().unwrap(),
            ));
        }
        None
    }

    #[cfg(feature = "testable")]
    pub fn resolve_route_pub(&self, destination: BTPAddress) -> Option<BTPAddress> {
        self.resolve_route(&destination)
    }

    // * * * * * * * * * * * * * * * * *
    // * * * * * * * * * * * * * * * * *
    // * * * * Relay Management  * * * *
    // * * * * * * * * * * * * * * * * *
    // * * * * * * * * * * * * * * * * *

    pub fn add_relays(&mut self, link: BTPAddress, relays: Vec<AccountId>) {
        self.assert_have_permission();
        self.assert_link_exists(&link);
        if let Some(link_property) = self.links.get(&link).as_mut() {
            link_property.relays_mut().set(&relays);
            self.links.set(&link, &link_property);
        }
    }

    pub fn add_relay(&mut self, link: BTPAddress, relay: AccountId) {
        self.assert_have_permission();
        self.assert_link_exists(&link);

        if let Some(link_property) = self.links.get(&link).as_mut() {
            require!(
                !link_property.relays().contains(&relay),
                format!(
                    "{}",
                    BmcError::RelayExist {
                        link: link.to_string()
                    }
                )
            );
            link_property.relays_mut().add(&relay);
            self.links.set(&link, &link_property);
        }
    }

    pub fn remove_relay(&mut self, link: BTPAddress, relay: AccountId) {
        self.assert_have_permission();
        require!(
            self.links.contains(&link),
            format!("{}", BmcError::LinkNotExist)
        );
        if let Some(link_property) = self.links.get(&link).as_mut() {
            require!(
                link_property.relays().contains(&relay),
                format!(
                    "{}",
                    BmcError::RelayNotExist {
                        link: link.to_string()
                    }
                )
            );
            link_property.relays_mut().remove(&relay);
            self.links.set(&link, &link_property);
        }
    }

    pub fn get_relays(&self, link: BTPAddress) -> Value {
        self.assert_link_exists(&link);
        if let Some(link_property) = self.links.get(&link).as_mut() {
            to_value(link_property.relays().to_vec()).unwrap()
        } else {
            to_value(Vec::new() as Vec<String>).unwrap()
        }
    }

    pub fn rotate_relay() {
        unimplemented!();
    }

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
                INTERNAL_SERVICE => {
                    self.handle_internal_service_message(source, message.clone().try_into())
                }
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
        self.send_message(source, &message.destination(),message.to_owned());
        true
    }

    #[cfg(not(feature = "testable"))]
    fn send_message(&mut self, source: &BTPAddress, message: BtpMessage<SerializedMessage>) {
        if let Some(next) = self.resolve_route(message.destination()) {
            bmc_contract::emit_message(
                next,
                message,
                env::current_account_id(),
                0,
                Gas::from(estimate::SEND_MESSAGE),
            );
        } else {
            self.send_error(
                source,
                &BtpException::Bmc(BmcError::Unreachable {
                    destination: message.destination().to_string(),
                }),
                message,
            );
        }
    }

    #[cfg(feature = "testable")]
    pub fn get_message(&self) -> Result<BtpMessage<SerializedMessage>, String> {
        self.event.get_message()
    }

    #[cfg(feature = "testable")]
    pub fn send_message(&mut self, previous: &BTPAddress, destination: &BTPAddress, message: BtpMessage<SerializedMessage>) {
        if let Some(next) = self.resolve_route(destination) {
            self.emit_message(next, message);
        } else {
            self.send_error(
                previous,
                &BtpException::Bmc(BmcError::Unreachable {
                    destination: message.destination().to_string(),
                }),
                message,
            );
        }
    }

    pub fn send_service_message(
        &mut self,
        source: &BTPAddress,
        message: BtpMessage<SerializedMessage>,
    ) {
        self.assert_sender_is_authorized_service(message.service());
        self.send_message(source, &message.destination().to_owned(), message);
    }

    #[private]
    pub fn emit_message(&mut self, link: BTPAddress, btp_message: BtpMessage<SerializedMessage>) {
        if let Some(link_property) = self.links.get(&link).as_mut() {
            link_property.tx_seq_mut().checked_add(1).unwrap();
            self.links.set(&link, &link_property);
            emit_message!(self, event, link_property.tx_seq(), link, btp_message);
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
