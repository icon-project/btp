use super::*;
use serde_json::{from_value, json};
use test_helper::types::Context;

pub static ICON_LINK_ADDRESS_AND_RELAY_1_ARE_PROVIDED_AS_ADD_RELAY_PARAM: fn(Context) -> Context =
    |mut context: Context| {
        context.add_method_params(
            "add_relay",
            json!({
                "link": format!("btp://{}/{}", ICON_NETWORK, ICON_BMC),
                "relay": context.accounts().get("relay_1").id()
            }),
        );

        context
    };

pub static BMC_OWNER_INVOKES_ADD_RELAY_IN_BMC: fn(Context) -> Context = |context: Context| {
    context
        .pipe(THE_TRANSACTION_IS_SIGNED_BY_BMC_OWNER)
        .pipe(USER_INVOKES_ADD_RELAY_IN_BMC)
};

pub static RELAY_1_IS_REGISTERED_FOR_ICON_LINK: fn(Context) -> Context =
    |context: Context| -> Context {
        context
            .pipe(RELAY_1_ACCOUNT_IS_CREATED)
            .pipe(ICON_LINK_ADDRESS_AND_RELAY_1_ARE_PROVIDED_AS_ADD_RELAY_PARAM)
            .pipe(BMC_OWNER_INVOKES_ADD_RELAY_IN_BMC)
    };

pub static ICON_LINK_ADDRESS_AND_RELAY_1_ACCOUNT_ID_AND_RELAY_2_ACCOUNT_ID_ARE_PROVIDED_AS_ADD_RELAYS_PARAM: fn(Context) -> Context =
    |mut context: Context| {
        context.add_method_params(
            "add_relays",
            json!({
                "link": format!("btp://{}/{}", ICON_NETWORK, ICON_BMC),
                "relays": [
                    context.accounts().get("relay_1").id(),
                    context.accounts().get("relay_2").id()
                ]
            }),
        );

        context
    };

pub static ALICE_INVOKES_ADD_RELAY_IN_BMC: fn(Context) -> Context = |context: Context| {
    context
        .pipe(THE_TRANSACTION_IS_SIGNED_BY_ALICE)
        .pipe(USER_INVOKES_ADD_RELAY_IN_BMC)
};

pub static ADDED_RELAYS_SHOULD_BE_IN_THE_LIST_OF_RELAYS: fn(Context) = |context: Context| {
    let context = context
        .pipe(ICON_LINK_ADDRESS_IS_PROVIDED_AS_GET_RELAYS_PARAM)
        .pipe(USER_INVOKES_GET_RELAYS_IN_BMC);

    let relays = context.method_responses("get_relays");
    let result: HashSet<_> = from_value::<Vec<String>>(relays)
        .unwrap()
        .into_iter()
        .collect();
    let expected: HashSet<_> = vec![
        context.accounts().get("relay_1").id().to_string(),
        context.accounts().get("relay_2").id().to_string(),
    ]
    .into_iter()
    .collect();

    assert_eq!(result, expected);
};

pub static ICON_LINK_ADDRESS_IS_PROVIDED_AS_GET_RELAYS_PARAM: fn(Context) -> Context =
    |mut context: Context| {
        context.add_method_params(
            "get_relays",
            json!({
                "link": format!("btp://{}/{}", ICON_NETWORK, ICON_BMC),
            }),
        );

        context
    };

pub static ICON_LINK_ADDRESS_AND_RELAY_1_ACCOUNT_ID_ARE_PROVIDED_AS_REMOVE_RELAY_PARAM:
    fn(Context) -> Context = |mut context: Context| {
    context.add_method_params(
        "remove_relay",
        json!({
            "link": format!("btp://{}/{}", ICON_NETWORK, ICON_BMC),
            "relay": context.accounts().get("relay_1").id()
        }),
    );
    context
};

pub static CHUCK_INVOKES_REMOVE_RELAY_IN_BMC: fn(Context) -> Context = |context: Context| {
    context
        .pipe(THE_TRANSACTION_IS_SIGNED_BY_CHUCK)
        .pipe(USER_INVOKES_REMOVE_RELAY_IN_BMC)
};

pub static BMC_SHOULD_THROW_UNAUTHORIZED_ERROR_ON_REMOVING_RELAY: fn(Context) =
    |context: Context| {
        let error = context.method_errors("remove_relay");
        assert!(error.to_string().contains("BMCRevertNotExistsPermission"));
    };

pub static BSC_LINK_ADDRESS_AND_RELAY_1_ACCOUNT_ID_ARE_PROVIDED_AS_ADD_RELAYS_PARAM: fn(
    Context,
)
    -> Context = |mut context: Context| {
    context.add_method_params(
        "add_relays",
        json!({
            "link": format!("btp://{}/{}", BSC_NETWORK, BSC_BMC),
            "relays": [
                context.accounts().get("relay_1").id()
            ]
        }),
    );
    context
};

pub static ALICE_INVOKES_ADD_RELAYS_IN_BMC: fn(Context) -> Context = |context: Context| {
    context
        .pipe(THE_TRANSACTION_IS_SIGNED_BY_ALICE)
        .pipe(USER_INVOKES_ADD_RELAYS_IN_BMC)
};

pub static BMC_SHOULD_THROW_LINK_DOES_NOT_EXIST_ERROR_ON_ADDING_RELAYS: fn(Context) =
    |context: Context| {
        let error = context.method_errors("add_relays");
        assert!(error.to_string().contains("BMCRevertNotExistsLink"));
    };

pub static ALICE_INVOKES_GET_RELAY_IN_BMC: fn(Context) -> Context = |context: Context| {
    context
        .pipe(THE_TRANSACTION_IS_SIGNED_BY_ALICE)
        .pipe(USER_INVOKES_GET_RELAYS_IN_BMC)
};

pub static LIST_OF_RELAYS_SHOULD_EXIST_FOR_THE_GIVEN_LINK: fn(Context) = |context: Context| {
    let relays = context.method_responses("get_relays");
    let result: HashSet<_> = from_value::<Vec<String>>(relays)
        .unwrap()
        .into_iter()
        .collect();
    let expected: HashSet<_> = vec![context.accounts().get("relay_1").id().to_string()]
        .into_iter()
        .collect();

    assert_eq!(result, expected);
};

pub static ICON_LINK_ADDRESS_AND_RELAY_1_ACCOUNT_ID_ARE_PROVIDED_AS_ADD_RELAYS_PARAM:
    fn(Context) -> Context = |mut context: Context| {
    context.add_method_params(
        "add_relays",
        json!({
            "link": format!("btp://{}/{}", ICON_NETWORK, ICON_BMC),
            "relays": [
                context.accounts().get("relay_1").id()
            ]
        }),
    );

    context
};

pub static CHUCK_INVOKES_ADD_RELAYS_IN_BMC: fn(Context) -> Context = |context: Context| {
    context
        .pipe(THE_TRANSACTION_IS_SIGNED_BY_CHUCK)
        .pipe(USER_INVOKES_ADD_RELAYS_IN_BMC)
};

pub static BMC_SHOULD_THROW_UNAUTHORIZED_ERROR_ON_ADDING_RELAYS: fn(Context) =
    |context: Context| {
        let error = context.method_errors("add_relays");
        assert!(error.to_string().contains("BMCRevertNotExistsPermission"));
    };

pub static ALICE_INVOKES_REMOVE_RELAY_IN_BMC: fn(Context) -> Context = |context: Context| {
    context
        .pipe(THE_TRANSACTION_IS_SIGNED_BY_ALICE)
        .pipe(USER_INVOKES_REMOVE_RELAY_IN_BMC)
};

pub static BMC_SHOULD_THROW_RELAY_DOES_NOT_EXIST_ERROR_ON_REMOVING_NON_EXISTING_RELAY: fn(Context) =
    |context: Context| {
        let error = context.method_errors("remove_relay");
        assert!(error.to_string().contains("BMCRevertNotExistRelay"));
    };

pub static BMC_SHOULD_THROW_UNAUTHORIZED_ERROR_ON_ADDING_RELAY: fn(Context) = |context: Context| {
    let error = context.method_errors("add_relay");
    assert!(error.to_string().contains("BMCRevertNotExistsPermission"));
};

pub static BMC_SHOULD_THROW_LINK_DOES_NOT_EXIST_ERROR_ON_ADDING_RELAY: fn(Context) =
    |context: Context| {
        let error = context.method_errors("add_relay");
        assert!(error.to_string().contains("BMCRevertNotExistsLink"));
    };

pub static ICON_LINK_ADDRESS_AND_RELAY_1_ARE_PROVIDED_AS_REMOVE_RELAY_PARAM: fn(
    Context,
) -> Context = |mut context: Context| {
    context.add_method_params(
        "remove_relay",
        json!({
            "link": format!("btp://{}/{}", ICON_NETWORK, ICON_BMC),
            "relay": context.accounts().get("relay_1").id()
        }),
    );

    context
};

pub static CHUCK_INVOKES_ADD_RELAY_IN_BMC: fn(Context) -> Context = |context: Context| {
    context
        .pipe(THE_TRANSACTION_IS_SIGNED_BY_CHUCK)
        .pipe(USER_INVOKES_ADD_RELAY_IN_BMC)
};

pub static REMOVED_RELAYS_SHOULD_NOT_BE_IN_THE_LIST_OF_RELAYS: fn(Context) = |context: Context| {
    let context = context
        .pipe(ICON_LINK_ADDRESS_IS_PROVIDED_AS_GET_RELAYS_PARAM)
        .pipe(USER_INVOKES_GET_RELAYS_IN_BMC);

    let relays = context.method_responses("get_relays");
    let result: HashSet<_> = from_value::<Vec<String>>(relays).into_iter().collect();
    let expected: HashSet<_> = from_value::<Vec<String>>(json!([])).into_iter().collect();

    assert_eq!(result, expected);
};

pub static ADDED_RELAY_SHOULD_BE_IN_THE_LIST_OF_RELAYS: fn(Context) = |context: Context| {
    let context = context
        .pipe(ICON_LINK_ADDRESS_IS_PROVIDED_AS_GET_RELAYS_PARAM)
        .pipe(USER_INVOKES_GET_RELAYS_IN_BMC);

    let relays = context.method_responses("get_relays");
    let result: HashSet<_> = from_value::<Vec<String>>(relays)
        .unwrap()
        .into_iter()
        .collect();
    let expected: HashSet<_> = vec![context.accounts().get("relay_1").id().to_string()]
        .into_iter()
        .collect();

    assert_eq!(result, expected);
};

pub static NON_EXISTING_LINK_ADDRESS_PROVIDED_AS_GET_RELAY_PARAM: fn(Context) -> Context =
    |mut context: Context| {
        context.add_method_params(
            "get_relays",
            json!({
                "link": format!("btp://{}/{}", BSC_NETWORK, BSC_BMC),
            }),
        );
        context
    };

pub static USER_INVOKES_GET_RELAY_METHOD_WITH_NON_EXISTING_LINK: fn(Context) -> Context =
    |context: Context| context.pipe(USER_INVOKES_GET_RELAYS_IN_BMC);

pub static BMC_SHOULD_THROW_LINK_DOES_NOT_EXIST_ERROR_ON_GETTING_RELAYS: fn(Context) =
    |context: Context| {
        let error = context.method_errors("get_relays");
        // assert!(error.to_string().contains("BMCRevertNotExistsRelay"));
        println!("{}", error);
    };

pub static NON_EXISTING_LINK_ADDRESS_AND_RELAY_1_IS_PROVIDED_AS_REMOVE_RELAY_PARAM_IN_BMC:
    fn(Context) -> Context = |mut context: Context| {
    context.add_method_params(
        "remove_relay",
        json!({
            "link": format!("btp://{}/{}", BSC_NETWORK, BSC_BMC),
            "relay": context.accounts().get("relay_1").id(),
        }),
    );
    context
};

pub static BMC_OWNER_INVOKES_REMOVE_RELAY_IN_BMC: fn(Context) -> Context =
    |context: Context| context.pipe(USER_INVOKES_REMOVE_RELAY_IN_BMC);

pub static BMC_SHOULD_THROW_LINK_DOES_NOT_EXIST_ERROR_ON_REMOVING_RELAY: fn(Context) =
    |context: Context| {
        let error = context.method_errors("remove_relay");
        assert!(error.to_string().contains("BMCRevertNotExistsLink"));
    };
