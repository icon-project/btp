mod steps;

#[cfg(test)]
mod manage_routes {
    use super::*;
    use kitten::*;
    use steps::*;

    #[workspaces::test(sandbox)]
    async fn remove_registered_route_as_authorized_success() {
        Kitten::given(NEW_CONTEXT)
            .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
            .and(BMV_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
            .and(BMC_CONTRACT_IS_OWNED_BY_ALICE)
            .and(VERIFIER_FOR_ICON_IS_ADDED)
            .and(ICON_LINK_IS_PRESENT_IN_BMC)
            .and(BSC_LINK_ADDRESS_AS_DESTINATION_AND_ICON_LINK_ADDRESS_AS_LINK_ARE_PROVIDED_AS_ADD_ROUTE_PARAM)
            .and(ALICE_INVOKES_ADD_ROUTE_IN_BMC)
            .and(BSC_LINK_ADDRESS_IS_PROVIDED_AS_REMOVE_ROUTE_PARAM)
            .when(ALICE_INVOKES_REMOVE_ROUTE_IN_BMC)
            .then(THE_REMOVED_ROUTE_SHOULD_NOT_BE_PRESENT_IN_THE_LIST_OF_ROUTES)
    }

    #[workspaces::test(sandbox)]
    async fn get_list_of_routes_from_bmc() {
        Kitten::given(NEW_CONTEXT)
            .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
            .and(BMV_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
            .and(ROUTE_TO_BSC_IS_PRESENT_IN_BMC)
            .when(USER_INVOKES_GET_ROUTES_IN_BMC)
            .then(USER_SHOULD_GET_EXISITING_ROUTES_LIST)
    }

    #[workspaces::test(sandbox)]
    async fn bmc_owner_cannot_add_route_with_a_invalid_btp_link_address() {
        Kitten::given(NEW_CONTEXT)
            .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
            .and(BMV_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
            .and(BMC_CONTRACT_IS_OWNED_BY_ALICE)
            .and(ICON_LINK_IS_PRESENT_IN_BMC)
            .and(BSC_LINK_ADDRESS_AS_DESTINATION_AND_INVALID_BTP_ADDRESS_AS_LINK_ARE_PROVIDED_AS_ADD_ROUTE_PARAM)
            .when(ALICE_INVOKES_ADD_ROUTE_IN_BMC)
            .then(BMC_SHOULD_THROW_INVALID_BTP_ADDRESS_ERROR)
    }

    #[workspaces::test(sandbox)]
    async fn bmc_owner_cannot_add_route_with_a_invalid_btp_route_address() {
        Kitten::given(NEW_CONTEXT)
            .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
            .and(BMV_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
            .and(BMC_CONTRACT_IS_OWNED_BY_ALICE)
            .and(ICON_LINK_IS_PRESENT_IN_BMC)
            .and(INVALID_BTP_ADDRESS_AS_DESTINATION_ADDRESS_AND_ICON_LINK_ADDRESS_AS_LINK_ARE_PROVIDED_AS_ADD_ROUTE_PARAM)
            .when(ALICE_INVOKES_ADD_ROUTE_IN_BMC)
            .then(BMC_SHOULD_THROW_INVALID_BTP_ADDRESS_ERROR)
    }
}
