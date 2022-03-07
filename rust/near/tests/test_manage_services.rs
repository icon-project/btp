mod steps;

#[cfg(test)]
mod manage_bsh_services {
    use super::*;
    use kitten::*;
    use steps::*;

    mod bmc {
        use super::*;

        #[tokio::test(flavor = "multi_thread")]
        async fn bmc_owner_cannot_remove_non_existing_service() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(NATIVE_COIN_BSH_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(BMC_CONTRACT_IS_OWNED_BY_ALICE)
                .and(NATIVE_COIN_BSH_NAME_IS_PROVIDED_AS_REMOVE_SERVICE_PARAM)
                .when(ALICE_INVOKES_REMOVE_SERVICE_IN_BMC)
                .then(BMC_SHOULD_THROW_SERVICE_DOES_NOT_EXIST_ERROR_ON_REMOVING_SERVICE);
        }

        #[tokio::test(flavor = "multi_thread")]
        async fn bmc_owner_can_delete_a_registered_bsh() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(NATIVE_COIN_BSH_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(BMC_CONTRACT_IS_OWNED_BY_ALICE)
                .and(NATIVE_COIN_BSH_IS_REGISTERED_IN_BMC)
                .and(NATIVE_COIN_BSH_NAME_IS_PROVIDED_AS_REMOVE_SERVICE_PARAM)
                .when(ALICE_INVOKES_REMOVE_SERVICE_IN_BMC)
                .then(THE_REMOVED_SERVICE_SHOULD_NOT_BE_IN_THE_LIST_OF_SERVICES);
        }

        #[tokio::test(flavor = "multi_thread")]
        async fn bmc_owner_cannot_add_an_existing_service() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(NATIVE_COIN_BSH_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(BMC_CONTRACT_IS_OWNED_BY_ALICE)
                .and(NATIVE_COIN_BSH_IS_REGISTERED_IN_BMC)
                .and(NATIVE_COIN_BSH_NAME_AND_ACCOUNT_ID_ARE_PROVIDED_AS_ADD_SERVICE_PARAM)
                .when(ALICE_INVOKES_ADD_SERVICE_IN_BMC)
                .then(BMC_SHOULD_THROW_SERVICE_ALREADY_EXISTS_ON_ADDING_SERVICE);
        }

        #[tokio::test(flavor = "multi_thread")]
        async fn non_bmc_owner_cannot_add_new_bsh() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(NATIVE_COIN_BSH_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(CHUCKS_ACCOUNT_IS_CREATED)
                .and(NATIVE_COIN_BSH_NAME_AND_ACCOUNT_ID_ARE_PROVIDED_AS_ADD_SERVICE_PARAM)
                .when(CHUCK_INVOKES_ADD_SERVICE_IN_BMC)
                .then(BMC_SHOULD_THROW_UNAUTHORISED_ERROR_ON_ADDING_SERVICE);
        }

        #[tokio::test(flavor = "multi_thread")]
        async fn user_can_query_all_the_registered_services() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(NATIVE_COIN_BSH_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(BMC_CONTRACT_IS_OWNED_BY_ALICE)
                .and(NATIVE_COIN_BSH_IS_REGISTERED_IN_BMC)
                .when(USER_INVOKES_GET_SERVICES_IN_BMC)
                .then(USER_SHOULD_GET_THE_EXISITING_LIST_OF_SERVICES);
        }

        #[tokio::test(flavor = "multi_thread")]
        async fn bmc_owner_can_add_new_bsh() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(NATIVE_COIN_BSH_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(BMC_CONTRACT_IS_OWNED_BY_ALICE)
                .and(NATIVE_COIN_BSH_NAME_AND_ACCOUNT_ID_ARE_PROVIDED_AS_ADD_SERVICE_PARAM)
                .when(ALICE_INVOKES_ADD_SERVICE_IN_BMC)
                .then(NATIVE_COIN_BSH_SHOULD_BE_ADDED_TO_THE_LIST_OF_SERVICES);
        }

        #[tokio::test(flavor = "multi_thread")]
        async fn non_bmc_owner_cannot_remove_an_existing_service() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(NATIVE_COIN_BSH_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(BMC_CONTRACT_IS_OWNED_BY_ALICE)
                .and(CHUCKS_ACCOUNT_IS_CREATED)
                .and(NATIVE_COIN_BSH_IS_REGISTERED_IN_BMC)
                .and(NATIVE_COIN_BSH_NAME_IS_PROVIDED_AS_REMOVE_SERVICE_PARAM)
                .when(CHUCK_INVOKES_REMOVE_SERVICE_IN_BMC)
                .then(BMC_SHOULD_THROW_UNAUTHORISED_ERROR_ON_REMOVING_SERVICE);
        }

        #[tokio::test(flavor = "multi_thread")]
        async fn bmc_can_send_error_messge_if_sevice_message_is_intended_to_unregistered_bsh() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(BMV_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(NATIVE_COIN_BSH_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(BMC_CONTRACT_IS_OWNED_BY_ALICE)
                .and(ICON_LINK_IS_PRESENT_IN_BMC)
                .and(RELAY_1_IS_REGISTERED_FOR_ICON_LINK)
                .and(CHARLIES_ACCOUNT_IS_CREATED)
                .and(NATIVE_COIN_BSH_SERVICE_IS_ADDED_TO_BMC)
                .and(WRAPPED_ICX_COIN_IS_REGESITERED_IN_NATIVE_COIN_BSH)
                .and(UNREGISTERED_BSH_RECEIVES_BTP_MESSAGE_TO_MINT_AND_TRANSFER_WRAPPED_NATIVE_COIN)
                .when(ALICE_INVOKES_HANDLE_BTP_MESSAGE_IN_BMC)
                .then(BMC_SHOULD_THROW_SERVICE_DOES_NOT_EXIST_ON_SENDING_SERVICE_MESSAGE);
        }
    }
}
