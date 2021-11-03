use super::*;

impl BtpMessageCenter {
    // * * * * * * * * * * * * * * * * *
    // * * * * * * * * * * * * * * * * *
    // * * * * Interval Services * * * *
    // * * * * * * * * * * * * * * * * *
    // * * * * * * * * * * * * * * * * *

    pub fn handle_init(
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

    pub fn handle_link(
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

    pub fn handle_unlink(
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

    pub fn handle_fee_gathering(
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
                bsh_contract::handle_fee_gathering(
                    fee_aggregator.clone(),
                    service.clone(),
                    account_id.clone(),
                    estimate::NO_DEPOSIT,
                    estimate::GATHER_FEE,
                );
            }
        });
        Ok(())
    }
}
