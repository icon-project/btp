mod steps;

#[cfg(test)]
mod handle_relay_message {
    use super::*;
    use kitten::*;
    use steps::*;

    mod bmc {
        use super::*;

        #[tokio::test(flavor = "multi_thread")]
        async fn bmc_throw_error_message_for_init_link_btp_message_receiving_from_unregistered_link() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(BMV_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(BMC_CONTRACT_IS_OWNED_BY_ALICE)
                .and(ICON_LINK_IS_PRESENT_IN_BMC)
                .and(RELAY_1_IS_REGISTERED_FOR_ICON_LINK)
                .and(BMC_INIT_LINK_RELAY_MESSAGE_IS_PROVIDED_AS_HANDLE_RELAY_MESSAGE_PARAM_FOR_NON_EXISTING_LINK)
                .when(RELAY_1_INVOKES_HANDLE_RELAY_MESSAGE_IN_BMC)
                .then(BMC_SHOULD_THROW_LINK_DOES_NOT_EXIST_ERROR_ON_HANDLING_RELAY_MESSAGES);
        }

        #[tokio::test(flavor = "multi_thread")]
        async fn unregistered_relay_cannot_send_relay_message_to_bmc() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(BMV_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(BMC_CONTRACT_IS_OWNED_BY_ALICE)
                .and(ICON_LINK_IS_PRESENT_IN_BMC)
                .and(RELAY_1_IS_REGISTERED_FOR_ICON_LINK)
                .and(BMC_INIT_LINK_RELAY_MESSAGE_IS_PROVIDED_AS_HANDLE_RELAY_MESSAGE_PARAM_FOR_NON_EXISTING_LINK)
                .when(RELAY_2_INVOKES_HANDLE_RELAY_MESSAGE_IN_BMC)
                .then(BMC_SHOULD_THROW_LINK_DOES_NOT_EXIST_ERROR_ON_HANDLING_RELAY_MESSAGES);
        }

        #[tokio::test(flavor = "multi_thread")]
        async fn bmc_can_handle_init_link_message_received_from_registered_link() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(BMV_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(BMC_CONTRACT_IS_OWNED_BY_ALICE)
                .and(ICON_LINK_IS_PRESENT_IN_BMC)
                .and(RELAY_1_IS_REGISTERED_FOR_ICON_LINK)
                .and(BMC_INIT_LINK_RELAY_MESSAGE_IS_PROVIDED_AS_HANDLE_RELAY_MESSAGE_PARAM)
                .when(ALICE_INVOKES_HANDLE_INIT_LINK_MESSAGE_IN_BMC)
                .then(ICON_LINK_STATUS_SHOULD_BE_UPDATED);
        }
    }
}
