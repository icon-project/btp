//! BMC Generic Contract

use crate::bmc_types::*;
use crate::{BmcManagement, Utils};
use bmv::Bmv;
use bsh_generic::BshGeneric;
use bsh_types::*;
use btp_common::BTPAddress;
use near_sdk::borsh::{self, BorshDeserialize, BorshSerialize};
use near_sdk::{env, near_bindgen, setup_alloc};

setup_alloc!();
#[near_bindgen]
#[derive(BorshDeserialize, BorshSerialize, Clone)]
pub struct BmcGeneric {
    // a network address BMV, i.e. btp://1234.pra/0xabcd
    bmc_btp_address: String,
    bmc_management: String,
}

impl Default for BmcGeneric {
    fn default() -> Self {
        Self {
            bmc_btp_address: "".to_string(),
            bmc_management: "".to_string(),
        }
    }
}

#[near_bindgen]
impl BmcGeneric {
    pub const UNKNOWN_ERR: u32 = 0;
    pub const BMC_ERR: u32 = 10;
    pub const BMV_ERR: u32 = 25;
    pub const BSH_ERR: u32 = 40;

    #[init]
    pub fn new(network: &str, bmc_mgt_addr: &str) -> Self {
        let bmc_btp_address = format!("btp://{}/{}", network, env::current_account_id());
        Self {
            bmc_btp_address,
            bmc_management: bmc_mgt_addr.to_string(),
        }
    }

    /// Get BMC BTP address
    pub fn get_bmc_btp_address(&self) -> String {
        self.bmc_btp_address.to_owned()
    }

    /// Verify and decode RelayMessage with BMV, and dispatch BTP Messages to registered BSHs
    /// Caller must be a registered relayer.
    pub fn handle_relay_message(&mut self, prev: &str, msg: &str) {
        let serialized_msgs = self
            .decode_msg_and_validate_relay(prev, msg)
            .expect("Failed to decode messages");
        let mut bmc_mgt = BmcManagement::default();

        // dispatch BTP messages
        for msg in &serialized_msgs {
            if let Ok(decoded) = self.clone().decode_btp_message(msg.as_slice()) {
                let message = decoded.clone();
                let _ = BmcMessage {
                    src: decoded.src,
                    dst: decoded.dst,
                    svc: decoded.svc,
                    sn: decoded.sn,
                    message: decoded.message,
                };
                if message.dst == self.get_bmc_btp_address() {
                    self.handle_message_internal(prev, &message)
                        .expect("Error in handling message");
                } else {
                    let net = BTPAddress::new(message.dst.clone())
                        .network_address()
                        .expect("Failed to retrieve network address");
                    if let Ok((next_link, _)) = bmc_mgt.resolve_route(&net) {
                        self.send_message_internal(&next_link, msg.as_slice())
                            .expect("Failed to send message");
                    }
                    if let Err(error) = bmc_mgt.resolve_route(&net) {
                        self.send_error_internal(prev, &message, Self::BMC_ERR, &error)
                            .expect("Failed to send error");
                    }
                }
            }
        }
        bmc_mgt
            .update_link_rx_seq(prev, serialized_msgs.len() as u128)
            .expect("Failed to update link");
    }

    pub fn decode_msg_and_validate_relay(
        &mut self,
        prev: &str,
        msg: &str,
    ) -> Result<Vec<Vec<u8>>, &str> {
        let mut bmc_mgt = BmcManagement::default();
        let mut bmv = Bmv::default();
        let net = BTPAddress::new(prev.to_string())
            .network_address()
            .expect("Failed to retrieve network address");
        let bmv_addr = bmc_mgt.get_bmv_service_by_net(&net);
        if bmv_addr == env::predecessor_account_id() {
            return Err("BMC revert, BMV does not exist");
        }
        let (prev_height, _, _) = bmv.get_status();

        // decode and verify relay message
        let serialized_msgs = bmv.handle_relay_message(
            &self.get_bmc_btp_address(),
            prev,
            bmc_mgt.get_link_rx_seq(prev),
            msg,
        );

        // rotate and check valid relay
        let (height, last_height, _) = bmv.get_status();
        let mut relay = bmc_mgt
            .rotate_relay(prev, height, last_height, !serialized_msgs.is_empty())
            .expect("Failed to rotate relay");
        let mut check = false;
        if relay == env::predecessor_account_id() {
            let relays = bmc_mgt.get_link_relays(prev);
            for entry in relays {
                if entry == env::current_account_id() {
                    check = true;
                    break;
                }
            }
            if !check {
                return Err("BMCRevertUnauthorized: unregistered relay");
            }
            relay = env::current_account_id();
        } else if relay != env::current_account_id() {
            return Err("BMCRevertUnauthorized: invalid relay");
        }
        bmc_mgt
            .update_relay_stats(&relay, height - prev_height, serialized_msgs.len() as u128)
            .expect("Failed to update relay stats");

        Ok(serialized_msgs)
    }

    pub fn decode_btp_message(&mut self, rlp: &[u8]) -> Result<BmcMessage, String> {
        Ok(BmcMessage::try_from_slice(rlp).expect("Failed to decode message"))
    }

    fn handle_message_internal(&mut self, prev: &str, msg: &BmcMessage) -> Result<(), &str> {
        let mut bsh_addr = String::from("");
        let mut bmc_mgt = BmcManagement::default();
        let mut bsh_generic = BshGeneric::default();
        if &msg.svc == "bmc" {
            let mut sm = BmcService::default();
            if let Ok(res) = BmcService::try_from_slice(&msg.message) {
                sm = res;
            } else {
                self.send_error_internal(prev, msg, Self::BMC_ERR, "BMCRevertParseFailure")
                    .expect("Failed to send error");
                return Ok(());
            }
            if &sm.service_type == "FeeGathering" {
                let mut gather_fee = GatherFeeMessage::default();
                if let Ok(res) = GatherFeeMessage::try_from_slice(&sm.payload) {
                    gather_fee = res;
                } else {
                    self.send_error_internal(prev, msg, Self::BMC_ERR, "BMCRevertParseFailure")
                        .expect("Failed to send error");
                    return Ok(());
                }
                for svc in &gather_fee.svcs {
                    bsh_addr = bmc_mgt.get_bsh_service_by_name(svc);
                    // If `svc` not found, ignore
                    if bsh_addr != env::predecessor_account_id() {
                        bsh_generic
                            .handle_fee_gathering(&gather_fee.fa, svc)
                            .expect("Failed to handle fee gathering");
                    }
                }
            } else if &sm.service_type == "Link" {
                let to = std::str::from_utf8(&sm.payload).expect("Failed to decode payload");
                let link = bmc_mgt.get_link(prev);
                let mut check = false;
                if link.is_connected {
                    for entry in link.reachable {
                        if to == entry {
                            check = true;
                            break;
                        }
                    }
                    if !check {
                        let links = vec![to.to_string()];
                        bmc_mgt
                            .update_link_reachable(prev, links)
                            .expect("Failed to update link");
                    }
                }
            } else if &sm.service_type == "Unlink" {
                let to = std::str::from_utf8(&sm.payload).expect("Failed to decode payload");
                let link = bmc_mgt.get_link(prev);
                if link.is_connected {
                    for i in 0..link.reachable.len() {
                        if to == link.reachable[i] {
                            bmc_mgt
                                .delete_link_reachable(prev, i)
                                .expect("Failed to delete link");
                        }
                    }
                }
            } else if &sm.service_type == "Init" {
                let links = vec![std::str::from_utf8(&sm.payload)
                    .expect("Failed to decode payload")
                    .to_string()];
                bmc_mgt
                    .update_link_reachable(prev, links)
                    .expect("Failed to update link");
            } else {
                return Err("BMCRevert: internal handler does not exist");
            }
        } else {
            bsh_addr = bmc_mgt.get_bsh_service_by_name(&msg.svc);
            if bsh_addr == env::predecessor_account_id() {
                self.send_error_internal(prev, msg, Self::BMC_ERR, "BMCRevertNotExistsBSH")
                    .expect("Failed to send error");
                return Ok(());
            }
            if msg.sn >= 0 {
                let net = BTPAddress::new(msg.src.to_string())
                    .network_address()
                    .expect("Failed to retrieve network address");
                if let Err(error) = bsh_generic.handle_btp_message(
                    &net,
                    &msg.svc,
                    msg.sn as u64,
                    msg.message.as_slice(),
                ) {
                    self.send_error_internal(prev, msg, Self::BSH_ERR, error)
                        .expect("Failed to send error");
                }
            } else {
                let err_msg =
                    Response::try_from_slice(&msg.message).expect("Failed to decode message");
                if let Err(error) = bsh_generic.handle_btp_error(
                    &msg.src,
                    &msg.svc,
                    msg.sn as u64,
                    err_msg.code,
                    &err_msg.message,
                ) {
                    let bmc_events = BmcEvents::ErrorOnBtpError {
                        svc: msg.svc.clone(),
                        sn: msg.sn,
                        code: err_msg.code,
                        err_msg: err_msg.message,
                        svc_err_code: Self::BSH_ERR as u64,
                        svc_err_msg: error.to_string(),
                    };
                    let bmc_events = bmc_events
                        .try_to_vec()
                        .expect("Failed to serialize BMC events");
                    env::log(&bmc_events);
                }
            }
        }
        Ok(())
    }

    fn send_message_internal(&mut self, to: &str, serialized_msg: &[u8]) -> Result<(), &str> {
        let mut bmc_mgt = BmcManagement::default();
        bmc_mgt
            .update_link_tx_seq(to)
            .expect("Failed to update link");
        let bmc_events = BmcEvents::Message {
            next: to.to_string(),
            seq: bmc_mgt.get_link_tx_seq(to),
            msg: serialized_msg.to_vec(),
        };
        let bmc_events = bmc_events
            .try_to_vec()
            .expect("Failed to serialize BMC events");
        env::log(&bmc_events);
        Ok(())
    }

    fn send_error_internal(
        &mut self,
        prev: &str,
        msg: &BmcMessage,
        err_code: u32,
        err_msg: &str,
    ) -> Result<(), &str> {
        if msg.sn > 0 {
            let res = Response {
                code: err_code as u64,
                message: err_msg.to_string(),
            };
            let encoded_res = res.try_to_vec().expect("Failed to serialize response");

            let bmc_msg = BmcMessage {
                src: self.get_bmc_btp_address(),
                dst: msg.src.clone(),
                svc: msg.svc.clone(),
                sn: msg.sn,
                message: encoded_res,
            };
            let serialized_msg = bmc_msg
                .try_to_vec()
                .expect("Failed to serialize BMC message");
            self.send_message_internal(prev, &serialized_msg)
                .expect("Failed to send message");
        }
        Ok(())
    }

    /// Send the message to a specific network
    /// Caller must be a registered BSH
    pub fn send_message(&mut self, to: &str, svc: &str, sn: i64, msg: &[u8]) -> Result<(), &str> {
        let mut bmc_mgt = BmcManagement::default();
        if self.bmc_management != env::current_account_id()
            || env::current_account_id() != bmc_mgt.get_bsh_service_by_name(svc)
        {
            return Err("BMCRevertUnauthorized");
        }
        if sn < 0 {
            return Err("BMCRevertInvalidSN");
        }
        //  In case BSH sends a REQUEST_COIN_TRANSFER,
        //  but `to` is a network which is not supported by BMC
        //  revert() therein
        if bmc_mgt.get_bmv_service_by_net(to) == env::predecessor_account_id() {
            return Err("BMCRevertNotExistBMV");
        }
        let (next_link, dst) = bmc_mgt.resolve_route(to).expect("Failed to resolve route");
        let bmc_msg = BmcMessage {
            src: self.get_bmc_btp_address(),
            dst,
            svc: svc.to_string(),
            sn,
            message: msg.to_vec(),
        };
        let rlp = bmc_msg
            .try_to_vec()
            .expect("Failed to serialize BMC message");
        self.send_message_internal(&next_link, &rlp)
            .expect("Failed to send message");
        Ok(())
    }

    /// Get status of BMC
    pub fn get_status(&self, link: &str) -> Result<LinkStats, &str> {
        let bmc_mgt = BmcManagement::default();
        let link_struct = bmc_mgt.get_link(link);
        if !link_struct.is_connected {
            return Err("BMCRevertNotExistsLink");
        }
        let relays = bmc_mgt.get_relay_status_by_link(link);
        let net = BTPAddress::new(link.to_string())
            .network_address()
            .expect("Failed to retrieve network address");
        let (height, offset, last_height) =
            Bmv::new(&bmc_mgt.get_bmv_service_by_net(&net)).get_status();
        let scale = u128::get_scale(
            link_struct.block_interval_src,
            link_struct.block_interval_dst,
        );
        let rotate_term = u128::get_rotate_term(link_struct.max_aggregation, scale);
        let verifier = VerifierStats {
            height_mta: height,
            offset_mta: offset,
            last_height,
            extra: 0,
        };

        Ok(LinkStats {
            rx_seq: link_struct.rx_seq,
            tx_seq: link_struct.tx_seq,
            verifier,
            relays,
            relay_idx: link_struct.relay_idx,
            rotate_height: link_struct.rotate_height,
            rotate_term,
            delay_limit: link_struct.delay_limit,
            max_aggregation: link_struct.max_aggregation,
            rx_height_src: link_struct.rx_height_src,
            rx_height: link_struct.rx_height,
            block_interval_src: link_struct.block_interval_src,
            block_interval_dst: link_struct.block_interval_dst,
            current_height: env::block_index() as u128,
        })
    }
}

#[cfg(not(target_arch = "wasm32"))]
#[cfg(test)]
mod tests {
    use super::*;
    use near_sdk::test_utils::VMContextBuilder;
    use near_sdk::{testing_env, MockedBlockchain, VMContext};
    use std::convert::TryInto;

    fn get_context(is_view: bool) -> VMContext {
        VMContextBuilder::new()
            .signer_account_id("bob_near".try_into().expect("Failed to convert"))
            .is_view(is_view)
            .build()
    }

    #[test]
    fn test_encode_and_decode_btp_message() {
        testing_env!(get_context(false));
        let rlp: Vec<u8> = vec![1, 2, 3, 4, 5];
        let bmc_msg = BmcMessage {
            src: "btp://1234.PARA/0x1234".to_string(),
            dst: "btp://5678.PARA/0x5678".to_string(),
            svc: "NEAR Protocol".to_string(),
            sn: 100,
            message: rlp,
        };
        let encoded_bmc_msg = bmc_msg
            .try_to_vec()
            .expect("Failed to serialize bmc message");
        let decoded_bmc_msg = BmcMessage::try_from_slice(&encoded_bmc_msg)
            .expect("Failed to deserialize bmc message");
        assert_eq!(bmc_msg.src, decoded_bmc_msg.src);
        assert_eq!(bmc_msg.dst, decoded_bmc_msg.dst);
        assert_eq!(bmc_msg.svc, decoded_bmc_msg.svc);
        assert_eq!(bmc_msg.sn, decoded_bmc_msg.sn);
        assert_eq!(bmc_msg.message, decoded_bmc_msg.message);
    }
}
