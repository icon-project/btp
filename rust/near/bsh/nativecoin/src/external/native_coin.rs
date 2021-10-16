use libraries::types::TokenId;
use near_sdk::json_types::U128;
use near_sdk::{ext_contract};

#[ext_contract(ext_self)]
pub trait NativeCoinServiceContract {    
    fn mt_resolve_transfer(
        &mut self,
        sender_id: AccountId,
        receiver_id: AccountId,
        token_ids: Vec<TokenId>,
        amounts: Vec<U128>,
    ) -> Vec<U128>;
}
