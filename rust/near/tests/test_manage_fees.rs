mod steps;

#[cfg(test)]
mod manage_fees {
    use super::*;
    use kitten::*;
    use steps::*;

    mod native_bsh {
        use super::*;

        #[tokio::test(flavor = "multi_thread")]
        async fn bsh_owner_can_update_fee_ratio() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(NATIVE_COIN_BSH_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(NATIVE_COIN_BSH_IS_REGISTERED)
                .and(NATIVE_COIN_BSH_CONTRACT_IS_OWNED_BY_BOB)
                .and(FEE_NUMERATOR_IS_PROVIDED_AS_SET_FEE_RATIO_PARAM)
                .when(BOB_INVOKES_SET_FEE_RATIO_IN_NATIVE_COIN_BSH)
                .then(FEE_RATIO_SHOULD_BE_UPDATED_IN_NATIVE_COIN_BSH);
        }

        #[tokio::test(flavor = "multi_thread")]
        async fn non_bsh_owner_cannot_update_fee_ratio() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(NATIVE_COIN_BSH_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(NATIVE_COIN_BSH_IS_REGISTERED)
                .and(NATIVE_COIN_BSH_CONTRACT_IS_NOT_OWNED_BY_CHUCK)
                .and(FEE_NUMERATOR_IS_PROVIDED_AS_SET_FEE_RATIO_PARAM)
                .when(CHUCK_INVOKES_SET_FEE_RATIO_IN_NATIVE_COIN_BSH)
                .then(NATIVE_COIN_BSH_SHOULD_THROW_UNAUTHORIZED_ERROR_ON_SETTING_FEE_RATI0);
        }

        #[tokio::test(flavor = "multi_thread")]
        async fn bsh_owner_cannot_update_fee_ratio_with_fee_numerator_higher_than_fee_denominator()
        {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(NATIVE_COIN_BSH_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(NATIVE_COIN_BSH_IS_REGISTERED)
                .and(NATIVE_COIN_BSH_CONTRACT_IS_OWNED_BY_BOB)
                .and(FEE_NUMERATOR_GREATER_THAN_FEE_DENOMINATOR_IS_PROVIDED_AS_SET_FEE_RATIO_PARAM)
                .when(BOB_INVOKES_SET_FEE_RATIO_IN_NATIVE_COIN_BSH)
                .then(NATIVE_COIN_BSH_SHOULD_THROW_INVALID_NUMERATOR_ERROR_ON_SETTING_FEE_RATI0);
        }

        #[tokio::test(flavor = "multi_thread")]
        async fn newly_added_bsh_owner_can_update_fee_ratio() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(NATIVE_COIN_BSH_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(NATIVE_COIN_BSH_IS_REGISTERED)
                .and(CHARLIES_ACCOUNT_IS_CREATED)
                .and(NATIVE_COIN_BSH_CONTRACT_IS_OWNED_BY_BOB)
                .and(CHARLIES_ACCOUNT_ID_IS_PROVIDED_AS_ADD_OWNER_PARAM)
                .and(BOB_INVOKES_ADD_OWNER_IN_NATIVE_COIN_BSH)
                .and(FEE_NUMERATOR_IS_PROVIDED_AS_SET_FEE_RATIO_PARAM)
                .when(CHARLIE_INVOKES_SET_FEE_RATIO_IN_NATIVE_COIN_BSH)
                .then(FEE_RATIO_SHOULD_BE_UPDATED_IN_NATIVE_COIN_BSH);
        }

        #[tokio::test(flavor = "multi_thread")]
        async fn removed_bsh_owner_cannot_update_fee_ratio() {
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
                .and(FEE_NUMERATOR_IS_PROVIDED_AS_SET_FEE_RATIO_PARAM)
                .when(CHARLIE_INVOKES_SET_FEE_RATIO_IN_NATIVE_COIN_BSH)
                .then(NATIVE_COIN_BSH_SHOULD_THROW_UNAUTHORIZED_ERROR_ON_SETTING_FEE_RATI0);
        }
    }

    mod token_bash {
        use super::*;

        #[tokio::test(flavor = "multi_thread")]
        async fn removed_bsh_owner_cannot_update_fee_ratio() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(BMC_CONTRACT_IS_OWNED_BY_ALICE)
                .and(TOKEN_BSH_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(TOKEN_BSH_CONTRACT_IS_OWNED_BY_BOB)
                .and(CHARLIE_IS_AN_EXISTING_OWNER_IN_TOKEN_BSH)
                .and(CHARLIES_ACCOUNT_ID_IS_PROVIDED_AS_REMOVE_OWNER_PARAM)
                .and(BOB_INVOKES_REMOVE_OWNER_IN_TOKEN_BSH)
                .and(FEE_NUMERATOR_IS_PROVIDED_AS_SET_FEE_RATIO_PARAM)
                .when(CHARLIE_INVOKES_SET_FEE_RATIO_IN_TOKEN_BSH)
                .then(TOKEN_BSH_SHOULD_THROW_UNAUTHORIZED_ERROR_ON_SETTING_FEE_RATIO);
        }

        #[tokio::test(flavor = "multi_thread")]
        async fn owner_can_update_fee_ratio_for_a_registered_token() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(BMC_CONTRACT_IS_OWNED_BY_ALICE)
                .and(TOKEN_BSH_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(TOKEN_BSH_CONTRACT_IS_OWNED_BY_BOB)
                .and(FEE_NUMERATOR_IS_PROVIDED_AS_SET_FEE_RATIO_PARAM)
                .when(BOB_INVOKES_SET_FEE_RATIO_IN_TOKEN_BSH)
                .then(FEE_RATIO_SHOULD_BE_UPDATED_IN_TOKEN_BSH);
        }

        #[tokio::test(flavor = "multi_thread")]
        async fn non_owner_cannot_update_fee_ratio_for_a_registered_token() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(BMC_CONTRACT_IS_OWNED_BY_ALICE)
                .and(TOKEN_BSH_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(TOKEN_BSH_CONTRACT_IS_NOT_OWNED_BY_CHUCK)
                .and(FEE_NUMERATOR_IS_PROVIDED_AS_SET_FEE_RATIO_PARAM)
                .when(CHUCK_INVOKES_SET_FEE_RATIO_IN_NATIVE_COIN_TOKEN_BSH)
                .then(TOKEN_BSH_SHOULD_THROW_UNAUTHORIZED_ERROR_ON_SETTING_FEE_RATIO);
        }
    }
}
