mod steps;

#[cfg(test)]
mod manage_verifiers {
    use super::*;
    use kitten::*;
    use steps::*;

    mod bmc {
        use super::*;

        #[workspaces::test(sandbox)]
        async fn add_verifier_authorized_success() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(BMV_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(ALICE_IS_BMC_CONTRACT_OWNER)
                .and(VERIFIER_NETWORKADDRESS_AND_VERIFIER_ADDRESS_PROVIDED_AS_ADD_VERIFIER_PARAM)
                .when(ALICE_INVOKES_ADD_VERIFIER_IN_BMC)
                .then(ADDED_VERIFIER_SHOULD_BE_IN_LIST_OF_VERIFIERS)
        }

        #[ignore]
        #[workspaces::test(sandbox)]
        async fn add_invalid_verifier_authorized_fail() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED)
                .and(BMV_CONTRACT_IS_DEPLOYED)
                .when(ADD_VERIFIER_INVOKED_WITH_INVALID_VERIFIER_ADDRESS)
                .then(BMC_SHOULD_THROW_INVALIDADDRESS_ERROR)
        }

        #[ignore]
        #[workspaces::test(sandbox)]
        async fn remove_verifier_authorized_success() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED)
                .and(BMV_CONTRACT_IS_DEPLOYED)
                .when(CHUCK_INVOKES_ADD_VERIFIER_IN_BMC)
                .then(VERIFIER_DELETED_SHOULD_NOT_BE_IN_LIST_OF_VERIFIERS)
        }

        #[ignore]
        #[workspaces::test(sandbox)]
        async fn remove_verifier_unauthorized_fail() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED)
                .and(BMV_CONTRACT_IS_DEPLOYED)
                .when(CHUCK_INVOKES_ADD_VERIFIER_IN_BMC)
                .then(BMC_SHOULD_THROW_UNAUTHORIZED_ERROR)
        }
        
        #[workspaces::test(sandbox)]
        async fn add_verifier_unauthorized_fail() {
            Kitten::given(NEW_CONTEXT)
            .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
            .and(BMV_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
            .and(ALICE_IS_BMC_CONTRACT_OWNER)
            .and(CHUCK_IS_NOT_A_BMC_OWNER)
            .and(VERIFIER_NETWORKADDRESS_AND_VERIFIER_ADDRESS_PROVIDED_AS_ADD_VERIFIER_PARAM)
            .when(CHUCK_INVOKES_ADD_VERIFIER_IN_BMC)
            .then(BMC_SHOULD_THROW_UNAUTHORIZED_ERROR_FOR_VERIFIER)
        }

        #[workspaces::test(sandbox)]
        async fn query_verifier_verifers_success() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(BMV_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(ALICE_IS_BMC_CONTRACT_OWNER)
                .and(ICON_BMV_AND_ICON_NETWORK_IS_PROVIDED_AS_ADD_VERIFIER_PARAM)
                .when(ALICE_INVOKES_ADD_VERIFIER_IN_BMC)
                .then(VERIFIERS_IN_BMC_ARE_QUERIED)          
            }
            #[workspaces::test(sandbox)]
            async fn add_existing_verifier_authorized_fail() {
                Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(BMV_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(ALICE_IS_BMC_CONTRACT_OWNER)
                .and(VERIFIER_FOR_ICON_IS_ADDED)
                .and(ICON_BMV_AND_ICON_NETWORK_IS_PROVIDED_AS_ADD_VERIFIER_PARAM)
                .when(ALICE_INVOKES_ADD_VERIFIER_IN_BMC)
                .then(BMC_SHOULD_THROW_VERIFIER_ALREADY_EXISTS_ERROR)
            }
        }
    }

