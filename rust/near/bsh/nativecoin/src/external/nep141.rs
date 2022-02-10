use near_sdk::json_types::U128;
use near_sdk::{ext_contract};
use near_contract_standards::fungible_token::metadata::{FungibleTokenMetadata};


#[ext_contract(ext_nep141)]
pub trait Nep141Service { 
    fn new(owner_id: AccountId, total_supply: U128, metadata: FungibleTokenMetadata);
    fn ft_transfer_call_with_storage_check(&mut self, receiver_id:AccountId,amount: Balance,memo:Option<String>);
    fn mint(&mut self,amount:U128);
    fn burn(&mut self, amount: U128);
}
