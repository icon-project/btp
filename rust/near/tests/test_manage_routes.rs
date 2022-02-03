mod steps;

#[cfg(test)]
mod manage_verifers {
    use super::*;
    use kitten::*;
    use steps::*;

    #[workspaces::test(sandbox)]
    async fn remove_registered_route_as_authorized_success() {
        Kitten::given(NEW_CONTEXT)
            .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
            .and(BMV_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
            .and(ALICE_IS_BMC_CONTRACT_OWNER)
            .and(VERIFIER_FOR_ICON_IS_ADDED)
            .and(ICON_LINK_ADDRESS_IS_PROVIDED_AND_ADDED_IN_BMC)
            .and(DESTINATION_AND_LINK_ADDRESS_ARE_PROVIDED_AS_ADD_ROUTE_PARAM)
            .and(ALICE_INVOKES_ADD_ROUTE_IN_BMC)
            .and(DESTINATION_ADDRESS_IS_PROVIDED_AS_REMOVE_ROUTE_PARAM)
            .when(ALICE_INVOKES_REMOVE_ROUTE_IN_BMC)
            .then(REMOVED_ROUTE_SHOULD_NOT_BE_PRESENT)
    }

    #[workspaces::test(sandbox)]
    async fn get_list_of_routes_from_bmc() {
        Kitten::given(NEW_CONTEXT)
            .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
            .and(BMV_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
            .and(ALICE_IS_BMC_CONTRACT_OWNER)
            .and(VERIFIER_FOR_ICON_IS_ADDED)
            .and(ICON_LINK_ADDRESS_IS_PROVIDED_AND_ADDED_IN_BMC)
            .and(DESTINATION_AND_LINK_ADDRESS_ARE_PROVIDED_AS_ADD_ROUTE_PARAM)
            .when(ALICE_INVOKES_ADD_ROUTE_IN_BMC)
            .then(ADDED_ROUTES_SHOULD_PRESENT_IN_ROUTES_LIST)
    }

    #[ignore]
    #[workspaces::test(sandbox)]
    async fn add_route_as_authorized_success() {
        Kitten::given(NEW_CONTEXT)
            .and(BMC_CONTRACT_IS_DEPLOYED)
            .when(ADD_ROUTE_INVOKED_BY_BMC_OWNER)
            .then(ROUTES_ARE_QURIED_IN_BMC)
            .and(ADDED_ROUTE_SHOULD_BE_PRESENT)
    }

    #[ignore]
    #[workspaces::test(sandbox)]
    async fn add_route_as_unauthorized_fail() {
        Kitten::given(NEW_CONTEXT)
            .and(BMC_CONTRACT_IS_DEPLOYED)
            .when(ADD_ROUTE_INVOKED_BY_NON_BMC_OWNER)
            .then(BMC_SHOULD_THROW_UNAUTHORIZED_ERROR)
    }

    #[ignore]
    #[workspaces::test(sandbox)]
    async fn add_existing_route_as_authorized_success() {
        Kitten::given(NEW_CONTEXT)
            .and(BMC_CONTRACT_IS_DEPLOYED)
            .when(ADD_EXISTING_ROUTE_INVOKED_BY_BMC_OWNER)
            .then(BMC_SHOULD_THROW_ALREADY_EXIST_ERROR)
    }

    #[ignore]
    #[workspaces::test(sandbox)]
    async fn remove_route_as_authorized_success() {
        Kitten::given(NEW_CONTEXT)
            .and(BMC_CONTRACT_IS_DEPLOYED)
            .when(REMOVE_ROUTE_INVOKED_BY_BMC_OWNER)
            .then(ROUTES_ARE_QURIED_IN_BMC)
            .and(REMOVED_ROUTE_SHOULD_NOT_BE_PRESENT)
    }

    #[ignore]
    #[workspaces::test(sandbox)]
    async fn remove_route_as_unauthorized_fail() {
        Kitten::given(NEW_CONTEXT)
            .and(BMC_CONTRACT_IS_DEPLOYED)
            .when(REMOVE_ROUTE_INVOKED_BY_NON_BMC_OWNER)
            .then(BMC_SHOULD_THROW_UNAUTHORIZED_ERROR)
    }

    #[ignore]
    #[workspaces::test(sandbox)]
    async fn remove_non_existing_route_as_authorized_fail() {
        Kitten::given(NEW_CONTEXT)
            .and(BMC_CONTRACT_IS_DEPLOYED)
            .when(REMOVE_NON_EXISTING_ROUTE_INVOKED_BY_BMC_OWNER)
            .then(BMC_SHOULD_THROW_NOTEXIST_ERROR)
    }
}
