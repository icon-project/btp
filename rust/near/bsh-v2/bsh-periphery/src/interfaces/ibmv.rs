use near_sdk::ext_contract;

#[ext_contract(ibmv)]
pub trait Ibmv {
    /**
       @return        Base64 encode of Merkle Tree
    */
    fn get_mta(&self) -> String;

    /**
       @return        Connected BMC address
    */
    fn get_connected_bmc(&self) -> AccountId;

    /**
       @return        Network address of the blockchain
    */
    fn get_net_address(&self) -> AccountId;

    /**
       @return        Hash of RLP encode from given list of validators
       @return        List of validators' addresses
    */
    fn get_validators(&self) -> (CryptoHash, Vec<AccountId>);

    /**
       @notice Used by the relay to resolve next BTP Message to send.
               Called by BMC.
       @return        Height of MerkleTreeAccumulator
       @return        Offset of MerkleTreeAccumulator
       @return        Block height of last relayed BTP Message
    */
    fn get_status(&self) -> (u128, usize, u128);

    /**
       @notice Decodes Relay Messages and process BTP Messages.
               If there is an error, then it sends a BTP Message containing the Error Message.
               BTP Messages with old sequence numbers are ignored. A BTP Message contains future sequence number will fail.
       @param bmc     BTP Address of the BMC handling the message
       @param prev    BTP Address of the previous BMC
       @param seq     Next sequence number to get a message
       @param msg     Serialized bytes of Relay Message
       @return        List of serialized bytes of a BTP Message
    */
    fn handle_relay_message(
        &mut self,
        bmc: AccountId,
        prev: AccountId,
        seq: u128,
        msg: String,
    ) -> Vec<Vec<u8>>;
}
