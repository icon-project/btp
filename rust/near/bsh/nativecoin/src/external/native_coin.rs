use crate::BTPAddress;
use libraries::types::TokenId;
use near_sdk::json_types::U128;
use near_sdk::{ext_contract, AccountId};

#[ext_contract(native_coin_service_contract)]
pub trait NativeCoinServiceContract {
    fn internal_transfer(fee_aggregator: BTPAddress, service: String);

    fn mt_transfer(
        &mut self,
        receiver_id: AccountId,
        token_id: TokenId,
        amount: U128,
        memo: Option<String>,
    );
    
    fn mt_resolve_transfer(
        &mut self,
        sender_id: AccountId,
        receiver_id: AccountId,
        token_ids: Vec<TokenId>,
        amounts: Vec<U128>,
    ) -> Vec<U128>;
}
