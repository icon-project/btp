mod steps;

#[cfg(test)]
mod manage_routes {
    use super::*;
    use kitten::*;
    use steps::*;

    #[tokio::test(flavor = "multi_thread")]
    async fn bmc_owner_can_remove_a_registered_route_to_the_next_bmc() {
        Kitten::given(NEW_CONTEXT)
            .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
            .and(BMV_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
            .and(BMC_CONTRACT_IS_OWNED_BY_ALICE)
            .and(ICON_LINK_IS_PRESENT_IN_BMC)
            .and(BSC_LINK_ADDRESS_AS_DESTINATION_AND_ICON_LINK_ADDRESS_AS_LINK_ARE_PROVIDED_AS_ADD_ROUTE_PARAM)
            .and(ALICE_INVOKES_ADD_ROUTE_IN_BMC)
            .and(BSC_LINK_ADDRESS_IS_PROVIDED_AS_REMOVE_ROUTE_PARAM)
            .when(ALICE_INVOKES_REMOVE_ROUTE_IN_BMC)
            .then(THE_REMOVED_ROUTE_SHOULD_NOT_BE_PRESENT_IN_THE_LIST_OF_ROUTES);
    }

    #[tokio::test(flavor = "multi_thread")]
    async fn users_can_get_list_of_routes_registered_in_a_bmc() {
        Kitten::given(NEW_CONTEXT)
            .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
            .and(BMV_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
            .and(ROUTE_TO_BSC_IS_PRESENT_IN_BMC)
            .when(USER_INVOKES_GET_ROUTES_IN_BMC)
            .then(USER_SHOULD_GET_THE_EXISITING_LIST_OF_ROUTES);
    }

    #[tokio::test(flavor = "multi_thread")]
    async fn bmc_owner_cannot_add_route_with_an_invalid_btp_link_address() {
        Kitten::given(NEW_CONTEXT)
            .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
            .and(BMV_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
            .and(BMC_CONTRACT_IS_OWNED_BY_ALICE)
            .and(ICON_LINK_IS_PRESENT_IN_BMC)
            .and(BSC_LINK_ADDRESS_AS_DESTINATION_AND_INVALID_BTP_ADDRESS_AS_LINK_ARE_PROVIDED_AS_ADD_ROUTE_PARAM)
            .when(ALICE_INVOKES_ADD_ROUTE_IN_BMC)
            .then(BMC_SHOULD_THROW_INVALID_BTP_ADDRESS_ERROR_ON_ADDING_ROUTE);
    }

    #[tokio::test(flavor = "multi_thread")]
    async fn bmc_owner_cannot_add_route_with_an_invalid_btp_route_address() {
        Kitten::given(NEW_CONTEXT)
            .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
            .and(BMV_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
            .and(BMC_CONTRACT_IS_OWNED_BY_ALICE)
            .and(ICON_LINK_IS_PRESENT_IN_BMC)
            .and(INVALID_BTP_ADDRESS_AS_DESTINATION_ADDRESS_AND_ICON_LINK_ADDRESS_AS_LINK_ARE_PROVIDED_AS_ADD_ROUTE_PARAM)
            .when(ALICE_INVOKES_ADD_ROUTE_IN_BMC)
            .then(BMC_SHOULD_THROW_INVALID_BTP_ADDRESS_ERROR_ON_ADDING_ROUTE);
    }

    #[tokio::test(flavor = "multi_thread")]
    async fn bmc_owner_cannot_add_existing_route_in_bmc() {
        Kitten::given(NEW_CONTEXT)
        .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
        .and(BMV_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
        .and(BMC_CONTRACT_IS_OWNED_BY_ALICE)
        .and(ICON_LINK_IS_PRESENT_IN_BMC)
        .and(ROUTE_TO_BSC_IS_PRESENT_IN_BMC)
        .and(BSC_LINK_ADDRESS_AS_DESTINATION_AND_ICON_LINK_ADDRESS_AS_LINK_ARE_PROVIDED_AS_ADD_ROUTE_PARAM)
        .when(ALICE_INVOKES_ADD_ROUTE_IN_BMC)
        .then(BMC_SHOULD_THROW_ROUTE_ALREADY_EXIST_ERROR_ON_ADDING_ROUTE);
    }

    #[tokio::test(flavor = "multi_thread")]
    async fn bmc_owner_cannot_remove_a_non_existing_route_from_bmc() {
        Kitten::given(NEW_CONTEXT)
            .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
            .and(BMV_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
            .and(BMC_CONTRACT_IS_OWNED_BY_ALICE)
            .and(ICON_LINK_IS_PRESENT_IN_BMC)
            .and(BSC_LINK_ADDRESS_IS_PROVIDED_AS_REMOVE_ROUTE_PARAM)
            .when(ALICE_INVOKES_REMOVE_ROUTE_IN_BMC)
            .then(BMC_SHOULD_THROW_ROUTE_DOES_NOT_EXIST_ERROR_ON_REMOVING_ROUTE);
    }

    #[tokio::test(flavor = "multi_thread")]
    async fn non_bmc_owner_cannot_add_a_route_to_the_linked_bmc_of_the_connected_cross_chain_bmc() {
        Kitten::given(NEW_CONTEXT)
        .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
        .and(BMV_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
        .and(CHUCKS_ACCOUNT_IS_CREATED)
        .and(BMC_CONTRACT_IS_NOT_OWNED_BY_CHUCK)
        .and(ICON_LINK_IS_PRESENT_IN_BMC)
        .and(BSC_LINK_ADDRESS_AS_DESTINATION_AND_ICON_LINK_ADDRESS_AS_LINK_ARE_PROVIDED_AS_ADD_ROUTE_PARAM)
        .when(CHUCK_INVOKES_ADD_ROUTE_IN_BMC)
        .then(BMC_SHOULD_THROW_UNAUTHORISED_ERROR_ON_ADDING_ROUTE);
    }

    #[tokio::test(flavor = "multi_thread")]
    async fn non_bmc_owner_cannot_remove_a_route_to_the_linked_bmc_of_the_connected_cross_chain_bmc() {
        Kitten::given(NEW_CONTEXT)
        .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
        .and(BMV_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
        .and(CHUCKS_ACCOUNT_IS_CREATED)
        .and(BMC_CONTRACT_IS_NOT_OWNED_BY_CHUCK)
        .and(ICON_LINK_IS_PRESENT_IN_BMC)
        .and(ROUTE_TO_BSC_IS_PRESENT_IN_BMC)
        .and(BSC_LINK_ADDRESS_IS_PROVIDED_AS_REMOVE_ROUTE_PARAM)
        .when(CHUCK_INVOKES_REMOVE_ROUTE_IN_BMC)
        .then(BMC_SHOULD_THROW_UNAUTHORISED_ERROR_ON_REMOVING_ROUTE);
    }
}
