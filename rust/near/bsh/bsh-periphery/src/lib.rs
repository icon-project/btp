//! BTP Service Handler (BSH) Contract

use libraries::types::{BTPAddress, Address};
use libraries::bsh_types::*;
use near_sdk::borsh::{self, BorshDeserialize, BorshSerialize};
use near_sdk::collections::UnorderedMap;
use near_sdk::ext_contract;
use near_sdk::{env, near_bindgen, setup_alloc, AccountId};

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

/**
  @title Interface of BSHCore contract
  @dev This contract is used to handle coin transferring service
  Note: The coin of following interface can be:
  Native Coin : The native coin of this chain
  Wrapped Native Coin : A tokenized ERC1155 version of another native coin like ICX
*/
#[ext_contract]
pub trait IbshCore {
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
       @notice Get a list of current Owners
       @dev Caller can be ANY
       @return      An array of addresses of current Owners
    */
    fn get_owners(&self) -> Option<Vec<AccountId>>;

    /**
       @notice update BSH Periphery address.
       @dev Caller must be an Owner of this contract
       bshPeriphery Must be different with the existing one.
       @param bsh_periphery    BSHPeriphery contract address.
    */
    fn update_bsh_periphery(&mut self, bsh_periphery: AccountId);

    /**
       @notice update base uri.
       @dev Caller must be an Owner of this contract
       the uri must be initilized in construction.
       @param new_uri    new uri
    */
    fn update_uri(&mut self, new_uri: AccountId);

    /**
       @notice set fee ratio.
       @dev Caller must be an Owner of this contract
       The transfer fee is calculated by fee_numerator/FEE_DEMONINATOR.
       The fee_numetator should be less than FEE_DEMONINATOR
       fee_numerator is set to `10` in construction by default, which means the default fee ratio is 0.1%.
       @param fee_numerator    the fee numerator
    */
    fn set_fee_ratio(&mut self, fee_numerator: u64);

    /**
       @notice set Fixed Fee.
       @dev Caller must be an Owner
       @param fixed_fee    A new value of Fixed Fee
    */
    fn set_fixed_fee(&mut self, fixed_fee: u64);

    /**
       @notice Registers a wrapped coin and id number of a supporting coin.
       @dev Caller must be an Owner of this contract
       name Must be different with the native coin name.
       @dev 'id' of a wrapped coin is generated by using keccak256
         'id' = 0 is fixed to assign to native coin
       @param name    Coin name.
    */
    fn register(&mut self, name: String);

    /**
       @notice Return all supported coins names
       @dev
       @return   An array of strings.
    */
    fn get_coin_names(&self) -> Option<Vec<String>>;

    /**
       @notice  Return an id number of Coin whose name is the same with given coin_name.
       @dev     Return None if not found.
       @return  An ID number of coin_name.
    */
    fn get_coin_id(&self, coin_name: String) -> Option<u64>;

    /**
       @notice  Check Validity of a coin_name
       @dev     Call by BSHPeriphery contract to validate a requested coin_name
       @return  true of false
    */
    fn is_valid_coin(&self, coin_name: String) -> bool;

    /**
       @notice  Return a usable/locked/refundable balance of an account based on coin_name.
       @return  Usable balance, the balance that users are holding.
       @return  Locked balance, when users transfer the coin,
               it will be locked until getting the Service Message Response.
       @return  Refundable balance, the balance that will be refunded to users.
    */
    fn get_balance_of(&self, owner: AccountId, coin_name: String) -> (u128, u128, u128);

    /**
       @notice Return a list Balance of an account.
       @dev The order of request's coin_names must be the same with the order of return balance
       Return 0 if not found.
       @return  An array of Usable Balances
       @return  An array of Locked Balances
       @return  An array of Refundable Balances
    */
    fn get_balance_of_batch(
        &self,
        owner: AccountId,
        coin_names: Vec<String>,
    ) -> (Vec<u128>, Vec<u128>, Vec<u128>);

    /**
       @notice Return a list accumulated Fees.
       @dev Only return the asset that has Asset's value greater than 0
       @return  An array of Asset
    */
    fn get_accumulated_fees(&self) -> Vec<Asset>;

    /**
       @notice Allow users to deposit `value` native coin into a BSHCore contract.
       @dev MUST specify value
       @param to  An address that a user expects to receive an amount of tokens.
    */
    #[payable]
    fn transfer_native_coin(&mut self, to: AccountId);

    /**
       @notice Allow users to deposit an amount of wrapped native coin `coin_name` from the `env::signer_account_id` address into the BSHCore contract.
       @dev Caller must set to approve that the wrapped tokens can be transferred out of the `env::signer_account_id` account by BSHCore contract.
       It MUST revert if the balance of the holder for token `coin_name` is lower than the `value` sent.
       @param coin_name    A given name of a wrapped coin
       @param value        An amount request to transfer.
       @param to           Target BTP address.
    */
    fn transfer(&mut self, coin_name: String, value: u128, to: AccountId);

    /**
       @notice Allow users to transfer multiple coins/wrapped coins to another chain
       @dev Caller must set to approve that the wrapped tokens can be transferred out of the `env::signer_account_id` account by BSHCore contract.
       It MUST revert if the balance of the holder for token `coin_name` is lower than the `value` sent.
       In case of transferring a native coin, it also checks `value` with `values[i]`
       It MUST revert if `value` is not equal to `values[i]`
       The number of requested coins MUST be as the same as the number of requested values
       The requested coins and values MUST be matched respectively
       @param coin_names   A list of requested transferring coins/wrapped coins
       @param values       A list of requested transferring values respectively with its coin name
       @param to           Target BTP address.
    */
    #[payable]
    fn transfer_batch(&mut self, coin_names: Vec<String>, values: Vec<u128>, to: AccountId);

    /**
        @notice Reclaim the token's refundable balance by an owner.
        @dev Caller must be an owner of coin
        The amount to claim must be smaller or equal than refundable balance
        @param coin_name   A given name of coin
        @param value       An amount of re-claiming tokens
    */
    fn reclaim(&mut self, coin_name: String, value: u128);

    /**
        @notice mint the wrapped coin.
        @dev Caller must be a BSHPeriphery contract
        Invalid coin_name will have an id = 0. However, id = 0 is also dedicated to Native Coin
        Thus, BSHPeriphery will check a validity of a requested coin_name before calling
        for the coin_name indicates with id = 0, it should send the Native Coin (Example: PRA) to user account
        @param to          The account receive the minted coin
        @param coinName    Coin name
        @param value       The minted amount
    */
    fn mint(&mut self, to: AccountId, coin_name: String, value: u128);

    /**
        @notice Handle a request of Fee Gathering
        @dev    Caller must be an BSHPeriphery contract
        @param  fa    BTP Address of Fee Aggregator
    */
    fn transfer_fees(&mut self, fa: AccountId);

    /**
        @notice Handle a response of a requested service
        @dev Caller must be an BSHPeriphery contract
        @param requester   An address of originator of a requested service
        @param coin_name    A name of requested coin
        @param value       An amount to receive on a destination chain
        @param fee         An amount of charged fee
    */
    fn handle_response_service(
        &mut self,
        requester: AccountId,
        coin_name: String,
        value: u128,
        fee: u128,
        rsp_code: usize,
    );
}

/**
   @title Interface of BSHPeriphery contract
   @dev This contract is used to handle communications among BMCService and BSHCore contract
*/
#[ext_contract]
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

setup_alloc!();

/**
   @title BSHPeriphery contract
   @dev This contract is used to handle communications among BMCService and BSHCore contract
   @dev This contract does not have its own Owners
        Instead, BSHCore manages ownership roles.
        Thus, BSHPeriphery should call bsh_core.is_owner() and pass an address for verification
        in case of implementing restrictions, if needed, in the future.
*/
#[near_bindgen]
#[derive(BorshDeserialize, BorshSerialize)]
pub struct BshPeriphery {
    bmc: AccountId,
    bsh_core: AccountId,
    /// A list of transferring requests
    pub requests: UnorderedMap<u64, PendingTransferCoin>,
    /// BSH service name
    pub service_name: String,
    /// A counter of sequence number of service message
    serial_no: u64,
    num_of_pending_requests: u64,
}

impl Default for BshPeriphery {
    fn default() -> Self {
        Self {
            bmc: "".to_string(),
            bsh_core: "".to_string(),
            requests: UnorderedMap::new(b"BshStorageKey::BshPeriphery".to_vec()),
            service_name: "".to_string(),
            serial_no: 0,
            num_of_pending_requests: 0,
        }
    }
}

#[near_bindgen]
impl BshPeriphery {
    pub const RC_OK: usize = 0;
    pub const RC_ERR: usize = 1;

    #[init]
    pub fn new(bmc: AccountId, bsh_core: AccountId, service_name: String) -> Self {
        Self {
            bmc,
            bsh_core,
            requests: UnorderedMap::new(b"BshStorageKey::BshPeriphery".to_vec()),
            service_name,
            serial_no: 0,
            num_of_pending_requests: 0,
        }
    }

    /**
        @notice Check whether BSHPeriphery has any pending transferring requests
        @return true or false
    */
    pub fn has_pending_requests(&self) -> bool {
        self.num_of_pending_requests != 0
    }

    /**
        @notice Send Service Message from BSHCore contract to BMCService contract
        @dev Caller must be BSHCore only
        @param to             A network address of destination chain
        @param coin_names     A list of coin name that are requested to transfer
        @param values         A list of an amount to receive at destination chain respectively with its coin name
        @param fees           A list of an amount of charging fee respectively with its coin name
    */
    pub fn send_service_message(
        &mut self,
        from: AccountId,
        to: AccountId,
        coin_names: Vec<String>,
        values: Vec<u128>,
        fees: Vec<u128>,
    ) -> Result<(), &str> {
        let btp_addr = BTPAddress::new(to.clone());
        let to_network = btp_addr
            .network_address()
            .expect("Failed to retrieve network address");
        let to_address = btp_addr
            .contract_address()
            .expect("Failed to retrieve contract address");

        let mut assets: Vec<Asset> = Vec::with_capacity(coin_names.len());
        let mut asset_details: Vec<AssetTransferDetail> = Vec::with_capacity(coin_names.len());

        for i in 0..coin_names.len() {
            assets.push(Asset {
                coin_name: coin_names[i].clone(),
                value: values[i],
            });
            asset_details.push(AssetTransferDetail {
                coin_name: coin_names[i].clone(),
                value: values[i],
                fee: fees[i],
            });
        }

        self.serial_no += 1;

        // Send Service Message to BMC
        let tc = TransferCoin {
            from: from,
            to: to_address,
            assets,
        };
        let tc_bytes = TransferCoin::try_to_vec(&tc).expect("Failed to serialize transfer coin");
        let sm = ServiceMessage {
            service_type: ServiceType::RequestCoinTransfer,
            data: tc_bytes,
        };
        let sm_bytes =
            ServiceMessage::try_to_vec(&sm).expect("Failed to serialize service message");

        ibmc_periphery::send_message(
            to_network,
            self.service_name.clone(),
            self.serial_no,
            sm_bytes,
            &env::current_account_id(),
            0,
            env::prepaid_gas(),
        );

        // Push pending transactions into Record list
        let pending_transfer_coin = PendingTransferCoin {
            from: from.clone(),
            to: to.clone(),
            coin_names,
            amounts: values,
            fees,
        };
        let _ = self
            .requests
            .insert(&self.serial_no, &pending_transfer_coin)
            .expect("Failed to insert request");
        self.num_of_pending_requests += 1;
        let bsh_event = BshEvents::TransferStart {
            from,
            to,
            sn: self.serial_no,
            asset_details,
        };
        let bsh_event = bsh_event
            .try_to_vec()
            .expect("Failed to serialize bsh event");
        env::log(&bsh_event);
        Ok(())
    }

    /**
        @notice BSH handle BTP Message from BMC contract
        @dev Caller must be BMC contract only
        @param from    An originated network address of a request
        @param svc     A service name of BSH contract
        @param sn      A serial number of a service request
        @param msg     An RLP message of a service request/service response
    */
    pub fn handle_btp_message(
        &mut self,
        from: AccountId,
        svc: AccountId,
        sn: u64,
        msg: &[u8],
    ) -> Result<(), &str> {
        if env::signer_account_id() != self.bmc {
            return Err("OnlyBMC: Unauthorized");
        }
        if self.service_name != svc {
            return Err("Invalid SVC");
        }
        let sm = ServiceMessage::try_from_slice(msg).expect("Failed to deserialize msg");

        if sm.service_type == ServiceType::RequestCoinRegister {
            let tc = TransferCoin::try_from_slice(sm.data.as_slice())
                .expect("Failed to deserialize SM data");
            //  check receiving address whether it is a valid address
            //  or revert if not a valid one
            let btp_addr = BTPAddress::new(tc.to.clone());
            if btp_addr.is_valid().is_ok() {
                if self.handle_request_service(tc.to, tc.assets).is_ok() {
                    self.send_response_message(
                        ServiceType::ResponseHandleService,
                        from.clone(),
                        sn,
                        "".to_string(),
                        Self::RC_OK,
                    );
                } else {
                    return Err("Invalid data");
                }
            } else {
                return Err("Invalid BTP address");
            }
            self.send_response_message(
                ServiceType::ResponseHandleService,
                from,
                sn,
                "Invalid address".to_string(),
                Self::RC_ERR,
            );
        } else if sm.service_type == ServiceType::ResponseHandleService {
            // Check whether `sn` is pending state
            let req = self.requests.get(&sn).expect("Failed to retrieve request");
            let res = req.from.as_bytes();

            if res.is_empty() {
                return Err("Invalid SN");
            }
            let response = Response::try_from_slice(sm.data.as_slice())
                .expect("Failed to deserialize service message");
            self.handle_response_service(sn, response.code, response.message)
                .expect("Error in handling response service");
        } else if sm.service_type == ServiceType::UnknownType {
            let bsh_event = BshEvents::UnknownResponse { from, sn };
            let bsh_event = bsh_event
                .try_to_vec()
                .expect("Failed to serialize bsh event");
            env::log(&bsh_event);
        } else {
            // If none of those types above BSH responds with a message of
            // RES_UNKNOWN_TYPE
            self.send_response_message(
                ServiceType::UnknownType,
                from,
                sn,
                "Unknown".to_string(),
                Self::RC_ERR,
            );
        }
        Ok(())
    }

    /**
        @notice BSH handle BTP Error from BMC contract
        @dev Caller must be BMC contract only
        @param svc     A service name of BSH contract
        @param sn      A serial number of a service request
        @param code    A response code of a message (RC_OK / RC_ERR)
        @param msg     A response message
    */
    pub fn handle_btp_error(
        &mut self,
        svc: AccountId,
        sn: u64,
        code: usize,
        msg: String,
    ) -> Result<(), &str> {
        if env::signer_account_id() != self.bmc {
            return Err("OnlyBMC: Unauthorized");
        }
        if svc != self.service_name {
            return Err("Invalid SVC");
        }
        let req = self.requests.get(&sn).expect("Failed to retrieve request");
        let res = req.from.as_bytes();
        if res.is_empty() {
            return Err("Invalid SN");
        }
        let emit_msg = format!("ErrorCode: {}, ErrorMsg: {}", code, msg);
        self.handle_response_service(sn, Self::RC_ERR, emit_msg)
            .expect("Error in handling response service");
        Ok(())
    }

    #[private]
    pub fn handle_response_service(
        &mut self,
        sn: u64,
        code: usize,
        msg: String,
    ) -> Result<(), &str> {
        let req = self.requests.get(&sn).expect("Failed to retrieve request");
        let caller = req.from;
        let data_len = req.coin_names.len();
        for i in 0..data_len {
            ibsh_core::handle_response_service(
                caller.clone(),
                req.coin_names[i].clone(),
                req.amounts[i],
                req.fees[i],
                code,
                &env::current_account_id(),
                0,
                env::prepaid_gas(),
            );
        }

        let _ = self.requests.remove(&sn);
        self.num_of_pending_requests -= 1;
        let bsh_event = BshEvents::TransferEnd {
            from: caller,
            sn,
            code,
            response: msg,
        };
        let bsh_event = bsh_event
            .try_to_vec()
            .expect("Failed to serialize bsh event");
        env::log(&bsh_event);
        Ok(())
    }

    /**
        @notice Handle a list of minting/transferring coins/tokens
        @dev Caller must be BMC contract only
        @param to          An address to receive coins/tokens
        @param assets      A list of requested coin respectively with an amount
    */
    pub fn handle_request_service(
        &mut self,
        to: AccountId,
        assets: Vec<Asset>,
    ) -> Result<(), &str> {
        if env::signer_account_id() != self.bmc {
            return Err("OnlyBMC: Unauthorized");
        }
        for i in 0..assets.len() {
            // if ibsh_core::is_valid_coin(
            //     assets[i].coin_name,
            //     &env::current_account_id(),
            //     0,
            //     env::prepaid_gas(),
            // ) {
            //     return Err("UnregisteredCoin");
            // }

            ibsh_core::mint(
                to.clone(),
                assets[i].coin_name.clone(),
                assets[i].value,
                &env::current_account_id(),
                0,
                env::prepaid_gas(),
            );
        }

        Ok(())
    }

    #[private]
    pub fn send_response_message(
        &mut self,
        service_type: ServiceType,
        to: AccountId,
        sn: u64,
        msg: String,
        code: usize,
    ) {
        let response = Response { code, message: msg };
        let res_bytes = Response::try_to_vec(&response).expect("Failed to serialize response");
        let sm = ServiceMessage {
            service_type,
            data: res_bytes,
        };
        let sm_bytes =
            ServiceMessage::try_to_vec(&sm).expect("Failed to serialize service message");

        ibmc_periphery::send_message(
            to,
            self.service_name.clone(),
            sn,
            sm_bytes,
            &env::current_account_id(),
            0,
            env::prepaid_gas(),
        );
    }

    /**
        @notice BSH handle Gather Fee Message request from BMC contract
        @dev Caller must be BMC contract only
        @param fa     A BTP address of fee aggregator
        @param svc    A name of the service
    */
    pub fn handle_fee_gathering(&mut self, fa: AccountId, svc: String) -> Result<(), &str> {
        if env::signer_account_id() != self.bmc {
            return Err("OnlyBMC: Unauthorized");
        }
        if self.service_name != svc {
            return Err("Invalid SVC");
        }
        //  If adress of Fee Aggregator (fa) is invalid BTP address format
        //  revert(). Then, BMC will catch this error
        let btp_addr = BTPAddress::new(fa.clone());
        if btp_addr.is_valid().is_ok() {
            ibsh_core::transfer_fees(fa, &env::current_account_id(), 0, env::prepaid_gas());
        }
        Ok(())
    }

    /// Return contract address
    pub fn get_contract_address(&self) -> String {
        env::current_account_id()
    }

    /// Update contract address
    pub fn set_contract_address(&mut self, new_addr: AccountId) {
        self.bsh_core = new_addr;
    }
}

#[cfg(not(target_arch = "wasm32"))]
#[cfg(test)]
mod tests {
    use super::*;
    use near_sdk::test_utils::VMContextBuilder;
    use near_sdk::MockedBlockchain;
    use near_sdk::{testing_env, VMContext};
    use std::convert::TryInto;

    fn get_context(is_view: bool) -> VMContext {
        VMContextBuilder::new()
            .signer_account_id("bob_near".try_into().expect("Failed to convert"))
            .is_view(is_view)
            .build()
    }

    #[test]
    fn test_has_pending_request() {
        testing_env!(get_context(true));
        let bsh = BshPeriphery::default();
        assert!(bsh.has_pending_requests());
    }

    #[test]
    fn test_that_request_retrieval_works() {
        testing_env!(get_context(false));
        let mut bsh = BshPeriphery::default();
        let pt1 = PendingTransferCoin {
            from: "btp://0x1.near/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string(),
            to: "btp://0x1.near/cx77ed9048b594b95199f326fc76e76a9d33dd665b".to_string(),
            coin_names: vec!["btc".to_string(), "ether".to_string(), "usdt".to_string()],
            amounts: vec![100, 200, 300],
            fees: vec![1, 2, 3],
        };
        let pt2 = PendingTransferCoin {
            from: "btp://0x1.near/cx67ed9048b594b95199f326fc76e76a9d33dd665b".to_string(),
            to: "btp://0x1.near/cx57ed9048b594b95199f326fc76e76a9d33dd665b".to_string(),
            coin_names: vec!["sol".to_string(), "near".to_string(), "dai".to_string()],
            amounts: vec![400, 500, 600],
            fees: vec![4, 5, 6],
        };
        let _ = bsh.requests.insert(&1, &pt1);
        let _ = bsh.requests.insert(&2, &pt2);
        assert!(bsh.requests.get(&2).is_some());
    }

    #[test]
    fn test_that_service_names_match() {
        testing_env!(get_context(true));
        let bsh = BshPeriphery::default();
        let svc = "";
        assert_eq!(bsh.service_name, svc.to_string(), "InvalidSvc");
    }

    #[test]
    fn test_that_serialization_and_deserialization_work() {
        testing_env!(get_context(true));
        let btc = Asset {
            coin_name: "btc".to_string(),
            value: 100,
        };
        let encoded_btc = btc.try_to_vec().expect("Failed to convert to vec");
        let decoded_btc = Asset::try_from_slice(&encoded_btc).expect("Failed to slice the vec");
        assert_eq!(btc, decoded_btc, "Data mismatch!");
    }
}