mod steps;

#[cfg(test)]
mod manage_token_transfer {
    use super::*;
    use kitten::*;
    use steps::*;

    mod native_coin_transfer {
        use super::*;
        #[tokio::test(flavor = "multi_thread")]
        async fn user_can_query_token_id_for_registered_wrapped_token() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(BMC_CONTRACT_IS_OWNED_BY_ALICE)
                .and(WNEAR_TOKEN_IS_REGISTERED_IN_TOKEN_BSH)
                .and(TOKEN_BSH_SERVICE_IS_ADDED_TO_BMC)
                .and(CHARLIES_ACCOUNT_IS_CREATED)
                .and(USER_INVOKES_GET_TOKEN_ID_FOR_WNEAR_FROM_TOKEN_BSH)
                .and(CHARLIE_DEPOSITS_WNEAR_TO_ACCOUNT)
                .when(CHARLIE_INVOKES_BALANCE_OF_IN_TOKEN_BSH)
                .then(BALNCE_SHOULD_BE_PRESENT_IN_CHARLIES_ACCOUNT_AFTER_GETTING_SUCCESS_RESPONSE);

        }
    }
}