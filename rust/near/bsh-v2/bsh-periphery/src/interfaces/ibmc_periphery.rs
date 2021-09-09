use near_sdk::ext_contract;

#[ext_contract(ibmc_periphery)]
pub trait IbmcPeriphery {
    /**
       @notice Get BMC BTP address
    */
    fn get_bmc_btp_address(&self) -> AccountId;

    /**
       @notice Verify and decode RelayMessage with BMV, and dispatch BTP Messages to registered BSHs
       @dev Caller must be a registered relayer.
       @param prev    BTP Address of the BMC generates the message
       @param msg     base64 encoded string of serialized bytes of Relay Message refer RelayMessage structure
    */
    fn handle_relay_message(&mut self, prev: AccountId, msg: String);

    /**
       @notice Send the message to a specific network.
       @dev Caller must be a registered BSH.
       @param to      Network Address of destination network
       @param svc     Name of the service
       @param sn      Serial number of the message, it should be positive
       @param msg     Serialized bytes of Service Message
    */
    fn send_message(&mut self, to: AccountId, svc: String, sn: u64, msg: Vec<u8>);

    /**
       @notice Get status of BMC.
       @param link    BTP Address of the connected BMC
       @return        The link status
    */
    fn get_status(&self, link: AccountId) -> LinkStats;
}
