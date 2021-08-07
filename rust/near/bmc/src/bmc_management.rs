//! BMC Management Contract

use crate::bmc_types::*;
use near_sdk::borsh::{self, BorshDeserialize, BorshSerialize};
use near_sdk::collections::UnorderedMap;
use near_sdk::{env, near_bindgen, setup_alloc};

setup_alloc!();
#[near_bindgen]
#[derive(BorshDeserialize, BorshSerialize)]
pub struct BmcManagement {
    owners: UnorderedMap<String, bool>,
    num_of_owners: u64,
    bsh_services: UnorderedMap<String, String>,
    bmv_services: UnorderedMap<String, String>,
    relay_stats: UnorderedMap<String, RelayStats>,
    routes: UnorderedMap<String, String>,
    links: UnorderedMap<String, Link>,
    list_of_bmv_names: Vec<String>,
    list_of_bsh_names: Vec<String>,
    list_of_route_keys: Vec<String>,
    list_of_link_names: Vec<String>,
    bmc_generic: String,
    pub serial_no: u64,
    addrs: Vec<String>,
    get_route_dst_from_net: UnorderedMap<String, String>,
    get_link_from_net: UnorderedMap<String, String>,
    get_link_from_reachable_net: UnorderedMap<String, Tuple>,
}

impl Default for BmcManagement {
    fn default() -> Self {
        let mut owners: UnorderedMap<String, bool> =
            UnorderedMap::new(BmcStorageKey::BmcManagement);
        let _ = owners.insert(&env::current_account_id(), &true);
        let num_of_owners: u64 = 1;
        Self {
            owners,
            num_of_owners,
            bsh_services: UnorderedMap::new(BmcStorageKey::BmcManagement),
            bmv_services: UnorderedMap::new(BmcStorageKey::BmcManagement),
            relay_stats: UnorderedMap::new(BmcStorageKey::BmcManagement),
            routes: UnorderedMap::new(BmcStorageKey::BmcManagement),
            links: UnorderedMap::new(BmcStorageKey::BmcManagement),
            list_of_bmv_names: vec![],
            list_of_bsh_names: vec![],
            list_of_route_keys: vec![],
            list_of_link_names: vec![],
            bmc_generic: "".to_string(),
            serial_no: 0,
            addrs: vec![],
            get_route_dst_from_net: UnorderedMap::new(BmcStorageKey::BmcManagement),
            get_link_from_net: UnorderedMap::new(BmcStorageKey::BmcManagement),
            get_link_from_reachable_net: UnorderedMap::new(BmcStorageKey::BmcManagement),
        }
    }
}

#[near_bindgen]
impl BmcManagement {
    pub const BLOCK_INTERVAL_MSEC: u32 = 1000;

    /// Update BMC generic
    /// Caller must be an owner of BTP network
    pub fn set_bmc_generic(&mut self, addr: &str) -> Result<(), &str> {
        if !self
            .owners
            .get(&env::current_account_id())
            .expect("Error in owner lookup")
        {
            return Err("BMCRevertUnauthorized");
        }
        if *addr == env::predecessor_account_id() {
            return Err("BMCRevertInvalidAddress");
        }
        if *addr == self.bmc_generic {
            return Err("BMCRevertAlreadyExistsBMCGeneric");
        }
        self.bmc_generic = addr.to_string();
        Ok(())
    }

    /// Add another owner
    /// Caller must be an owner of BTP network
    pub fn add_owner(&mut self, owner: &str) -> Result<(), &str> {
        if !self
            .owners
            .get(&env::current_account_id())
            .expect("Error in owner lookup")
        {
            return Err("BMCRevertUnauthorized");
        }
        let _ = self.owners.insert(&owner.to_string(), &true);
        self.num_of_owners += 1;
        Ok(())
    }

    /// Remove an existing owner
    /// Caller must be an owner of BTP network
    pub fn remove_owner(&mut self, owner: &str) -> Result<(), &str> {
        if !self
            .owners
            .get(&env::current_account_id())
            .expect("Error in owner lookup")
        {
            return Err("BMCRevertUnauthorized");
        }
        if self.num_of_owners <= 1 {
            return Err("BMCRevertLastOwner");
        }
        if !self
            .owners
            .get(&owner.to_string())
            .expect("Error in owner lookup")
        {
            return Err("BMCRevertNotExistsPermission");
        }
        let _ = self.owners.remove(&owner.to_string());
        Ok(())
    }

    /// Check whether one specific address has owner role
    /// Caller can be ANY
    pub fn is_owner(&self, owner: &str) -> bool {
        self.owners
            .get(&owner.to_string())
            .expect("Error in owner lookup")
    }

    /// Register the smart contract for the service
    /// Caller must be an operator of BTP network
    pub fn add_service(&mut self, svc: &str, addr: &str) -> Result<(), &str> {
        if !self
            .owners
            .get(&env::current_account_id())
            .expect("Error in owner lookup")
        {
            return Err("BMCRevertUnauthorized");
        }
        if *addr == env::predecessor_account_id() {
            return Err("BMCRevertInvalidAddress");
        }
        if self
            .bsh_services
            .get(&svc.to_string())
            .expect("Error in SVC lookup")
            != env::predecessor_account_id()
        {
            return Err("BMCRevertAlreadyExistsBSH");
        }
        let _ = self
            .bsh_services
            .insert(&svc.to_string(), &addr.to_string());
        self.list_of_bsh_names.push(svc.to_string());
        Ok(())
    }

    /// De-register the smart contract for the service
    /// Caller must be an operator of BTP network
    pub fn remove_service(&mut self, svc: &str) -> Result<(), &str> {
        if !self
            .owners
            .get(&env::current_account_id())
            .expect("Error in owner lookup")
        {
            return Err("BMCRevertUnauthorized");
        }
        if self
            .bsh_services
            .get(&svc.to_string())
            .expect("Error in SVC lookup")
            == env::predecessor_account_id()
        {
            return Err("BMCRevertNotExistsBSH");
        }
        let _ = self.bsh_services.remove(&svc.to_string());
        let index = self
            .list_of_bsh_names
            .iter()
            .position(|x| *x == svc)
            .expect("Error in lookup");
        let _ = self.list_of_bsh_names.remove(index);
        Ok(())
    }

    /// Get registered services
    /// Returns an array of services
    pub fn get_services(&self) -> Vec<Service> {
        let mut services: Vec<Service> = Vec::with_capacity(self.list_of_bsh_names.len());
        for bsh_name in &self.list_of_bsh_names {
            let service = Service {
                svc: bsh_name.clone(),
                addr: self
                    .bsh_services
                    .get(bsh_name)
                    .expect("Error in name lookup"),
            };
            services.push(service);
        }
        services
    }

    /// Register BMV for the network
    /// Caller must be an operator of BTP network
    pub fn add_verifier(&mut self, net: &str, addr: &str) -> Result<(), &str> {
        if !self
            .owners
            .get(&env::current_account_id())
            .expect("Error in owner lookup")
        {
            return Err("BMCRevertUnauthorized");
        }
        if self
            .bmv_services
            .get(&net.to_string())
            .expect("Error in BMV lookup")
            != env::predecessor_account_id()
        {
            return Err("BMCRevertAlreadyExistsBMV");
        }
        let _ = self
            .bmv_services
            .insert(&net.to_string(), &addr.to_string());
        self.list_of_bmv_names.push(net.to_string());
        Ok(())
    }

    /// De-register BMV for the network
    /// Caller must be an operator of BTP network
    pub fn remove_verifier(&mut self, net: &str) -> Result<(), &str> {
        if !self
            .owners
            .get(&env::current_account_id())
            .expect("Error in owner lookup")
        {
            return Err("BMCRevertUnauthorized");
        }
        if self
            .bmv_services
            .get(&net.to_string())
            .expect("Error in BMV lookup")
            == env::predecessor_account_id()
        {
            return Err("BMCRevertNotExistsBMV");
        }
        let _ = self.bmv_services.remove(&net.to_string());
        let index = self
            .list_of_bmv_names
            .iter()
            .position(|x| *x == net)
            .expect("Error in lookup");
        let _ = self.list_of_bmv_names.remove(index);
        Ok(())
    }

    /// Get registered verifiers
    /// Returns an array of verifiers
    pub fn get_verifiers(&self) -> Vec<Verifier> {
        let mut verifiers: Vec<Verifier> = Vec::with_capacity(self.list_of_bmv_names.len());
        for bmv_name in &self.list_of_bmv_names {
            let verifier = Verifier {
                net: bmv_name.clone(),
                addr: self
                    .bmv_services
                    .get(bmv_name)
                    .expect("Error in name lookup"),
            };
            verifiers.push(verifier);
        }
        verifiers
    }

    /// Initialize status information for the link
    /// Caller must be an operator of BTP network
    pub fn add_link(&mut self, _link: &str) -> Result<(), &str> {
        todo!()
    }

    /// Set the link and status information
    /// Caller must be an operator of BTP network
    pub fn set_link(
        &mut self,
        _link: &str,
        _block_interval: u128,
        _max_agg: u128,
        _delay_limit: u128,
    ) {
        todo!()
    }

    /// Remove the link and status information
    /// Caller must be an operator of BTP network
    pub fn remove_link(&mut self, _link: &str) {
        todo!()
    }

    /// Add route to the BMC
    /// Caller must be an operator of BTP network
    pub fn add_route(&mut self, _dst: &str, _link: &str) {
        todo!()
    }

    /// Remove route to the BMC
    /// Caller must be an operator of BTP network
    pub fn remove_route(&mut self, _dst: &str) {
        todo!()
    }

    /// Register Relay for the network
    /// Caller must be an operator of BTP network
    pub fn add_relay(&mut self, _link: &str, _addrs: &[&str]) {
        todo!()
    }

    /// Unregister Relay for the network
    /// Caller must be an operator of BTP network
    pub fn remove_relay(&mut self, _link: &str, _addrs: &[&str]) {
        todo!()
    }

    /// Get registered links
    /// Returns an array of links (BTP addresses of the BMCs)
    pub fn get_links(&self) -> Vec<String> {
        todo!()
    }

    /// Get routing information
    /// Returns an array of routes
    pub fn get_routes(&self) -> Vec<Route> {
        todo!()
    }

    /// Get registered relays
    /// Returns a list of relays
    pub fn get_relays(&self, _link: &str) -> Vec<String> {
        todo!()
    }

    /// Get BSH services by name. Only called by BMC generic
    /// Returns BSH service address
    pub fn get_bsh_service_by_name(&self, _service_name: &str) -> String {
        todo!()
    }

    /// Get BMV services by net. Only called by BMC generic
    /// Returns BMV service address
    pub fn get_bmv_service_by_net(&self, _net: &str) -> String {
        todo!()
    }

    /// Get link info. Only called by BMC generic
    /// Returns link info
    pub fn get_link(&self, _to: &str) -> Link {
        todo!()
    }

    /// Get rotation sequence by link. Only called by BMC generic
    /// Returns rotation sequence
    pub fn get_link_rx_seq(&self, _prev: &str) -> u128 {
        todo!()
    }

    /// Get transaction sequence by link. Only called by BMC generic
    /// Returns transaction sequence
    pub fn get_link_tx_seq(&self, _prev: &str) -> u128 {
        todo!()
    }

    /// Get relays by link. Only called by BMC generic
    /// Returns a list of relays' addresses
    pub fn get_link_relays(&self, _prev: &str) -> Vec<String> {
        todo!()
    }

    /// Get relays status by link. Only called by BMC generic
    /// Returns relay status of all relays
    pub fn get_relay_status_by_link(&self, _prev: &str) -> Vec<RelayStats> {
        todo!()
    }

    /// Update rotation sequence by link. Only called by BMC generic
    pub fn update_link_rx_seq(&mut self, _prev: &str, _val: u128) {
        todo!()
    }

    /// Increase transaction sequence by 1
    pub fn update_link_tx_seq(&mut self, _prev: &str) {
        todo!()
    }

    /// Add a reachable BTP address to link. Only called by BMC generic
    pub fn update_link_reachable(&mut self, _prev: &str, _to: &[&str]) {
        todo!()
    }

    /// Remove a reachable BTP address. Only called by BMC generic
    pub fn delete_link_reachable(&mut self, _prev: &str, _index: u128) {
        todo!()
    }

    /// Update relay status. Only called by BMC generic
    pub fn update_relay_stats(
        &mut self,
        _relay: &str,
        _block_count_val: u128,
        _msg_count_val: u128,
    ) {
        todo!()
    }

    /// Resolve next BMC. Only called by BMC generic
    /// Returns BTP address of next BMC and destined BMC
    pub fn resolve_route(&mut self, _dst_net: &str) -> Result<(String, String), &str> {
        todo!()
    }

    /// Rotate relay for relay address. Only called by BMC generic
    /// Returns relay address
    pub fn rotate_relay(
        &mut self,
        _link: &str,
        _current_height: u128,
        _relay_msg_height: u128,
        _has_msg: bool,
    ) -> String {
        todo!()
    }
}
