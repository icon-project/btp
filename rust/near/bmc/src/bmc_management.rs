//! BMC Management Contract

use crate::{BmcGeneric, Utils};
use bmv::Bmv;
use btp_common::BTPAddress;
use libraries::bmc_types::*;
use near_sdk::borsh::{self, BorshDeserialize, BorshSerialize};
use near_sdk::collections::UnorderedMap;
use near_sdk::{env, near_bindgen, setup_alloc, AccountId};

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
    pub fn set_bmc_generic(&mut self, addr: AccountId) -> Result<(), &str> {
        if !self
            .owners
            .get(&env::signer_account_id())
            .expect("Error in owner lookup")
        {
            return Err("BMCRevertUnauthorized");
        }
        if addr == env::predecessor_account_id() {
            return Err("BMCRevertInvalidAddress");
        }
        if addr == self.bmc_generic {
            return Err("BMCRevertAlreadyExistsBMCGeneric");
        }
        self.bmc_generic = addr;
        Ok(())
    }

    /// Add another owner
    /// Caller must be an owner of BTP network
    pub fn add_owner(&mut self, owner: AccountId) -> Result<(), &str> {
        if !self
            .owners
            .get(&env::signer_account_id())
            .expect("Error in owner lookup")
        {
            return Err("BMCRevertUnauthorized");
        }
        let _ = self.owners.insert(&owner, &true);
        self.num_of_owners += 1;
        Ok(())
    }

    /// Remove an existing owner
    /// Caller must be an owner of BTP network
    pub fn remove_owner(&mut self, owner: AccountId) -> Result<(), &str> {
        if !self
            .owners
            .get(&env::signer_account_id())
            .expect("Error in owner lookup")
        {
            return Err("BMCRevertUnauthorized");
        }
        if self.num_of_owners <= 1 {
            return Err("BMCRevertLastOwner");
        }
        if !self.owners.get(&owner).expect("Error in owner lookup") {
            return Err("BMCRevertNotExistsPermission");
        }
        let _ = self.owners.remove(&owner);
        Ok(())
    }

    /// Check whether one specific address has owner role
    /// Caller can be ANY
    pub fn is_owner(&self, owner: AccountId) -> bool {
        self.owners.get(&owner).expect("Error in owner lookup")
    }

    /// Register the smart contract for the service
    /// Caller must be an operator of BTP network
    pub fn add_service(&mut self, svc: String, addr: AccountId) -> Result<(), &str> {
        if !self
            .owners
            .get(&env::signer_account_id())
            .expect("Error in owner lookup")
        {
            return Err("BMCRevertUnauthorized");
        }
        if addr == env::predecessor_account_id() {
            return Err("BMCRevertInvalidAddress");
        }
        if self.bsh_services.get(&svc).expect("Error in SVC lookup")
            != env::predecessor_account_id()
        {
            return Err("BMCRevertAlreadyExistsBSH");
        }
        let _ = self.bsh_services.insert(&svc, &addr);
        self.list_of_bsh_names.push(svc);
        Ok(())
    }

    /// De-register the smart contract for the service
    /// Caller must be an operator of BTP network
    pub fn remove_service(&mut self, svc: String) -> Result<(), &str> {
        if !self
            .owners
            .get(&env::signer_account_id())
            .expect("Error in owner lookup")
        {
            return Err("BMCRevertUnauthorized");
        }
        if self.bsh_services.get(&svc).expect("Error in SVC lookup")
            == env::predecessor_account_id()
        {
            return Err("BMCRevertNotExistsBSH");
        }
        let _ = self.bsh_services.remove(&svc);
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
    pub fn add_verifier(&mut self, net: AccountId, addr: AccountId) -> Result<(), &str> {
        if !self
            .owners
            .get(&env::signer_account_id())
            .expect("Error in owner lookup")
        {
            return Err("BMCRevertUnauthorized");
        }
        if self.bmv_services.get(&net).expect("Error in BMV lookup")
            != env::predecessor_account_id()
        {
            return Err("BMCRevertAlreadyExistsBMV");
        }
        let _ = self.bmv_services.insert(&net, &addr);
        self.list_of_bmv_names.push(net);
        Ok(())
    }

    /// De-register BMV for the network
    /// Caller must be an operator of BTP network
    pub fn remove_verifier(&mut self, net: AccountId) -> Result<(), &str> {
        if !self
            .owners
            .get(&env::signer_account_id())
            .expect("Error in owner lookup")
        {
            return Err("BMCRevertUnauthorized");
        }
        if self.bmv_services.get(&net).expect("Error in BMV lookup")
            == env::predecessor_account_id()
        {
            return Err("BMCRevertNotExistsBMV");
        }
        let _ = self.bmv_services.remove(&net);
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
    pub fn add_link(&mut self, link: AccountId) -> Result<(), &str> {
        let net = BTPAddress::new(link.clone())
            .network_address()
            .expect("Failed to retrieve network address");
        if self
            .bmv_services
            .get(&net)
            .expect("Error in address lookup")
            == env::predecessor_account_id()
        {
            return Err("BMCRevertNotExistsBMV");
        }
        if self
            .links
            .get(&link)
            .expect("Error in link lookup")
            .is_connected
        {
            return Err("BMCRevertAlreadyExistsLink");
        }
        let link_struct = Link {
            relays: vec![],
            reachable: vec![],
            rx_seq: 0,
            tx_seq: 0,
            block_interval_src: Self::BLOCK_INTERVAL_MSEC as u128,
            block_interval_dst: 0,
            max_aggregation: 10,
            delay_limit: 3,
            relay_idx: 0,
            rotate_height: 0,
            rx_height: 0,
            rx_height_src: 0,
            is_connected: true,
        };
        let _ = self.links.insert(&link, &link_struct);

        // propagate an event "LINK"
        self.propagate_internal("Link".to_string(), link.clone());

        let links = self.list_of_link_names.clone();
        self.list_of_link_names.push(link.clone());
        let _ = self.get_link_from_net.insert(&net, &link);

        // init link
        self.send_internal(link, "Init".to_string(), links);

        Ok(())
    }

    /// Remove the link and status information
    /// Caller must be an operator of BTP network
    pub fn remove_link(&mut self, link: AccountId) -> Result<(), &str> {
        if !self
            .owners
            .get(&env::signer_account_id())
            .expect("Error in owner lookup")
        {
            return Err("BMCRevertUnauthorized");
        }
        if !self
            .links
            .get(&link)
            .expect("Error in link lookup")
            .is_connected
        {
            return Err("BMCRevertNotExistsLink");
        }
        let _ = self.links.remove(&link);
        let net = BTPAddress::new(link.clone())
            .network_address()
            .expect("Failed to retrieve network address");
        let _ = self.get_link_from_net.remove(&net);
        self.propagate_internal("Unlink".to_string(), link.clone());
        let index = self
            .list_of_link_names
            .iter()
            .position(|x| *x == link)
            .expect("Error in lookup");
        let _ = self.list_of_link_names.remove(index);
        Ok(())
    }

    /// Get registered links
    /// Returns an array of links (BTP addresses of the BMCs)
    pub fn get_links(&self) -> &Vec<String> {
        &self.list_of_link_names
    }

    /// Set the link and status information
    /// Caller must be an operator of BTP network
    pub fn set_link(
        &mut self,
        link: AccountId,
        block_interval: u128,
        max_agg: u128,
        delay_limit: u128,
    ) -> Result<(), &str> {
        if !self
            .owners
            .get(&env::signer_account_id())
            .expect("Error in owner lookup")
        {
            return Err("BMCRevertUnauthorized");
        }
        if !self
            .links
            .get(&link)
            .expect("Error in link lookup")
            .is_connected
        {
            return Err("BMCRevertNotExistsLink");
        }
        if max_agg < 1 && delay_limit < 1 {
            return Err("BMCRevertInvalidParam");
        }
        let mut link_struct = self.links.get(&link).expect("Failed to get Link");
        let mut scale = u128::get_scale(
            link_struct.block_interval_src,
            link_struct.block_interval_dst,
        );
        let mut reset_rotate_height = false;
        let mut rotate_term = u128::get_rotate_term(link_struct.max_aggregation, scale);
        if rotate_term == 0 {
            reset_rotate_height = true;
        }
        link_struct.block_interval_dst = block_interval;
        link_struct.max_aggregation = max_agg;
        link_struct.delay_limit = delay_limit;

        scale = u128::get_scale(link_struct.block_interval_src, block_interval);
        rotate_term = u128::get_rotate_term(max_agg, scale);
        if reset_rotate_height && rotate_term > 0 {
            link_struct.rotate_height = env::block_index() as u128 + rotate_term;
            link_struct.rx_height = env::block_index() as u128;
            let net = BTPAddress::new(link.clone())
                .network_address()
                .expect("Failed to retrieve network address");
            let (height, _, _) = Bmv::new(net).get_status(); // refactor this and all others
            link_struct.rx_height_src = height;
        }
        let _ = self.links.insert(&link, &link_struct);
        Ok(())
    }

    /// Rotate relay for relay address. Only called by BMC generic
    /// Returns relay address
    pub fn rotate_relay(
        &mut self,
        link: AccountId,
        current_height: u128,
        relay_msg_height: u128,
        has_msg: bool,
    ) -> Result<String, &str> {
        if env::current_account_id() != self.bmc_generic {
            return Err("BMCRevertUnauthorized");
        }
        let mut link_struct = self
            .links
            .get(&link.to_string())
            .expect("Failed to get Link");
        let scale = u128::get_scale(
            link_struct.block_interval_src,
            link_struct.block_interval_dst,
        );
        let rotate_term = u128::get_rotate_term(link_struct.max_aggregation, scale);
        let mut base_height: u128 = 0;
        let mut rotate_count: u128 = 0;
        if rotate_term > 0 {
            if has_msg {
                let res = u128::ceil_div(
                    (relay_msg_height - link_struct.rx_height_src) * 10_u128.pow(6),
                    scale,
                );
                let mut guess_height = link_struct.rx_height + res - 1;
                if guess_height > current_height {
                    guess_height = current_height;
                }
                if guess_height < link_struct.rotate_height {
                    rotate_count =
                        u128::ceil_div(link_struct.rotate_height - guess_height, rotate_term) - 1;
                } else {
                    rotate_count =
                        u128::ceil_div(guess_height - link_struct.rotate_height, rotate_term);
                }
                base_height = link_struct.rotate_height + ((rotate_count - 1) * rotate_term);
                let mut skip_count =
                    u128::ceil_div(current_height - guess_height, link_struct.delay_limit);
                if skip_count > 0 {
                    skip_count -= 1;
                    rotate_count += skip_count;
                    base_height = current_height;
                }
                link_struct.rx_height = current_height;
                link_struct.rx_height_src = relay_msg_height;
                let _ = self.links.insert(&link.to_string(), &link_struct);
            } else {
                if current_height < link_struct.rotate_height {
                    rotate_count =
                        u128::ceil_div(link_struct.rotate_height - current_height, rotate_term) - 1;
                } else {
                    rotate_count =
                        u128::ceil_div(current_height - link_struct.rotate_height, rotate_term);
                }
                base_height = link_struct.rotate_height + ((rotate_count - 1) * rotate_term);
            }
            return Ok(self.rotate(link, rotate_term, rotate_count, base_height));
        }

        Ok(env::predecessor_account_id())
    }

    fn rotate(
        &mut self,
        link: AccountId,
        rotate_term: u128,
        rotate_count: u128,
        base_height: u128,
    ) -> String {
        let mut link_struct = self.links.get(&link).expect("Failed to get Link");
        if rotate_term > 0 && rotate_count > 0 {
            link_struct.rotate_height = base_height + rotate_term;
            link_struct.relay_idx += rotate_count;
            if link_struct.relay_idx >= link_struct.relays.len() as u128 {
                link_struct.relay_idx %= link_struct.relays.len() as u128;
            }
            let _ = self.links.insert(&link, &link_struct);
        }
        link_struct.relays[link_struct.relay_idx as usize].clone()
    }

    fn propagate_internal(&mut self, service_type: String, link: AccountId) {
        let rlp_bytes = link.as_bytes().to_vec();
        let bmc_service = BmcService {
            service_type: service_type.to_string(),
            payload: rlp_bytes,
        };
        let encoded_bmc_service = bmc_service
            .try_to_vec()
            .expect("Failed to encode BMC Service");
        let mut bmc_generic = BmcGeneric::default();
        for name in &self.list_of_link_names {
            if self
                .links
                .get(name)
                .expect("Failed to get Link")
                .is_connected
            {
                let net = BTPAddress::new(name.to_string())
                    .network_address()
                    .expect("Failed to retrieve network address");
                bmc_generic
                    .send_message(net, "bmc".to_string(), 0, &encoded_bmc_service)
                    .expect("Failed to send BMC message");
            }
        }
    }

    fn send_internal(&mut self, target: String, service_type: String, links: Vec<AccountId>) {
        let mut rlp_bytes: Vec<u8> = vec![];
        if links.is_empty() {
            rlp_bytes.push(0xc0);
        } else {
            for link in links {
                let code = u8::deserialize(&mut link.as_bytes()).expect("Error in conversion");
                rlp_bytes.push(code);
            }
        }
        let bmc_service = BmcService {
            service_type,
            payload: rlp_bytes,
        };
        let encoded_bmc_service = bmc_service
            .try_to_vec()
            .expect("Failed to encode BMC Service");
        let mut bmc_generic = BmcGeneric::default();
        let net = BTPAddress::new(target)
            .network_address()
            .expect("Failed to retrieve network address");
        bmc_generic
            .send_message(net, "bmc".to_string(), 0, &encoded_bmc_service)
            .expect("Failed to send BMC message");
    }

    /// Add route to the BMC
    /// Caller must be an operator of BTP network
    pub fn add_route(&mut self, dst: AccountId, link: AccountId) -> Result<(), &str> {
        if !self
            .owners
            .get(&env::signer_account_id())
            .expect("Error in owner lookup")
        {
            return Err("BMCRevertUnauthorized");
        }
        if !self
            .routes
            .get(&dst)
            .expect("Error in route lookup")
            .is_empty()
        {
            return Err("BTPRevertAlreadyExistRoute");
        }
        let net = BTPAddress::new(dst.clone())
            .network_address()
            .expect("Failed to retrieve network address");
        let _ = self.routes.insert(&dst, &link);
        self.list_of_route_keys.push(dst.clone());
        let _ = self.get_route_dst_from_net.insert(&net, &dst);
        Ok(())
    }

    /// Remove route to the BMC
    /// Caller must be an operator of BTP network
    pub fn remove_route(&mut self, dst: AccountId) -> Result<(), &str> {
        if !self
            .owners
            .get(&env::signer_account_id())
            .expect("Error in owner lookup")
        {
            return Err("BMCRevertUnauthorized");
        }
        if !self
            .routes
            .get(&dst)
            .expect("Error in route lookup")
            .is_empty()
        {
            return Err("BTPRevertAlreadyExistRoute");
        }
        let _ = self.routes.remove(&dst);
        let net = BTPAddress::new(dst.clone())
            .network_address()
            .expect("Failed to retrieve network address");
        let _ = self.get_route_dst_from_net.remove(&net);
        let index = self
            .list_of_route_keys
            .iter()
            .position(|x| *x == dst)
            .expect("Error in lookup");
        let _ = self.list_of_route_keys.remove(index);
        Ok(())
    }

    /// Get routing information
    /// Returns an array of routes
    pub fn get_routes(&self) -> Vec<Route> {
        let mut routes: Vec<Route> = Vec::with_capacity(self.list_of_route_keys.len());
        for key in &self.list_of_route_keys {
            let route = Route {
                dst: key.clone(),
                next: self.routes.get(key).expect("Error in lookup"),
            };
            routes.push(route);
        }
        routes
    }

    /// Register Relay for the network
    /// Caller must be an operator of BTP network
    pub fn add_relay(&mut self, link: AccountId, addrs: Vec<AccountId>) -> Result<(), &str> {
        if !self
            .owners
            .get(&env::signer_account_id())
            .expect("Error in owner lookup")
        {
            return Err("BMCRevertUnauthorized");
        }
        if !self
            .links
            .get(&link)
            .expect("Error in link lookup")
            .is_connected
        {
            return Err("BMCRevertNotExistsLink");
        }
        self.links.get(&link).expect("Error in link lookup").relays = addrs.clone();
        for addr in addrs {
            let relay_stats = RelayStats {
                addr: addr.clone(),
                block_count: 0,
                msg_count: 0,
            };
            let _ = self.relay_stats.insert(&addr, &relay_stats);
        }
        Ok(())
    }

    /// Unregister Relay for the network
    /// Caller must be an operator of BTP network
    pub fn remove_relay(&mut self, link: AccountId, addr: AccountId) -> Result<(), &str> {
        if !self
            .owners
            .get(&env::signer_account_id())
            .expect("Error in owner lookup")
        {
            return Err("BMCRevertUnauthorized");
        }
        if !self
            .links
            .get(&link)
            .expect("Error in link lookup")
            .is_connected
            && self
                .links
                .get(&link)
                .expect("Error in link lookup")
                .relays
                .is_empty()
        {
            return Err("BMCRevertUnauthorized");
        }
        for relay in self.links.get(&link).expect("Error in link lookup").relays {
            if addr != relay {
                self.addrs.push(relay);
            }
        }
        self.links.get(&link).expect("Error in link lookup").relays = self.addrs.clone();
        self.addrs.clear();
        Ok(())
    }

    /// Get registered relays
    /// Returns a list of relays
    pub fn get_relays(&self, link: AccountId) -> Vec<AccountId> {
        self.links
            .get(&link)
            .expect("Failed to retrieve Link")
            .relays
    }

    /// Get BSH services by name. Only called by BMC generic
    /// Returns BSH service address
    pub fn get_bsh_service_by_name(&self, service_name: String) -> String {
        self.bsh_services
            .get(&service_name)
            .expect("Failed to get service name")
    }

    /// Get BMV services by net. Only called by BMC generic
    /// Returns BMV service address
    pub fn get_bmv_service_by_net(&self, net: AccountId) -> String {
        self.bmv_services
            .get(&net)
            .expect("Failed to retrieve BMV service")
    }

    /// Get link info. Only called by BMC generic
    /// Returns link info
    pub fn get_link(&self, to: AccountId) -> Link {
        self.links.get(&to).expect("Failed to retrieve Link")
    }

    /// Get rotation sequence by link. Only called by BMC generic
    /// Returns rotation sequence
    pub fn get_link_rx_seq(&self, prev: AccountId) -> u128 {
        self.links
            .get(&prev)
            .expect("Failed to retrieve Link")
            .rx_seq
    }

    /// Get transaction sequence by link. Only called by BMC generic
    /// Returns transaction sequence
    pub fn get_link_tx_seq(&self, prev: AccountId) -> u128 {
        self.links
            .get(&prev)
            .expect("Failed to retrieve Link")
            .tx_seq
    }

    /// Get relays by link. Only called by BMC generic
    /// Returns a list of relays' addresses
    pub fn get_link_relays(&self, prev: AccountId) -> Vec<AccountId> {
        self.links
            .get(&prev)
            .expect("Failed to retrieve Link")
            .relays
    }

    /// Get relays status by link. Only called by BMC generic
    /// Returns relay status of all relays
    pub fn get_relay_status_by_link(&self, prev: AccountId) -> Vec<RelayStats> {
        let link = self.links.get(&prev).expect("Failed to retrieve Link");
        let mut relays: Vec<RelayStats> = Vec::with_capacity(link.relays.len());
        for relay in &self
            .links
            .get(&prev)
            .expect("Failed to retrieve Link")
            .relays
        {
            let relay_stat = self
                .relay_stats
                .get(relay)
                .expect("Failed to retrieve RelayStats");
            relays.push(relay_stat);
        }
        relays
    }

    /// Update rotation sequence by link. Only called by BMC generic
    pub fn update_link_rx_seq(&mut self, prev: AccountId, val: u128) -> Result<(), &str> {
        if env::current_account_id() != self.bmc_generic {
            return Err("BMCRevertUnauthorized");
        }
        self.links.get(&prev).expect("Error in lookup").rx_seq += val;
        Ok(())
    }

    /// Increase transaction sequence by 1
    pub fn update_link_tx_seq(&mut self, prev: AccountId) -> Result<(), &str> {
        if env::current_account_id() != self.bmc_generic {
            return Err("BMCRevertUnauthorized");
        }
        self.links.get(&prev).expect("Error in lookup").tx_seq += 1;
        Ok(())
    }

    /// Add a reachable BTP address to link. Only called by BMC generic
    pub fn update_link_reachable(
        &mut self,
        prev: AccountId,
        to: Vec<AccountId>,
    ) -> Result<(), &str> {
        if env::current_account_id() != self.bmc_generic {
            return Err("BMCRevertUnauthorized");
        }
        for link in to {
            let prev = prev.clone();
            self.links
                .get(&prev)
                .expect("Error in lookup")
                .reachable
                .push(link.clone());
            let net = BTPAddress::new(link.clone())
                .network_address()
                .expect("Failed to retrieve network address");
            let tuple = Tuple { prev, to: link };
            let _ = self.get_link_from_reachable_net.insert(&net, &tuple);
        }
        Ok(())
    }

    /// Remove a reachable BTP address. Only called by BMC generic
    pub fn delete_link_reachable(&mut self, prev: AccountId, index: usize) -> Result<(), &str> {
        if env::current_account_id() != self.bmc_generic {
            return Err("BMCRevertUnauthorized");
        }
        let net = BTPAddress::new(
            self.links.get(&prev).expect("Error in lookup").reachable[index].clone(),
        )
        .network_address()
        .expect("Failed to retrieve network address");
        let _ = self.get_link_from_reachable_net.remove(&net);
        let _ = self
            .links
            .remove(&prev)
            .expect("Error in link lookup")
            .reachable[index];
        let idx = self
            .links
            .get(&prev)
            .expect("Failed to get Link")
            .reachable
            .len()
            - 1;
        let link = self.links.get(&prev).expect("Error in lookup").reachable[idx].clone();
        self.links.get(&prev).expect("Error in lookup").reachable[index] = link;
        let _ = self
            .links
            .get(&prev)
            .expect("Error in lookup")
            .reachable
            .pop();
        Ok(())
    }

    /// Update relay status. Only called by BMC generic
    pub fn update_relay_stats(
        &mut self,
        relay: AccountId,
        block_count_val: u128,
        msg_count_val: u128,
    ) -> Result<(), &str> {
        if env::current_account_id() != self.bmc_generic {
            return Err("BMCRevertUnauthorized");
        }
        self.relay_stats
            .get(&relay)
            .expect("Error in lookup")
            .block_count += block_count_val;
        self.relay_stats
            .get(&relay)
            .expect("Error in lookup")
            .msg_count += msg_count_val;
        Ok(())
    }

    /// Resolve next BMC. Only called by BMC generic
    /// Returns BTP address of next BMC and destined BMC
    pub fn resolve_route(&mut self, dst_net: AccountId) -> Result<(String, String), String> {
        if env::current_account_id() != self.bmc_generic {
            return Err("BMCRevertUnauthorized".to_string());
        }
        // search in routes
        let mut dst = self
            .get_route_dst_from_net
            .get(&dst_net)
            .expect("Error retrieving route");
        if !dst.as_bytes().is_empty() {
            let route = self.routes.get(&dst).expect("Failed to get route");
            return Ok((route, dst));
        }

        // search in links
        dst = self
            .get_link_from_net
            .get(&dst_net)
            .expect("Error in lookup");
        if !dst.as_bytes().is_empty() {
            return Ok((dst.clone(), dst));
        }

        // search link by reachable net
        let res = self
            .get_link_from_reachable_net
            .get(&dst_net)
            .expect("Error in link lookup");
        if !res.to.as_bytes().is_empty() {
            let error = format!("BMCRevertUnreachable: {} is unreachable", dst_net);
            return Err(error);
        }
        Ok((res.prev, res.to))
    }
}
