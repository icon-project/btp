mod steps;

#[cfg(test)]
mod manage_relays {
    use super::*;
    use kitten::*;
    use steps::*;

    mod bmc {
        use super::*;

        #[tokio::test(flavor = "multi_thread")]
        async fn bmc_owner_can_add_bmr_as_list_and_overwrite_an_existing_bmr() {
          Kitten::given(NEW_CONTEXT)
            .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
            .and(BMV_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
            .and(BMC_CONTRACT_IS_OWNED_BY_ALICE)
            .and(ICON_LINK_IS_PRESENT_IN_BMC)
            .and(RELAY_1_IS_REGISTERED_FOR_ICON_LINK)
            .and(RELAY_2_ACCOUNT_IS_CREATED)
            .and(ICON_LINK_ADDRESS_AND_RELAY_1_ACCOUNT_ID_AND_RELAY_2_ACCOUNT_ID_ARE_PROVIDED_AS_ADD_RELAYS_PARAM)
            .when(ALICE_INVOKES_ADD_RELAYS_IN_BMC)
            .then(RELAY_1_AND_RELAY_2_SHOULD_BE_ADDED_TO_THE_LIST_OF_RELAYS);
        }

        #[tokio::test(flavor = "multi_thread")]
        async fn non_bmc_owner_cannot_delete_a_registered_bmr() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(BMV_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(CHUCKS_ACCOUNT_IS_CREATED)
                .and(ICON_LINK_IS_PRESENT_IN_BMC)
                .and(RELAY_1_IS_REGISTERED_FOR_ICON_LINK)
                .and(ICON_LINK_ADDRESS_AND_RELAY_1_ACCOUNT_ID_ARE_PROVIDED_AS_REMOVE_RELAY_PARAM)
                .when(CHUCK_INVOKES_REMOVE_RELAY_IN_BMC)
                .then(BMC_SHOULD_THROW_UNAUTHORIZED_ERROR_ON_REMOVING_RELAY);
        }

        #[tokio::test(flavor = "multi_thread")]
        async fn bmc_owner_cannot_register_bmr_as_list_for_non_existing_link() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(BMV_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(BMC_CONTRACT_IS_OWNED_BY_ALICE)
                .and(ICON_LINK_IS_PRESENT_IN_BMC)
                .and(RELAY_1_ACCOUNT_IS_CREATED)
                .and(BSC_LINK_ADDRESS_AND_RELAY_1_ACCOUNT_ID_ARE_PROVIDED_AS_ADD_RELAYS_PARAM)
                .when(ALICE_INVOKES_ADD_RELAYS_IN_BMC)
                .then(BMC_SHOULD_THROW_LINK_DOES_NOT_EXIST_ERROR_ON_ADDING_RELAYS);
        }

        #[tokio::test(flavor = "multi_thread")]
        async fn non_bmc_owner_cannot_add_bmr_as_list() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(BMV_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(CHUCKS_ACCOUNT_IS_CREATED)
                .and(VERIFIER_FOR_ICON_IS_PRESENT_IN_BMC)
                .and(ICON_LINK_IS_PRESENT_IN_BMC)
                .and(RELAY_1_ACCOUNT_IS_CREATED)
                .and(ICON_LINK_ADDRESS_AND_RELAY_1_ACCOUNT_ID_ARE_PROVIDED_AS_ADD_RELAYS_PARAM)
                .when(CHUCK_INVOKES_ADD_RELAYS_IN_BMC)
                .then(BMC_SHOULD_THROW_UNAUTHORIZED_ERROR_ON_ADDING_RELAYS);
        }

        #[tokio::test(flavor = "multi_thread")]
        async fn users_can_get_list_of_registered_relays_for_the_given_link() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(BMV_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(ICON_LINK_IS_PRESENT_IN_BMC)
                .and(RELAY_1_IS_REGISTERED_FOR_ICON_LINK)
                .and(ICON_LINK_ADDRESS_IS_PROVIDED_AS_GET_RELAYS_PARAM)
                .when(USER_INVOKES_GET_RELAYS_IN_BMC)
                .then(LIST_OF_RELAYS_SHOULD_EXIST_FOR_THE_GIVEN_LINK);
        }

        #[tokio::test(flavor = "multi_thread")]
        async fn bmc_owner_cannot_delete_a_non_existing_bmr() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(BMV_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(BMC_CONTRACT_IS_OWNED_BY_ALICE)
                .and(ICON_LINK_IS_PRESENT_IN_BMC)
                .and(RELAY_1_ACCOUNT_IS_CREATED)
                .and(ICON_LINK_ADDRESS_AND_RELAY_1_ACCOUNT_ID_ARE_PROVIDED_AS_REMOVE_RELAY_PARAM)
                .when(ALICE_INVOKES_REMOVE_RELAY_IN_BMC)
                .then(BMC_SHOULD_THROW_RELAY_DOES_NOT_EXIST_ERROR_ON_REMOVING_NON_EXISTING_RELAY);
        }

        #[tokio::test(flavor = "multi_thread")]
        async fn bmc_owner_can_add_bmr_as_list() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(BMV_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(BMC_CONTRACT_IS_OWNED_BY_ALICE)
                .and(ICON_LINK_IS_PRESENT_IN_BMC)
                .and(RELAY_1_ACCOUNT_IS_CREATED)
                .and(RELAY_2_ACCOUNT_IS_CREATED)
                .and(ICON_LINK_ADDRESS_AND_RELAY_1_ACCOUNT_ID_AND_RELAY_2_ACCOUNT_ID_ARE_PROVIDED_AS_ADD_RELAYS_PARAM)
                .when(ALICE_INVOKES_ADD_RELAYS_IN_BMC)
                .then(RELAY_1_AND_RELAY_2_SHOULD_BE_ADDED_TO_THE_LIST_OF_RELAYS);
        }

        #[tokio::test(flavor = "multi_thread")]
        async fn non_bmc_owner_cannot_add_a_bmr() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(BMV_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(CHUCKS_ACCOUNT_IS_CREATED)
                .and(BMC_CONTRACT_IS_NOT_OWNED_BY_CHUCK)
                .and(ICON_LINK_IS_PRESENT_IN_BMC)
                .and(RELAY_1_ACCOUNT_IS_CREATED)
                .and(ICON_LINK_ADDRESS_AND_RELAY_1_ARE_PROVIDED_AS_ADD_RELAY_PARAM)
                .when(CHUCK_INVOKES_ADD_RELAY_IN_BMC)
                .then(BMC_SHOULD_THROW_UNAUTHORIZED_ERROR_ON_ADDING_RELAY);
        }
        #[tokio::test(flavor = "multi_thread")]
        async fn bmc_owner_cannot_register_bmr_for_non_existing_link() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(BMV_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(BMC_CONTRACT_IS_OWNED_BY_ALICE)
                .and(RELAY_1_IS_REGISTERED_FOR_ICON_LINK)
                .and(ICON_LINK_ADDRESS_AND_RELAY_1_ARE_PROVIDED_AS_ADD_RELAY_PARAM)
                .when(ALICE_INVOKES_ADD_RELAY_IN_BMC)
                .then(BMC_SHOULD_THROW_LINK_DOES_NOT_EXIST_ERROR_ON_ADDING_RELAY);
        }

        #[tokio::test(flavor = "multi_thread")]
        async fn bmc_owner_can_remove_a_registered_bmr() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(BMV_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(BMC_CONTRACT_IS_OWNED_BY_ALICE)
                .and(ICON_LINK_IS_PRESENT_IN_BMC)
                .and(RELAY_1_IS_REGISTERED_FOR_ICON_LINK)
                .and(ICON_LINK_ADDRESS_AND_RELAY_1_ARE_PROVIDED_AS_REMOVE_RELAY_PARAM)
                .when(ALICE_INVOKES_REMOVE_RELAY_IN_BMC)
                .then(REMOVED_RELAYS_SHOULD_NOT_BE_IN_THE_LIST_OF_RELAYS);
        }

        #[tokio::test(flavor = "multi_thread")]
        async fn bmc_owner_can_register_a_bmr() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(BMV_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(BMC_CONTRACT_IS_OWNED_BY_ALICE)
                .and(ICON_LINK_IS_PRESENT_IN_BMC)
                .and(RELAY_1_ACCOUNT_IS_CREATED)
                .and(ICON_LINK_ADDRESS_AND_RELAY_1_ARE_PROVIDED_AS_ADD_RELAY_PARAM)
                .when(ALICE_INVOKES_ADD_RELAY_IN_BMC)
                .then(RELAY_1_SHOULD_BE_ADDED_TO_THE_LIST_OF_RELAYS);
        }

        #[tokio::test(flavor = "multi_thread")]
        async fn users_cannot_get_list_of_relays_if_link_does_not_exist() {
            Kitten::given(NEW_CONTEXT)
              .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
              .and(BMV_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
              .and(ICON_LINK_IS_PRESENT_IN_BMC)
              .and(RELAY_1_IS_REGISTERED_FOR_ICON_LINK)
              .and(NON_EXISTING_LINK_ADDRESS_IS_PROVIDED_AS_GET_RELAY_PARAM)
              .when(USER_INVOKES_GET_RELAYS_IN_BMC)
              .then(BMC_SHOULD_THROW_LINK_DOES_NOT_EXIST_ERROR_ON_GETTING_RELAYS);
          }

          #[tokio::test(flavor = "multi_thread")]
          async fn bmc_owner_cannot_delete_bmr_for_non_existing_link(){
            Kitten::given(NEW_CONTEXT)
            .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
            .and(BMV_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
            .and(ICON_LINK_IS_PRESENT_IN_BMC)
            .and(RELAY_1_IS_REGISTERED_FOR_ICON_LINK)
            .and(NON_EXISTING_LINK_ADDRESS_AND_RELAY_1_ARE_PROVIDED_AS_REMOVE_RELAY_PARAM)
            .when(BMC_OWNER_INVOKES_REMOVE_RELAY_IN_BMC)
            .then(BMC_SHOULD_THROW_LINK_DOES_NOT_EXIST_ERROR_ON_REMOVING_RELAY);
          }
    }
}