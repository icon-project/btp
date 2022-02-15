use libraries::types::AssetId;
use near_sdk::ext_contract;
use near_sdk::json_types::U128;

#[ext_contract(ext_receiver)]
pub trait MultiTokenReceiver {
    fn mt_on_transfer(
        &mut self,
        sender_id: AccountId,
        coin_ids: Vec<AssetId>,
        amounts: Vec<U128>,
        msg: String,
    ) -> PromiseOrValue<Vec<U128>>;
}
