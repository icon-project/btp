use super::*;
use libraries::types::LinkStatus;
use serde_json::{from_value, json};
use test_helper::types::Context;

pub static ICON_LINK_IS_ADDED: fn(Context) -> Context = |context: Context| {
    context
        .pipe(VERIFIER_FOR_ICON_IS_ADDED)
        .pipe(ICON_LINK_ADDRESS_IS_PROVIDED_AS_ADD_LINK_PARAM)
        .pipe(BMC_OWNER_INVOKES_ADD_LINK_IN_BMC)
};

pub static ICON_LINK_IS_INITIALIZED: fn(Context) -> Context = |context: Context| {
    context
        .pipe(ICON_LINK_ADDRESS_IS_PROVIDED_AS_SET_LINK_PARAM)
        .pipe(BMC_OWNER_INVOKES_SET_LINK_IN_BMC)
};

pub static ICON_LINK_IS_PRESENT_IN_BMC: fn(Context) -> Context = |context: Context| {
    context
        .pipe(ICON_LINK_IS_ADDED)
        .pipe(ICON_LINK_IS_INITIALIZED)
};

pub static BMC_OWNER_INVOKES_SET_LINK_IN_BMC: fn(Context) -> Context = |context: Context| {
    context
        .pipe(THE_TRANSACTION_IS_SIGNED_BY_BMC_OWNER)
        .pipe(USER_INVOKES_SET_LINK_IN_BMC)
};

pub static ICON_LINK_ADDRESS_IS_PROVIDED_AS_SET_LINK_PARAM: fn(Context) -> Context =
    |mut context: Context| {
        context.add_method_params(
            "set_link",
            json!({
                "link": format!("btp://{}/{}", ICON_NETWORK, ICON_BMC),
                "block_interval": 15000,
                "max_aggregation": 5,
                "delay_limit": 4
            }),
        );

        context
    };

pub static ICON_LINK_ADDRESS_IS_PROVIDED_AS_ADD_LINK_PARAM: fn(Context) -> Context =
    |mut context: Context| {
        context.add_method_params(
            "add_link",
            json!({ "link": format!("btp://{}/{}", ICON_NETWORK, ICON_BMC) }),
        );

        context
    };

pub static BMC_OWNER_INVOKES_ADD_LINK_IN_BMC: fn(Context) -> Context = |context: Context| {
    context
        .pipe(THE_TRANSACTION_IS_SIGNED_BY_BMC_OWNER)
        .pipe(USER_INVOKES_ADD_LINK_IN_BMC)
};

pub static ALICE_INVOKES_ADD_LINK_IN_BMC: fn(Context) -> Context = |context: Context| {
    context
        .pipe(THE_TRANSACTION_IS_SIGNED_BY_ALICE)
        .pipe(USER_INVOKES_ADD_LINK_IN_BMC)
};

pub static ICON_LINK_SHOULD_BE_ADDED_TO_THE_LIST_OF_LINKS: fn(Context) = |context: Context| {
    let context = context
        .pipe(ICON_LINK_ADDRESS_IS_PROVIDED_AS_GET_STATUS_PARAM)
        .pipe(USER_INVOKES_GET_LINKS_IN_BMC);

    let link = context.method_responses("get_links");

    let result: HashSet<_> = from_value::<Vec<String>>(link)
        .unwrap()
        .into_iter()
        .collect();
    let expected: HashSet<_> =
        vec!["btp://0x1.icon/0xc294b1A62E82d3f135A8F9b2f9cAEAA23fbD6Cf5".to_string()]
            .into_iter()
            .collect();

    assert_eq!(result, expected);
};

pub static ICON_LINK_ADDRESS_IS_PROVIDED_AS_GET_STATUS_PARAM: fn(Context) -> Context =
    |mut context: Context| {
        context.add_method_params(
            "get_status",
            json!({ "link": format!("btp://{}/{}", ICON_NETWORK, ICON_BMC) }),
        );

        context
    };

pub static CHUCK_INVOKES_ADD_LINK_IN_BMC: fn(Context) -> Context = |context: Context| {
    context
        .pipe(THE_TRANSACTION_IS_SIGNED_BY_CHUCK)
        .pipe(USER_INVOKES_ADD_LINK_IN_BMC)
};

pub static BMC_SHOULD_THROW_UNAUTHORIZED_ERROR_ON_ADDING_LINK: fn(Context) = |context: Context| {
    let error = context.method_errors("add_link");
    assert!(error.to_string().contains("BMCRevertNotExistsPermission"));
};

pub static ALICE_INVOKES_SET_LINK_IN_BMC: fn(Context) -> Context = |context: Context| {
    context
        .pipe(THE_TRANSACTION_IS_SIGNED_BY_ALICE)
        .pipe(USER_INVOKES_SET_LINK_IN_BMC)
};

pub static USER_SHOULD_GET_THE_ICON_LINK_STATUS: fn(Context) = |context: Context| {
    let context = context
        .pipe(ICON_LINK_ADDRESS_IS_PROVIDED_AS_GET_STATUS_PARAM)
        .pipe(USER_INVOKES_GET_STATUS_IN_BMC);
    let result: LinkStatus = from_value(context.method_responses("get_status")).unwrap();
    assert_eq!(result.delay_limit(), 4);
};

pub static BMC_SHOULD_THROW_VERIFIER_NOT_EXISITING_ERROR: fn(Context) = |context: Context| {
    let error = context.method_errors("add_link");
    assert!(error.to_string().contains("BMCRevertNotExistBMV"));
};

pub static BMC_SHOULD_THROW_LINK_DOES_NOT_EXIST_ERROR_ON_SETTING_LINK: fn(Context) =
    |context: Context| {
        let error = context.method_errors("set_link");
        assert!(error.to_string().contains("BMCRevertNotExistsLink"));
    };

pub static SET_LINK_IN_BMC_IS_INVOKED_BY_ALICE: fn(Context) -> Context = |context: Context| {
    context
        .pipe(THE_TRANSACTION_IS_SIGNED_BY_ALICE)
        .pipe(USER_INVOKES_SET_LINK_IN_BMC)
};

pub static ALICE_INVOKES_GET_STATUS_IN_BMC: fn(Context) -> Context =
    |context: Context| context.pipe(USER_INVOKES_GET_STATUS_IN_BMC);

pub static ICON_LINK_ADDRESS_IS_PROVIDED_AS_REMOVE_LINK_PARAM: fn(Context) -> Context =
    |mut context: Context| {
        context.add_method_params(
            "remove_link",
            json!({ "link": format!("btp://{}/{}", ICON_NETWORK, ICON_BMC) }),
        );

        context
    };

pub static ALICE_INVOKES_REMOVE_LINK_IN_BMC: fn(Context) -> Context = |context: Context| {
    context
        .pipe(THE_TRANSACTION_IS_SIGNED_BY_ALICE)
        .pipe(USER_INVOKES_REMOVE_LINK_IN_BMC)
};

pub static BMC_SHOULD_THROW_LINK_NOT_EXISTING_ERROR: fn(Context) = |context: Context| {
    let error = context.method_errors("remove_link");
    assert!(error.to_string().contains("BMCRevertNotExistsLink"));
};

pub static SET_LINK_IN_BMC_IS_INVOKED_ALICE: fn(Context) -> Context = |context: Context| {
    context
        .pipe(THE_TRANSACTION_IS_SIGNED_BY_ALICE)
        .pipe(ICON_LINK_ADDRESS_IS_PROVIDED_AS_ADD_LINK_PARAM)
        .pipe(USER_INVOKES_SET_LINK_IN_BMC)
};

pub static MAX_AGGREGATION_IS_PROVIDED_AS_SET_LINK_PARAM: fn(Context) -> Context =
    |mut context: Context| {
        context.add_method_params(
            "set_link",
            json!({
                "max_aggregation": 10,
            }),
        );

        context
    };

pub static MAX_AGGREGATION_IN_ICON_LINK_STATUS_SHOULD_BE_UPDATED: fn(Context) =
    |context: Context| {
        let context = context
            .pipe(ICON_LINK_ADDRESS_IS_PROVIDED_AS_GET_STATUS_PARAM)
            .pipe(USER_INVOKES_GET_STATUS_IN_BMC);
        let result: LinkStatus = from_value(context.method_responses("get_status")).unwrap();
        // TODO: complete test case
    };

pub static CHUCK_INVOKES_SET_LINK_IN_BMC: fn(Context) -> Context = |context: Context| {
    context
        .pipe(THE_TRANSACTION_IS_SIGNED_BY_CHUCK)
        .pipe(USER_INVOKES_SET_LINK_IN_BMC)
};

pub static BMC_SHOULD_THROW_UNAUTHORIZED_ERROR_ON_SETTING_LINK: fn(Context) = |context: Context| {
    let error = context.method_errors("set_link");
    assert!(error.to_string().contains("BMCRevertNotExistsPermission"));
};

pub static ICON_LINK_ADDRESS_WITH_0_DELAY_IS_PROVIDED_AS_SET_LINK_PARAM: fn(Context) -> Context =
    |mut context: Context| {
        context.add_method_params(
            "set_link",
            json!({
                "link": format!("btp://{}/{}", ICON_NETWORK, ICON_BMC),
                "block_interval": 15000,
                "max_aggregation": 5,
                "delay_limit": 0
            }),
        );

        context
    };

pub static BMC_SHOULD_THROW_INVALID_PARAM_ERROR_ON_SETTING_LINK: fn(Context) =
    |context: Context| {
        let error = context.method_errors("set_link");
        assert!(error.to_string().contains("BMCRevertInvalidParam"));
    };

pub static ICON_LINK_ADDRESS_WITH_0_MAX_AGGREGATION_IS_PROVIDED_AS_SET_LINK_PARAM: fn(
    Context,
) -> Context = |mut context: Context| {
    context.add_method_params(
        "set_link",
        json!({
            "link": format!("btp://{}/{}", ICON_NETWORK, ICON_BMC),
            "block_interval": 15000,
            "max_aggregation": 0,
            "delay_limit": 4
        }),
    );
    context
};

pub static BMC_SHOULD_THROW_UNAUTHORIZED_ERROR_ON_REMOVING_LINK: fn(Context) =
    |context: Context| {
        let error = context.method_errors("remove_link");

        assert!(error.to_string().contains("BMCRevertNotExistsPermission"));
    };

pub static CHUCK_INVOKES_REMOVE_LINK_IN_BMC: fn(Context) -> Context = |context: Context| {
    context
        .pipe(THE_TRANSACTION_IS_SIGNED_BY_CHUCK)
        .pipe(USER_INVOKES_REMOVE_LINK_IN_BMC)
};

pub static INVALID_LINK_ADDRESS_IS_PROVIDED_AS_ADD_LINK_PARAM: fn(Context) -> Context =
    |mut context: Context| {
        context.add_method_params(
            "add_link",
            json!({ "link": format!("http://{}/{}", ICON_NETWORK, ICON_BMC) }),
        );

        context
    };

pub static BMC_SHOULD_THROW_INVALID_PARAM_ERROR_ON_ADDING_LINK: fn(Context) = |context: Context| {
    let error = context.method_errors("add_link");

    assert!(error.to_string().contains("InvalidBtpAddress"));
};

pub static BMC_SHOULD_THROW_ALREADY_EXISTING_LINK_ERROR_ON_ADDING_LINK: fn(Context) = |context: Context| {
    let error = context.method_errors("add_link");

    assert!(error.to_string().contains("BMCRevertAlreadyExistsLink"));
};
