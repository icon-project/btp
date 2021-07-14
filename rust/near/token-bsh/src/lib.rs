//! Token BSH Contract

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

use bsh_generic::other_bsh_types::*;

//use btp_common::BTPAddress;
use bsh_generic::BshGeneric;
use near_sdk::borsh::{self, BorshDeserialize, BorshSerialize};
use near_sdk::{env, metadata, near_bindgen, setup_alloc};
use std::collections::HashMap;

setup_alloc!();
metadata! {
    /// Token BSH contract.
    /// This contract is used to handle coin transferring service.
    /// The coin can be native, or wrapped native.
    /// This struct implements `Default`: https://github.com/near/near-sdk-rs#writing-rust-contract
    #[near_bindgen]
    #[derive(BorshDeserialize, BorshSerialize, Clone, Debug, Default)]
    pub struct TokenBsh {
        owners: HashMap<String, bool>,
        list_of_owners: Vec<String>,
        /// Address of generic BSH contract
        bsh_generic: BshGeneric,
        aggregation_fee: HashMap<String, u64>,
        balances: HashMap<String, HashMap<String, Balance>>,
        coins: HashMap<String, u64>,
        coin_names: Vec<String>,
        charged_coins: Vec<String>,
        charged_amounts: Vec<u64>,
        fee_numerator: u64,
    }
}

#[near_bindgen]
impl TokenBsh {
    pub const FEE_DENOMINATOR: u64 = u64::pow(10, 4);
    pub const RC_OK: usize = 0;
    pub const RC_ERR: usize = 1;

    #[init]
    pub fn new(uri: &str, native_coin_name: &str, fee_numerator: u64) -> Self {
        let mut owners: HashMap<String, bool> = HashMap::new();
        let mut list_of_owners: Vec<String> = vec![];
        let mut coins: HashMap<String, u64> = HashMap::new();
        let mut coin_names: Vec<String> = vec![];
        let fee_numerator = fee_numerator;
        let mut bsh_generic = BshGeneric::default();

        let _ = owners.insert(env::current_account_id(), true);
        list_of_owners.push(env::current_account_id());
        let _ = coins.insert(native_coin_name.to_string(), 0);
        coin_names.push(native_coin_name.to_string());
        bsh_generic.bsh_contract = uri.to_string();

        let bsh_event = BshEvents::SetOwnership {
            promoter: &env::predecessor_account_id(),
            new_owner: &env::current_account_id(),
        };
        let bsh_event = bsh_event
            .try_to_vec()
            .expect("Failed to serialize bsh event");
        env::log(&bsh_event);

        Self {
            owners,
            list_of_owners,
            bsh_generic,
            aggregation_fee: HashMap::new(),
            balances: HashMap::new(),
            coins,
            coin_names,
            charged_coins: vec![],
            charged_amounts: vec![],
            fee_numerator,
        }
    }

    /// Add another owner.
    /// Caller must be an owner of BTP network
    pub fn add_owner(&mut self, owner: &str) {
        assert!(
            self.owners[&env::current_account_id()] == true,
            "Unauthorized"
        );
        assert!(self.owners[owner] == false, "ExistedOwner");
        let _ = self.owners.insert(owner.to_string(), true);
        self.list_of_owners.push(owner.to_string());
        let bsh_event = BshEvents::SetOwnership {
            promoter: &env::current_account_id(),
            new_owner: owner,
        };
        let bsh_event = bsh_event
            .try_to_vec()
            .expect("Failed to serialize bsh event");
        env::log(&bsh_event);
    }

    /// Remove an existing owner.
    /// Caller must be an owner of BTP network
    pub fn remove_owner(&mut self, owner: &str) {
        assert!(
            self.owners[&env::current_account_id()] == true,
            "Unauthorized"
        );
        assert!(self.list_of_owners.len() > 1, "Unable to remove last owner");
        assert_eq!(self.owners[owner], true, "Removing owner not found");
        let _ = self.owners.remove(owner);
        self.remove(owner);
        let bsh_event = BshEvents::RemoveOwnership {
            remover: &env::current_account_id(),
            former_owner: owner,
        };
        let bsh_event = bsh_event
            .try_to_vec()
            .expect("Failed to serialize bsh event");
        env::log(&bsh_event);
    }

    fn remove(&mut self, addr: &str) {
        for i in 0..self.list_of_owners.len() {
            if self.list_of_owners[i] == addr.to_string() {
                self.list_of_owners[i] = self.list_of_owners[self.list_of_owners.len() - 1].clone();
                let _ = self.list_of_owners.pop();
                break;
            }
        }
    }

    /// Check whether one specific address has `Owner` role.
    /// Anyone can call this function.
    /// Address needs to be verified.
    pub fn is_owner(&self, owner: &str) -> bool {
        self.owners[owner]
    }

    /// Get a list of current owners.
    /// Anyone can call this function.
    /// Returns an array of addresses.
    pub fn get_owners(&self) -> &Vec<String> {
        &self.list_of_owners
    }

    /// Update generic BSH address.
    /// Caller must be an owner of this contract.
    /// `address` must be different from the existing BSH generic contract address.
    pub fn update_generic_bsh_addr(&mut self, addr: &str) {
        assert!(
            self.owners[&env::current_account_id()] == true,
            "Unauthorized"
        );
        assert_ne!(
            addr.to_string(),
            env::predecessor_account_id(),
            "InvalidSetting"
        );
        if self.bsh_generic.bsh_contract != env::predecessor_account_id() {
            assert!(
                self.bsh_generic.has_pending_requests() == false,
                "HasPendingRequest"
            );
        }
        self.bsh_generic.bsh_contract = addr.to_string();
    }

    /// Update base URI.
    /// Caller must be an owner of this contract.
    /// The URI must be initialized in construction.
    pub fn update_uri(&mut self, _new_uri: &str) {
        assert!(
            self.owners[&env::current_account_id()] == true,
            "Unauthorized"
        );
        // TODO: set_uri(_new_uri);
    }

    /// Set fee ratio.
    /// Caller must be an owner of this contract.
    /// The transfer fee is calculated as fee_numerator / FEE_DEMONINATOR.
    /// The fee_numetator should be less than FEE_DEMONINATOR.
    /// fee_numerator is set to `10` in construction by default, which means the default fee ratio is 0.1%.
    pub fn set_fee_ratio(&mut self, fee_numerator: u64) {
        assert!(
            self.owners[&env::current_account_id()] == true,
            "Unauthorized"
        );
        assert!(fee_numerator <= Self::FEE_DENOMINATOR, "InvalidSetting");
        self.fee_numerator = fee_numerator;
    }

    /// Register a wrapped coin and ID number of a supporting coin.
    /// Caller must be an owner of this contract.
    /// `name` must be different from the native coin name.
    /// ID of a wrapped coin is generated by using keccak256.
    /// ID = 0 is fixed to assign to native coin.
    pub fn register(&mut self, name: &str) {
        assert!(
            self.owners[&env::current_account_id()] == true,
            "Unauthorized"
        );
        assert!(self.coins[name] == 0, "TokenExists");
        let name_bytes = env::keccak256(name.as_bytes());
        let name_ptr = name_bytes.as_ptr() as u64;
        let _ = self.coins.insert(name.to_string(), name_ptr);
        self.coin_names.push(name.to_string());
    }

    /// Return all supported coin names.
    /// Returns an array of strings.
    pub fn get_coin_names(&self) -> &Vec<String> {
        &self.coin_names
    }

    /// Return an ID number of the given coin name.
    /// Return `None` if nothing found.
    pub fn get_coin_id(&self, coin_name: &str) -> Option<&u64> {
        self.coins.get(coin_name)
    }

    /// Check validity of a coin name.
    /// Call generic BSH contract to validate a requested coin name.
    pub fn is_valid_coin(&self, coin_name: &str) -> bool {
        self.coins[coin_name] != 0 || coin_name.to_string() == self.coin_names[0]
    }

    /// Return a usable/locked/refundable balance of an account based on the coin name.
    /// [] - usable_balance: what users are holding.
    /// [] - locked_balance: when users transfer the coin, it will be locked until
    ///      service message response is received.
    /// [] - refundable_balance: what will be refunded to users.
    pub fn get_balance_of(&self, owner: &str, coin_name: &str) -> (u64, u64, u64) {
        todo!()
    }

    /// Return a list of balances in an account.
    /// The order of coin names must match requested balances.
    /// Return 0 if nothing found.
    /// [] - usable_balances: an array of usable balances.
    /// [] - locked_balances: an array of locked balances.
    /// [] - refundable_balances: an array of refundable balances.
    pub fn get_balance_of_batch(
        owner: &str,
        coin_names: &[&str],
    ) -> (Vec<u64>, Vec<u64>, Vec<u64>) {
        todo!()
    }

    /// Return a list of accumulated fees.
    /// Only return an asset if it has a value greater than 0.
    /// Returns an array of assets.
    pub fn get_accumulated_fees() -> Vec<Asset> {
        todo!()
    }

    /// Allow users to deposit an amount of native coin into the contract.
    /// The amount must be specified.
    #[payable]
    pub fn transfer(to: &str) {
        todo!()
    }

    /// Allow users to deposit an amount of wrapped native coin into the contract.
    /// Caller must set condition to approve transfer of wrapped tokens out of source account.
    /// Revert if balance of source account is less than specified transfer amount.
    pub fn transfer_to(coin_name: &str, value: u64, to: &str) {
        todo!()
    }

    fn send_service_message(from: &str, to: &str, coin_name: &str, value: u64, charge_amt: u64) {
        todo!()
    }

    /// Allow users to transfer multiple coins/wrapped coins to another chain.
    /// Caller must set condition to approve transfer of wrapped tokens out of source account.
    /// Revert if balance of source account is less than specified transfer amount.
    #[payable]
    pub fn transfer_batch(coin_names: &[&str], values: &[u64], to: &[&str]) {
        todo!()
    }

    /// Reclaim the token's refundable balance.
    /// Caller must be an owner of coin.
    /// The amount to claim must be less than or equal to the refundable balance.
    pub fn reclaim(coin_name: &str, value: u64) {
        todo!()
    }

    /// Return coin for the failed transfer.
    /// Caller must be itself.
    pub fn refund(to: &str, coin_name: &str, value: u64) {
        todo!()
    }

    /// Mint the wrapped coin.
    /// Caller must be a generic BSH contract.
    /// Generic BSH contract must check validity of requested coin name.
    pub fn mint(to: &str, coin_name: &str, value: u64) {
        todo!()
    }

    /// Handle response of a requested service.
    /// Caller must be a generic BSH contract.
    pub fn handle_response_service(
        requester: &str,
        coin_name: &str,
        value: u64,
        fee: u64,
        rsp_code: u64,
    ) {
        todo!()
    }

    /// Handle a request of fee gathering.
    /// Caller must be a generic BSH contract.
    /// `fa`: BTP address of fee aggregator.
    pub fn transfer_fees(fa: &str) {
        todo!()
    }

    fn lock_balance(to: &str, coin_name: &str, value: u64) {
        todo!()
    }
}
