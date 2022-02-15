use super::*;
use libraries::types::{HashedCollection, HashedValue};
use serde_json::{from_value, json, Value};
use test_helper::types::Context;

pub static BOB_IS_BSH_CONTRACT_OWNER: fn(Context) -> Context = |mut context: Context| {
    let bsh_signer = context.contracts().get("bsh").to_owned();
    context.accounts_mut().add("bob", &bsh_signer);
    context
};

pub static BOB_INVOKES_TRANSFER_NATIVE_COIN_FORM_BSH: fn(Context) -> Context =
    |mut context: Context| {
        let signer = context.accounts().get("bob").to_owned();
        context.set_signer(&signer);
        BSH_CONTRACT.transfer_native_coin(context)
    };

pub static BOB_INVOKES_GET_BALANCE_FORM_BSH: fn(Context) -> Context = |mut context: Context| {
    let signer = context.accounts().get("bob").to_owned();
    context.set_signer(&signer);
    BSH_CONTRACT.get_balance_of(context)
};

pub static BOB_INVOKES_REGISTER_NEW_COIN_FORM_BSH: fn(Context) -> Context =
    |mut context: Context| {
        let signer = context.accounts().get("bob").to_owned();
        context.set_signer(&signer);
        BSH_CONTRACT.register(context)
    };

pub static CHUCK_INVOKES_REGISTER_NEW_COIN_FORM_BSH: fn(Context) -> Context =
    |mut context: Context| {
        let signer = context.accounts().get("bob").to_owned();
        context.set_signer(&signer);
        BSH_CONTRACT.register(context)
    };

pub static BOB_INVOKES_ADD_OWNER: fn(Context) -> Context = |mut context: Context| {
    let signer = context.accounts().get("bob").to_owned();
    context.set_signer(&signer);
    BSH_CONTRACT.add_owner(context)
};

pub static BOB_INVOKES_REMOVE_OWNER: fn(Context) -> Context = |mut context: Context| {
    let signer = context.accounts().get("bob").to_owned();
    context.set_signer(&signer);
    BSH_CONTRACT.remove_owner(context)
};

pub static BSH_OWNER_REGISTERS_NEW_COIN: fn(Context) -> Context = |mut context: Context| {
    BOBS_ACCOUNT_IS_CREATED(context)
        .pipe(BOB_IS_BSH_CONTRACT_OWNER)
        .pipe(NEW_COIN_NAME_IS_PROVIDED_AS_REGISTER_PARAM)
        .pipe(BOB_INVOKES_REGISTER_NEW_COIN_FORM_BSH)
};

pub static NEW_COIN_NAME_IS_PROVIDED_AS_REGISTER_PARAM: fn(Context) -> Context =
    |mut context: Context| {
        context.add_method_params(
            "register",
            json!({
                "name":"coin1"
            }),
        );
        context
    };

pub static NEW_BSH_PERIPHERY_ADDRESS_IS_PROVIDED_AS_UPDATE_PERIPHERY_PARAM: fn(Context) -> Context =
    |mut context: Context| {
        context.add_method_params(
            "update_bsh_periphery",
            json!({
                "bsh_periphery":"Addresss"
            }),
        );
        context
    };

pub static NEW_URI_IS_PROVIDED_AS_UPDATE_URI_PARAM: fn(Context) -> Context =
    |mut context: Context| {
        context.add_method_params(
            "update_uri",
            json!({
                "new_uri":"Addresss"
            }),
        );
        context
    };

pub static FEE_NUMERATOR_IS_PROVIDED_AS_SET_FEE_RATIO_PARAM: fn(Context) -> Context =
    |mut context: Context| {
        context.add_method_params(
            "set_fee_ratio",
            json!({
                "fee_numerator":"valueinu64"
            }),
        );
        context
    };

pub static FIXED_FEE_IS_PROVIDED_AS_SET_FIXED_PARAM: fn(Context) -> Context =
    |mut context: Context| {
        context.add_method_params(
            "set_fixed_fee",
            json!({
                "fixed_fee":"valueinu64"
            }),
        );
        context
    };

pub static COIN_NAME_IS_PROVIDED_AS_GET_BALANCE_PARAM: fn(Context) -> Context =
    |mut context: Context| {
        context.add_method_params(
            "get_balance_of",
            json!({
                "owner":"accountId",
                "coin_name":"coin1"
            }),
        );
        context
    };

pub static TOADDRESS_IS_PROVIDED_AS_TRANSFER_NATIVE_COIN_PARAM: fn(Context) -> Context =
    |mut context: Context| {
        context.add_method_params(
            "transfer_native_coin",
            json!({
                "to":"accountId"
            }),
        );
        context
    };

pub static INVALID_TOADDRESS_IS_PROVIDED_AS_TRANSFER_NATIVE_COIN_PARAM: fn(Context) -> Context =
    |mut context: Context| {
        context.add_method_params(
            "transfer_native_coin",
            json!({
                "to":"invalidaccountId"
            }),
        );
        context
    };

pub static COIN_NAMES_ARE_QURIED_IN_BSH: fn(Context) -> Context =
    |context: Context| BSH_CONTRACT.get_coin_names(context);

pub static COIN_REGISTERED_SHOULD_BE_PRESENT: fn(Context) = |context: Context| {
    let coins = context.method_responses("get_coin_names");
    let result: HashSet<_> = from_value::<Vec<String>>(coins)
        .unwrap()
        .into_iter()
        .collect();
    let expected: HashSet<_> = vec!["coin1".to_string()].into_iter().collect();
    assert_eq!(result, expected);
};

pub static NON_BSH_OWNER_REGISTERS_NEW_COIN: fn(Context) -> Context = |mut context: Context| {
    CHUCKS_ACCOUNT_IS_CREATED(context)
        .pipe(CHUCK_IS_NOT_A_BSH_OWNER)
        .pipe(NEW_COIN_NAME_IS_PROVIDED_AS_REGISTER_PARAM)
        .pipe(CHUCK_INVOKES_REGISTER_NEW_COIN_FORM_BSH)
};

pub static BSH_OWNER_REGISTERS_EXISTING_COIN: fn(Context) -> Context = |mut context: Context| {
    BOBS_ACCOUNT_IS_CREATED(context)
        .pipe(BOB_IS_BSH_CONTRACT_OWNER)
        .pipe(NEW_COIN_NAME_IS_PROVIDED_AS_REGISTER_PARAM)
        .pipe(BOB_INVOKES_REGISTER_NEW_COIN_FORM_BSH)
        .pipe(NEW_COIN_NAME_IS_PROVIDED_AS_REGISTER_PARAM)
        .pipe(BOB_INVOKES_REGISTER_NEW_COIN_FORM_BSH)
};

pub static BSH_OWNER_INVOKES_NATIVE_COIN_TRANSFER_TO_VALID_ADDRESS: fn(Context) -> Context =
    |mut context: Context| {
        BOBS_ACCOUNT_IS_CREATED(context)
            .pipe(BOB_IS_BSH_CONTRACT_OWNER)
            .pipe(TOADDRESS_IS_PROVIDED_AS_TRANSFER_NATIVE_COIN_PARAM)
            .pipe(BOB_INVOKES_TRANSFER_NATIVE_COIN_FORM_BSH)
    };

pub static COIN_BALANCES_ARE_QURIED: fn(Context) -> Context = |mut context: Context| {
    COIN_NAME_IS_PROVIDED_AS_GET_BALANCE_PARAM(context).pipe(BOB_INVOKES_GET_BALANCE_FORM_BSH)
};

pub static COIN_VALUE_SENT_SHOULD_BE_EQUAL_TO_COIN_VALUE_DEDUCTED: fn(Context) =
    |context: Context| {
        let coins = context.method_responses("get_balance_of");
        let result: HashSet<_> = from_value::<Vec<String>>(coins)
            .unwrap()
            .into_iter()
            .collect();
        let expected: HashSet<_> = vec!["coin1".to_string()].into_iter().collect();
        assert_eq!(result, expected);
    };

pub static BSH_OWNER_INVOKES_NATIVE_COIN_TRANSFER_TO_INVALID_ADDRESS: fn(Context) -> Context =
    |mut context: Context| {
        INVALID_TOADDRESS_IS_PROVIDED_AS_TRANSFER_NATIVE_COIN_PARAM(context)
            .pipe(BOB_INVOKES_TRANSFER_NATIVE_COIN_FORM_BSH)
    };

pub static BSH_OWNER_INVOKES_NATIVE_COIN_WITH_NOVALUE: fn(Context) -> Context =
    |mut context: Context| context;

pub static BSH_SHOULD_THROW_FAIL_TO_TRANSFER_ERROR: fn(Context) -> Context =
    |mut context: Context| context;

pub static BSH_OWNER_INVOKES_NATIVE_COIN_TRANSFER_TO_UNSUPPORTED_NETWORK: fn(Context) -> Context =
    |mut context: Context| context;

pub static CHARLIES_ACCOUNT_ID_IS_PROVIDED_AS_ADD_OWNER_PARAM: fn(Context) -> Context =
    |mut context: Context| {
        let charlie = context.accounts().get("charlie").to_owned();
        context.add_method_params(
            "add_owner",
            json!({
                "owner": charlie.account_id()
            }),
        );
        context
    };

pub static ACCOUNT_ID_IS_PROVIDED_AS_REMOVE_OWNER_PARAM: fn(Context) -> Context =
    |mut context: Context| {
        let charlie = context.accounts().get("charlie").to_owned();
        context.add_method_params(
            "remove_owner",
            json!({
                "owner": charlie.account_id()
            }),
        );
        context
    };
pub static BSH_OWNER_ADDS_NEW_OWNER: fn(Context) -> Context = |mut context: Context| {
    BOBS_ACCOUNT_IS_CREATED(context)
        .pipe(BOB_IS_BSH_CONTRACT_OWNER)
        .pipe(ACCOUNT_ID_IS_PROVIDED_AS_ADD_OWNER_PARAM)
        .pipe(BOB_INVOKES_ADD_OWNER)
};

pub static ONWERS_ARE_QURIED_IN_BSH: fn(Context) -> Context =
    |context: Context| BSH_CONTRACT.get_owners(context);

pub static ADDED_OWNER_SHOULD_BE_PRESENT: fn(Context) = |context: Context| {
    let owners = context.method_responses("get_owners");

    let result: HashSet<_> = from_value::<Vec<String>>(owners)
        .unwrap()
        .into_iter()
        .collect();
    let expected: HashSet<_> = vec![
        context.accounts().get("bob").account_id().to_string(),
        context.accounts().get("charlie").account_id().to_string(),
    ]
    .into_iter()
    .collect();
    assert_eq!(result, expected);
};

pub static BSH_OWNER_ADD_EXSISTING_OWNER: fn(Context) -> Context = |mut context: Context| {
    BSH_OWNER_ADDS_NEW_OWNER(context)
        .pipe(ACCOUNT_ID_IS_PROVIDED_AS_ADD_OWNER_PARAM)
        .pipe(BOB_INVOKES_ADD_OWNER)
};

pub static CHUCK_INVOKES_ADD_OWNER_IN_BSH: fn(Context) -> Context = |mut context: Context| {
    let signer = context.accounts().get("chucks").to_owned();
    context.set_signer(&signer);
    BMC_CONTRACT.add_owner(context)
};

pub static CHUCK_INVOKES_REMOVE_OWNER_IN_BSH: fn(Context) -> Context = |mut context: Context| {
    let signer = context.accounts().get("chucks").to_owned();
    context.set_signer(&signer);
    BMC_CONTRACT.remove_owner(context)
};

pub static NON_BSH_OWNER_INVOKES_ADD_OWNER: fn(Context) -> Context = |mut context: Context| {
    CHUCKS_ACCOUNT_IS_CREATED(context)
        .pipe(ACCOUNT_ID_IS_PROVIDED_AS_ADD_OWNER_PARAM)
        .pipe(CHUCK_INVOKES_ADD_OWNER_IN_BSH)
};

pub static NON_BSH_OWNER_INVOKES_REMOVE_OWNER: fn(Context) -> Context = |mut context: Context| {
    BSH_OWNER_ADDS_NEW_OWNER(context)
        .pipe(CHUCKS_ACCOUNT_IS_CREATED)
        .pipe(ACCOUNT_ID_IS_PROVIDED_AS_REMOVE_OWNER_PARAM)
        .pipe(CHUCK_INVOKES_REMOVE_OWNER_IN_BSH)
};

pub static BSH_OWNER_INVOKES_REMOVE_OWNER: fn(Context) -> Context = |mut context: Context| {
    BSH_OWNER_ADDS_NEW_OWNER(context)
        .pipe(ACCOUNT_ID_IS_PROVIDED_AS_REMOVE_OWNER_PARAM)
        .pipe(BOB_INVOKES_REMOVE_OWNER)
};

pub static BSH_OWNER_TRIES_TO_REMOVE_NON_EXSISTING_OWNER: fn(Context) -> Context =
    |mut context: Context| {
        BSH_OWNER_INVOKES_REMOVE_OWNER(context).pipe(BSH_OWNER_INVOKES_REMOVE_OWNER)
    };

pub static REMOVE_ONWER_SHOULD_NOT_BE_PRESENT: fn(Context) = |context: Context| {
    let owners = context.method_responses("get_owners");

    let result: HashSet<_> = from_value::<Vec<String>>(owners)
        .unwrap()
        .into_iter()
        .collect();
    let expected: HashSet<_> = vec![
        context.accounts().get("bob").account_id().to_string(),
        context.accounts().get("charlie").account_id().to_string(),
    ]
    .into_iter()
    .collect();
    assert_ne!(result, expected);
};

pub static CHUCK_INVOKES_UPDATE_BSH_PERIPHERY_FORM_BSH: fn(Context) -> Context =
    |mut context: Context| {
        let signer = context.accounts().get("chuck").to_owned();
        context.set_signer(&signer);
        BSH_CONTRACT.update_bsh_periphery(context)
    };

pub static BOB_INVOKES_UPDATE_BSH_PERIPHERY_FORM_BSH: fn(Context) -> Context =
    |mut context: Context| {
        let signer = context.accounts().get("bob").to_owned();
        context.set_signer(&signer);
        BSH_CONTRACT.update_bsh_periphery(context)
    };

pub static UPDATE_BSH_PERIPHERY_INVOKED_BY_BSH_OWNER: fn(Context) -> Context =
    |mut context: Context| {
        BOBS_ACCOUNT_IS_CREATED(context)
            .pipe(BOB_IS_BSH_CONTRACT_OWNER)
            .pipe(FEE_NUMERATOR_IS_PROVIDED_AS_SET_FEE_RATIO_PARAM)
            .pipe(BOB_INVOKES_UPDATE_BSH_PERIPHERY_FORM_BSH)
    };

pub static UPDATE_BSH_PERIPHERY_INVOKED_BY_NON_BSH_OWNER: fn(Context) -> Context =
    |mut context: Context| {
        CHUCKS_ACCOUNT_IS_CREATED(context)
            .pipe(CHUCK_IS_NOT_A_BSH_OWNER)
            .pipe(FEE_NUMERATOR_IS_PROVIDED_AS_SET_FEE_RATIO_PARAM)
            .pipe(CHUCK_INVOKES_UPDATE_BSH_PERIPHERY_FORM_BSH)
    };

pub static BOB_INVOKES_UPDATE_URI_FORM_BSH: fn(Context) -> Context = |mut context: Context| {
    let signer = context.accounts().get("bob").to_owned();
    context.set_signer(&signer);
    BSH_CONTRACT.update_uri(context)
};

pub static CHCUK_INVOKES_UPDATE_URI_FORM_BSH: fn(Context) -> Context = |mut context: Context| {
    let signer = context.accounts().get("chuck").to_owned();
    context.set_signer(&signer);
    BSH_CONTRACT.update_uri(context)
};

pub static UPDATE_URI_INVOKED_BY_BSH_OWNER: fn(Context) -> Context = |mut context: Context| {
    BOBS_ACCOUNT_IS_CREATED(context)
        .pipe(BOB_IS_BSH_CONTRACT_OWNER)
        .pipe(NEW_URI_IS_PROVIDED_AS_UPDATE_URI_PARAM)
        .pipe(BOB_INVOKES_UPDATE_URI_FORM_BSH)
};

pub static UPDATE_URI_INVOKED_BY_NON_BSH_OWNER: fn(Context) -> Context = |mut context: Context| {
    CHUCKS_ACCOUNT_IS_CREATED(context)
        .pipe(CHUCK_IS_NOT_A_BSH_OWNER)
        .pipe(NEW_URI_IS_PROVIDED_AS_UPDATE_URI_PARAM)
        .pipe(CHCUK_INVOKES_UPDATE_URI_FORM_BSH)
};

pub static BOB_INVOKES_SET_FEE_RATIO_FORM_BSH: fn(Context) -> Context = |mut context: Context| {
    let signer = context.accounts().get("bob").to_owned();
    context.set_signer(&signer);
    BSH_CONTRACT.set_fee_ratio(context)
};

pub static CHCUK_INVOKES_SET_FEE_RATIO_FORM_BSH: fn(Context) -> Context = |mut context: Context| {
    let signer = context.accounts().get("chuck").to_owned();
    context.set_signer(&signer);
    BSH_CONTRACT.set_fee_ratio(context)
};

pub static SET_FEE_RATIO_INVOKED_BY_BSH_OWNER: fn(Context) -> Context = |mut context: Context| {
    BOBS_ACCOUNT_IS_CREATED(context)
        .pipe(BOB_IS_BSH_CONTRACT_OWNER)
        .pipe(FEE_NUMERATOR_IS_PROVIDED_AS_SET_FEE_RATIO_PARAM)
        .pipe(BOB_INVOKES_SET_FEE_RATIO_FORM_BSH)
};

pub static SET_FEE_RATIO_INVOKED_BY_NON_BSH_OWNER: fn(Context) -> Context =
    |mut context: Context| {
        CHUCK_ACCOUNT_IS_CREATED(context)
            .pipe(CHUCK_IS_NOT_A_BSH_OWNER)
            .pipe(FEE_NUMERATOR_IS_PROVIDED_AS_SET_FEE_RATIO_PARAM)
            .pipe(CHCUK_INVOKES_SET_FEE_RATIO_FORM_BSH)
    };

pub static BOB_INVOKES_SET_FIXED_FEE_FORM_BSH: fn(Context) -> Context = |mut context: Context| {
    let signer = context.accounts().get("bob").to_owned();
    context.set_signer(&signer);
    BSH_CONTRACT.set_fixed_fee(context)
};

pub static CHCUK_INVOKES_SET_FIXED_FEE_FORM_BSH: fn(Context) -> Context = |mut context: Context| {
    let signer = context.accounts().get("chuck").to_owned();
    context.set_signer(&signer);
    BSH_CONTRACT.set_fixed_fee(context)
};

pub static SET_FIXED_FEE_INVOKED_BY_BSH_OWNER: fn(Context) -> Context = |mut context: Context| {
    BOBS_ACCOUNT_IS_CREATED(context)
        .pipe(BOB_IS_BSH_CONTRACT_OWNER)
        .pipe(FIXED_FEE_IS_PROVIDED_AS_SET_FIXED_PARAM)
        .pipe(BOB_INVOKES_SET_FIXED_FEE_FORM_BSH)
};

pub static SET_FIXED_FEE_INVOKED_BY_NON_BSH_OWNER: fn(Context) -> Context =
    |mut context: Context| {
        CHUCKS_ACCOUNT_IS_CREATED(context)
            .pipe(CHUCK_IS_NOT_A_BSH_OWNER)
            .pipe(FIXED_FEE_IS_PROVIDED_AS_SET_FIXED_PARAM)
            .pipe(CHCUK_INVOKES_SET_FIXED_FEE_FORM_BSH)
    };
