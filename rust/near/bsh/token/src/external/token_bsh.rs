use libraries::types::{AccountBalance, TokenId};
use near_sdk::ext_contract;
use near_sdk::json_types::U128;

#[ext_contract(ext_self)]
pub trait TokenService {
    fn on_mint(
        &mut self,
        amount: u128,
        token_id: TokenId,
        token_symbol: String,
        receiver_id: AccountId
    );

    fn on_withdraw(
        &mut self,
        account: AccountId,
        amount: u128,
        token_id: TokenId,
        token_symbol: String,
    );
}
