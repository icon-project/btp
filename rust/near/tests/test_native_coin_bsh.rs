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
                .when(USER_INVOKES_GET_COINS_IN_NATIVE_COIN_BSH)
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
    }

}