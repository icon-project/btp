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
                .and(BMC_CONTRACT_IS_OWNED_BY_ALICE)
                .and(ICON_NETWORK_ADDRESS_AND_VERIFIER_ACCOUNT_ID_ARE_PROVIDED_AS_ADD_VERIFIER_PARAM)
                .when(ALICE_INVOKES_ADD_VERIFIER_IN_BMC)
                .then(THE_ADDED_VERIFIER_SHOULD_BE_IN_THE_LIST_OF_VERIFIERS)
        }

        #[workspaces::test(sandbox)]
        async fn add_verifier_unauthorized_fail() {
            Kitten::given(NEW_CONTEXT)
            .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
            .and(BMV_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
            .and(CHUCKS_ACCOUNT_IS_CREATED)
            .and(BMC_CONTRACT_IS_NOT_OWNED_BY_CHUCK)
            .and(ICON_NETWORK_ADDRESS_AND_VERIFIER_ACCOUNT_ID_ARE_PROVIDED_AS_ADD_VERIFIER_PARAM)
            .when(CHUCK_INVOKES_ADD_VERIFIER_IN_BMC)
            .then(BMC_SHOULD_THROW_UNAUTHORIZED_ERROR_FOR_VERIFIER)
        }

        #[workspaces::test(sandbox)]
        async fn query_verifiers_success() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(BMV_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(BMC_CONTRACT_IS_OWNED_BY_ALICE)
                .and(VERIFIER_FOR_ICON_IS_ADDED)
                .when(USER_INVOKES_GET_VERIFIERS_IN_BMC)
                .then(USER_SHOULD_GET_EXISITING_VERIFIERS_LIST)
        }

        #[workspaces::test(sandbox)]
        async fn add_existing_verifier_authorized_fail() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(BMV_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(BMC_CONTRACT_IS_OWNED_BY_ALICE)
                .and(VERIFIER_FOR_ICON_IS_ADDED)
                .and(ICON_BMV_ACCOUNT_ID_AND_ICON_NETWORK_ADDRESS_ARE_PROVIDED_AS_ADD_VERIFIER_PARAM)
                .when(ALICE_INVOKES_ADD_VERIFIER_IN_BMC)
                .then(BMC_SHOULD_THROW_VERIFIER_ALREADY_EXISTS_ERROR)
        }
    }
}
