mod steps;

#[cfg(test)]
mod manage_verifers {
    use super::*;
    use kitten::*;
    use steps::*;

    mod bmc {
        use super::*;
        #[runner::test(sandbox)]
        async fn handle_relay_message_with_invalid_relay_fails(){
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED)
                .and(CHCUCK_IS_NOT_REGISTERED_RELAYER)
                .when(HANDLE_RELAY_MESSSAGE_IS_INVOKED_WITH_INVALID_RELAY)
                .then(BMC_SHOULD_THROW_UNAUTHORIZED_ERROR)

        }

        #[runner::test(sandbox)]
        async fn dispatch_service_message_to_bsh(){
            Kitten::given(NEW_CONTEXT)
            .and(BMC_CONTRACT_IS_DEPLOYED)
            .when(HANDLE_RELAY_MESSAGE_INVOKED_WITH_EXISTING_RELAY)
            .then(BMC_SHOULD_SEND_SERVICE_MESSAGE_TO_BSH)
        }

        #[runner::test(snadbox)]
        async fn init_link_via_btp_message(){
            Kitten::given(NEW_CONTEXT)
            .and(BMC_CONTRACT_IS_DEPLOYED)
            .when(HANDLE_RELAY_MESSAGE_INVOKED_WITH_EXISTING_RELAY_WITH_INIT_MESSAGE)
            .then(BMC_INITIALIZES_LINK)
        }

        #[runner::test(sandbox)]
        async fn process_link_via_btp_message(){
            Kitten::given(NEW_CONTEXT)
            .and(BMC_CONTRACT_IS_DEPLOYED)
            .when(HANDLE_RELAY_MESSAGE_INVOKED_WITH_EXISTING_RELAY_WITH_LINK_MESSAGE)
            .then(BMC_SHOULD_EMIT_EVEN_TO_PROPAGATE_LINKS_EVENT)
        }

        #[runner::test(sandbox)]
        async fn process_unlink_via_btp_message(){
            Kitten::given(NEW_CONTEXT)
            .and(BMC_CONTRACT_IS_DEPLOYED)
            .when(HANDLE_RELAY_MESSAGE_INVOKED_WITH_EXISTING_RELAY_WITH_UNLINK_MESSAGE)
            .then(BMC_SHOULD_EMIT_EVEN_TO_PROPAGATE_UNLINKS_EVENT)
        }

        #[runner:test(sandbox)]
        async fn hand_realy_message_revert_event_handle_not_exist(){
            Kitten::given(NEW_CONTEXT)
            .and(BMC_CONTRACT_IS_DEPLOYED)
            .when(HANDLE_REALY_MESSAGE_INVOKED_WITH_INVALID_EVENT_HANDLER_EVENT_MESSAGE)
            .then(BMC_SHOULD_THROW_NOTEXIST_ERROR)
        }

        #[runner::test(sandbox)]
        async fn emit_event_to_send_btp_error_response_if_routes_failed_to_resolve(){
            Kitten::given(NEW_CONTEXT)
            .and(BMC_CONTRACT_IS_DEPLOYED)
            .when(HANDLE_RELAY_MESSAGE_INVOKED_WITH_NO_ROUTES)
            .then(BMC_EMITS_EVENT_TO_PREVIOUS_BMC)
        }

        #[runner::test(sandbox)]
        async fn emit_event_to_send_btp_error_response_if_routes_succeed_to_resolve(){
            Kitten::given(NEW_CONTEXT)
            .and(BMC_CONTRACT_IS_DEPLOYED)
            .when(HANDLE_RELAY_MESSAGE_INVOKED_WITH_ROUTES)
            .then(BMC_EMITS_EVENT_TO_DESTINED_BMC)
        }
        #[runner::test(sandbox)]
        async fn emit_event_to_send_btp_error_response_if_destined_bmc_is_current_bmc_address_in_btp_message(){
            Kitten::given(NEW_CONTEXT)
            .and(BMC_CONTRACT_IS_DEPLOYED)
            .when(HANDLE_RELAY_MESSAGE_INVOKED_WITH_SERIAL_NUMBER_10)
            .then(BMC_EMITS_EVENT_TO_PREVIOUS_BMC)
        }

        #[runner::test(sandbox)]
        async fn dispathc_gather_fee_message_to_bsh_services(){
            Kitten::given(NEW_CONTEXT)
            .and(BMC_CONTRACT_IS_DEPLOYED)
            .when(HANDLE_RELAY_MESSAGE_INVOKED_WITH_SERVICE_TYPE_FEE_GATHERING)
            .then(BMC_EMITS_EVENT_TO_BSH_SERVICES)
        }
    }
}
