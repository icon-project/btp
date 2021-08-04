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
    clippy::unwrap_used,
    trivial_casts,
    trivial_numeric_casts,
    unused_extern_crates,
    unused_import_braces,
    unused_qualifications,
    unused_results
)]

use btp_common::BTPAddress;
use libraries::bsh_types::*;
use near_sdk::borsh::{self, BorshDeserialize, BorshSerialize};
use near_sdk::collections::UnorderedMap;
use near_sdk::{env, near_bindgen, setup_alloc};

setup_alloc!();
/// BSH Generic contract is used to handle communications
/// among BMC Service and a BSH core contract.
/// This struct implements `Default`: https://github.com/near/near-sdk-rs#writing-rust-contract
#[near_bindgen]
#[derive(BorshDeserialize, BorshSerialize)]
pub struct BshGeneric {
    bmc_contract: String,
    bsh_contract: String,
    /// A list of transferring requests
    /// Use `HashMap` because `LookupMap` doesn't implement
    /// Clone, Debug, and Default traits
    pub requests: UnorderedMap<u64, PendingTransferCoin>,
    /// BSH Service name
    pub service_name: String,
    /// A counter of sequence number of service message
    serial_no: u64,
    num_of_pending_requests: u64,
}

impl Default for BshGeneric {
    fn default() -> Self {
        Self {
            bmc_contract: "".to_string(),
            bsh_contract: "".to_string(),
            requests: UnorderedMap::new(BshStorageKey::BshGeneric),
            service_name: "".to_string(),
            serial_no: 0,
            num_of_pending_requests: 0,
        }
    }
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
            requests: UnorderedMap::new(BshStorageKey::BshGeneric),
            service_name: service_name.to_string(),
            serial_no: 0,
            num_of_pending_requests: 0,
        }
    }

    /// Check whether BSH has any pending transferring requests
    pub fn has_pending_requests(&self) -> bool {
        self.num_of_pending_requests != 0
    }

    /// Send Service Message from BSH contract to BMCService contract
    pub fn send_service_message(
        &mut self,
        from: &str,
        to: &str,
        coin_names: Vec<String>,
        values: Vec<u128>,
        fees: Vec<u128>,
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
        // Send Service Message to BMC
        // BMC: bmc.send_message();

        // Push pending transactions into Record list
        let pending_transfer_coin = PendingTransferCoin {
            from: from.to_string(),
            to: to.to_string(),
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
        if self.service_name != *svc {
            return Err("Invalid SVC");
        }
        let sm = ServiceMessage::try_from_slice(msg).expect("Failed to deserialize msg");

        if sm.service_type == ServiceType::RequestCoinRegister {
            let tc = TransferCoin::try_from_slice(sm.data.as_slice())
                .expect("Failed to deserialize SM data");
            //  check receiving address whether it is a valid address
            //  or revert if not a valid one
            let btp_addr = BTPAddress(tc.to.clone());
            if btp_addr.is_valid().is_ok() {
                if self.handle_request_service(&tc.to, tc.assets).is_ok() {
                    self.send_response_message(
                        ServiceType::ResponseHandleService,
                        from,
                        sn,
                        "",
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
                "Invalid address",
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
            self.handle_response_service(sn, response.code, response.message.as_str())
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
        if *svc != self.service_name {
            return Err("Invalid SVC");
        }
        let req = self.requests.get(&sn).expect("Failed to retrieve request");
        let res = req.from.as_bytes();
        if res.is_empty() {
            return Err("Invalid SN");
        }
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

    /// Handle a list of minting/transferring coins/tokens
    #[payable]
    pub fn handle_request_service(&mut self, _to: &str, assets: Vec<Asset>) -> Result<(), &str> {
        if env::current_account_id() != env::signer_account_id() {
            return Err("Unauthorized");
        }
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
        if self.service_name != *svc {
            return Err("Invalid SVC");
        }
        //  If adress of Fee Aggregator (fa) is invalid BTP address format
        //  revert(). Then, BMC will catch this error
        let btp_addr = BTPAddress(fa.to_string());
        if btp_addr.is_valid().is_ok() {
            // BSH core: bsh_core.transfer_fees(fa);
        }
        Ok(())
    }

    /// Return contract address
    pub fn get_contract_address(&self) -> String {
        env::current_account_id()
    }

    /// Update contract address
    pub fn set_contract_address(&mut self, new_addr: &str) {
        self.bsh_contract = new_addr.to_string();
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
        let bsh = BshGeneric::default();
        assert!(bsh.has_pending_requests());
    }

    #[test]
    fn test_that_request_retrieval_works() {
        testing_env!(get_context(false));
        let mut bsh = BshGeneric::default();
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
        let bsh = BshGeneric::default();
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
