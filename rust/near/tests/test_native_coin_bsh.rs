mod steps;

#[cfg(test)]
mod manage_bsh_services {
    use super::*;
    use kitten::*;
    use steps::*;

    mod native_bsh {
        use super::*;

        #[tokio::test(flavor = "multi_thread")]
        async fn user_can_qurey_registered_native_coins() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(NATIVE_COIN_BSH_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(NATIVE_COIN_BSH_IS_REGISTERED)
                .and(NATIVE_COIN_BSH_CONTRACT_IS_OWNED_BY_BOB)
                .and(NEW_WRAPPED_COIN_IS_REGISTERED_IN_NATIVE_COIN_BSH)
                .and(USER_INVOKES_GET_COINS_IN_NATIVE_COIN_BSH )
                .and(COIN_NAME_IS_PROVIDED_AS_GET_COIN_ID_PARAM)
                .when(USER_INVOKES_GET_COIN_ID_IN_NATIVE_COIN_BSH)
                .then(REGSITERED_COIN_IDS_ARE_QUERIED);
        }

        #[tokio::test(flavor = "multi_thread")]
        async fn user_can_qurey_coin_id_for_registered_native_coins() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(NATIVE_COIN_BSH_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(NATIVE_COIN_BSH_IS_REGISTERED)
                .and(NATIVE_COIN_BSH_CONTRACT_IS_OWNED_BY_BOB)
                .and(NEW_WRAPPED_COIN_IS_REGISTERED_IN_NATIVE_COIN_BSH)
                .and(COIN_NAME_IS_PROVIDED_AS_GET_COIN_ID_PARAM)
                .when(USER_INVOKES_GET_COIN_ID_IN_NATIVE_COIN_BSH)
                .then(REGSITERED_COIN_IDS_ARE_QUERIED);
        }

        #[tokio::test(flavor = "multi_thread")]
        async fn user_cannot_qurey_coin_id_for_unregistered_native_coins() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(NATIVE_COIN_BSH_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(NATIVE_COIN_BSH_IS_REGISTERED)
                .and(NATIVE_COIN_BSH_CONTRACT_IS_OWNED_BY_BOB)
                .and(NEW_WRAPPED_COIN_IS_REGISTERED_IN_NATIVE_COIN_BSH)
                .and(UNREGISTERED_COIN_NAME_IS_PROVIDED_AS_GET_COIN_ID_PARAM)
                .when(USER_INVOKES_GET_COIN_ID_IN_NATIVE_COIN_BSH)
                .then(NATIVE_COIN_BSH_SHOULD_THROW_INVALID_COIN_ERROR_ON_GETTING_COIN_ID);
        }

        #[tokio::test(flavor = "multi_thread")]
        async fn removed_bsh_owner_cannot_register_new_coin() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(NATIVE_COIN_BSH_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(NATIVE_COIN_BSH_IS_REGISTERED)
                .and(CHARLIES_ACCOUNT_IS_CREATED)
                .and(NATIVE_COIN_BSH_CONTRACT_IS_OWNED_BY_BOB)
                .and(CHARLIES_ACCOUNT_ID_IS_PROVIDED_AS_ADD_OWNER_PARAM)
                .and(BOB_INVOKES_ADD_OWNER_IN_NATIVE_COIN_BSH)
                .and(CHARLIES_ACCOUNT_ID_IS_PROVIDED_AS_REMOVE_OWNER_PARAM)
                .and(BOB_INVOKES_REMOVE_OWNER_IN_NATIVE_COIN_BSH)
                .and(NEW_COIN_NAME_IS_PROVIDED_AS_REGISTER_WARPPED_COIN_PARAM)
                .when(CHARLIE_INVOKES_REGISTER_NEW_WRAPPED_COIN_IN_NATIVE_COIN_BSH)
                .then(NATIVE_COIN_BSH_SHOULD_THROW_UNAUTHORIZED_ERROR_ON_REGSITERING_NEW_COIN);
        }

        #[tokio::test(flavor = "multi_thread")]
        async fn bsh_owner_cannot_register_already_existing_coin() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(NATIVE_COIN_BSH_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(NATIVE_COIN_BSH_IS_REGISTERED)
                .and(NATIVE_COIN_BSH_CONTRACT_IS_OWNED_BY_BOB)
                .and(NEW_WRAPPED_COIN_IS_REGISTERED_IN_NATIVE_COIN_BSH)
                .and(NEW_COIN_NAME_IS_PROVIDED_AS_REGISTER_WARPPED_COIN_PARAM)
                .when(BOB_INVOKES_REGISTER_NEW_WRAPPED_COIN_IN_NATIVE_COIN_BSH)
                .then(NATIVE_COIN_BSH_SHOULD_THROW_ALREADY_EXISTING_ERROR_ON_REGISTERING_COIN);
        }
    }

}