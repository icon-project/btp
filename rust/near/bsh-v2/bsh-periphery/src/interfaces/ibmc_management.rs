use near_sdk::ext_contract;

#[ext_contract(ibmc_management)]
pub trait IbmcManagement {
    /**
      @notice Update BMC periphery.
      @dev Caller must be an Onwer of BTP network
      @param addr    Address of a new periphery.
    */
    fn set_bmc_periphery(&mut self, addr: AccountId);

    /**
      @notice Adding another Onwer.
      @dev Caller must be an Onwer of BTP network
      @param owner    Address of a new Onwer.
    */
    fn add_owner(&mut self, owner: AccountId);

    /**
      @notice Removing an existing Owner.
      @dev Caller must be an Owner of BTP network
      @dev If only one Owner left, unable to remove the last Owner
      @param owner    Address of an Owner to be removed.
    */
    fn remove_owner(&mut self, owner: AccountId);

    /**
      @notice Checking whether one specific address has Owner role.
      @dev Caller can be ANY
      @param owner    Address needs to verify.
    */
    fn is_owner(&self, owner: AccountId) -> bool;

    /**
      @notice Add the smart contract for the service.
      @dev Caller must be an operator of BTP network.
      @param svc     Name of the service
      @param addr    Service's contract address
    */
    fn add_service(&mut self, svc: String, addr: AccountId);

    /**
      @notice De-registers the smart contract for the service.
      @dev Caller must be an operator of BTP network.
      @param svc     Name of the service
    */
    fn remove_service(&mut self, svc: String);

    /**
      @notice Get registered services.
      @return services   An array of Service.
    */
    fn get_services(&self) -> Vec<Service>;

    /**
      @notice Registers BMV for the network.
      @dev Caller must be an operator of BTP network.
      @param net     Network Address of the blockchain
      @param addr    Address of BMV
    */
    fn add_verifier(&mut self, net: AccountId, addr: AccountId);

    /**
      @notice De-registers BMV for the network.
      @dev Caller must be an operator of BTP network.
      @param net     Network Address of the blockchain
    */
    fn remove_verifier(&mut self, net: AccountId);

    /**
      @notice Get registered verifiers.
      @return verifiers   An array of Verifier.
    */
    fn get_verifiers(&self) -> Vec<Verifier>;

    /**
      @notice Initializes status information for the link.
      @dev Caller must be an operator of BTP network.
      @param link    BTP Address of connected BMC
    */
    fn add_link(&mut self, link: AccountId);

    /**
      @notice Removes the link and status information.
      @dev Caller must be an operator of BTP network.
      @param link    BTP Address of connected BMC
    */
    fn remove_link(&mut self, link: AccountId);

    /**
      @notice Get registered links.
      @return   An array of links ( BTP Addresses of the BMCs ).
    */
    fn get_links(&self) -> &Vec<String>;

    /**
      @notice Set the link and status information.
      @dev Caller must be an operator of BTP network.
      @param link    BTP Address of connected BMC
      @param block_interval    Block interval of a connected link
      @param max_aggregation   Set max aggreation of a connected link
      @param delay_limit       Set delay limit of a connected link
    */
    fn set_link(&mut self, link: AccountId, block_interval: u128, max_agg: u128, delay_limit: u128);

    /**
       @notice rotate relay for relay address. Only called by BMC periphery.
       @param link               BTP network address of connected BMC
       @param current_height     current block height of MTA from BMV
       @param relay_msg_height   block height of last relayed BTP Message
       @param hasMsg             check if message exists
       @return                   relay address
    */
    fn rotate_relay(
        &mut self,
        link: AccountId,
        current_height: u128,
        relay_msg_height: u128,
        has_msg: bool,
    ) -> String;

    /**
      @notice Add route to the BMC.
      @dev Caller must be an operator of BTP network.
      @param dst     BTP Address of the destination BMC
      @param link    BTP Address of the next BMC for the destination
    */
    fn add_route(&mut self, dst: AccountId, link: AccountId);

    /**
      @notice Remove route to the BMC.
      @dev Caller must be an operator of BTP network.
      @param dst     BTP Address of the destination BMC
    */
    fn remove_route(&mut self, dst: AccountId);

    /**
      @notice Get routing information.
      @return An array of Route.
    */
    fn get_routes(&self) -> Vec<Route>;

    /**
      @notice Registers relay for the network.
      @dev Caller must be an operator of BTP network.
      @param link      BTP Address of connected BMC
      @param addrs     A list of Relays
    */
    fn add_relay(&mut self, link: AccountId, addrs: Vec<AccountId>);

    /**
      @notice Unregisters Relay for the network.
      @dev Caller must be an operator of BTP network.
      @param link      BTP Address of connected BMC
      @param addrs     A list of Relays
    */
    fn remove_relay(&mut self, link: AccountId, addr: AccountId);

    /**
      @notice Get registered relays.
      @param link        BTP Address of the connected BMC.
      @return            A list of relays.
    */
    fn get_relays(&self, link: AccountId) -> Vec<AccountId>;

    /**
       @notice Get BSH services by name. Only called by BMC periphery.
       @param service_name  BSH service name
       @return              BSH service address
    */
    fn get_bsh_service_by_name(&self, service_name: String) -> String;

    /**
       @notice Get BMV services by net. Only called by BMC periphery.
       @param net       net of the connected network
       @return          BMV service address
    */
    fn get_bmv_service_by_net(&self, net: AccountId) -> String;

    /**
       @notice Get link info. Only called by BMC periphery.
       @param to     link's BTP address
       @return       Link info
    */
    fn get_link(&self, to: AccountId) -> Link;

    /**
       @notice Get rotation sequence by link. Only called by BMC periphery.
       @param prev     BTP Address of the previous BMC
       @return         Rotation sequence
    */
    fn get_link_rx_seq(&self, prev: AccountId) -> u128;

    /**
       @notice Get transaction sequence by link. Only called by BMC periphery.
       @param prev    BTP Address of the previous BMC
       @return        Transaction sequence
    */
    fn get_link_tx_seq(&self, prev: AccountId) -> u128;

    /**
       @notice Get relays by link. Only called by BMC periphery.
       @param prev    BTP Address of the previous BMC
       @return        List of relays' addresses
    */
    fn get_link_relays(&self, prev: AccountId) -> Vec<AccountId>;

    /**
       @notice Get relays status by link. Only called by BMC periphery.
       @param prev    BTP Address of the previous BMC
       @return        Relay status of all relays
    */
    fn get_relay_status_by_link(&self, prev: AccountId) -> Vec<RelayStats>;

    /**
       @notice Update rotation sequence by link. Only called by BMC periphery.
       @param prev    BTP Address of the previous BMC
       @param val     increment value
    */
    fn update_link_rx_seq(&mut self, prev: AccountId, val: u128);

    /**
       @notice Increase transaction sequence by 1.
       @param prev    BTP Address of the previous BMC
    */
    fn update_link_tx_seq(&mut self, prev: AccountId);

    /**
       @notice Add a reachable BTP address to link. Only called by BMC periphery.
       @param prev   BTP Address of the previous BMC
       @param to     BTP Address of the reachable
    */
    fn update_link_reachable(&mut self, prev: AccountId, to: Vec<AccountId>);

    /**
       @notice Remove a reachable BTP address. Only called by BMC periphery.
       @param index   reachable index to remove
    */
    fn delete_link_reachable(&mut self, prev: AccountId, index: usize);

    /**
       @notice Update relay status. Only called by BMC periphery.
       @param relay                relay address
       @param block_count_val      increment value for block counter
       @param msg_count_val        increment value for message counter
    */
    fn update_relay_stats(
        &mut self,
        relay: AccountId,
        block_count_val: u128,
        msg_count_val: u128,
    ) -> Result<(), &str>;

    /**
       @notice resolve next BMC. Only called by BMC periphery.
       @param dst_net     net of BTP network address
       @return            BTP address of next BMC and destinated BMC
    */
    fn resolve_route(&mut self, dst_net: AccountId) -> (String, String);
}
