mod steps;

#[cfg(test)]
mod manage_verifiers {
    use super::*;
    use kitten::*;
    use steps::*;

    mod bmc {
        use super::*;

        #[ignore]
        #[workspaces::test(sandbox)]
        async fn add_verifier_authorized_success() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED)
                .and(BMV_CONTRACT_IS_DEPLOYED)
                .when(VERIFIER_IS_ADDED_BY_BMC_CONTRACT_OWNER)
                .then(VERIFIERS_IN_BMC_ARE_QUERIED)
                .and(ADDED_VERIFIER_SHOULD_BE_IN_LIST_OF_VERIFIERS)
        }

        #[ignore]
        #[workspaces::test(sandbox)]
        async fn add_existing_verifier_authorized_fail() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED)
                .and(BMV_CONTRACT_IS_DEPLOYED)
                .when(EXISTING_VERIFIER_IS_ADDED_AGAIN_BY_BMC_OWNER)
                .then(BMC_SHOULD_THROW_ALREADY_EXIST_ERROR)
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
                .when(REMOVE_VERIFIER_INOKED_BY_BMC_OWNER)
                .then(VERIFIER_DELETED_SHOULD_NOT_BE_IN_LIST_OF_VERIFIERS)
        }

        #[ignore]
        #[workspaces::test(sandbox)]
        async fn remove_verifier_unauthorized_fail() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED)
                .and(BMV_CONTRACT_IS_DEPLOYED)
                .when(REMOVE_VERIFIER_INOKED_BY_NON_BMC_OWNER)
                .then(BMC_SHOULD_THROW_UNAUTHORIZED_ERROR)
        }

        #[ignore]
        #[workspaces::test(sandbox)]
        async fn remove_nonexisting_verifier_authorized_fail() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(ALICE_IS_BMC_CONTRACT_OWNER)
                .when(REMOVE_VERIFIER_INOKED_BY_BMC_OWNER)
                .then(BMC_SHOULD_THROW_NOTEXIST_ERROR)
        }
        
        #[workspaces::test(sandbox)]
        async fn add_verifier_unauthorized_fail() {
            Kitten::given(NEW_CONTEXT)
            .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
            .and(BMV_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
            .and(ALICE_IS_BMC_CONTRACT_OWNER)
            .and(CHUCK_IS_NOT_A_BMC_OWNER)
            .and(ICON_BMV_AND_ICON_NETWORK_IS_PROVIDED_AS_ADD_VERIFIER_PARAM)
            .when(CHUCK_INVOKED_ADD_VERIFIER_IN_BMC)
            .then(BMC_SHOULD_THROW_UNAUTHORIZED_ERROR_FOR_VERIFIER)
        }
    }
}
