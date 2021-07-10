//! BSH Generic Contract

#![forbid(
    arithmetic_overflow,
    mutable_transmutes,
    no_mangle_const_items,
    unknown_crate_types
)]
#![warn(
    bad_style,
    deprecated,
    improper_ctypes,
    non_shorthand_field_patterns,
    overflowing_literals,
    stable_features,
    unconditional_recursion,
    unknown_lints,
    unused,
    unused_allocation,
    unused_attributes,
    unused_comparisons,
    unused_features,
    unused_parens,
    unused_variables,
    while_true,
    clippy::unicode_not_nfc,
    clippy::wrong_pub_self_convention,
    clippy::unwrap_used,
    trivial_casts,
    trivial_numeric_casts,
    unused_extern_crates,
    unused_import_braces,
    unused_qualifications,
    unused_results
)]

pub mod bsh_types;
pub mod errors;

pub use bsh_types::*;
pub use errors::BSHError;

use btp_common::BTPAddress;
use near_sdk::borsh::{self, BorshDeserialize, BorshSerialize};
use near_sdk::serde::{Deserialize, Serialize};
use near_sdk::{env, metadata, near_bindgen, setup_alloc};
use std::collections::HashMap;

setup_alloc!();

metadata! {
    /// BSH Generic contract is used to handle communications
    /// among BMC Service and a BSH core contract.
    #[near_bindgen]
    #[derive(BorshDeserialize, BorshSerialize, Clone, Debug, Default, Deserialize, Serialize)]
    #[serde(crate = "near_sdk::serde")]
    pub struct BshGeneric {
        /// A list of transferring requests
        requests: HashMap<u64, PendingTransferCoin>,
        /// BSH Service name
        service_name: String,
        /// A counter of sequence number of service message
        serial_no: u64,
        num_of_pending_requests: u64,
    }
}

#[near_bindgen]
impl BshGeneric {
    pub const RC_OK: u64 = 0;
    pub const RC_ERR: u64 = 1;

    #[init]
    pub fn initialize(_bmc: &str, _bsh_contract: &str, _service_name: &str) {
        unimplemented!() // requires BMC and BSH core
    }

    /// Check whether BSH has any pending transferring requests
    pub fn has_pending_request(&self) -> bool {
        self.num_of_pending_requests != 0
    }

    /// Send Service Message from BSH contract to BMCService contract
    pub fn send_service_message(
        &mut self,
        from: &str,
        to: &str,
        coin_names: Vec<String>,
        values: Vec<u64>,
        fees: Vec<u64>,
    ) {
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
            .unwrap();
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
    }

    /// BSH handle BTP Message from BMC contract
    pub fn handle_btp_message(
        &mut self,
        from: &str,
        svc: &str,
        sn: u64,
        msg: &[u8],
    ) -> Result<(), BSHError> {
        assert_eq!(self.service_name.as_str(), svc, "Invalid Svc");
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
                    return Err(BSHError::InvalidData);
                }
            } else {
                return Err(BSHError::InvalidBtpAddress);
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
            let res = self.requests.get(&sn).unwrap().from.as_bytes();
            assert_ne!(res.len(), 0, "InvalidSN");
            let response: Response = bincode::deserialize(sm.data.as_slice()).unwrap();
            self.handle_response_service(sn, response.code, response.message.as_str());
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
    pub fn handle_btp_error(&mut self, _src: &str, svc: &str, sn: u64, code: u64, msg: &str) {
        assert_eq!(svc, self.service_name.as_str(), "InvalidSvc");
        let res = self.requests.get(&sn).unwrap().from.as_bytes();
        assert_ne!(res.len(), 0, "InvalidSN");
        self.handle_response_service(sn, code, msg);
    }

    fn handle_response_service(&mut self, sn: u64, code: u64, msg: &str) {
        let caller = self.requests.get(&sn).unwrap().from.as_str();
        let data_len = self.requests.get(&sn).unwrap().coin_names.len();
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
    }

    /// Handle a list of minting/transferring coins/tokens
    #[payable]
    pub fn handle_request_service(
        &mut self,
        _to: &str,
        assets: Vec<Asset>,
    ) -> Result<(), BSHError> {
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

    fn send_response_message(
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
    pub fn handle_fee_gathering(&mut self, fa: &str, svc: &str) {
        assert_eq!(self.service_name.as_str(), svc, "InvalidSvc");
        //  If adress of Fee Aggregator (fa) is invalid BTP address format
        //  revert(). Then, BMC will catch this error
        let btp_addr = BTPAddress(fa.to_string());
        if let Ok(_) = btp_addr.is_valid() {
            // BSH core: bsh_core.transfer_fees(fa);
        }
    }
}
