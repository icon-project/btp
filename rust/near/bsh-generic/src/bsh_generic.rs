//! BSH Generic Contract

use crate::bsh_types::*;

use btp_common::BTPAddress;
use near_sdk::borsh::{self, BorshDeserialize, BorshSerialize};
use near_sdk::serde::{Deserialize, Serialize};
use near_sdk::{env, near_bindgen, setup_alloc};
use std::collections::HashMap;

setup_alloc!();

/// BSH Generic contract is used to handle communications
/// among BMC Service and a BSH core contract.
/// This struct implements `Default`: https://github.com/near/near-sdk-rs#writing-rust-contract
#[near_bindgen]
#[derive(BorshDeserialize, BorshSerialize, Clone, Debug, Default, Deserialize, Serialize)]
#[serde(crate = "near_sdk::serde")]
pub struct BshGeneric {
    bmc_contract: String,
    bsh_contract: String,
    /// A list of transferring requests
    /// Use `HashMap` because `LookupMap` doesn't implement
    /// Clone, Debug, and Default traits
    requests: HashMap<u64, PendingTransferCoin>,
    /// BSH Service name
    service_name: String,
    /// A counter of sequence number of service message
    serial_no: u64,
    num_of_pending_requests: u64,
}

#[near_bindgen]
impl BshGeneric {
    pub const RC_OK: u64 = 0;
    pub const RC_ERR: u64 = 1;

    #[init]
    pub fn new(bmc: &str, bsh_contract: &str, service_name: &str) -> Self {
        // TODO: fully implement after BMC and BSH core contracts are written

        Self {
            bmc_contract: bmc.to_string(),
            bsh_contract: bsh_contract.to_string(),
            requests: HashMap::new(),
            service_name: service_name.to_string(),
            serial_no: 0,
            num_of_pending_requests: 0,
        }
    }

    /// Check whether BSH has any pending transferring requests
    pub fn has_pending_requests(&self) -> Result<bool, &str> {
        Ok(self.num_of_pending_requests != 0)
    }

    /// Send Service Message from BSH contract to BMCService contract
    pub fn send_service_message(
        &mut self,
        from: &str,
        to: &str,
        coin_names: Vec<String>,
        values: Vec<u64>,
        fees: Vec<u64>,
    ) -> Result<(), &str> {
        let btp_addr = BTPAddress(to.to_string());
        let _network_addr = btp_addr
            .network_address()
            .expect("Failed to retrieve network address")
            .as_str();
        let _contract_addr = btp_addr
            .contract_address()
            .expect("Failed to retrieve contract address")
            .as_str();

        let mut assets: Vec<Asset> = Vec::with_capacity(coin_names.len());
        let mut asset_details: Vec<AssetTransferDetail> = Vec::with_capacity(coin_names.len());

        for i in 0..coin_names.len() {
            assets[i] = Asset {
                coin_name: coin_names[i].clone(),
                value: values[i],
            };
            asset_details[0] = AssetTransferDetail {
                coin_name: coin_names[i].clone(),
                value: values[i],
                fee: fees[i],
            };
        }
        // Send Service Message to BMC
        // BMC: bmc.send_message();

        // Push pending transactions into Record list
        let pending_transfer_coin = PendingTransferCoin {
            from: from.to_string(),
            to: to.to_string(),
            coin_names: coin_names.clone(),
            amounts: values.clone(),
            fees: fees.clone(),
        };
        let _ = self
            .requests
            .insert(self.serial_no, pending_transfer_coin)
            .expect("Failed to insert request");
        self.num_of_pending_requests += 1;
        let bsh_event = BshEvents::TransferStart {
            from,
            to,
            sn: self.serial_no,
            asset_details: asset_details.clone(),
        };
        let bsh_event = bincode::serialize(&bsh_event).expect("Failed to serialize bsh event");
        env::log(&bsh_event);
        self.serial_no += 1;
        Ok(())
    }

    /// BSH handle BTP Message from BMC contract
    pub fn handle_btp_message(
        &mut self,
        from: &str,
        svc: &str,
        sn: u64,
        msg: &[u8],
    ) -> Result<(), &str> {
        assert_eq!(self.service_name, svc.to_string(), "Invalid Svc");
        let sm: ServiceMessage = bincode::deserialize(msg).expect("Failed to deserialize msg");

        if sm.service_type == ServiceType::RequestCoinRegister {
            let tc: TransferCoin =
                bincode::deserialize(sm.data.as_slice()).expect("Failed to deserialize sm data");
            //  check receiving address whether it is a valid address
            //  or revert if not a valid one
            let btp_addr = BTPAddress(tc.to.clone());
            if let Ok(_) = btp_addr.is_valid() {
                if let Ok(_) = self.handle_request_service(&tc.to, tc.assets) {
                    self.send_response_message(
                        ServiceType::ResponseHandleService,
                        from,
                        sn,
                        "",
                        Self::RC_OK,
                    );
                } else {
                    return Err("InvalidData");
                }
            } else {
                return Err("InvalidBtpAddress");
            }
            self.send_response_message(
                ServiceType::ResponseHandleService,
                from,
                sn,
                "InvalidAddress",
                Self::RC_ERR,
            );
        } else if sm.service_type == ServiceType::ResponseHandleService {
            // Check whether `sn` is pending state
            let req = self.requests.get(&sn).expect("Failed to retrieve request");
            let res = req.from.as_bytes();

            assert_ne!(res.len(), 0, "InvalidSN");
            let response: Response = bincode::deserialize(sm.data.as_slice())
                .expect("Failed to deserialize service message");
            self.handle_response_service(sn, response.code, response.message.as_str())
                .expect("Error in handling response service");
        } else if sm.service_type == ServiceType::UnknownType {
            let bsh_event = BshEvents::UnknownResponse { from, sn };
            let bsh_event = bincode::serialize(&bsh_event).expect("Failed to serialize bsh event");
            env::log(&bsh_event);
        } else {
            // If none of those types above BSH responds with a message of
            // RES_UNKNOWN_TYPE
            self.send_response_message(ServiceType::UnknownType, from, sn, "Unknown", Self::RC_ERR);
        }
        Ok(())
    }

    /// BSH handle BTP Error from BMC contract
    pub fn handle_btp_error(
        &mut self,
        _src: &str,
        svc: &str,
        sn: u64,
        code: u64,
        msg: &str,
    ) -> Result<(), &str> {
        assert_eq!(svc.to_string(), self.service_name, "InvalidSvc");
        let req = self.requests.get(&sn).expect("Failed to retrieve request");
        let res = req.from.as_bytes();
        assert_ne!(res.len(), 0, "InvalidSN");
        self.handle_response_service(sn, code, msg)
            .expect("Error in handling response service");
        Ok(())
    }

    #[private]
    pub fn handle_response_service(&mut self, sn: u64, code: u64, msg: &str) -> Result<(), &str> {
        let req = self.requests.get(&sn).expect("Failed to retrieve request");
        let caller = req.from.as_str();
        let data_len = req.coin_names.len();
        for _i in 0..data_len {
            // BSH core: bsh_core.handle_response_service();
        }

        let _ = self.clone().requests.remove(&sn);
        self.num_of_pending_requests -= 1;
        let bsh_event = BshEvents::TransferEnd {
            from: caller,
            sn,
            code,
            response: msg,
        };
        let bsh_event = bincode::serialize(&bsh_event).expect("Failed to serialize bsh event");
        env::log(&bsh_event);
        Ok(())
    }

    /// Handle a list of minting/transferring coins/tokens
    #[payable]
    pub fn handle_request_service(&mut self, _to: &str, assets: Vec<Asset>) -> Result<(), &str> {
        assert_eq!(
            env::current_account_id(),
            env::signer_account_id(),
            "Unauthorized"
        );
        for _i in 0..assets.len() {
            // BSH core: assert(bsh_core.is_valid_coin(assets[i].coin_name), "UnregisteredCoin");
        }
        // BSH core: if let Ok(_) = bsh_core.mint() {}
        Ok(())
    }

    #[private]
    pub fn send_response_message(
        &mut self,
        _service_type: ServiceType,
        _to: &str,
        _sn: u64,
        _msg: &str,
        _code: u64,
    ) {
        // BMC: bmc.send_message();
    }

    /// BSH handle `Gather Fee Message` request from BMC contract
    /// fa: fee aggregator
    #[payable]
    pub fn handle_fee_gathering(&mut self, fa: &str, svc: &str) -> Result<(), &str> {
        assert_eq!(self.service_name, svc.to_string(), "InvalidSvc");
        //  If adress of Fee Aggregator (fa) is invalid BTP address format
        //  revert(). Then, BMC will catch this error
        let btp_addr = BTPAddress(fa.to_string());
        if let Ok(_) = btp_addr.is_valid() {
            // BSH core: bsh_core.transfer_fees(fa);
        }
        Ok(())
    }
}
