mod steps;

#[cfg(test)]
mod handle_relay_message {
    use super::*;
    use kitten::*;
    use steps::*;

    mod bmc {
        use super::*;

        #[workspaces::test(testnet)]
        async fn handle_relay_message_as_registered_relay_success() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(BMV_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
                .and(ALICE_IS_BMC_CONTRACT_OWNER)
                .and(ICON_LINK_IS_ADDED_AND_INITIALIZED)
                .and(RELAY_1_IS_REGISTERED)
                .and(VALID_RELAY_MESSAGE_IS_PROVIDED_AS_HANDLE_RELAY_MESSAGE_PARAM)
                .when(RELAY_1_INVOKES_HANDLE_RELAY_MESSAGE_IN_BMC)
        }
    }
}
