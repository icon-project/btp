use near_sdk::{ext_contract, AccountId};
use libraries::types::{TokenId};
use near_sdk::json_types::U128;

#[ext_contract(ext_receiver)]
pub trait MultiTokenReceiver {
    fn mt_on_transfer(
        &mut self,
        sender_id: AccountId,
        token_ids: Vec<TokenId>,
        amounts: Vec<U128>,
        msg: String,
    ) -> PromiseOrValue<Vec<U128>>;
}