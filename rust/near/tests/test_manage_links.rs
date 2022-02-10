mod steps;

#[cfg(test)]
mod manage_links {
    use super::*;
    use kitten::*;
    use steps::*;

    mod bmc {
        use super::*;

        #[tokio::test(flavor = "multi_thread")]
        async fn bmc_owner_can_add_a_link_connecting_cross_chain_bmc() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(BMV_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(BMC_CONTRACT_IS_OWNED_BY_ALICE)
                .and(VERIFIER_FOR_ICON_IS_ADDED)
                .and(ICON_LINK_ADDRESS_IS_PROVIDED_AS_ADD_LINK_PARAM)
                .when(ALICE_INVOKES_ADD_LINK_IN_BMC)
                .then(ICON_LINK_SHOULD_BE_ADDED_TO_THE_LIST_OF_LINKS);
        }

        #[tokio::test(flavor = "multi_thread")]
        async fn non_bmc_owner_cannot_add_a_link_connecting_cross_chain_bmc() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(BMV_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(BMC_CONTRACT_IS_NOT_OWNED_BY_CHUCK)
                .and(CHUCKS_ACCOUNT_IS_CREATED)
                .and(VERIFIER_FOR_ICON_IS_ADDED)
                .and(ICON_LINK_ADDRESS_IS_PROVIDED_AS_ADD_LINK_PARAM)
                .when(CHUCK_INVOKES_ADD_LINK_IN_BMC)
                .then(BMC_SHOULD_THROW_UNAUTHORIZED_ERROR_ON_ADDING_LINK);
        }

        #[tokio::test(flavor = "multi_thread")]
        async fn bmc_owner_can_set_link_config_on_a_registered_link() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(BMV_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(BMC_CONTRACT_IS_OWNED_BY_ALICE)
                .and(ICON_LINK_IS_PRESENT_IN_BMC)
                .and(ICON_LINK_ADDRESS_IS_PROVIDED_AS_SET_LINK_PARAM)
                .when(ALICE_INVOKES_SET_LINK_IN_BMC)
                .then(USER_SHOULD_GET_THE_ICON_LINK_STATUS);
        }

        #[tokio::test(flavor = "multi_thread")]
        async fn bmc_owner_cannot_add_link_if_the_verifier_does_not_exist() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(BMV_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(BMC_CONTRACT_IS_OWNED_BY_ALICE)
                .and(ICON_LINK_ADDRESS_IS_PROVIDED_AS_ADD_LINK_PARAM)
                .when(ALICE_INVOKES_ADD_LINK_IN_BMC)
                .then(BMC_SHOULD_THROW_VERIFIER_NOT_EXISITING_ERROR);
        }

        #[tokio::test(flavor = "multi_thread")]
        async fn bmc_owner_cannot_set_link_config_if_the_link_is_not_registered() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(BMV_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(BMC_CONTRACT_IS_OWNED_BY_ALICE)
                .and(ICON_LINK_ADDRESS_IS_PROVIDED_AS_SET_LINK_PARAM)
                .when(ALICE_INVOKES_SET_LINK_IN_BMC)
                .then(BMC_SHOULD_THROW_LINK_DOES_NOT_EXIST_ERROR_ON_SETTING_LINK);
        }

        #[tokio::test(flavor = "multi_thread")]
        async fn bmr_operator_can_get_link_status_of_connected_bmc() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(BMV_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(ICON_LINK_IS_PRESENT_IN_BMC)
                .when(USER_INVOKES_GET_STATUS_IN_BMC)
                .then(USER_SHOULD_GET_THE_ICON_LINK_STATUS);
        }

        #[tokio::test(flavor = "multi_thread")]
        async fn bmc_owner_cannot_remove_a_link_if_link_does_not_exist() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(BMV_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(BMC_CONTRACT_IS_OWNED_BY_ALICE)
                .and(VERIFIER_FOR_ICON_IS_ADDED)
                .and(ICON_LINK_ADDRESS_IS_PROVIDED_AS_REMOVE_LINK_PARAM)
                .when(ALICE_INVOKES_REMOVE_LINK_IN_BMC)
                .then(BMC_SHOULD_THROW_LINK_NOT_EXISTING_ERROR);
        }

        #[tokio::test(flavor = "multi_thread")]
        async fn non_bmc_owner_cannot_set_link_config_on_a_registered_link() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(BMV_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(CHUCKS_ACCOUNT_IS_CREATED)
                .and(BMC_CONTRACT_IS_NOT_OWNED_BY_CHUCK)
                .and(ICON_LINK_IS_PRESENT_IN_BMC)
                .and(ICON_LINK_ADDRESS_IS_PROVIDED_AS_SET_LINK_PARAM)
                .when(CHUCK_INVOKES_SET_LINK_IN_BMC)
                .then(BMC_SHOULD_THROW_UNAUTHORIZED_ERROR_ON_SETTING_LINK);
        }

        #[tokio::test(flavor = "multi_thread")]
        async fn bmc_owner_cannot_set_link_config_if_delay_limit_param_is_less_than_1() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(BMV_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(BMC_CONTRACT_IS_OWNED_BY_ALICE)
                .and(ICON_LINK_IS_PRESENT_IN_BMC)
                .and(ICON_LINK_ADDRESS_WITH_0_DELAY_IS_PROVIDED_AS_SET_LINK_PARAM)
                .when(ALICE_INVOKES_SET_LINK_IN_BMC)
                .then(BMC_SHOULD_THROW_INVALID_PARAM_ERROR_ON_SETTING_LINK);
        }

        #[tokio::test(flavor = "multi_thread")]
        async fn bmc_owner_cannot_set_link_config_with_max_aggregation_param_less_than_1() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(BMV_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(BMC_CONTRACT_IS_OWNED_BY_ALICE)
                .and(ICON_LINK_IS_PRESENT_IN_BMC)
                .and(ICON_LINK_ADDRESS_WITH_0_MAX_AGGREGATION_IS_PROVIDED_AS_SET_LINK_PARAM)
                .when(ALICE_INVOKES_SET_LINK_IN_BMC)
                .then(BMC_SHOULD_THROW_INVALID_PARAM_ERROR_ON_SETTING_LINK);
        }

        #[tokio::test(flavor = "multi_thread")]
        async fn bmc_owner_cannot_add_link_with_an_invalid_btp_link_address() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(BMV_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(BMC_CONTRACT_IS_OWNED_BY_ALICE)
                .and(VERIFIER_FOR_ICON_IS_ADDED)
                .and(INVALID_LINK_ADDRESS_IS_PROVIDED_AS_ADD_LINK_PARAM)
                .when(ALICE_INVOKES_ADD_LINK_IN_BMC)
                .then(BMC_SHOULD_THROW_INVALID_PARAM_ERROR_ON_ADDING_LINK);
        }

        #[tokio::test(flavor = "multi_thread")]
        async fn non_bmc_owner_cannor_remove_a_link_connecting_cross_chain_bmc() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(BMV_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(CHUCKS_ACCOUNT_IS_CREATED)
                .and(BMC_CONTRACT_IS_NOT_OWNED_BY_CHUCK)
                .and(VERIFIER_FOR_ICON_IS_ADDED)
                .and(ICON_LINK_IS_ADDED)
                .and(ICON_LINK_ADDRESS_IS_PROVIDED_AS_REMOVE_LINK_PARAM)
                .when(CHUCK_INVOKES_REMOVE_LINK_IN_BMC)
                .then(BMC_SHOULD_THROW_UNAUTHORIZED_ERROR_ON_REMOVING_LINK);
        }

        #[ignore]
        #[tokio::test(flavor = "multi_thread")]
        async fn bmc_owner_can_set_max_aggregation_to_link_status() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(BMV_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(BMC_CONTRACT_IS_OWNED_BY_ALICE)
                .and(ICON_LINK_IS_PRESENT_IN_BMC)
                .and(MAX_AGGREGATION_IS_PROVIDED_AS_SET_LINK_PARAM)
                .when(ALICE_INVOKES_SET_LINK_IN_BMC)
                .then(MAX_AGGREGATION_IN_ICON_LINK_STATUS_SHOULD_BE_UPDATED);
        }

        #[tokio::test(flavor = "multi_thread")]
        async fn bmc_owner_cannot_add_link_if_link_is_already_existing() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(BMV_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(BMC_CONTRACT_IS_OWNED_BY_ALICE)
                .and(ICON_LINK_IS_PRESENT_IN_BMC)
                .and(ICON_LINK_ADDRESS_IS_PROVIDED_AS_ADD_LINK_PARAM)
                .when(ALICE_INVOKES_ADD_LINK_IN_BMC)
                .then(BMC_SHOULD_THROW_LINK_ALREADY_EXISTS_ERROR_ON_ADDING_LINK);
        }
    }
}
