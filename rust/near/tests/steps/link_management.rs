use super::*;
use super::{BMC_CONTRACT, BMV_CONTRACT};
use libraries::types::{Address, BTPAddress, LinkStatus};
use serde_json::{from_value, json};
use test_helper::types::Context;

pub static ADD_LINK_INVOKED_WITH_EXISTING_ADDRESS_BY_BMC_OWNER: fn(Context) -> Context =
    |mut context: Context| {
        VERIFIER_NETWORKADDRESS_AND_VERIFIER_ADDRESS_PROVIDED_AS_ADD_VERIFIER_PARAM(context)
            .pipe(ALICE_INVOKES_ADD_VERIFIER_IN_BMC)
            .pipe(LINK_ADDRESS_PROVIDED_AS_ADD_LINK_PARAM)
            .pipe(ALICE_INVOKES_ADD_LINK_IN_BMC)
            .pipe(LINK_ADDRESS_PROVIDED_AS_ADD_LINK_PARAM)
            .pipe(ALICE_INVOKES_ADD_LINK_IN_BMC)
    };

pub static ADD_LINK_INVOKED_BY_BMC_OWNER: fn(Context) -> Context = |mut context: Context| {
    //provide the link to be added
    //link provided shoud be in verifier
    VERIFIER_NETWORKADDRESS_AND_VERIFIER_ADDRESS_PROVIDED_AS_ADD_VERIFIER_PARAM(context)
        .pipe(ALICE_INVOKES_ADD_VERIFIER_IN_BMC)
        .pipe(LINK_ADDRESS_PROVIDED_AS_ADD_LINK_PARAM)
        .pipe(ALICE_INVOKES_ADD_LINK_IN_BMC)
};

pub static REMOVE_LINK_INVKED_BY_BMC_OWNER: fn(Context) -> Context = |mut context: Context| {
    VERIFIER_NETWORKADDRESS_AND_VERIFIER_ADDRESS_PROVIDED_AS_ADD_VERIFIER_PARAM(context)
        .pipe(ALICE_INVOKES_ADD_VERIFIER_IN_BMC)
        .pipe(LINK_ADDRESS_PROVIDED_AS_ADD_LINK_PARAM)
        .pipe(ALICE_INVOKES_ADD_LINK_IN_BMC)
        .pipe(LINK_ADDRESS_PROVIDED_AS_REMOVE_LINK_PARAM)
        .pipe(ALICE_INVOKES_REMOVE_LINK_IN_BMC)
};

pub static REMOVE_LINK_INVKED_BY_NON_BMC_OWNER: fn(Context) -> Context = |mut context: Context| {
    VERIFIER_NETWORKADDRESS_AND_VERIFIER_ADDRESS_PROVIDED_AS_ADD_VERIFIER_PARAM(context)
        .pipe(ALICE_INVOKES_ADD_VERIFIER_IN_BMC)
        .pipe(LINK_ADDRESS_PROVIDED_AS_ADD_LINK_PARAM)
        .pipe(ALICE_INVOKES_ADD_LINK_IN_BMC)
        .pipe(LINK_ADDRESS_PROVIDED_AS_REMOVE_LINK_PARAM)
        .pipe(CHUCK_INVOKES_REMOVE_LINK_IN_BMC)
};

pub static REMOVE_NON_EXISTING_LINK_BY_BMC_OWNER: fn(Context) -> Context =
    |mut context: Context| {
        VERIFIER_NETWORKADDRESS_AND_VERIFIER_ADDRESS_PROVIDED_AS_ADD_VERIFIER_PARAM(context)
            .pipe(ALICE_INVOKES_ADD_VERIFIER_IN_BMC)
            .pipe(LINK_ADDRESS_PROVIDED_AS_ADD_LINK_PARAM)
            .pipe(ALICE_INVOKES_ADD_LINK_IN_BMC)
            .pipe(NON_EXISTING_LINK_ADDRESS_PROVIDED_AS_REMOVE_LINK_PARAM)
            .pipe(ALICE_INVOKES_REMOVE_LINK_IN_BMC)
    };

pub static LINK_ADDRESS_PROVIDED_AS_ADD_LINK_PARAM: fn(Context) -> Context =
    |mut context: Context| {
        context.add_method_params(
            "add_link",
            json!({
                "link": "BTPADDRESS"
            }),
        );
        context
    };

pub static NON_EXISTING_LINK_ADDRESS_PROVIDED_AS_REMOVE_LINK_PARAM: fn(Context) -> Context =
    |mut context: Context| {
        context.add_method_params(
            "remove_link",
            json!({
                "link": "NONEXISTING_BTPADDRESS"
            }),
        );
        context
    };

pub static LINK_ADDRESS_PROVIDED_AS_REMOVE_LINK_PARAM: fn(Context) -> Context =
    |mut context: Context| {
        context.add_method_params(
            "remove_link",
            json!({
                "link": "BTPADDRESS"
            }),
        );
        context
    };

pub static INVALID_LINK_ADDRESS_PROVIDED_AS_ADD_LINK_PARAM: fn(Context) -> Context =
    |mut context: Context| {
        context.add_method_params(
            "add_link",
            json!({
                "link": "INVALIDVERIFERADDRESS"
            }),
        );
        context
    };

pub static ADD_LINK_WITH_INVALID_ADDRESS_INVOKED_BY_BMC_OWNER: fn(Context) -> Context =
    |mut context: Context| {
        //provide the link to be added
        //link provided shoud be in verifier
        VERIFIER_NETWORKADDRESS_AND_VERIFIER_ADDRESS_PROVIDED_AS_ADD_VERIFIER_PARAM(context)
            .pipe(ALICE_INVOKES_ADD_VERIFIER_IN_BMC)
            .pipe(INVALID_LINK_ADDRESS_PROVIDED_AS_ADD_LINK_PARAM)
            .pipe(ALICE_INVOKES_ADD_LINK_IN_BMC)
    };

pub static ADD_LINK_INVOKED_BY_NON_BMC_OWNER: fn(Context) -> Context = |mut context: Context| {
    //provide the link to be added
    //link provided shoud be in verifier
    VERIFIER_NETWORKADDRESS_AND_VERIFIER_ADDRESS_PROVIDED_AS_ADD_VERIFIER_PARAM(context)
        .pipe(ALICE_INVOKES_ADD_VERIFIER_IN_BMC)
        .pipe(LINK_ADDRESS_PROVIDED_AS_ADD_LINK_PARAM)
        .pipe(CHUCK_INVOKES_ADD_LINK_IN_BMC)
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

pub static ICON_LINK_ADDRESS_IS_PROVIDED_AS_GET_STATUS_PARAM: fn(Context) -> Context =
    |mut context: Context| {
        context.add_method_params(
            "get_status",
            json!({ "link": format!("btp://{}/{}", ICON_NETWORK, ICON_BMC) }),
        );
        context
    };

pub static LINKS_ARE_QURIED_IN_BMC: fn(Context) -> Context =
    |context: Context| BMC_CONTRACT.get_links(context);

pub static QUERY_LINK_STATUS_BMC: fn(Context) -> Context =
    |context: Context| BMC_CONTRACT.get_status(context);

pub static ICON_LINK_STATUS_SHOULD_BE_UPDATED: fn(Context) = |context: Context| {
    let context = context
        .pipe(ICON_LINK_ADDRESS_IS_PROVIDED_AS_GET_STATUS_PARAM)
        .pipe(QUERY_LINK_STATUS_BMC);
    let result: LinkStatus = from_value(context.method_responses("get_status")).unwrap();
    assert_eq!(
        result.delay_limit(),
        4
    );
};

pub static ADDED_LINK_SHOULD_BE_IN_LIST: fn(Context) = |mut context: Context| {
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

pub static LEN_OF_LINKS_SHOULD_BE_11: fn(Context) = |mut context: Context| {
    let link = context.method_responses("get_links");

    let result: HashSet<_> = from_value::<Vec<String>>(link)
        .unwrap()
        .into_iter()
        .collect();
    assert_eq!(result.len(), 11);
};

pub static LEN_OF_LINKS_SHOULD_BE_13: fn(Context) = |mut context: Context| {
    let link = context.method_responses("get_links");

    let result: HashSet<_> = from_value::<Vec<String>>(link)
        .unwrap()
        .into_iter()
        .collect();
    assert_eq!(result.len(), 13);
};

pub static REMOVED_LINK_SHOULD_NOT_BE_PRESENT: fn(Context) = |context: Context| {
    let link = context.method_responses("get_links");

    let result: HashSet<_> = from_value::<Vec<String>>(link)
        .unwrap()
        .into_iter()
        .collect();
    let expected: HashSet<_> = vec!["BTPADDRES_OF_THE_LINK_PROVIDED".to_string()]
        .into_iter()
        .collect();
    assert_ne!(result, expected);
};

pub static ICON_LINK_ADDRESS_IS_PROVIDED_AS_ADD_LINK_PARAM: fn(Context) -> Context =
    |mut context: Context| {
        context.add_method_params(
            "add_link",
            json!({ "link": format!("btp://{}/{}", ICON_NETWORK, ICON_BMC) }),
        );
        context
    };

pub static ICON_LINK_IS_ADDED: fn(Context) -> Context = |context: Context| {
    context
        .pipe(VERIFIER_FOR_ICON_IS_ADDED)
        .pipe(ICON_LINK_ADDRESS_IS_PROVIDED_AS_ADD_LINK_PARAM)
        .pipe(ALICE_INVOKES_ADD_LINK_IN_BMC)
};

pub static USER_INVOKES_ADD_LINK_IN_BMC: fn(Context) -> Context =
    |context: Context| BMC_CONTRACT.add_link(context, 300000000000000);

pub static USER_INVOKES_SET_LINK_IN_BMC: fn(Context) -> Context =
    |context: Context| BMC_CONTRACT.set_link(context, 300000000000000);

pub static USER_INVOKES_REMOVE_LINK_IN_BMC: fn(Context) -> Context =
    |context: Context| BMC_CONTRACT.remove_link(context);

pub static ALICE_INVOKES_ADD_LINK_IN_BMC: fn(Context) -> Context = |context: Context| {
    context
        .pipe(ALICE_IS_THE_SIGNER)
        .pipe(USER_INVOKES_ADD_LINK_IN_BMC)
};

pub static ALICE_INVOKES_SET_LINK_IN_BMC: fn(Context) -> Context = |context: Context| {
    context
        .pipe(ALICE_IS_THE_SIGNER)
        .pipe(USER_INVOKES_SET_LINK_IN_BMC)
};

pub static ALICE_INVOKES_REMOVE_LINK_IN_BMC: fn(Context) -> Context = |context: Context| {
    context
        .pipe(ALICE_IS_THE_SIGNER)
        .pipe(USER_INVOKES_REMOVE_LINK_IN_BMC)
};

pub static CHUCK_INVOKES_ADD_LINK_IN_BMC: fn(Context) -> Context = |context: Context| {
    context
        .pipe(CHUCK_IS_THE_SIGNER)
        .pipe(USER_INVOKES_ADD_LINK_IN_BMC)
};

pub static CHUCK_INVOKES_REMOVE_LINK_IN_BMC: fn(Context) -> Context = |context: Context| {
    context
        .pipe(CHUCK_IS_THE_SIGNER)
        .pipe(USER_INVOKES_REMOVE_LINK_IN_BMC)
};

pub static ALREADY_HAVE_10_EXISTING_LINKS: fn(Context) -> Context = |context: Context| {
    let links = (2..12)
        .collect::<Vec<u16>>()
        .iter()
        .map(|index| {
            BTPAddress::new(format!(
                "btp://0x{}.icon/0xc294b1A62E82d3f135A8F9b2f9cAEAA23fbD6Cf5",
                index
            ))
        })
        .collect::<Vec<BTPAddress>>();

    links.into_iter().for_each(|link| {
        let mut context = context.clone();
        context.add_method_params(
            "add_verifier",
            json!({
                "network": link.network_address().unwrap(),
                "verifier": "test.near"
            }),
        );
        context = context.pipe(BMC_OWNER_INVOKES_ADD_VERIFIER_IN_BMC);
        context.add_method_params("add_link", json!({ "link": link.to_string()}));
        context.pipe(USER_INVOKES_ADD_LINK_IN_BMC);
    });

    context
};

pub static ALREADY_HAVE_12_EXISTING_LINKS: fn(Context) -> Context = |context: Context| {
    let links = (2..14)
        .collect::<Vec<u16>>()
        .iter()
        .map(|index| {
            BTPAddress::new(format!(
                "btp://0x{}.icon/0xc294b1A62E82d3f135A8F9b2f9cAEAA23fbD6Cf5",
                index
            ))
        })
        .collect::<Vec<BTPAddress>>();

    links.into_iter().for_each(|link| {
        let mut context = context.clone();
        context.add_method_params(
            "add_verifier",
            json!({
                "network": link.network_address().unwrap(),
                "verifier": "test.near"
            }),
        );
        context = context.pipe(BMC_OWNER_INVOKES_ADD_VERIFIER_IN_BMC);
        context.add_method_params("add_link", json!({ "link": link.to_string()}));
        context.pipe(USER_INVOKES_ADD_LINK_IN_BMC);
    });

    context
};
