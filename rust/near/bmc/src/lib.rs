use near_sdk::borsh::{self, BorshDeserialize, BorshSerialize};
use near_sdk::AccountId;
use near_sdk::{env, near_bindgen};
mod link;
mod permission;
mod route;
use btp_common::BTPAddress;
use permission::{Owner, Owners};
use route::{Route, Routes};
#[macro_use]
extern crate lazy_static;

use link::{Link, Links};

#[global_allocator]
static ALLOC: near_sdk::wee_alloc::WeeAlloc = near_sdk::wee_alloc::WeeAlloc::INIT;

#[near_bindgen]
#[derive(BorshDeserialize, BorshSerialize)]
pub struct BTPMessageCenter {
    message: Message,
    links: Links,
    routes: Routes,
    owners: Owners,
}

#[derive(Default, BorshDeserialize, BorshSerialize)]
pub struct Message {
    payload: Vec<u8>,
}

#[near_bindgen]
impl Default for BTPMessageCenter {
    fn default() -> Self {
        let message: Message = Default::default();
        let links = Links::new();
        let routes = Routes::new();
        let owners = Owners::new();
        Self {
            message,
            links,
            routes,
            owners,
        }
    }
}

#[near_bindgen]
impl BTPMessageCenter {
    pub fn get_message(&self) -> String {
        return String::from_utf8(self.message.payload.clone()).unwrap_or_default();
    }

    pub fn send_message(&mut self, message: String) {
        self.message.payload = message.as_bytes().to_vec()
    }

    pub fn add_link(&mut self, link: &BTPAddress) {
        assert!(
            self.owners.is_owner(env::signer_account_id()),
            "BMCRevertUnauthorized"
        );

        self.links
            .add_link(link)
            .expect_err("BMCRevertUnauthorized");
    }
    pub fn add_routes(&mut self, dst: &BTPAddress, link: &BTPAddress) {
        self.routes
            .add_route(dst, link)
            .expect_err("Not able to add route");
    }

    pub fn remove_route(&mut self, dst: &BTPAddress) {
        assert!(
            self.owners.is_owner(env::signer_account_id()),
            "BMCRevertUnauthorized"
        );
        self.routes.remove_route(dst).expect_err("Failed to remove");
    }

    pub fn add_owner(&mut self, address: &AccountId) {
        assert!(
            self.owners.is_owner(env::signer_account_id()),
            "BMCRevertUnauthorized"
        );

        self.owners
            .add_owner(address)
            .expect_err("failed to add owner");
    }
}
#[cfg(test)]
mod tests {
    use super::*;
    use near_sdk::MockedBlockchain;
    use near_sdk::{testing_env, VMContext};

    fn get_context(input: Vec<u8>, is_view: bool) -> VMContext {
        VMContext {
            current_account_id: "alice.testnet".to_string(),
            signer_account_id: "robert.testnet".to_string(),
            signer_account_pk: vec![0, 1, 2],
            predecessor_account_id: "jane.testnet".to_string(),
            input,
            block_index: 0,
            block_timestamp: 0,
            account_balance: 0,
            account_locked_balance: 0,
            storage_usage: 0,
            attached_deposit: 0,
            prepaid_gas: 10u64.pow(18),
            random_seed: vec![0, 1, 2],
            is_view,
            output_data_receivers: vec![],
            epoch_height: 19,
        }
    }

    #[test]
    fn send_message() {
        let context = get_context(vec![], false);
        testing_env!(context);
        let mut contract = BTPMessageCenter {
            ..Default::default()
        };
        let mut s = String::from("");
        contract.send_message("dddddd".to_string());
        assert_eq!("dddddd".to_string(), contract.get_message());
    }
}
