mod steps;

#[cfg(test)]
mod manage_coins_and_tokens {
    use super::*;
    use kitten::*;
    use steps::*;

    mod native_coin_bsh {
        use super::*;

        #[tokio::test(flavor = "multi_thread")]
        async fn user_can_query_registered_native_coins() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(NATIVE_COIN_BSH_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(NATIVE_COIN_BSH_IS_REGISTERED_IN_BMC)
                .when(USER_INVOKES_GET_COINS_IN_NATIVE_COIN_BSH)
                .then(USER_SHOULD_GET_THE_REGISTERED_COIN);
        }

        #[tokio::test(flavor = "multi_thread")]
        async fn user_can_query_coin_id_for_registered_native_coins() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(NATIVE_COIN_BSH_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(NATIVE_COIN_BSH_CONTRACT_IS_OWNED_BY_BOB)
                .and(COIN_NAME_IS_PROVIDED_AS_GET_COIN_ID_PARAM)
                .when(USER_INVOKES_GET_COIN_ID_IN_NATIVE_COIN_BSH)
                .then(USER_SHOULD_GET_THE_QUERIED_COIN_ID);
        }

        #[tokio::test(flavor = "multi_thread")]
        async fn user_cannot_query_coin_id_for_unregistered_native_coins() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(NATIVE_COIN_BSH_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(NATIVE_COIN_BSH_CONTRACT_IS_OWNED_BY_BOB)
                .and(UNREGISTERED_COIN_NAME_IS_PROVIDED_AS_GET_COIN_ID_PARAM)
                .when(USER_INVOKES_GET_COIN_ID_IN_NATIVE_COIN_BSH_CONTRACT)
                .then(NATIVE_COIN_BSH_SHOULD_THROW_INVALID_COIN_ERROR_ON_GETTING_COIN_ID);
        }

        #[tokio::test(flavor = "multi_thread")]
        async fn removed_bsh_owner_cannot_register_new_coin() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(NATIVE_COIN_BSH_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(NATIVE_COIN_BSH_CONTRACT_IS_OWNED_BY_BOB)
                .and(CHARLIE_WAS_AN_OWNER_IN_NATIVE_COIN_BSH)
                .and(NEP141_CONTRACT_IS_DEPLOYED)
                .and(WRAPPED_ICX_METADATA_IS_PROVIDED_AS_REGISTER_PARAM)
                .when(CHARLIE_INVOKES_REGISTER_IN_NATIVE_COIN_BSH)
                .then(NATIVE_COIN_BSH_SHOULD_THROW_UNAUTHORIZED_ERROR_ON_REGSITERING_NEW_COIN);
        }

        #[tokio::test(flavor = "multi_thread")]
        async fn bsh_owner_cannot_register_already_existing_coin() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(NATIVE_COIN_BSH_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(NATIVE_COIN_BSH_CONTRACT_IS_OWNED_BY_BOB)
                .and(NEP141_CONTRACT_IS_DEPLOYED)
                .and(WRAPPED_ICX_IS_REGISTERED_IN_NATIVE_COIN_BSH)
                .and(WRAPPED_ICX_METADATA_IS_PROVIDED_AS_REGISTER_PARAM)
                .when(BOB_INVOKES_REGISTER_IN_NATIVE_COIN_BSH)
                .then(NATIVE_COIN_BSH_SHOULD_THROW_ALREADY_EXISTING_ERROR_ON_REGISTERING_COIN);
        }

        #[tokio::test(flavor = "multi_thread")]
        async fn bsh_owner_can_register_new_wrapped_native_coin() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(NATIVE_COIN_BSH_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(NATIVE_COIN_BSH_IS_REGISTERED_IN_BMC)
                .and(NATIVE_COIN_BSH_CONTRACT_IS_OWNED_BY_BOB)
                .and(NEP141_CONTRACT_IS_DEPLOYED)
                .and(WRAPPED_ICX_METADATA_IS_PROVIDED_AS_REGISTER_PARAM)
                .when(BOB_INVOKES_REGISTER_IN_NATIVE_COIN_BSH)
                .then(WRAPPED_ICX_SHOULD_BE_PRESENT);
        }

        #[tokio::test(flavor = "multi_thread")]
        async fn non_bsh_owner_cannot_register_new_wrapped_native_coin() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(NATIVE_COIN_BSH_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(NATIVE_COIN_BSH_IS_REGISTERED_IN_BMC)
                .and(NEP141_CONTRACT_IS_DEPLOYED)
                .and(NATIVE_COIN_BSH_CONTRACT_IS_OWNED_BY_BOB)
                .and(CHUCKS_ACCOUNT_IS_CREATED)
                .and(WRAPPED_ICX_METADATA_IS_PROVIDED_AS_REGISTER_PARAM)
                .when(CHUCK_INVOKES_REGISTER_IN_NATIVE_COIN_BSH)
                .then(NATIVE_COIN_BSH_SHOULD_THROW_UNAUTHORIZED_ERROR_ON_REGISTERING_COIN);
        }

        #[tokio::test(flavor = "multi_thread")]
        async fn new_bsh_owner_can_register_new_wrapped_native_coin() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(NATIVE_COIN_BSH_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(NATIVE_COIN_BSH_IS_REGISTERED_IN_BMC)
                .and(NEP141_CONTRACT_IS_DEPLOYED)
                .and(NATIVE_COIN_BSH_CONTRACT_IS_OWNED_BY_BOB)
                .and(CHARLIES_ACCOUNT_IS_CREATED)
                .and(BOB_ADDED_CHARLIE_AS_A_NEW_OWNER_IN_NATIVE_COIN_BSH)
                .and(WRAPPED_ICX_METADATA_IS_PROVIDED_AS_REGISTER_PARAM)
                .when(CHARLIE_INVOKES_REGISTER_IN_NATIVE_COIN_BSH)
                .then(WRAPPED_ICX_SHOULD_BE_PRESENT);
        }

        #[tokio::test(flavor = "multi_thread")]
        async fn removed_bsh_owner_cannot_register_new_wrapped_native_coin() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(NATIVE_COIN_BSH_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(NATIVE_COIN_BSH_IS_REGISTERED_IN_BMC)
                .and(CHARLIES_ACCOUNT_IS_CREATED)
                .and(NEP141_CONTRACT_IS_DEPLOYED)
                .and(NATIVE_COIN_BSH_CONTRACT_IS_OWNED_BY_BOB)
                .and(CHARLIE_WAS_AN_OWNER_IN_NATIVE_COIN_BSH)
                .and(WRAPPED_ICX_METADATA_IS_PROVIDED_AS_REGISTER_PARAM)
                .when(CHARLIE_INVOKES_REGISTER_IN_NATIVE_COIN_BSH)
                .then(NATIVE_COIN_BSH_SHOULD_THROW_UNAUTHORIZED_ERROR_ON_REGISTERING_COIN);
        }
    }

    mod token_bsh {
        use super::*;
        #[tokio::test(flavor = "multi_thread")]
        async fn user_cannot_query_token_id_for_a_unregistered_token() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(BMC_CONTRACT_IS_OWNED_BY_ALICE)
                .and(TOKEN_BSH_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(TOKEN_BSH_CONTRACT_IS_OWNED_BY_BOB)
                .when(USER_INVOKES_TOKEN_ID_FOR_UNREGISTERED_TOKEN)
                .then(TOKEN_BSH_SHOULD_THROUGH_ERROR_ON_GETTING_TOKEN_ID);
        }

        #[tokio::test(flavor = "multi_thread")]
        async fn owner_can_query_token_id_for_a_registered_token() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(BMC_CONTRACT_IS_OWNED_BY_ALICE)
                .and(TOKEN_BSH_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(CHARLIES_ACCOUNT_IS_CREATED)
                .and(WNEAR_TOKEN_IS_REGISTERED_IN_TOKEN_BSH)
                .and(WNEAR_TOKEN_NAME_IS_PROVIDED_AS_GET_TOKEN_ID_PARAM)
                .when(USER_INVOKES_TOKEN_ID_IN_TOKEN_BSH)
                .then(TOKEN_ID_SHOULD_BE_PRESENT_FOR_THE_REGISTERED_TOKEN);
        }
        #[tokio::test(flavor = "multi_thread")]
        async fn non_bsh_owner_cannot_register_new_wrapped_token() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(TOKEN_BSH_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(TOKEN_BSH_CONTRACT_IS_OWNED_BY_BOB)
                .and(CHUCKS_ACCOUNT_IS_CREATED)
                .and(NEW_TOKEN_NAME_IS_PROVIDED_AS_REGISTER_WARPPED_TOKEN_PARAM)
                .when(CHUCK_INVOKES_REGISTER_NEW_WRAPPED_TOKEN_IN_TOKEN_BSH)
                .then(TOKEN_BSH_SHOULD_THROW_UNAUTHORIZED_ERROR_ON_REGISTERING_TOKEN);
        }

        #[tokio::test(flavor = "multi_thread")]
        async fn bsh_owner_cannot_register_an_existing_token() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(BMC_CONTRACT_IS_OWNED_BY_ALICE)
                .and(TOKEN_BSH_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(TOKEN_BSH_CONTRACT_IS_OWNED_BY_BOB)
                .and(CHARLIES_ACCOUNT_IS_CREATED)
                .and(BALN_TOKEN_IS_REGISTERED_IN_TOKEN_BSH)
                .and(BALN_TOKEN_METADATA_IS_PROVIDED_AS_REGISTER_TOKEN_PARAM_IN_TOKEN_BSH)
                .when(BOB_INVOKES_REGISTER_NEW_TOKEN_IN_TOKEN_BSH)
                .then(TOKEN_BSH_SHOULD_THROW_TOKEN_ALREADY_EXISTS_ERROR_ON_REGISTERING_TOKEN);
        }

        #[tokio::test(flavor = "multi_thread")]
        async fn user_can_query_token_id_for_registered_wrapped_token() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(BMC_CONTRACT_IS_OWNED_BY_ALICE)
                .and(BALN_TOKEN_IS_REGISTERED_IN_TOKEN_BSH)
                .and(TOKEN_BSH_SERVICE_IS_ADDED_TO_BMC)
                .and(USER_INVOKES_GET_TOKEN_ID_FOR_BALN_FROM_TOKEN_BSH)
                .and(BALN_TOKEN_ID_SHOULD_BE_PRESENT_IN_TOKEN_TOKEN_BSH);
        }
    }
}
