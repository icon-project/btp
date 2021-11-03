use super::*;

#[near_bindgen]
impl BtpMessageCenter {
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

        // TODO
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
}

impl BtpMessageCenter {
    pub fn increment_link_rx_seq(&mut self, link: &BTPAddress) {
        if let Some(link_property) = self.links.get(link).as_mut() {
            link_property.rx_seq_mut().checked_add(1).unwrap();
            self.links.set(&link, &link_property);
        }
    }
}
