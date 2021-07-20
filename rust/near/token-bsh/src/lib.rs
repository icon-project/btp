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

use bsh_generic::BshGeneric;
use btp_common::BTPAddress;
use near_sdk::borsh::{self, BorshDeserialize, BorshSerialize};
use near_sdk::collections::UnorderedMap;
use near_sdk::{env, metadata, near_bindgen, setup_alloc, BorshStorageKey};

#[derive(BorshSerialize, BorshStorageKey)]
enum TokenBshKey {
    TokenBsh,
}

setup_alloc!();
metadata! {
    /// Token BSH contract.
    /// This contract is used to handle coin transferring service.
    /// The coin can be native, or wrapped native.
    /// This struct implements `Default`: https://github.com/near/near-sdk-rs#writing-rust-contract
    #[near_bindgen]
    #[derive(BorshDeserialize, BorshSerialize)]
    pub struct TokenBsh {
        owners: UnorderedMap<String, bool>,
        list_of_owners: Vec<String>,
        bsh_generic: BshGeneric,
        aggregation_fee: UnorderedMap<String, u128>,
        // mapping from account ID to coin balances
        // first string refers to account ID
        // second string refers to coin name
        coin_balances: UnorderedMap<String, UnorderedMap<String, Balance>>,
        // mapping from coin ID to account balances
        account_balances: UnorderedMap<u64, UnorderedMap<String, u128>>,
        // map of coin name to coin ID
        coins: UnorderedMap<String, u64>,
        coin_names: Vec<String>,
        charged_coins: Vec<String>,
        charged_amounts: Vec<u128>,
        fee_numerator: u128,
    }
}

impl Default for TokenBsh {
    fn default() -> Self {
        Self {
            owners: UnorderedMap::new(TokenBshKey::TokenBsh),
            list_of_owners: vec![],
            bsh_generic: BshGeneric::default(),
            aggregation_fee: UnorderedMap::new(TokenBshKey::TokenBsh),
            coin_balances: UnorderedMap::new(TokenBshKey::TokenBsh),
            account_balances: UnorderedMap::new(TokenBshKey::TokenBsh),
            coins: UnorderedMap::new(TokenBshKey::TokenBsh),
            coin_names: vec![],
            charged_coins: vec![],
            charged_amounts: vec![],
            fee_numerator: 0,
        }
    }
}

#[near_bindgen]
impl TokenBsh {
    pub const FEE_DENOMINATOR: u128 = u128::pow(10, 4);
    pub const RC_OK: usize = 0;
    pub const RC_ERR: usize = 1;

    #[init]
    pub fn new(native_coin_name: &str, fee_numerator: u128) -> Self {
        let mut owners: UnorderedMap<String, bool> = UnorderedMap::new(TokenBshKey::TokenBsh);
        let list_of_owners: Vec<String> = vec![env::current_account_id()];
        let mut coins: UnorderedMap<String, u64> = UnorderedMap::new(TokenBshKey::TokenBsh);
        let coin_names: Vec<String> = vec![native_coin_name.to_string()];

        let _ = owners.insert(&env::current_account_id(), &true);
        let _ = coins.insert(&native_coin_name.to_string(), &0);

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
            bsh_generic: BshGeneric::default(),
            aggregation_fee: UnorderedMap::new(TokenBshKey::TokenBsh),
            coin_balances: UnorderedMap::new(TokenBshKey::TokenBsh),
            account_balances: UnorderedMap::new(TokenBshKey::TokenBsh),
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
        assert!(is_valid_btp_address(owner), "Invalid BTP address");
        assert!(
            self.owners
                .get(&env::current_account_id())
                .expect("Error: "),
            "Unauthorized"
        );
        assert!(
            !self.owners.get(&owner.to_string()).expect("Error: "),
            "Owner already exists"
        );
        let _ = self.owners.insert(&owner.to_string(), &true);
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
        assert!(is_valid_btp_address(owner), "Invalid BTP address");
        assert!(
            self.owners
                .get(&env::current_account_id())
                .expect("Error: "),
            "Unauthorized"
        );
        assert!(self.list_of_owners.len() > 1, "Unable to remove last owner");
        assert!(
            self.owners.get(&owner.to_string()).expect("Error: "),
            "Owner not found"
        );
        let _ = self.owners.remove(&owner.to_string());
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
        assert!(is_valid_btp_address(addr), "Invalid BTP address");
        for i in 0..self.list_of_owners.len() {
            if self.list_of_owners[i] == *addr {
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
        assert!(is_valid_btp_address(owner), "Invalid BTP address");
        self.owners
            .get(&owner.to_string())
            .expect("Owner lookup error")
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
        assert!(is_valid_btp_address(addr), "Invalid BTP address");
        assert!(
            self.owners
                .get(&env::current_account_id())
                .expect("Error: "),
            "Unauthorized"
        );
        assert_ne!(
            addr.to_string(),
            env::predecessor_account_id(),
            "InvalidSetting"
        );
        if self.bsh_generic.get_contract_address() != env::predecessor_account_id() {
            assert!(
                !self.bsh_generic.has_pending_requests(),
                "HasPendingRequest"
            );
        }
        self.bsh_generic.set_contract_address(addr);
    }

    /// Set fee ratio.
    /// Caller must be an owner of this contract.
    /// The transfer fee is calculated as fee_numerator / FEE_DEMONINATOR.
    /// The fee_numetator should be less than FEE_DEMONINATOR.
    /// fee_numerator is set to `10` in construction by default, which means the default fee ratio is 0.1%.
    pub fn set_fee_ratio(&mut self, fee_numerator: u128) {
        assert!(
            self.owners
                .get(&env::current_account_id())
                .expect("Error: "),
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
            self.owners
                .get(&env::current_account_id())
                .expect("Error: "),
            "Unauthorized"
        );
        assert!(
            self.coins.get(&name.to_string()).expect("Error: ") == 0,
            "TokenExists"
        );
        let name_bytes = env::keccak256(name.as_bytes());
        let id = u64::try_from_slice(name_bytes.as_slice()).expect("Error in conversion");
        let _ = self.coins.insert(&name.to_string(), &id);
        self.coin_names.push(name.to_string());
    }

    /// Return all supported coin names.
    /// Returns an array of strings.
    pub fn get_coin_names(&self) -> &Vec<String> {
        &self.coin_names
    }

    /// Return an ID number of the given coin name.
    /// Return `None` if nothing found.
    pub fn get_coin_id(&self, coin_name: &str) -> u64 {
        self.coins
            .get(&coin_name.to_string())
            .expect("Failed to get coin ID")
    }

    /// Check validity of a coin name.
    /// Call generic BSH contract to validate a requested coin name.
    pub fn is_valid_coin(&self, coin_name: &str) -> bool {
        self.coins
            .get(&coin_name.to_string())
            .expect("Failed to retrieve coin ID")
            != 0
            || *coin_name != self.coin_names[0]
    }

    /// Return a usable/locked/refundable balance of an account based on the coin name.
    /// [] - usable_balance: what users are holding.
    /// [] - locked_balance: when users transfer the coin, it will be locked until
    ///      service message response is received.
    /// [] - refundable_balance: what will be refunded to users.
    pub fn get_balance_of(&self, owner: &str, coin_name: &str) -> (u128, u128, u128) {
        assert!(is_valid_btp_address(owner), "Invalid BTP address");
        let (locked_balance, refundable_balance) = self.get_coin_balances(owner, coin_name);

        if *coin_name == self.coin_names[0] {
            return (env::account_balance(), locked_balance, refundable_balance);
        }
        let coin_id = self
            .coins
            .get(&coin_name.to_string())
            .expect("Error in coin name lookup");
        (
            self.balance_of(owner, coin_id),
            locked_balance,
            refundable_balance,
        )
    }

    /// Return a list of balances in an account.
    /// The order of coin names must match requested balances.
    /// Return 0 if nothing found.
    /// [] - usable_balances: an array of usable balances.
    /// [] - locked_balances: an array of locked balances.
    /// [] - refundable_balances: an array of refundable balances.
    pub fn get_balance_of_batch(
        &self,
        owner: &str,
        coin_names: &[&str],
    ) -> (Vec<u128>, Vec<u128>, Vec<u128>) {
        assert!(is_valid_btp_address(owner), "Invalid BTP address");
        let mut usable_balances: Vec<u128> = Vec::with_capacity(coin_names.len());
        let mut locked_balances: Vec<u128> = Vec::with_capacity(coin_names.len());
        let mut refundable_balances: Vec<u128> = Vec::with_capacity(coin_names.len());
        for i in 0..coin_names.len() {
            let (usable_bal, locked_bal, refundable_bal) =
                self.get_balance_of(owner, coin_names[i]);
            usable_balances.push(usable_bal);
            locked_balances.push(locked_bal);
            refundable_balances.push(refundable_bal);
        }
        (usable_balances, locked_balances, refundable_balances)
    }

    fn balance_of(&self, account: &str, coin_id: u64) -> u128 {
        assert!(is_valid_btp_address(account), "Invalid BTP address");
        assert!(
            *account != env::predecessor_account_id(),
            "Balance query for the zero address"
        );
        self.account_balances
            .get(&coin_id)
            .expect("Error in coin ID lookup")
            .get(&account.to_string())
            .expect("Error in account lookup")
    }

    /// Return a list of accumulated fees.
    /// Only return an asset if it has a value greater than 0.
    /// Returns an array of assets.
    pub fn get_accumulated_fees(&self) -> Vec<Asset> {
        let mut accumulated_fees: Vec<Asset> = Vec::with_capacity(self.coin_names.len());
        for i in 0..self.coin_names.len() {
            let asset = Asset {
                coin_name: self.coin_names[i].clone(),
                value: self
                    .aggregation_fee
                    .get(&self.coin_names[i])
                    .expect("Failed to get value"),
            };
            accumulated_fees.push(asset);
        }
        accumulated_fees
    }

    /// Allow users to deposit an amount of native coin into the contract.
    /// The amount must be specified.
    #[payable]
    pub fn transfer(&mut self, to: &str) {
        assert!(is_valid_btp_address(to), "Invalid BTP address");
        let charge_amt = env::attached_deposit()
            .checked_mul(self.fee_numerator)
            .expect("Failed to safely multiply")
            .checked_div(Self::FEE_DENOMINATOR)
            .expect("Failed to safely divide");
        assert!(charge_amt > 0, "InvalidAmount");
        self.send_service_message(
            &env::signer_account_id(),
            to,
            &self.coin_names[0].clone(),
            env::attached_deposit(),
            charge_amt,
        );
    }

    fn safe_transfer_from(&mut self, from: &str, to: &str, coin_name: &str, amount: u128) {
        assert!(is_valid_btp_address(from), "Invalid BTP address");
        assert!(is_valid_btp_address(to), "Invalid BTP address");
        let coin_id = self
            .coins
            .get(&coin_name.to_string())
            .expect("Error in coin name lookup");
        let from_balance = self
            .account_balances
            .get(&coin_id)
            .expect("Error in coin ID lookup")
            .get(&from.to_string())
            .expect("Error in account lookup");
        let to_balance = self
            .account_balances
            .get(&coin_id)
            .expect("Error in coin ID lookup")
            .get(&to.to_string())
            .expect("Error in account lookup");

        assert!(from_balance >= amount, "Insufficient balance for transfer");

        let mut bals = self
            .account_balances
            .get(&coin_id)
            .expect("Error in coin ID lookup");
        let _ = bals.insert(&from.to_string(), &(from_balance - amount));
        let _ = bals.insert(&to.to_string(), &(to_balance + amount));
        let _ = self.account_balances.insert(&coin_id, &bals);
    }

    /// Allow users to deposit an amount of wrapped native coin into the contract.
    /// Caller must set condition to approve transfer of wrapped tokens out of source account.
    /// Revert if balance of source account is less than specified transfer amount.
    pub fn transfer_to_contract(&mut self, coin_name: &str, value: u128, to: &str) {
        assert!(is_valid_btp_address(to), "Invalid BTP address");
        assert!(
            self.coins
                .get(&coin_name.to_string())
                .expect("Failed to retrieve coin ID")
                != 0,
            "UnregisteredCoin"
        );
        let charge_amt = value
            .checked_mul(self.fee_numerator)
            .expect("Failed to safely multiply")
            .checked_div(Self::FEE_DENOMINATOR)
            .expect("Failed to safely divide");
        assert!(charge_amt > 0, "InvalidAmount");
        self.safe_transfer_from(
            &env::signer_account_id(),
            &env::current_account_id(),
            coin_name,
            value,
        );
        self.send_service_message(&env::signer_account_id(), to, coin_name, value, charge_amt);
    }

    fn send_service_message(
        &mut self,
        from: &str,
        to: &str,
        coin_name: &str,
        value: u128,
        charge_amt: u128,
    ) {
        assert!(is_valid_btp_address(from), "Invalid BTP address");
        assert!(is_valid_btp_address(to), "Invalid BTP address");
        self.lock_balance(from, coin_name, value);
        let coins: Vec<String> = vec![coin_name.to_string()];
        let amounts: Vec<u128> = vec![value
            .checked_sub(charge_amt)
            .expect("Failed to safely subtract")];
        let fees: Vec<u128> = vec![charge_amt];
        let _ = self
            .bsh_generic
            .send_service_message(from, to, coins, amounts, fees);
    }

    /// Allow users to transfer multiple coins/wrapped coins to another chain.
    /// Caller must set condition to approve transfer of wrapped tokens out of source account.
    /// Revert if balance of source account is less than specified transfer amount.
    #[payable]
    pub fn transfer_batch(&mut self, coin_names: &[String], values: &[u128], to: &str) {
        assert!(is_valid_btp_address(to), "Invalid BTP address");
        assert!(coin_names.len() == values.len(), "InvalidRequest");
        let size = coin_names.len();
        let mut amounts: Vec<u128> = Vec::with_capacity(size);
        let mut charge_amts: Vec<u128> = Vec::with_capacity(size);
        for i in 0..size {
            charge_amts.push(
                values[i]
                    .checked_mul(self.fee_numerator)
                    .expect("Failed to safely multiply")
                    .checked_div(Self::FEE_DENOMINATOR)
                    .expect("Failed to safely divide"),
            );
            if coin_names[i] == self.coin_names[i] {
                assert!(
                    charge_amts[i] > 0 && values[i] == env::attached_deposit(),
                    "InvalidAmount"
                );
            } else {
                let id = self
                    .coins
                    .get(&coin_names[i].to_string())
                    .expect("Failed to retrieve coin ID");
                assert!(id != 0, "UnregisteredCoin");
                assert!(charge_amts[i] > 0, "InvalidAmount");
                self.safe_transfer_from(
                    &env::signer_account_id(),
                    &env::current_account_id(),
                    &coin_names[i],
                    values[i],
                );
            }
            amounts.push(
                values[i]
                    .checked_sub(charge_amts[i])
                    .expect("Failed to safely subtract"),
            );
            self.lock_balance(&env::signer_account_id(), &coin_names[i], values[i]);
        }
        let _ = self.bsh_generic.send_service_message(
            &env::signer_account_id(),
            to,
            coin_names.to_vec(),
            amounts,
            charge_amts,
        );
    }

    /// Reclaim the token's refundable balance.
    /// Caller must be an owner of coin.
    /// The amount to claim must be less than or equal to the refundable balance.
    pub fn reclaim(&mut self, coin_name: &str, value: u128) {
        let (locked_balance, refundable_balance) =
            self.get_coin_balances(&env::signer_account_id(), coin_name);
        assert!(refundable_balance >= value, "Imbalance");
        let balance = Balance {
            locked_balance,
            refundable_balance,
        };
        let _ = balance
            .refundable_balance
            .checked_sub(value)
            .expect("Failed to safely subtract");

        let mut bals = self
            .coin_balances
            .get(&env::signer_account_id())
            .expect("Error in account lookup");
        let _ = bals.insert(&coin_name.to_string(), &balance);

        let _ = self.coin_balances.insert(&coin_name.to_string(), &bals);
        self.refund(&env::signer_account_id(), coin_name, value)
            .expect("Failed to refund");
    }

    /// Return coin for the failed transfer.
    /// Caller must be itself.
    pub fn refund(&mut self, to: &str, coin_name: &str, value: u128) -> Result<(), &str> {
        assert!(is_valid_btp_address(to), "Invalid BTP address");
        assert!(
            env::signer_account_id() == env::current_account_id(),
            "Unauthorized"
        );
        let id = self
            .coins
            .get(&coin_name.to_string())
            .expect("Failed to get coin ID");
        if id == 0 {
            self.transfer_to_contract(coin_name, value, to);
            return Ok(());
        } else {
            self.safe_transfer_from(&env::current_account_id(), to, coin_name, value);
        }
        Ok(())
    }

    /// Mint the wrapped coin.
    /// Caller must be a generic BSH contract.
    /// Generic BSH contract must check validity of requested coin name.
    pub fn mint(&mut self, to: &str, coin_name: &str, value: u128) {
        assert!(is_valid_btp_address(to), "Invalid BTP address");
        let bsh_generic_addr = self.bsh_generic.get_contract_address();
        assert!(env::signer_account_id() == bsh_generic_addr, "Unauthorized");
        let id = self
            .coins
            .get(&coin_name.to_string())
            .expect("Failed to get coin ID");
        if id == 0 {
            self.transfer_to_contract(coin_name, value, to);
        } else {
            let mut coin = self
                .account_balances
                .get(&id)
                .expect("Failed to get coin info");
            let balance = coin
                .get(&coin_name.to_string())
                .expect("Error in retrieving balance");
            let _ = coin.insert(&coin_name.to_string(), &(balance + value));

            let _ = self.account_balances.insert(&id, &coin);
        }
    }

    /// Handle response of a requested service.
    /// Caller must be a generic BSH contract.
    pub fn handle_response_service(
        &mut self,
        requester: &str,
        coin_name: &str,
        value: u128,
        fee: u128,
        rsp_code: u128,
    ) -> Result<(), &str> {
        assert!(is_valid_btp_address(requester), "Invalid BTP address");
        let bsh_generic_addr = self.bsh_generic.get_contract_address();
        assert!(env::signer_account_id() == bsh_generic_addr, "Unauthorized");
        if *requester == env::current_account_id() {
            if rsp_code == Self::RC_ERR as u128 {
                let agg_fee = self
                    .aggregation_fee
                    .get(&coin_name.to_string())
                    .expect("Error in lookup");
                let _ = self.aggregation_fee.insert(
                    &coin_name.to_string(),
                    &(agg_fee.checked_add(value).expect("Failed to safely add")),
                );
            }
            return Ok(());
        }
        let amount = value.checked_add(fee).expect("Failed to safely add");

        let (locked_balance, refundable_balance) = self.get_coin_balances(requester, coin_name);

        let balance = Balance {
            locked_balance,
            refundable_balance,
        };
        let _ = balance
            .locked_balance
            .checked_sub(amount)
            .expect("Failed to safely subtract");
        let mut coin = self
            .coin_balances
            .get(&requester.to_string())
            .expect("Error in account lookup");
        let _ = coin.insert(&coin_name.to_string(), &balance);
        let _ = self.coin_balances.insert(&requester.to_string(), &coin);

        if rsp_code == Self::RC_ERR as u128 {
            if self.refund(requester, coin_name, amount).is_err() {
                let _ = balance
                    .refundable_balance
                    .checked_add(amount)
                    .expect("Failed to safely add");
                let _ = coin.insert(&coin_name.to_string(), &balance);
                let _ = self.coin_balances.insert(&requester.to_string(), &coin);
            }
        } else if rsp_code == Self::RC_OK as u128 {
            let id = self
                .coins
                .get(&coin_name.to_string())
                .expect("Failed to retrieve coin ID");
            if id != 0 {
                self.burn(&env::signer_account_id(), coin_name, value);
            }
            let agg_fee = self
                .aggregation_fee
                .get(&coin_name.to_string())
                .expect("Error in lookup");
            let _ = self.aggregation_fee.insert(
                &coin_name.to_string(),
                &(agg_fee.checked_add(fee).expect("Failed to safely add")),
            );
        }

        Ok(())
    }

    fn burn(&mut self, account: &str, coin_name: &str, amount: u128) {
        assert!(is_valid_btp_address(account), "Invalid BTP address");
        assert!(
            account != env::current_account_id(),
            "Burn from zero address not allowed"
        );
        let id = self
            .coins
            .get(&coin_name.to_string())
            .expect("Failed to retrieve coin ID");
        let mut coin = self
            .account_balances
            .get(&id)
            .expect("Error in account lookup");
        let account_balance = coin
            .get(&coin_name.to_string())
            .expect("Error in coin name lookup");
        assert!(account_balance >= amount, "Burn amount exceeds balance");
        let _ = coin.insert(&coin_name.to_string(), &(account_balance - amount));
        let _ = self.account_balances.insert(&id, &coin);
    }

    /// Handle a request of fee gathering.
    /// Caller must be a generic BSH contract.
    /// `fa`: BTP address of fee aggregator.
    pub fn transfer_fees(&mut self, fa: &str) {
        assert!(is_valid_btp_address(fa), "Invalid BTP address");
        for i in 0..self.coin_names.len() {
            let aggr_fee = self
                .aggregation_fee
                .get(&self.coin_names[i])
                .expect("Failed to get agrregation fee");
            if aggr_fee != 0 {
                self.charged_coins.push(self.coin_names[i].clone());
                self.charged_amounts.push(aggr_fee);
                let _ = self.aggregation_fee.remove(&self.coin_names[i]);
            }
        }
        let charged_fees: Vec<u128> = Vec::with_capacity(self.charged_coins.len());
        self.bsh_generic
            .send_service_message(
                &env::current_account_id(),
                fa,
                self.charged_coins.clone(),
                self.charged_amounts.clone(),
                charged_fees,
            )
            .expect("Failed to send service message");
        self.charged_coins.clear();
        self.charged_amounts.clear();
    }

    fn lock_balance(&mut self, account: &str, coin_name: &str, value: u128) {
        assert!(is_valid_btp_address(account), "Invalid BTP address");
        let (locked_balance, refundable_balance) = self.get_coin_balances(account, coin_name);

        let balance = Balance {
            locked_balance,
            refundable_balance,
        };
        let _ = balance
            .locked_balance
            .checked_add(value)
            .expect("Failed to safely add");
        let mut coin = self
            .coin_balances
            .get(&account.to_string())
            .expect("Error in account lookup");
        let _ = coin.insert(&coin_name.to_string(), &balance);
        let _ = self.coin_balances.insert(&account.to_string(), &coin);
    }

    /// Return contract address
    pub fn get_contract_address(&self) -> String {
        env::current_account_id()
    }

    fn get_coin_balances(&self, addr: &str, coin_name: &str) -> (u128, u128) {
        let locked_balance = self
            .coin_balances
            .get(&addr.to_string())
            .expect("Error in owner lookup")
            .get(&coin_name.to_string())
            .expect("Error in coin name lookup")
            .locked_balance;
        let refundable_balance = self
            .coin_balances
            .get(&addr.to_string())
            .expect("Error in owner lookup")
            .get(&coin_name.to_string())
            .expect("Error in coin name lookup")
            .refundable_balance;
        (locked_balance, refundable_balance)
    }
}

/// Helper for checking validity of BTP addresses
pub fn is_valid_btp_address(addr: &str) -> bool {
    let btp_addr = BTPAddress(addr.to_string());
    BTPAddress::is_valid(&btp_addr).expect("Failed to validate BTP address")
}
