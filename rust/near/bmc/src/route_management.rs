use super::*;

#[near_bindgen]
impl BtpMessageCenter {
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

    #[cfg(feature = "testable")]
    pub fn resolve_route_pub(&self, destination: BTPAddress) -> Option<BTPAddress> {
        self.resolve_route(&destination)
    }
}

impl BtpMessageCenter {
    pub fn resolve_route(&self, destination: &BTPAddress) -> Option<BTPAddress> {
        if self.links.contains(destination) {
            Some(destination.clone())
        } else if self
            .connections
            .contains(&Connection::Link(destination.network_address().unwrap()))
        {
            self.connections
                .get(&Connection::Link(destination.network_address().unwrap()))
        } else if self
            .connections
            .contains(&Connection::Route(destination.network_address().unwrap()))
        {
            self.connections
                .get(&Connection::Route(destination.network_address().unwrap()))
        } else if self.connections.contains(&Connection::LinkReachable(
            destination.network_address().unwrap(),
        )) {
            self.connections.get(&Connection::LinkReachable(
                destination.network_address().unwrap(),
            ))
        } else {
            None
        }
    }
}
