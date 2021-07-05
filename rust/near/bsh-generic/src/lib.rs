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
pub mod utils;
pub use bsh_types::*;

use log;
use near_sdk::borsh::{self, BorshDeserialize, BorshSerialize};
use near_sdk::serde::{Deserialize, Serialize};
use near_sdk::{metadata, near_bindgen, setup_alloc};
use std::collections::HashMap;

setup_alloc!();

metadata! {
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
    #[init]
    pub fn initialize(_bmc: &str, _bsh_contract: &str, _service_name: &str) {
        unimplemented!()
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
        //  Send Service Message to BMC
        //  Throws an error if `to` address is an invalid BTP Address format
        let (_to_network, _to_address) = utils::split_btp_address(to);
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

        // TODO: bmc.send_message();

        // Push pending transactions into Record list
        let pending_transfer_coin = PendingTransferCoin {
            from: from.to_string(),
            to: to.to_string(),
            coin_names: coin_names.clone(),
            amounts: values.clone(),
            fees: fees.clone(),
        };
        let _ = self.requests.insert(self.serial_no, pending_transfer_coin).unwrap();
        self.num_of_pending_requests += 1;
        let bsh_event = BshEvents::TransferStart {
            from,
            to,
            sn: self.serial_no,
            asset_details: asset_details.clone(),
        };
        log::info!("New BSH event: {:?}", bsh_event);
        self.serial_no += 1;

    }

    /// BSH handle BTP Message from BMC contract
    pub fn handle_btp_message(&mut self, from: &str, svc: &str, sn: u64, msg: &[u8]) {
        assert_eq!(self.service_name.as_str(), svc, "Invalid Svc");
        let mut sm: ServiceMessage = bincode::deserialize(msg).unwrap();
        let mut err_msg = String::new();

        if sm.service_type == ServiceType::RequestCoinRegister {
            let mut tc: TransferCoin = bincode::deserialize(sm.data.as_slice()).unwrap();
            
        }
    }

    /// BSH handle BTP Error from BMC contract
    pub fn handle_btp_error(&mut self, src: &str, svc: &str, sn: u64, code: u64, msg: &str) {
        todo!()
    }

    /// BSH handle Gather Fee Message request from BMC contract
    /// fa: fee aggregator
    pub fn handle_fee_gathering(&mut self, fa: &str, svc: &str) {
        todo!()
    }
}
