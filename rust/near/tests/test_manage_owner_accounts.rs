mod steps;

#[cfg(test)]
mod manage_owner_accounts {
    use super::*;
    use kitten::*;
    use steps::*;

    mod bmc {
        use super::*;

        #[tokio::test(flavor = "multi_thread")]
        async fn non_bmc_owner_cannot_remove_owners() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(BMC_CONTRACT_IS_OWNED_BY_ALICE)
                .and(CHARLIE_IS_AN_EXISITNG_OWNER_IN_BMC)
                .and(CHUCKS_ACCOUNT_IS_CREATED)
                .and(BMC_CONTRACT_IS_NOT_OWNED_BY_CHUCK)
                .and(CHARLIES_ACCOUNT_ID_IS_PROVIDED_AS_REMOVE_OWNER_PARAM)
                .when(CHUCK_INVOKES_REMOVE_OWNER_IN_BMC)
                .then(BMC_SHOULD_THROW_UNAUTHORISED_ERROR_ON_REMOVING_OWNER);
        }

        #[tokio::test(flavor = "multi_thread")]
        async fn bmc_contract_account_owner_can_add_new_bmc_owner() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(BMC_CONTRACT_IS_OWNED_BY_ALICE)
                .and(CHARLIES_ACCOUNT_IS_CREATED)
                .and(CHARLIES_ACCOUNT_ID_IS_PROVIDED_AS_ADD_OWNER_PARAM)
                .when(ALICE_INVOKES_ADD_OWNER_IN_BMC)
                .then(CHARLIES_ACCOUNT_ID_SHOULD_BE_IN_THE_LIST_OF_BMC_OWNERS);
        }

        #[tokio::test(flavor = "multi_thread")]
        async fn recently_added_bmc_owner_can_add_new_bmc_owner() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(BMC_CONTRACT_IS_OWNED_BY_ALICE)
                .and(CHARLIE_IS_AN_EXISITNG_OWNER_IN_BMC)
                .and(BOBS_ACCOUNT_IS_CREATED)
                .and(BOBS_ACCOUNT_ID_IS_PROVIDED_AS_ADD_OWNER_PARAM)
                .when(CHARLIE_INVOKES_ADD_OWNER_IN_BMC)
                .then(BOBS_ACCOUNT_ID_SHOULD_BE_IN_THE_LIST_OF_BMC_OWNERS);
        }

        #[tokio::test(flavor = "multi_thread")]
        async fn bmc_contract_account_owner_can_remove_other_owners() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(BMC_CONTRACT_IS_OWNED_BY_ALICE)
                .and(CHARLIE_IS_AN_EXISITNG_OWNER_IN_BMC)
                .and(CHARLIES_ACCOUNT_ID_IS_PROVIDED_AS_REMOVE_OWNER_PARAM)
                .when(ALICE_INVOKES_REMOVE_OWNER_IN_BMC)
                .then(CHARLIES_ACCOUNT_ID_SHOULD_NOT_BE_IN_THE_LIST_OF_BMC_OWNERS);
        }

        #[tokio::test(flavor = "multi_thread")]
        async fn add_new_bmc_owner_by_existing_bmc_owner() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(BMC_CONTRACT_IS_OWNED_BY_ALICE)
                .and(CHARLIES_ACCOUNT_IS_CREATED)
                .and(CHARLIES_ACCOUNT_ID_IS_PROVIDED_AS_ADD_OWNER_PARAM)
                .when(ALICE_INVOKES_ADD_OWNER_IN_BMC)
                .then(CHARLIES_ACCOUNT_ID_SHOULD_BE_IN_THE_LIST_OF_BMC_OWNERS);
        }

        #[tokio::test(flavor = "multi_thread")]
        async fn non_bmc_owner_cannot_add_new_owners() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(BMC_CONTRACT_IS_OWNED_BY_ALICE)
                .and(CHUCKS_ACCOUNT_IS_CREATED)
                .and(CHARLIES_ACCOUNT_IS_CREATED)
                .and(CHARLIES_ACCOUNT_ID_IS_PROVIDED_AS_ADD_OWNER_PARAM)
                .when(CHUCK_INVOKES_ADD_OWNER_IN_BMC)
                .then(BMC_SHOULD_THROW_UNAUTHORISED_ERROR_ON_ADDING_OWNER);
        }

        #[tokio::test(flavor = "multi_thread")]
        async fn bmc_owner_cannot_remove_self_if_self_is_the_last_owner() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(BMC_CONTRACT_IS_OWNED_BY_ALICE)
                .and(ALICES_ACCOUNT_ID_IS_PROVIDED_AS_REMOVE_OWNER_PARAM)
                .when(ALICE_INVOKES_REMOVE_OWNER_IN_BMC)
                .then(BMC_SHOULD_THROW_LAST_OWNER_ERROR_ON_REMOVING_OWNER);
        }

        #[tokio::test(flavor = "multi_thread")]
        async fn new_bmc_owner_cannot_add_already_existing_owner() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(BMC_CONTRACT_IS_OWNED_BY_ALICE)
                .and(CHARLIE_IS_AN_EXISITNG_OWNER_IN_BMC)
                .and(CHARLIES_ACCOUNT_ID_IS_PROVIDED_AS_ADD_OWNER_PARAM)
                .when(ALICE_INVOKES_ADD_OWNER_IN_BMC)
                .then(BMC_SHOULD_THROW_OWNER_ALREADY_EXISTS_ERROR_ON_ADDING_OWNER);
        }

        #[tokio::test(flavor = "multi_thread")]
        async fn newly_added_bmc_owner_can_remove_bmc_owner() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(BMC_CONTRACT_IS_OWNED_BY_ALICE)
                .and(CHARLIE_IS_AN_EXISITNG_OWNER_IN_BMC)
                .and(ALICES_ACCOUNT_ID_IS_PROVIDED_AS_REMOVE_OWNER_PARAM)
                .when(CHARLIE_INVOKES_REMOVE_OWNER_IN_BMC)
                .then(ALICES_ACCOUNT_ID_SHOULD_NOT_BE_IN_THE_LIST_OF_BMC_OWNERS);
        }

        #[tokio::test(flavor = "multi_thread")]
        async fn bmc_owner_can_remove_another_bmc_owner() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(BMC_CONTRACT_IS_OWNED_BY_ALICE)
                .and(CHARLIE_IS_AN_EXISITNG_OWNER_IN_BMC)
                .and(CHARLIES_ACCOUNT_ID_IS_PROVIDED_AS_REMOVE_OWNER_PARAM)
                .when(ALICE_INVOKES_REMOVE_OWNER_IN_BMC)
                .then(CHARLIES_ACCOUNT_ID_SHOULD_NOT_BE_IN_THE_LIST_OF_BMC_OWNERS);
        }

        #[tokio::test(flavor = "multi_thread")]
        async fn bmc_owner_cannot_remove_non_existing_owner() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(BMC_CONTRACT_IS_OWNED_BY_ALICE)
                .and(CHARLIES_ACCOUNT_IS_CREATED)
                .and(CHARLIES_ACCOUNT_ID_IS_PROVIDED_AS_REMOVE_OWNER_PARAM)
                .when(ALICE_INVOKES_REMOVE_OWNER_IN_BMC)
                .then(BMC_SHOULD_THROW_OWNER_DOES_NOT_EXIST_ERROR_ON_REMOVING_OWNER);
        }

        #[tokio::test(flavor = "multi_thread")]
        async fn bmc_owner_can_remove_self() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(BMC_CONTRACT_IS_OWNED_BY_ALICE)
                .and(CHARLIE_IS_AN_EXISITNG_OWNER_IN_BMC)
                .and(ALICES_ACCOUNT_ID_IS_PROVIDED_AS_REMOVE_OWNER_PARAM)
                .when(ALICE_INVOKES_REMOVE_OWNER_IN_BMC)
                .then(ALICES_ACCOUNT_ID_SHOULD_NOT_BE_IN_THE_LIST_OF_BMC_OWNERS);
        }
    }

    mod nativecoin_bsh {
        use super::*;

        #[tokio::test(flavor = "multi_thread")]
        async fn newly_added_bsh_owner_can_add_new_bsh_owners() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(NATIVE_COIN_BSH_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(NATIVE_COIN_BSH_CONTRACT_IS_OWNED_BY_BOB)
                .and(CHARLIE_IS_AN_EXISTING_OWNER_IN_NATIVE_COIN_BSH)
                .and(ALICES_ACCOUNT_IS_CREATED)
                .and(ALICES_ACCOUNT_ID_IS_PROVIDED_AS_ADD_OWNER_PARAM)
                .when(CHARLIE_INVOKES_ADD_OWNER_IN_NATIVE_COIN_BSH)
                .then(ALICES_ACCOUNT_ID_SHOULD_BE_ADDED_TO_THE_LIST_OF_NATIVE_COIN_BSH_OWNERS);
        }

        #[tokio::test(flavor = "multi_thread")]
        async fn bsh_contract_account_owner_can_remove_other_owners() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(NATIVE_COIN_BSH_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(NATIVE_COIN_BSH_CONTRACT_IS_OWNED_BY_BOB)
                .and(CHARLIE_IS_AN_EXISTING_OWNER_IN_NATIVE_COIN_BSH)
                .and(CHARLIES_ACCOUNT_ID_IS_PROVIDED_AS_REMOVE_OWNER_PARAM)
                .when(BOB_INVOKES_REMOVE_OWNER_IN_NATIVE_COIN_BSH)
                .then(CHARLIES_ACCOUNT_ID_SHOULD_NOT_BE_IN_THE_LIST_OF_NATIVE_COIN_BSH_OWNERS);
        }

        #[tokio::test(flavor = "multi_thread")]
        async fn newly_added_bsh_owner_can_remove_other_owners() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(NATIVE_COIN_BSH_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(NATIVE_COIN_BSH_CONTRACT_IS_OWNED_BY_BOB)
                .and(CHARLIE_IS_AN_EXISTING_OWNER_IN_NATIVE_COIN_BSH)
                .and(BOBS_ACCOUNT_ID_IS_PROVIDED_AS_REMOVE_OWNER_PARAM)
                .when(CHARLIE_INVOKES_REMOVE_OWNER_IN_NATIVE_COIN_BSH)
                .then(BOBS_ACCOUNT_ID_SHOULD_NOT_BE_IN_THE_LIST_OF_NATIVE_COIN_BSH_OWNERS);
        }

        #[tokio::test(flavor = "multi_thread")]
        async fn non_bsh_owner_cannot_add_new_owners() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(NATIVE_COIN_BSH_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(NATIVE_COIN_BSH_CONTRACT_IS_NOT_OWNED_BY_CHUCK)
                .and(CHARLIES_ACCOUNT_IS_CREATED)
                .and(CHARLIES_ACCOUNT_ID_IS_PROVIDED_AS_ADD_OWNER_PARAM)
                .when(CHUCK_INVOKES_ADD_OWNER_IN_NATIVE_COIN_BSH)
                .then(NATIVE_COIN_BSH_SHOULD_THROW_UNAUTHORISED_ERROR_ON_ADDING_OWNER);
        }

        #[tokio::test(flavor = "multi_thread")]
        async fn non_bsh_owner_cannot_remove_an_owner() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(NATIVE_COIN_BSH_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(NATIVE_COIN_BSH_CONTRACT_IS_NOT_OWNED_BY_CHUCK)
                .and(CHARLIE_IS_AN_EXISTING_OWNER_IN_NATIVE_COIN_BSH)
                .and(CHARLIES_ACCOUNT_ID_IS_PROVIDED_AS_REMOVE_OWNER_PARAM)
                .when(CHUCK_INVOKES_REMOVE_OWNER_IN_NATIVE_COIN_BSH)
                .then(NATIVE_COIN_BSH_SHOULD_THROW_UNAUTHORISED_ERROR_ON_REMOVING_OWNER);
        }

        #[tokio::test(flavor = "multi_thread")]
        async fn bsh_owner_cannot_remove_non_existing_owner() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(NATIVE_COIN_BSH_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(NATIVE_COIN_BSH_CONTRACT_IS_OWNED_BY_BOB)
                .and(CHARLIES_ACCOUNT_IS_CREATED)
                .and(CHARLIES_ACCOUNT_ID_IS_PROVIDED_AS_REMOVE_OWNER_PARAM)
                .when(BOB_INVOKES_REMOVE_OWNER_IN_NATIVE_COIN_BSH)
                .then(NATIVE_COIN_BSH_SHOULD_THROW_OWNER_DOES_NOT_EXIST_ERROR_ON_REMOVING_OWNER);
        }

        #[tokio::test(flavor = "multi_thread")]
        async fn bsh_owner_cannot_add_already_existing_owner() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(NATIVE_COIN_BSH_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(NATIVE_COIN_BSH_CONTRACT_IS_OWNED_BY_BOB)
                .and(CHARLIE_IS_AN_EXISTING_OWNER_IN_NATIVE_COIN_BSH)
                .and(CHARLIES_ACCOUNT_ID_IS_PROVIDED_AS_ADD_OWNER_PARAM)
                .when(BOB_INVOKES_ADD_OWNER_IN_NATIVE_COIN_BSH)
                .then(NATIVE_COIN_BSH_SHOULD_THROW_OWNER_ALREADY_EXIST_ERROR_ON_REMOVING_OWNER);
        }

        #[tokio::test(flavor = "multi_thread")]
        async fn bsh_owner_can_remove_self_from_owners_list() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(NATIVE_COIN_BSH_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(NATIVE_COIN_BSH_CONTRACT_IS_OWNED_BY_BOB)
                .and(CHARLIE_IS_AN_EXISTING_OWNER_IN_NATIVE_COIN_BSH)
                .and(CHARLIES_ACCOUNT_ID_IS_PROVIDED_AS_REMOVE_OWNER_PARAM)
                .when(CHARLIE_INVOKES_REMOVE_OWNER_IN_NATIVE_COIN_BSH)
                .then(CHARLIES_ACCOUNT_ID_SHOULD_NOT_BE_IN_THE_LIST_OF_NATIVE_COIN_BSH_OWNERS);
        }

        #[tokio::test(flavor = "multi_thread")]
        async fn bsh_owner_cannot_remove_self_if_self_is_the_last_owner() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(NATIVE_COIN_BSH_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(NATIVE_COIN_BSH_CONTRACT_IS_OWNED_BY_BOB)
                .and(BOBS_ACCOUNT_ID_IS_PROVIDED_AS_REMOVE_OWNER_PARAM)
                .when(BOB_INVOKES_REMOVE_OWNER_IN_NATIVE_COIN_BSH)
                .then(NATIVE_COIN_BSH_SHOULD_THROW_LAST_OWNER_ERROR_ON_REMOVING_OWNER);
        }

        #[tokio::test(flavor = "multi_thread")]
        async fn bsh_contract_account_owner_can_add_new_bsh_owner() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(NATIVE_COIN_BSH_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(NATIVE_COIN_BSH_CONTRACT_IS_OWNED_BY_BOB)
                .and(CHARLIES_ACCOUNT_IS_CREATED)
                .and(CHARLIES_ACCOUNT_ID_IS_PROVIDED_AS_ADD_OWNER_PARAM)
                .when(BOB_INVOKES_ADD_OWNER_IN_NATIVE_COIN_BSH)
                .then(CHARLIES_ACCOUNT_ID_SHOULD_BE_ADDED_TO_THE_LIST_OF_NATIVE_COIN_BSH_OWNERS);
        }
    }

    mod token_bsh {
        use super::*;
        
        #[tokio::test(flavor = "multi_thread")]
        async fn newly_added_bsh_owner_can_add_new_bsh_owners() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(TOKEN_BSH_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(TOKEN_BSH_CONTRACT_IS_OWNED_BY_BOB)
                .and(CHARLIE_IS_AN_EXISTING_OWNER_IN_TOKEN_BSH)
                .and(ALICES_ACCOUNT_IS_CREATED)
                .and(ALICES_ACCOUNT_ID_IS_PROVIDED_AS_ADD_OWNER_PARAM)
                .when(CHARLIE_INVOKES_ADD_OWNER_IN_TOKEN_BSH)
                .then(ALICES_ACCOUNT_ID_SHOULD_BE_ADDED_TO_THE_LIST_OF_TOKEN_BSH_OWNERS);
        }

        #[tokio::test(flavor = "multi_thread")]
        async fn bsh_contract_account_owner_can_remove_other_owners() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(TOKEN_BSH_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(TOKEN_BSH_CONTRACT_IS_OWNED_BY_BOB)
                .and(CHARLIE_IS_AN_EXISTING_OWNER_IN_TOKEN_BSH)
                .and(CHARLIES_ACCOUNT_ID_IS_PROVIDED_AS_REMOVE_OWNER_PARAM)
                .when(BOB_INVOKES_REMOVE_OWNER_IN_TOKEN_BSH)
                .then(CHARLIES_ACCOUNT_ID_SHOULD_NOT_BE_IN_THE_LIST_OF_TOKEN_BSH_OWNERS);
        }

        #[tokio::test(flavor = "multi_thread")]
        async fn newly_added_bsh_owner_can_remove_other_owners() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(TOKEN_BSH_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(TOKEN_BSH_CONTRACT_IS_OWNED_BY_BOB)
                .and(CHARLIE_IS_AN_EXISTING_OWNER_IN_TOKEN_BSH)
                .and(BOBS_ACCOUNT_ID_IS_PROVIDED_AS_REMOVE_OWNER_PARAM)
                .when(CHARLIE_INVOKES_REMOVE_OWNER_IN_TOKEN_BSH)
                .then(BOBS_ACCOUNT_ID_SHOULD_NOT_BE_IN_THE_LIST_OF_TOKEN_BSH_OWNERS);
        }

        #[tokio::test(flavor = "multi_thread")]
        async fn non_bsh_owner_cannot_add_new_owners() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(TOKEN_BSH_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(TOKEN_BSH_CONTRACT_IS_NOT_OWNED_BY_CHUCK)
                .and(CHARLIES_ACCOUNT_IS_CREATED)
                .and(CHARLIES_ACCOUNT_ID_IS_PROVIDED_AS_ADD_OWNER_PARAM)
                .when(CHUCK_INVOKES_ADD_OWNER_IN_TOKEN_BSH)
                .then(TOKEN_BSH_SHOULD_THROW_UNAUTHORISED_ERROR_ON_ADDING_OWNER);
        }

        #[tokio::test(flavor = "multi_thread")]
        async fn non_bsh_owner_cannot_remove_an_owner() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(TOKEN_BSH_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(TOKEN_BSH_CONTRACT_IS_NOT_OWNED_BY_CHUCK)
                .and(CHARLIE_IS_AN_EXISTING_OWNER_IN_TOKEN_BSH)
                .and(CHARLIES_ACCOUNT_ID_IS_PROVIDED_AS_REMOVE_OWNER_PARAM)
                .when(CHUCK_INVOKES_REMOVE_OWNER_IN_TOKEN_BSH)
                .then(TOKEN_BSH_SHOULD_THROW_UNAUTHORISED_ERROR_ON_REMOVING_OWNER);
        }

        #[tokio::test(flavor = "multi_thread")]
        async fn bsh_owner_cannot_remove_non_existing_owner() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(TOKEN_BSH_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(TOKEN_BSH_CONTRACT_IS_OWNED_BY_BOB)
                .and(CHARLIES_ACCOUNT_IS_CREATED)
                .and(CHARLIES_ACCOUNT_ID_IS_PROVIDED_AS_REMOVE_OWNER_PARAM)
                .when(BOB_INVOKES_REMOVE_OWNER_IN_TOKEN_BSH)
                .then(TOKEN_BSH_SHOULD_THROW_OWNER_DOES_NOT_EXIST_ERROR_ON_REMOVING_OWNER);
        }

        #[tokio::test(flavor = "multi_thread")]
        async fn bsh_owner_cannot_add_already_existing_owner() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(TOKEN_BSH_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(TOKEN_BSH_CONTRACT_IS_OWNED_BY_BOB)
                .and(CHARLIE_IS_AN_EXISTING_OWNER_IN_TOKEN_BSH)
                .and(CHARLIES_ACCOUNT_ID_IS_PROVIDED_AS_ADD_OWNER_PARAM)
                .when(BOB_INVOKES_ADD_OWNER_IN_TOKEN_BSH)
                .then(TOKEN_BSH_SHOULD_THROW_OWNER_ALREADY_EXIST_ERROR_ON_REMOVING_OWNER);
        }

        #[tokio::test(flavor = "multi_thread")]
        async fn bsh_owner_can_remove_self_from_owners_list() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(TOKEN_BSH_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(TOKEN_BSH_CONTRACT_IS_OWNED_BY_BOB)
                .and(CHARLIE_IS_AN_EXISTING_OWNER_IN_TOKEN_BSH)
                .and(CHARLIES_ACCOUNT_ID_IS_PROVIDED_AS_REMOVE_OWNER_PARAM)
                .when(CHARLIE_INVOKES_REMOVE_OWNER_IN_TOKEN_BSH)
                .then(CHARLIES_ACCOUNT_ID_SHOULD_NOT_BE_IN_THE_LIST_OF_TOKEN_BSH_OWNERS);
        }

        #[tokio::test(flavor = "multi_thread")]
        async fn bsh_owner_cannot_remove_self_if_self_is_the_last_owner() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(TOKEN_BSH_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(TOKEN_BSH_CONTRACT_IS_OWNED_BY_BOB)
                .and(BOBS_ACCOUNT_ID_IS_PROVIDED_AS_REMOVE_OWNER_PARAM)
                .when(BOB_INVOKES_REMOVE_OWNER_IN_TOKEN_BSH)
                .then(TOKEN_BSH_SHOULD_THROW_LAST_OWNER_ERROR_ON_REMOVING_OWNER);
        }

        #[tokio::test(flavor = "multi_thread")]
        async fn bsh_contract_account_owner_can_add_new_bsh_owner() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(TOKEN_BSH_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(TOKEN_BSH_CONTRACT_IS_OWNED_BY_BOB)
                .and(CHARLIES_ACCOUNT_IS_CREATED)
                .and(CHARLIES_ACCOUNT_ID_IS_PROVIDED_AS_ADD_OWNER_PARAM)
                .when(BOB_INVOKES_ADD_OWNER_IN_TOKEN_BSH)
                .then(CHARLIES_ACCOUNT_ID_SHOULD_BE_ADDED_TO_THE_LIST_OF_TOKEN_BSH_OWNERS);
        }
    }
}
