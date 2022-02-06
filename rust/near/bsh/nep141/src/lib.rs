use near_contract_standards::fungible_token::metadata::{
    FungibleTokenMetadata, FungibleTokenMetadataProvider,
};
use near_contract_standards::fungible_token::FungibleToken;
use near_sdk::borsh::{self, BorshDeserialize, BorshSerialize};
use near_sdk::collections::LazyOption;
use near_sdk::json_types::U128;
use near_sdk::{
    env, log, near_bindgen, require, AccountId, Balance, BorshStorageKey, PanicOnDefault,
    PromiseOrValue,
};

#[near_bindgen]
#[derive(BorshDeserialize, BorshSerialize, PanicOnDefault)]
pub struct Contract {
    token: FungibleToken,
    metadata: LazyOption<FungibleTokenMetadata>,
    owner: AccountId,
}

#[derive(BorshSerialize, BorshStorageKey)]
enum StorageKey {
    FungibleToken,
    Metadata,
}

#[near_bindgen]
impl Contract {
    /// Initializes the contract with the given total supply owned by the given `owner_id` with
    /// the given fungible token metadata.
    #[init]
    pub fn new(owner_id: AccountId, total_supply: U128, metadata: FungibleTokenMetadata) -> Self {
        require!(!env::state_exists(), "Already initialized");
        metadata.assert_valid();
        let mut this = Self {
            token: FungibleToken::new(StorageKey::FungibleToken),
            metadata: LazyOption::new(StorageKey::Metadata, Some(&metadata)),
            owner: owner_id.clone(),
        };
        this.token.internal_register_account(&owner_id);
        this.token.internal_deposit(&owner_id, total_supply.into());

        this
    }

    pub fn mint(&mut self, amount: U128) {
        require!(env::predecessor_account_id() == self.owner);
        self.token
            .internal_deposit(&env::predecessor_account_id(), amount.into())
    }

    pub fn burn(&mut self, amount: U128) {
        require!(env::predecessor_account_id() == self.owner);
        self.token
            .internal_withdraw(&env::predecessor_account_id(), amount.into())
    }

    pub fn ft_transfer_call_with_storage_check(
        &mut self,
        receiver_id: AccountId,
        amount: Balance,
        memo: Option<String>,
    ) {
        require!(env::predecessor_account_id() == self.owner);
        if let None = self.storage_balance_of(receiver_id.clone()) {
            self.token.internal_register_account(&receiver_id);
        }
        
        self.token.internal_transfer(
            &env::predecessor_account_id(),
            &receiver_id.clone(),
            amount,
            memo,
        )
    }

    fn on_account_closed(&mut self, account_id: AccountId, balance: Balance) {
        log!("Closed @{} with {}", account_id, balance);
    }

    fn on_tokens_burned(&mut self, account_id: AccountId, amount: Balance) {
        log!("Account @{} burned {}", account_id, amount);
    }
}

near_contract_standards::impl_fungible_token_core!(Contract, token, on_tokens_burned);
near_contract_standards::impl_fungible_token_storage!(Contract, token, on_account_closed);

#[near_bindgen]
impl FungibleTokenMetadataProvider for Contract {
    fn ft_metadata(&self) -> FungibleTokenMetadata {
        self.metadata.get().unwrap()
    }
}
