mod steps;

#[cfg(test)]
mod manage_verifiers {
    use super::*;
    use kitten::*;
    use steps::*;

    mod bmc {
        use super::*;

        #[tokio::test(flavor = "multi_thread")]
        async fn bmc_owner_can_register_a_cross_chain_bmv() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(BMV_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(BMC_CONTRACT_IS_OWNED_BY_ALICE)
                .and(ICON_NETWORK_ADDRESS_AND_VERIFIER_ACCOUNT_ID_ARE_PROVIDED_AS_ADD_VERIFIER_PARAM)
                .when(ALICE_INVOKES_ADD_VERIFIER_IN_BMC)
                .then(THE_ADDED_VERIFIER_SHOULD_BE_IN_THE_LIST_OF_VERIFIERS);
        }

        #[tokio::test(flavor = "multi_thread")]
        async fn non_bmc_owner_cannot_register_a_cross_chain_bmv() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(BMV_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(CHUCKS_ACCOUNT_IS_CREATED)
                .and(BMC_CONTRACT_IS_NOT_OWNED_BY_CHUCK)
                .and(ICON_NETWORK_ADDRESS_AND_VERIFIER_ACCOUNT_ID_ARE_PROVIDED_AS_ADD_VERIFIER_PARAM)
                .when(CHUCK_INVOKES_ADD_VERIFIER_IN_BMC)
                .then(BMC_SHOULD_THROW_UNAUTHORIZED_ERROR_ON_ADDING_VERIFIER);
        }

        #[tokio::test(flavor = "multi_thread")]
        async fn user_can_query_all_the_registered_verifiers() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(BMV_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(BMC_CONTRACT_IS_OWNED_BY_ALICE)
                .and(VERIFIER_FOR_ICON_IS_ADDED)
                .when(USER_INVOKES_GET_VERIFIERS_IN_BMC)
                .then(USER_SHOULD_GET_THE_EXISITING_LIST_OF_VERIFIERS);
        }

        #[tokio::test(flavor = "multi_thread")]
        async fn bmc_owner_cannot_add_a_verifier_for_an_existing_network() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(BMV_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(BMC_CONTRACT_IS_OWNED_BY_ALICE)
                .and(VERIFIER_FOR_ICON_IS_ADDED)
                .and(ICON_BMV_ACCOUNT_ID_AND_ICON_NETWORK_ADDRESS_ARE_PROVIDED_AS_ADD_VERIFIER_PARAM,)
                .when(ALICE_INVOKES_ADD_VERIFIER_IN_BMC)
                .then(BMC_SHOULD_THROW_VERIFIER_ALREADY_EXISTS_ERROR_ON_ADDING_VERIFIER);
        }

        #[tokio::test(flavor = "multi_thread")]
        async fn add_verifier_unauthorized_fail() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(BMV_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(BMC_CONTRACT_IS_OWNED_BY_ALICE)
                .and(CHUCKS_ACCOUNT_IS_CREATED)
                .and(ICON_NETWORK_ADDRESS_AND_VERIFIER_ACCOUNT_ID_ARE_PROVIDED_AS_ADD_VERIFIER_PARAM,)
                .when(CHUCK_INVOKES_ADD_VERIFIER_IN_BMC)
                .then(BMC_SHOULD_THROW_UNAUTHORIZED_ERROR_ON_ADDING_VERIFIER);
        }

        #[tokio::test(flavor = "multi_thread")]
        async fn remove_non_existing_verifier_authorized_fail() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(BMV_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(BMC_CONTRACT_IS_OWNED_BY_ALICE)
                .and(ICON_NETWORK_ADDRESS_PROVIDED_AS_REMOVE_VERIFIER_PARAM)
                .when(ALICE_INVOKES_REMOVE_VERIFIER_IN_BMC)
                .then(BMC_SHOULD_THROW_VERIFIER_DOES_NOT_EXISTS_ERROR_ON_REMOVING_VERIFIER);
        }
    }
}
