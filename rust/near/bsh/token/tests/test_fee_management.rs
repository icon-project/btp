use token_service::TokenService;
use near_sdk::{env, json_types::U128, testing_env, AccountId, VMContext};
pub mod accounts;
use accounts::*;
use libraries::types::{
    messages::{BtpMessage, TokenServiceMessage, TokenServiceType}, AccumulatedAssetFees, FungibleToken, AccountBalance, BTPAddress, MultiTokenCore, Token, Math, WrappedI128,
};
mod token;
use std::convert::TryInto;
use token::*;

fn get_context(
    input: Vec<u8>,
    is_view: bool,
    signer_account_id: AccountId,
    attached_deposit: u128,
    storage_usage: u64,
    account_balance: u128,
) -> VMContext {
    VMContext {
        current_account_id: alice().to_string(),
        signer_account_id: signer_account_id.to_string(),
        signer_account_pk: vec![0, 1, 2],
        predecessor_account_id: signer_account_id.to_string(),
        input,
        block_index: 0,
        block_timestamp: 0,
        account_balance,
        account_locked_balance: 0,
        storage_usage,
        attached_deposit,
        prepaid_gas: 10u64.pow(18),
        random_seed: vec![0, 1, 2],
        is_view,
        output_data_receivers: vec![],
        epoch_height: 19,
    }
}

#[test]
#[cfg(feature = "testable")]
fn handle_fee_gathering() {
    let context = |account_id: AccountId, deposit: u128| {
        get_context(vec![], false, account_id, deposit, env::storage_usage(), 0)
    };
    testing_env!(context(alice(), 0));
    let destination =
        BTPAddress::new("btp://0x1.icon/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string());
    let mut contract = TokenService::new(
        "TokenBSH".to_string(),
        bmc(),
        "0x1.near".into(),
        1000.into()
    );
    let w_near = <Token<FungibleToken>>::new(WNEAR.to_owned());
    let token_id = contract.token_id(w_near.name().to_owned());

    testing_env!(context(alice(), 0));
    contract.register(w_near.clone());

    testing_env!(context(wnear(), 0));
    contract.ft_on_transfer(chuck(), U128::from(1000), "".to_string());

    testing_env!(context(chuck(), 0));
    contract.transfer(token_id.clone(), destination.clone(), U128::from(999));

    let result = contract.account_balance(chuck(), token_id.clone());
    let mut expected = AccountBalance::default();

    expected.deposit_mut().add(1).unwrap();
    expected.locked_mut().add(900).unwrap();
    expected.locked_mut().add(99).unwrap();

    assert_eq!(result, Some(expected));

    let result = contract.balance_of(alice(), token_id.clone());
    assert_eq!(result, U128::from(0));

    let btp_message = &BtpMessage::new(
        BTPAddress::new("btp://0x1.icon/0x12345678".to_string()),
        BTPAddress::new("btp://1234.iconee/0x12345678".to_string()),
        "TokenBSH".to_string(),
        WrappedI128::new(1),
        vec![],
        Some(TokenServiceMessage::new(
            TokenServiceType::ResponseHandleService {
                code: 0,
                message: "Transfer Success".to_string(),
            },
        )),
    );

    testing_env!(context(bmc(), 0));
    contract.handle_btp_message(btp_message.try_into().unwrap());

    let result = contract.balance_of(alice(), token_id.clone());
    assert_eq!(result, U128::from(999));

    let result = contract.account_balance(chuck(), token_id.clone());
    let mut expected = AccountBalance::default();
    expected.deposit_mut().add(1).unwrap();

    assert_eq!(result, Some(expected));

    let accumulted_fees = contract.accumulated_fees();

    assert_eq!(accumulted_fees, vec![
        AccumulatedAssetFees{
            name: w_near.name().to_string(),
            network: w_near.network().to_string(),
            accumulated_fees: 99
        }
    ]);

    let fee_aggregator = BTPAddress::new("btp://0x1.icon/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string());
    contract.handle_fee_gathering(fee_aggregator, "TokenBSH".to_string());

    let accumulted_fees = contract.accumulated_fees();

    assert_eq!(accumulted_fees, vec![
        AccumulatedAssetFees{
            name: w_near.name().to_string(),
            network: w_near.network().to_string(),
            accumulated_fees: 0
        }
    ]);

    let result = contract.balance_of(alice(), token_id.clone());
    assert_eq!(result, U128::from(900));
}
