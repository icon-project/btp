mod steps;

#[cfg(test)]
mod manage_verifers {
    use super::*;
    use kitten::*;
    use steps::*;

    mod bmc {

        #[runner::test(sandbox)]
        async fn add_route_as_authorized_success() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED)
                .when(ADD_ROUTE_INVOKED_BY_BMC_OWNER)
                .then(ROUTES_ARE_QURIED_IN_BMC)
                .and(ADDED_ROUTE_SHOULD_BE_PRESENT)
        }

        async fn add_route_as_unauthorized_fail() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED)
                .when(ADD_ROUTE_INVOKED_BY_NON_BMC_OWNER)
                .then(BMC_SHOULD_THROW_UNAUTHORIZED_ERROR)
        }

        async fn add_existing_route_as_authorized_success() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED)
                .when(ADD_EXISTING_ROUTE_INVOKED_BY_BMC_OWNER)
                .then(BMC_SHOULD_THROW_ALREADY_EXIST_ERROR)
        }

        async fn remove_route_as_authorized_success() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED)
                .when(REMOVE_ROUTE_INVOKED_BY_BMC_OWNER)
                .then(ROUTES_ARE_QURIED_IN_BMC)
                .and(REMOVED_ROUTE_SHOULD_NOT_BE_PRESENT)
        }

        async fn remove_route_as_unauthorized_fail() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED)
                .when(REMOVE_ROUTE_INVOKED_BY_NON_BMC_OWNER)
                .then(BMC_SHOULD_THROW_UNAUTHORIZED_ERROR)
        }

        async fn remove_non_existing_route_as_authorized_fail() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED)
                .when(REMOVE_NON_EXISTING_ROUTE_INVOKED_BY_BMC_OWNER)
                .then(BMC_SHOULD_THROW_NOTEXIST_ERROR)
        }
    }
}
