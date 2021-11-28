mod steps;

#[cfg(test)]
mod manage_verifers {
    use super::*;
    use kitten::*;
    use steps::*;

    mod bmc {
        use super::*;
        
        #[ignore]
        #[workspaces::test(sandbox)]
        async fn add_relay_as_authorized_success(){
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED)
                .when(ADD_RELAY_INVOKED_BY_BMC_OWNER)
                .then(RELAYS_ARE_QURIED_IN_BMC)
                .and(ADDED_RELAYS_SHOULD_BE_IN_LIST)

        }

        #[ignore]
        #[workspaces::test(sandbox)]
        async fn add_relays_as_authorized_success(){
            Kitten::given(NEW_CONTEXT)
            .and(BMC_CONTRACT_IS_DEPLOYED)
            .when(ADD_RELAYS_INVOKED_BY_BMC_OWNER)
            .then(RELAYS_ARE_QURIED_IN_BMC)
            .and(ADDED_RELAYS_SHOULD_BE_IN_LIST)
        }

        #[ignore]
        #[workspaces::test(sandbox)]
        async fn add_relay_as_unauthorized_fail(){
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED)
                .when(ADD_RELAY_INVOKED_BY_NON_BMC_OWNER)
                .then(BMC_SHOULD_THROW_UNAUTHORIZED_ERROR)
        }

        #[ignore]
        #[workspaces::test(sandbox)]
        async fn add_relays_as_unauthorized_fail(){
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED)
                .when(ADD_RELAYS_INVOKED_BY_NON_BMC_OWNER)
                .then(BMC_SHOULD_THROW_UNAUTHORIZED_ERROR)
        }

        #[ignore]
        #[workspaces::test(sandbox)]
        async fn add_relay_to_non_existing_link_authorized_fail(){
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED)
                .when(ADD_RELAY_WITH_NON_EXISTING_LINK_INVOKED_BY_BMC_OWNER)
                .then(BMC_SHOULD_THROW_NOTEXIST_ERROR)

        }

        #[ignore]
        #[workspaces::test(sandbox)]
        async fn remove_relay_as_authorized_success(){
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED)
                .when(REMOVE_RELAY_INVOKED_BY_BMC_OWNER)
                .then(RELAYS_ARE_QURIED_IN_BMC)
                .and(DELETED_RELAY_SHOULD_NOT_BE_IN_LIST)
        }
        
        #[ignore]
        #[workspaces::test(sandbox)]
        async fn remove_relay_as_unauthorized_success(){
            Kitten::given(NEW_CONTEXT)
            .and(BMC_CONTRACT_IS_DEPLOYED)
            .when(REMOVE_RELAYS_INVOKED_BY_NON_BMC_OWNER)
            .then(BMC_SHOULD_THROW_UNAUTHORIZED_ERROR)
        }

        #[ignore]
        #[workspaces::test(sandbox)]
        async fn remove_non_existing_relay_authorized_success(){
            Kitten::given(NEW_CONTEXT)
            .and(BMC_CONTRACT_IS_DEPLOYED)
            .when(REMOVE_NON_EXISTING_RELAY_INVOKED_BY_BMC_OWNER)
            .then(BMC_SHOULD_THROW_NOTEXIST_ERROR)
        }

    }

}