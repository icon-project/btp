use near_sdk::ext_contract;

#[ext_contract]
pub trait Ibsh {
    /**
       @notice BSH handle BTP Message from BMC contract
       @dev Caller must be BMC contract only
       @param from    An originated network address of a request
       @param svc     A service name of BSH contract
       @param sn      A serial number of a service request
       @param msg     An RLP message of a service request/service response
    */
    fn handle_btp_message(&mut self, from: AccountId, svc: String, sn: u128, msg: Vec<u8>);

    /**
       @notice BSH handle BTP Error from BMC contract
       @dev Caller must be BMC contract only
       @param svc     A service name of BSH contract
       @param sn      A serial number of a service request
       @param code    A response code of a message (RC_OK / RC_ERR)
       @param msg     A response message
    */
    fn handle_btp_error(&mut self, src: String, svc: String, sn: u128, code: u32, msg: String);

    /**
       @notice BSH handle Gather Fee Message request from BMC contract
       @dev Caller must be BMC contract only
       @param fa     A BTP address of fee aggregator
       @param svc    A name of the service
    */
    fn handle_fee_gathering(&mut self, fa: AccountId, svc: String);
}
