use nativecoin_service::NativeCoinService;
use near_sdk::{env, json_types::U128, testing_env, AccountId, VMContext};
use std::{collections::HashSet, convert::TryInto};
pub mod accounts;
use accounts::*;
use libraries::types::{
    messages::{BtpMessage, TokenServiceMessage, TokenServiceType},
    Account, AccountBalance, TransferableAsset, BTPAddress, WrappedNativeCoin, Asset, Math,
    WrappedI128,
};
mod token;
use std::convert::TryFrom;
use token::*;

pub type Coin = Asset<WrappedNativeCoin>;

fn get_context(
    input: Vec<u8>,
    is_view: bool,
    signer_account_id: AccountId,
    attached_deposit: u128,
    storage_usage: u64,
) -> VMContext {
    VMContext {
        current_account_id: alice().to_string(),
        signer_account_id: signer_account_id.to_string(),
        signer_account_pk: vec![0, 1, 2],
        predecessor_account_id: signer_account_id.to_string(),
        input,
        block_index: 0,
        block_timestamp: 0,
        account_balance: 2,
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
fn handle_transfer_mint_registered_icx() {
    let context = |account_id: AccountId, deposit: u128| {
        get_context(vec![], false, account_id, deposit, env::storage_usage())
    };
    testing_env!(context(alice(), 0));
    let nativecoin = Coin::new(NATIVE_COIN.to_owned());
    let mut contract = NativeCoinService::new(
        "nativecoin".to_string(),
        bmc(),
        "0x1.near".into(),
        nativecoin.clone(),
        1000.into()
    );

    let destination =
        BTPAddress::new("btp://0x1.icon/cx87ed9048b594b95199f326fc76e76a9d33dd665b".to_string());

    let icx_coin = <Coin>::new(ICON_COIN.to_owned());
    contract.register(icx_coin.clone());

    let coin_id = contract.coin_id(icx_coin.name().to_owned());

    let btp_message = &BtpMessage::new(
        BTPAddress::new("btp://0x1.icon/0x12345678".to_string()),
        BTPAddress::new("btp://1234.iconee/0x12345678".to_string()),
        "nativecoin".to_string(),
        WrappedI128::new(1),
        vec![],
        Some(TokenServiceMessage::new(
            TokenServiceType::RequestTokenTransfer {
                sender: chuck().to_string(),
                receiver: destination.account_id().to_string(),
                assets: vec![TransferableAsset::new(icx_coin.name().to_owned(), 900, 99)],
            },
        )),
    );

    testing_env!(context(bmc(), 0));
    contract.handle_btp_message(btp_message.try_into().unwrap());

    testing_env!(context(alice(), 0));
    contract.on_mint(900,coin_id,icx_coin.symbol().to_string(),destination.account_id());


    let result = contract
        .balance_of(
            destination.account_id(),
            contract.coin_id(icx_coin.name().to_owned()),
        );
    assert_eq!(result, U128::from(900));
}
