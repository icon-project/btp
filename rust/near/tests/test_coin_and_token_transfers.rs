mod steps;

#[cfg(test)]
mod manage_token_transfer {
    use super::*;
    use kitten::*;
    use steps::*;

    mod native_coin_transfer {
        use super::*;
        #[tokio::test(flavor = "multi_thread")]
        async fn btp_message_recieved_from_icon_to_mint_and_transfer_wrapped_native_coin_to_receiver(
        ) {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(BMC_CONTRACT_IS_OWNED_BY_ALICE)
                .and(WRAPPED_ICX_COIN_IS_REGESITERED_IN_NATIVE_COIN_BSH)
                .and(NATIVE_COIN_BSH_SERVICE_IS_ADDED_TO_BMC)
                .and(CHARLIES_ACCOUNT_IS_CREATED)
                .and(NATIVE_COIN_BSH_HANDLES_RECEIVED_SERVICE_MESSAGE)
                .and(USER_INVOKES_GET_COIN_ID_FROM_NATIVE_COIN_BSH_FOR_WRAPPED_COIN)
                .when(USER_INVOKES_BALANCE_OF_TOKEN_BSH)
                .then(AMOUNT_SHOULD_BE_PRESENT_IN_TOKEN_BSH_ACCOUNT);
        }

        #[tokio::test(flavor = "multi_thread")]
        async fn user_can_deposit_wrapped_fungible_coin() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(BMC_CONTRACT_IS_OWNED_BY_ALICE)
                .and(WRAPPED_ICX_COIN_IS_REGESITERED_IN_NATIVE_COIN_BSH)
                .and(NATIVE_COIN_BSH_SERVICE_IS_ADDED_TO_BMC)
                .and(CHARLIES_ACCOUNT_IS_CREATED)
                .and(NATIVE_COIN_BSH_HANDLES_RECEIVED_SERVICE_MESSAGE)
                .and(CHARLIE_WITHDRAWS_AMOUNT)
                .and(CHARLIE_DEPOSITS_TO_WRAPPED_FUNGIBLE_COIN)
                .when(USER_INVOKES_BALANCE_OF_TOKEN_BSH)
                .then(AMOUNT_SHOULD_BE_PRESENT_IN_TOKEN_BSH_ACCOUNT_ON_DEPOSITING);
        }

        #[tokio::test(flavor = "multi_thread")]
        async fn user_can_transfer_native_near_coin_to_cross_chain() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(BMC_CONTRACT_IS_OWNED_BY_ALICE)
                .and(NATIVE_COIN_BSH_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(NATIVE_COIN_BSH_CONTRACT_IS_OWNED_BY_BOB)
                .and(NATIVE_COIN_BSH_SERVICE_IS_ADDED_TO_BMC)
                .and(CHARLIES_ACCOUNT_IS_CREATED_AND_AMOUNT_DEPOSITED)
                .when(CHARLIES_TRANSFERS_NATIVE_NEAR_COIN_TO_CROSS_CHAIN)
                .then(TRANSFERED_AMOUNT_SHOULD_BE_DEDUCTED_FROM_ACCOUNT_ON_TRANSFERING_NATIVE_COIN);
        }

        #[tokio::test(flavor = "multi_thread")]
        async fn user_can_transfer_wrapped_native_coin_to_cross_chain() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(BMC_CONTRACT_IS_OWNED_BY_ALICE)
                .and(WRAPPED_ICX_COIN_IS_REGESITERED_IN_NATIVE_COIN_BSH)
                .and(NATIVE_COIN_BSH_SERVICE_IS_ADDED_TO_BMC)
                .and(CHARLIES_ACCOUNT_IS_CREATED)
                .and(NATIVE_COIN_BSH_HANDLES_RECEIVED_SERVICE_MESSAGE)
                .and(CHARLIE_WITHDRAWS_AMOUNT)
                .and(CHARLIE_DEPOSITS_TO_WRAPPED_FUNGIBLE_COIN)
                .when(CHARLIE_TRANSFER_WRAPPED_NATIVE_COIN_TO_CROSS_CHAIN)
                .then(TRANSFERED_AMOUNT_SHOULD_BE_DEDUCTED_FROM_WRAPPED_TOKEN_BALANCE);
        }

        #[tokio::test(flavor = "multi_thread")]
        async fn bsh_can_handle_btp_message_to_unlock_and_transfer_native_coin_to_receiver() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(BMC_CONTRACT_IS_OWNED_BY_ALICE)
                .and(WRAPPED_ICX_COIN_IS_REGESITERED_IN_NATIVE_COIN_BSH)
                .and(NATIVE_COIN_BSH_SERVICE_IS_ADDED_TO_BMC)
                .and(CHARLIES_ACCOUNT_IS_CREATED)
                .and(NATIVE_COIN_BSH_HANDLES_RECEIVED_SERVICE_MESSAGE)
                .and(BSH_RECEIVES_RESPONSE_HANDLE_BTP_MESSAGE_TO_NATIVE_COIN)
                .and(ALICE_INVOKES_HANDLE_SERVICE_MESSAGE_IN_NATIVE_COIN_BSH)
                .when(CHARLIE_INVOKES_BALANCE_FROM_NATIVE_COIN_BSH)
                .then(BALANCE_SHOULD_BE_UNLOCKED_AFTER_GETTING_SUCCESS_RESPONSE);
        }

        #[tokio::test(flavor = "multi_thread")]
        async fn user_cannot_transfer_0_native_near_coin_to_cross_chain() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(BMC_CONTRACT_IS_OWNED_BY_ALICE)
                .and(NATIVE_COIN_BSH_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(NATIVE_COIN_BSH_CONTRACT_IS_OWNED_BY_BOB)
                .and(NATIVE_COIN_BSH_SERVICE_IS_ADDED_TO_BMC)
                .and(CHARLIES_ACCOUNT_IS_CREATED_AND_AMOUNT_DEPOSITED)
                .when(CHARLIES_TRANSFERS_0_NATIVE_NEAR_COIN_TO_CROSS_CHAIN)
                .then(NATIVE_COIN_BSH_SHOULD_THROW_USER_CANNOT_TRANSFER_0_COIN_ERROR_ON_TRANSFERRING_COIN);
        }
    }
}
