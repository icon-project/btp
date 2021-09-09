use near_sdk::ext_contract;

/**
   @title Interface of BSHPeriphery contract
   @dev This contract is used to handle communications among BMCService and BSHCore contract
*/
#[ext_contract(ibsh_periphery)]
pub trait IbshPeriphery: Ibsh {
    /**
       @notice Check whether BSHPeriphery has any pending transferring requests
       @return true or false
    */
    fn has_pending_request(&self) -> bool;

    /**
       @notice Send Service Message from BSHCore contract to BMCService contract
       @dev Caller must be BSHCore only
       @param to             A network address of destination chain
       @param coin_names     A list of coin name that are requested to transfer
       @param values         A list of an amount to receive at destination chain respectively with its coin name
       @param fees           A list of an amount of charging fee respectively with its coin name
    */
    fn send_service_message(
        &mut self,
        from: AccountId,
        to: AccountId,
        coin_names: Vec<String>,
        values: Vec<u128>,
        fees: Vec<u64>,
    );

    /**
       @notice BSH handle BTP Message from BMC contract
       @dev Caller must be BMC contract only
       @param from    An originated network address of a request
       @param svc     A service name of BSHPeriphery contract
       @param sn      A serial number of a service request
       @param msg     An RLP message of a service request/service response
    */
    fn handle_btp_message(&mut self, from: AccountId, svc: String, sn: u64, msg: Vec<u8>);

    /**
       @notice BSH handle BTP Error from BMC contract
       @dev Caller must be BMC contract only
       @param svc     A service name of BSHPeriphery contract
       @param sn      A serial number of a service request
       @param code    A response code of a message (RC_OK / RC_ERR)
       @param msg     A response message
    */
    fn handle_btp_error(&mut self, src: String, svc: String, sn: u64, code: u32, msg: String);

    /**
       @notice BSH handle Gather Fee Message request from BMC contract
       @dev Caller must be BMC contract only
       @param fa     A BTP address of fee aggregator
       @param svc    A name of the service
    */
    fn handle_fee_gathering(&mut self, fa: AccountId, svc: String);
}
