use btp_common::errors::{BtpException, Exception};
use near_sdk::borsh::{self, BorshDeserialize, BorshSerialize};
use near_sdk::{
    env,
    json_types::{Base64VecU8, U128, U64},
    log, near_bindgen, require, serde_json, Gas, PanicOnDefault,
};
use libraries::{types::Owners, types::Tokens, types::FungibleToken, types::Balances, types::Network};
use near_sdk::AccountId;

#[near_bindgen]
#[derive(BorshDeserialize, BorshSerialize, PanicOnDefault)]
pub struct TokenService {
    network: Network,
    base_service: AccountId,
    bmc: AccountId,
    owners: Owners,
    tokens: Tokens<FungibleToken>,
    balances: Balances
}

#[near_bindgen]
impl TokenService {
    #[init]
    pub fn new(bmc: AccountId, base_service: AccountId, network: Network) -> Self {
        let mut owners = Owners::new();
        owners.add(&env::current_account_id());
        Self {
            network,
            owners,
            base_service,
            bmc,
            tokens: <Tokens<FungibleToken>>::new(),
            balances: Balances::new()
        }
    }

    pub fn register(){}
}