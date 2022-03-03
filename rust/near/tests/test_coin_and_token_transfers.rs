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
                .and(BMV_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(BMC_CONTRACT_IS_OWNED_BY_ALICE)
                .and(ICON_LINK_IS_PRESENT_IN_BMC)
                .and(NATIVE_COIN_BSH_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(NATIVE_COIN_BSH_SERVICE_IS_ADDED_TO_BMC)
                .and(WRAPPED_ICX_COIN_IS_REGESITERED_IN_NATIVE_COIN_BSH)
                .and(CHARLIES_ACCOUNT_IS_CREATED)
                .and(NATIVE_COIN_BSH_HANDLES_RECEIVED_SERVICE_MESSAGE)
                .when(CHARLIE_INVOKES_WRAPPED_COIN_BALANCE_IN_NATIVE_COIN_BSH)
                .then(AMOUNT_SHOULD_BE_PRESENT_IN_NATIVE_COIN_BSH_ACCOUNT);
        }

        #[tokio::test(flavor = "multi_thread")]
        async fn user_can_deposit_wrapped_fungible_coin() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(BMC_CONTRACT_IS_OWNED_BY_ALICE)
                .and(BMV_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(ICON_LINK_IS_PRESENT_IN_BMC)
                .and(NATIVE_COIN_BSH_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(WRAPPED_ICX_COIN_IS_REGESITERED_IN_NATIVE_COIN_BSH)
                .and(NATIVE_COIN_BSH_SERVICE_IS_ADDED_TO_BMC)
                .and(CHARLIES_ACCOUNT_IS_CREATED)
                .and(NATIVE_COIN_BSH_HANDLES_RECEIVED_SERVICE_MESSAGE)
                .and(CHARLIE_WITHDRAWS_AMOUNT_FROM_NATIVE_COIN_BSH)
                .and(CHARLIE_DEPOSITS_TO_WRAPPED_FUNGIBLE_COIN)
                .when(CHARLIE_INVOKES_BALANCE_IN_NATIVE_COIN_BSH)
                .then(AMOUNT_SHOULD_BE_PRESENT_IN_TOKEN_BSH_ACCOUNT_ON_DEPOSITING);
        }

        #[tokio::test(flavor = "multi_thread")]
        async fn user_can_transfer_native_near_coin_to_cross_chain() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(BMV_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(BMC_CONTRACT_IS_OWNED_BY_ALICE)
                .and(ICON_LINK_IS_PRESENT_IN_BMC)
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
                .and(BMV_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(NATIVE_COIN_BSH_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(BMC_CONTRACT_IS_OWNED_BY_ALICE)
                .and(ICON_LINK_IS_PRESENT_IN_BMC)
                .and(WRAPPED_ICX_COIN_IS_REGESITERED_IN_NATIVE_COIN_BSH)
                .and(NATIVE_COIN_BSH_SERVICE_IS_ADDED_TO_BMC)
                .and(CHARLIES_ACCOUNT_IS_CREATED)
                .and(NATIVE_COIN_BSH_HANDLES_RECEIVED_SERVICE_MESSAGE)
                .and(CHARLIE_WITHDRAWS_AMOUNT_FROM_NATIVE_COIN_BSH)
                .and(CHARLIE_DEPOSITS_TO_WRAPPED_FUNGIBLE_COIN)
                .when(CHARLIE_TRANSFER_WRAPPED_NATIVE_COIN_TO_CROSS_CHAIN)
                .then(TRANSFERED_AMOUNT_SHOULD_BE_DEDUCTED_FROM_WRAPPED_TOKEN_BALANCE);
        }

        #[tokio::test(flavor = "multi_thread")]
        async fn bsh_can_handle_btp_message_to_unlock_and_transfer_native_coin_to_receiver() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(NATIVE_COIN_BSH_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
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

        #[tokio::test(flavor = "multi_thread")]
        async fn user_can_withdraw_wrapped_native_coin() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(NATIVE_COIN_BSH_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(BMC_CONTRACT_IS_OWNED_BY_ALICE)
                .and(WRAPPED_ICX_COIN_IS_REGESITERED_IN_NATIVE_COIN_BSH)
                .and(NATIVE_COIN_BSH_SERVICE_IS_ADDED_TO_BMC)
                .and(CHARLIES_ACCOUNT_IS_CREATED)
                .and(NATIVE_COIN_BSH_HANDLES_RECEIVED_SERVICE_MESSAGE)
                .and(CHARLIE_WITHDRAWS_AMOUNT_FROM_NATIVE_COIN_BSH)
                .when(CHARLIE_INVOKES_BALANCE_IN_NATIVE_COIN_BSH)
                .then(AFTER_WITHDRAW_CHARLIES_AMOUNT_SHOULD_BE_DEDUCTED_AND_BALANCE_SHOULD_BE_PRESENT_IN_NATIVE_COIN_BSH_ACCOUNT);
        }

        #[tokio::test(flavor = "multi_thread")]
        async fn users_can_query_withdrawble_balance_of_wrapped_native_coin() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(NATIVE_COIN_BSH_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(BMC_CONTRACT_IS_OWNED_BY_ALICE)
                .and(WRAPPED_ICX_COIN_IS_REGESITERED_IN_NATIVE_COIN_BSH)
                .and(NATIVE_COIN_BSH_NAME_AND_ACCOUNT_ID_ARE_PROVIDED_AS_ADD_SERVICE_PARAM)
                .and(ALICE_INVOKES_ADD_SERVICE_IN_BMC)
                .and(CHARLIES_ACCOUNT_IS_CREATED)
                .and(NATIVE_COIN_BSH_HANDLES_RECEIVED_SERVICE_MESSAGE)
                .when(CHARLIE_INVOKES_WRAPPED_COIN_BALANCE_IN_NATIVE_COIN_BSH)
                .then(CHARLIES_WITHDRAWABLE_AMOUNT_IN_NATIVE_COIN_BSH_ACCOUNT);
        }

        #[tokio::test(flavor = "multi_thread")]
        async fn users_can_query_withdrawable_balance_of_near_native_coin() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(BMC_CONTRACT_IS_OWNED_BY_ALICE)
                .and(NATIVE_COIN_BSH_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(NATIVE_COIN_BSH_CONTRACT_IS_OWNED_BY_BOB)
                .and(NATIVE_COIN_BSH_SERVICE_IS_ADDED_TO_BMC)
                .and(CHARLIES_ACCOUNT_IS_CREATED_AND_AMOUNT_DEPOSITED)
                .when(CHARLIE_INVOKES_NATIVE_COIN_BALANCE_IN_NATIVE_COIN_BSH)
                .then(BALANCE_OF_CHARLIES_ACCOUNT_SHOULD_BE_PRESENT_IN_THE_ACCOUNT);
        }

        #[tokio::test(flavor = "multi_thread")]
        async fn user_can_deposit_wrapped_native_coin_using_ft_on_transfer() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(NATIVE_COIN_BSH_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(BMC_CONTRACT_IS_OWNED_BY_ALICE)
                .and(WRAPPED_ICX_COIN_IS_REGESITERED_IN_NATIVE_COIN_BSH)
                .and(NATIVE_COIN_BSH_SERVICE_IS_ADDED_TO_BMC)
                .and(CHARLIES_ACCOUNT_IS_CREATED)
                .and(NATIVE_COIN_BSH_HANDLES_RECEIVED_SERVICE_MESSAGE)
                .and(CHARLIE_WITHDRAWS_AMOUNT_FROM_NATIVE_COIN_BSH)
                .and(CHARLIE_DEPOSITS_TO_WRAPPED_FUNGIBLE_COIN)
                .when(CHARLIE_INVOKES_FT_BALANCE_IN_NATIVE_COIN_BSH)
                .then(DEPOSITED_AMOUNT_SHOULD_BE_ADDED_TO_ACCOUNT);
        }

        #[tokio::test(flavor = "multi_thread")]
        async fn user_cannot_withdraw_more_than_available_deposit_to_wallet() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(NATIVE_COIN_BSH_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(BMC_CONTRACT_IS_OWNED_BY_ALICE)
                .and(WRAPPED_ICX_COIN_IS_REGESITERED_IN_NATIVE_COIN_BSH)
                .and(NATIVE_COIN_BSH_SERVICE_IS_ADDED_TO_BMC)
                .and(CHARLIES_ACCOUNT_IS_CREATED)
                .and(NATIVE_COIN_BSH_HANDLES_RECEIVED_SERVICE_MESSAGE)
                .when(CHARLIE_WITHDRAWS_AMOUNT_MORE_THAN_AVAILABLE_DEPOSIT)
                .then(NATIVE_COIN_BSH_SHOULD_THROW_ERROR_ON_WITHDRAWAL);
        }

        #[tokio::test(flavor = "multi_thread")]
        async fn user_cannot_transfer_more_than_available_deposit_to_cross_chain() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(NATIVE_COIN_BSH_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(BMC_CONTRACT_IS_OWNED_BY_ALICE)
                .and(WRAPPED_ICX_COIN_IS_REGESITERED_IN_NATIVE_COIN_BSH)
                .and(NATIVE_COIN_BSH_SERVICE_IS_ADDED_TO_BMC)
                .and(CHARLIES_ACCOUNT_IS_CREATED)
                .and(NATIVE_COIN_BSH_HANDLES_RECEIVED_SERVICE_MESSAGE)
                .and(CHARLIE_WITHDRAWS_AMOUNT_FROM_NATIVE_COIN_BSH)
                .and(CHARLIE_DEPOSITS_TO_WRAPPED_FUNGIBLE_COIN)
                .when(CHARLIE_TRANSFERS_WRAPPED_NATIVE_COINS_MORE_THAN_AVAILABLE_DEPOSIT_TO_CROSS_CHAIN)
                .then(NATIVE_COIN_BSH_SHOULD_THROW_NO_MINIMUM_DEPOSIT_ON_TRANSFERING_COIN);
        }

        #[tokio::test(flavor = "multi_thread")]
        async fn user_cannot_deposit_more_than_available_balance() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(NATIVE_COIN_BSH_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(BMC_CONTRACT_IS_OWNED_BY_ALICE)
                .and(WRAPPED_ICX_COIN_IS_REGESITERED_IN_NATIVE_COIN_BSH)
                .and(NATIVE_COIN_BSH_SERVICE_IS_ADDED_TO_BMC)
                .and(CHARLIES_ACCOUNT_IS_CREATED)
                .and(NATIVE_COIN_BSH_HANDLES_RECEIVED_SERVICE_MESSAGE)
                .and(CHARLIE_WITHDRAWS_AMOUNT_FROM_NATIVE_COIN_BSH)
                .when(CHARLIE_DEPOSITS_MORE_THAN_AVAILABLE_BALANCE)
                .then(BSH_SHOULD_THROW_NOT_ENOUGH_BALANCE_ERROR_ON_DEPOSITING_AMOUNT);
        }

        #[tokio::test(flavor = "multi_thread")]
        async fn user_cannot_reclaim_more_than_failed_amount_to_deposit() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(NATIVE_COIN_BSH_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(BMC_CONTRACT_IS_OWNED_BY_ALICE)
                .and(WRAPPED_ICX_COIN_IS_REGESITERED_IN_NATIVE_COIN_BSH)
                .and(NATIVE_COIN_BSH_SERVICE_IS_ADDED_TO_BMC)
                .and(CHARLIES_ACCOUNT_IS_CREATED)
                .and(NATIVE_COIN_BSH_HANDLES_RECEIVED_SERVICE_MESSAGE)
                .and(CHARLIE_WITHDRAWS_AMOUNT_FROM_NATIVE_COIN_BSH)
                .and(CHARLIE_TRANSFERS_WRAPPED_NATIVE_COINS_INVALID_LINK_TO_CROSS_CHAIN)
                .and(BSH_RECEIVES_AND_HANDLE_RESPONSE_HANDLE_BTP_MESSAGE_FOR_FAILED_TRANSFER_TO_NATIVE_COIN_BSH)
                .and(CHARLIE_INVOKES_RECLAIM_MORE_THAN_FAILED_AMOUNT_IN_NATIVE_COIN_BSH)
                .and(NATIVE_COIN_BSH_SHOULD_THROW_NO_MINIMUM_REFUNDABLE_AMOUNT_ON_RECLAIMING_AMOUNT);
        }
    }

    mod token_bsh_transfer {

        use super::*;

        #[tokio::test(flavor = "multi_thread")]
        async fn users_can_withdraw_wrapped_fungible_token() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(BMC_CONTRACT_IS_OWNED_BY_ALICE)
                .and(BALN_TOKEN_IS_REGISTERED_IN_TOKEN_BSH)
                .and(TOKEN_BSH_SERVICE_IS_ADDED_TO_BMC)
                .and(CHARLIES_ACCOUNT_IS_CREATED)
                .and(TOKEN_BSH_HANDLES_RECIEVED_SERVICE_MESSAGE)
                .and(CHARLIE_WITHDRAWS_AMOUNT_TOKEN_BSH)
                .when(CHARLIE_INVOKES_BALANCE_OF_IN_TOKEN_BSH)
                .then(AFTER_WITHDRAW_AMOUNT_SHOULD_BE_PRESENT_IN_TOKEN_BSH_ACCOUNT);
        }

        #[tokio::test(flavor = "multi_thread")]
        async fn implement_ft_on_transfer_for_wrapped_token_deposit() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(BMC_CONTRACT_IS_OWNED_BY_ALICE)
                .and(BALN_TOKEN_IS_REGISTERED_IN_TOKEN_BSH)
                .and(TOKEN_BSH_SERVICE_IS_ADDED_TO_BMC)
                .and(CHARLIES_ACCOUNT_IS_CREATED)
                .and(TOKEN_BSH_HANDLES_RECIEVED_SERVICE_MESSAGE)
                .and(CHARLIE_WITHDRAWS_AMOUNT_TOKEN_BSH)
                .and(CHARLIE_DEPOSITS_TO_WRAPPED_FUNGIBLE_TOKEN)
                .when(CHARLIE_INVOKES_BALANCE_OF_IN_TOKEN_BSH)
                .then(AFTER_DEPOSIT_AMOUNT_SHOULD_BE_PRESENT_IN_CHARLIES_ACCOUNT);
        }

        #[tokio::test(flavor = "multi_thread")]
        async fn bsh_can_respond_back_with_response_message_if_the_transfer_has_been_completed() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(BMC_CONTRACT_IS_OWNED_BY_ALICE)
                .and(BALN_TOKEN_IS_REGISTERED_IN_TOKEN_BSH)
                .and(TOKEN_BSH_SERVICE_IS_ADDED_TO_BMC)
                .and(CHARLIES_ACCOUNT_IS_CREATED)
                .and(TOKEN_BSH_HANDLES_RECIEVED_SERVICE_MESSAGE)
                .and(CHARLIE_INVOKES_BALN_TOKEN_BALANCE_IN_TOKEN_BSH)
                .when(CHARLIE_INVOKES_BALN_TOKEN_BALANCE_IN_TOKEN_BSH)
                .then(BALNCE_SHOULD_BE_PRESENT_IN_CHARLIES_ACCOUNT_AFTER_GETTING_SUCCESS_RESPONSE);
        }

        #[tokio::test(flavor = "multi_thread")]
        async fn users_can_deposit_native_near_token() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(BMC_CONTRACT_IS_OWNED_BY_ALICE)
                .and(CHARLIES_ACCOUNT_IS_CREATED)
                .and(WNEAR_TOKEN_IS_REGISTERED_IN_TOKEN_BSH)
                .and(TOKEN_BSH_SERVICE_IS_ADDED_TO_BMC)
                .and(CHARLIE_DEPOSITS_WNEAR_TO_CHARLIES_TOKEN_BSH_ACCOUNT)
                .when(CHARLIE_INVOKES_WNEAR_TOKEN_BALANCE_IN_TOKEN_BSH)
                .then(BALANCE_SHOULD_BE_PRESENT_IN_CHARLIES_ACCOUNT_AFTER_DEPOSIT);
        }

        #[tokio::test(flavor = "multi_thread")]
        async fn user_cannot_transfer_0_native_near_tokens_to_cross_chain() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(BMC_CONTRACT_IS_OWNED_BY_ALICE)
                .and(CHARLIES_ACCOUNT_IS_CREATED)
                .and(WNEAR_TOKEN_IS_REGISTERED_IN_TOKEN_BSH)
                .and(TOKEN_BSH_SERVICE_IS_ADDED_TO_BMC)
                .and(USER_INVOKES_GET_TOKEN_ID_FOR_WNEAR_FROM_TOKEN_BSH)
                .and(CHARLIE_DEPOSITS_WNEAR_TO_CHARLIES_TOKEN_BSH_ACCOUNT)
                .when(CHARLIE_TRANSFERS_0_NATIVE_NEAR_TOKENS_TO_CROSS_CHAIN)
                .then(NATIVE_COIN_BSH_SHOULD_THROW_USER_CANNOT_TRANSFER_0_COIN_ERROR_ON_TRANSFERRING_COIN);
        }

        #[tokio::test(flavor = "multi_thread")]
        async fn user_cannot_reclaim_more_than_failed_amount_to_deposit() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(BMC_CONTRACT_IS_OWNED_BY_ALICE)
                .and(BALN_TOKEN_IS_REGISTERED_IN_TOKEN_BSH)
                .and(TOKEN_BSH_SERVICE_IS_ADDED_TO_BMC)
                .and(CHARLIES_ACCOUNT_IS_CREATED)
                .and(TOKEN_BSH_HANDLES_RECIEVED_SERVICE_MESSAGE)
                .and(CHARLIE_WITHDRAWS_AMOUNT_TOKEN_BSH)
                .and(CHARLIE_TRANSFERS_BALN_TOKENS_TO_INVALID_DESTINATION_IN_CROSS_CHAIN)
                .and(BSH_RECEIVES_AND_HANDLE_RESPONSE_HANDLE_BTP_MESSAGE_FOR_FAILED_TRANSFER_TO_TOKEN_BSH)
                .when(CHARLIE_INVOKES_RECLAIM_MORE_THAN_FAILED_AMOUNT_IN_TOKEN_BSH)
                .then(TOKEN_BSH_SHOULD_THROW_NO_MINIMUM_REFUNDABLE_AMOUNT_ON_RECLAIMING_AMOUNT);
        }
    }
}
