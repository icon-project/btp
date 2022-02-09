use lazy_static::lazy_static;
use serde_json::json;
use test_helper::types::{Bmc, BmcContract, Context, Contract};

lazy_static! {
    pub static ref BMC_CONTRACT: Contract<'static, Bmc> =
        BmcContract::new("bmc", "res/BMC_CONTRACT.wasm");
}

pub static BMC_CONTRACT_IS_DEPLOYED: fn(Context) -> Context =
    |context| BMC_CONTRACT.deploy(context);

pub static BMC_CONTRACT_INITIALZIED: fn(Context) -> Context = |mut context: Context| {
    context.add_method_params(
        "new",
        json!({
            "network": super::NEAR_NETWORK,
            "block_interval": 1500
        }),
    );

    BMC_CONTRACT.initialize(context)
};

pub static BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED: fn(Context) -> Context = |context: Context| {
    context
        .pipe(BMC_CONTRACT_IS_DEPLOYED)
        .pipe(BMC_CONTRACT_INITIALZIED)
};

/*  * * * * * * * * * * * *
* * * * * * * * * * * * * *
*   Links         * * * * *
* * * * * * * * * * * * * *
* * * * * * * * * * * * * *
*/

pub static USER_INVOKES_ADD_LINK_IN_BMC: fn(Context) -> Context =
    |context: Context| BMC_CONTRACT.add_link(context, 30000000000000);

pub static USER_INVOKES_REMOVE_LINK_IN_BMC: fn(Context) -> Context =
    |context: Context| BMC_CONTRACT.remove_link(context);

pub static USER_INVOKES_SET_LINK_IN_BMC: fn(Context) -> Context =
    |context: Context| BMC_CONTRACT.set_link(context, 30000000000000);

pub static USER_INVOKES_GET_LINKS_IN_BMC: fn(Context) -> Context =
    |context: Context| BMC_CONTRACT.get_links(context);

pub static USER_INVOKES_GET_STATUS_IN_BMC: fn(Context) -> Context =
    |context: Context| BMC_CONTRACT.get_status(context);

/*  * * * * * * * * * * * *
* * * * * * * * * * * * * *
*   Routes         * * * * *
* * * * * * * * * * * * * *
* * * * * * * * * * * * * *
*/

pub static USER_INVOKES_ADD_ROUTE_IN_BMC: fn(Context) -> Context =
    |context: Context| BMC_CONTRACT.add_route(context);

pub static USER_INVOKES_REMOVE_ROUTE_IN_BMC: fn(Context) -> Context =
    |context: Context| BMC_CONTRACT.remove_route(context);

pub static USER_INVOKES_GET_ROUTES_IN_BMC: fn(Context) -> Context =
    |context: Context| BMC_CONTRACT.get_routes(context);

/*  * * * * * * * * * * * *
* * * * * * * * * * * * * *
*   Relay     * * * * *
* * * * * * * * * * * * * *
* * * * * * * * * * * * * *
*/

pub static USER_INVOKES_ADD_RELAY_IN_BMC: fn(Context) -> Context =
    |context: Context| BMC_CONTRACT.add_relay(context);

pub static USER_INVOKES_ADD_RELAYS_IN_BMC: fn(Context) -> Context =
    |context: Context| BMC_CONTRACT.add_relays(context);

pub static USER_INVOKES_REMOVE_RELAY_IN_BMC: fn(Context) -> Context =
    |context: Context| BMC_CONTRACT.remove_relay(context);

pub static USER_INVOKES_GET_RELAYS_IN_BMC: fn(Context) -> Context =
    |context: Context| BMC_CONTRACT.get_relays(context);

/*  * * * * * * * * * * * *
* * * * * * * * * * * * * *
*   Verifiers     * * * * *
* * * * * * * * * * * * * *
* * * * * * * * * * * * * *
*/

pub static USER_INVOKES_ADD_VERIFIER_IN_BMC: fn(Context) -> Context =
    |context: Context| BMC_CONTRACT.add_verifier(context, 30000000000000);

pub static USER_INVOKES_REMOVE_VERIFIER_IN_BMC: fn(Context) -> Context =
    |context: Context| BMC_CONTRACT.remove_verifier(context, 30000000000000);

pub static USER_INVOKES_GET_VERIFIERS_IN_BMC: fn(Context) -> Context =
    |context: Context| BMC_CONTRACT.get_verifiers(context);

/*  * * * * * * * * * * * *
* * * * * * * * * * * * * *
*   Owners      * * * * * *
* * * * * * * * * * * * * *
* * * * * * * * * * * * * *
*/

pub static USER_INVOKES_REMOVE_OWNER_IN_BMC: fn(Context) -> Context =
    |context: Context| BMC_CONTRACT.remove_owner(context);

pub static USER_INVOKES_ADD_OWNER_IN_BMC: fn(Context) -> Context =
    |context: Context| BMC_CONTRACT.add_owner(context);

pub static USER_INVOKES_GET_OWNERS_IN_BMC: fn(Context) -> Context =
    |context: Context| BMC_CONTRACT.get_owners(context);

/*  * * * * * * * * * * * *
* * * * * * * * * * * * * *
*   Services    * * * * * *
* * * * * * * * * * * * * *
* * * * * * * * * * * * * *
*/

pub static USER_INVOKES_ADD_SERVICE_IN_BMC: fn(Context) -> Context =
    |context: Context| BMC_CONTRACT.add_service(context);

pub static USER_INVOKES_REMOVE_SERVICE_IN_BMC: fn(Context) -> Context =
    |context: Context| BMC_CONTRACT.remove_service(context);

pub static USER_INVOKES_GET_SERVICES_IN_BMC: fn(Context) -> Context =
    |context: Context| BMC_CONTRACT.get_services(context);

/*  * * * * * * * * * * * *
* * * * * * * * * * * * * *
*   Handle Messages * * * *
* * * * * * * * * * * * * *
* * * * * * * * * * * * * *
*/

pub static USER_INVOKES_HANDLE_RELAY_MESSAGE_IN_BMC: fn(Context) -> Context =
    |context: Context| BMC_CONTRACT.handle_relay_message(context, 300_000_000_000_000);
