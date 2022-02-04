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
                .and(ADDED_RELAYS_SHOULD_BE_IN_BMC_RELAY_LIST)

        }

        #[ignore]
        #[workspaces::test(sandbox)]
        async fn add_relays_as_authorized_success(){
            Kitten::given(NEW_CONTEXT)
            .and(BMC_CONTRACT_IS_DEPLOYED)
            .when(ADD_RELAYS_INVOKED_BY_BMC_OWNER)
            .then(RELAYS_ARE_QURIED_IN_BMC)
            .and(ADDED_RELAYS_SHOULD_BE_IN_BMC_RELAY_LIST)
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

        #[workspaces::test(sandbox)]
        async fn bmc_owner_can_add_bmr_as_list_and_overwrite_an_existing_bmr() {
          Kitten::given(NEW_CONTEXT)
            .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
            .and(BMV_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
            .and(ALICE_IS_BMC_CONTRACT_OWNER)
            .and(VERIFIER_FOR_ICON_IS_ADDED)
            .and(ICON_LINK_ADDRESS_IS_PROVIDED_AND_ADDED_IN_BMC)
            .and(RELAY_1_IS_REGISTERED)
            .and(ICON_LINK_ADDRESS_AND_RELAY_1_IS_PROVIDED_AS_ADD_RELAY_PARAM)
            .and(ALICE_INVOKES_ADD_RELAY_IN_BMC)
            .when(ALICE_INVOKES_GET_RELAY_IN_BMC)
            .then(ADDED_RELAYS_SHOULD_BE_IN_BMC_RELAY_LIST)
        }
    }

}