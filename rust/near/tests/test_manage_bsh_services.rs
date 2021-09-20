mod steps;

#[cfg(test)]
mod manage_bsh_services {
    use super::*;
    use kitten::*;
    use steps::*;

    mod bmc {
        use super::*;

        #[runner::test(sandbox)]
        async fn request_service_as_bsh_contract_owner_success() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED)
                .and(BSH_CONTRACT_IS_DEPLOYED)
                .and(BOB_IS_BSH_CONTRACT_OWNER)
                .and(SERVICE_NAME_AND_SMARTCONTRACT_ADDRESS_PROVIDED_AS_REQUEST_SERVICE_PARAM)
                .when(BOB_INVOKES_REQUEST_SERVICE_IN_BMC_FROM_BSH)
                .then(ON_QUERYING_REQUESTS_IN_BMC)
                .and(REQUEST_ADDED_SHOULD_BE_IN_REQUETS)
        }

        #[runner::test(sandbox)]
        async fn request_service_unauthorized_fail() {
            Kitten::given(NEW_CONTEXT)
            .and(BMC_CONTRACT_IS_DEPLOYED)
            .and(BSH_CONTRACT_IS_DEPLOYED)
            .and(CHUCK_IS_NOT_BSH_CONTRACT_OWNER)
            .and(SERVICE_NAME_AND_SMARTCONTRACT_ADDRESS_PROVIDED_AS_REQUEST_SERVICE_PARAM)
            .when(CHUCK_INVOKES_REQUEST_SERVICE_IN_BMC_FROM_BSH)
            .then(BMC_SHOULD_THROW_UNAUTHORIZED_ERROR)

        }

        #[runner::test(sandbox)]
        async fn request_service_of_invalid_contract_address_authorized_fail() {
            Kitten::given(NEW_CONTEXT)
            .and(BMC_CONTRACT_IS_DEPLOYED)
            .and(BSH_CONTRACT_IS_DEPLOYED)
            .and(BOB_IS_BSH_CONTRACT_OWNER)
            .and(SERVICE_NAME_AND_INVALID_SMARTCONTRACT_ADDRESS_PROVIDED_AS_REQUEST_SERVICE_PARAM)
            .when(BOB_INVOKES_REQUEST_SERVICE_IN_BMC_FROM_BSH)
            .then(BMC_SHOULD_THROW_INVALIDADDRESS_ERROR)
        }

        #[runner::test(sandbox)]
        async fn request_service_of_existing_service_authorized_fail() {
            Kitten::given(NEW_CONTEXT)
            .and(BMC_CONTRACT_IS_DEPLOYED)
            .and(BSH_CONTRACT_IS_DEPLOYED)
            .and(BOB_IS_BSH_CONTRACT_OWNER)
            .and(SERVICE_NAME_AND_SMARTCONTRACT_ADDRESS_PROVIDED_AS_REQUEST_SERVICE_PARAM)
            .and(BOB_INVOKES_REQUEST_SERVICE_IN_BMC_FROM_BSH)
            .and(EXISTING_SERVICE_NAME_AND_SMARTCONTRACT_ADDRESS_PROVIDED_AS_REQUEST_SERVICE_PARAM)
            .when(BOB_INVOKES_REQUEST_SERVICE_IN_BMC_FROM_BSH)
            .then(BMC_SHOULD_THROW_REQUESTEXIST_ERROR)
        }


        #[runner::test(sandbox)]
        async fn approve_service_authorized_success() {
            Kitten::given(NEW_CONTEXT)
            .and(BMC_CONTRACT_IS_DEPLOYED)
            .and(BSH_CONTRACT_IS_DEPLOYED)
            .and(BOB_IS_BSH_CONTRACT_OWNER)
            .and(SERVICE_NAME_AND_SMARTCONTRACT_ADDRESS_PROVIDED_AS_REQUEST_SERVICE_PARAM)
            .and(BOB_INVOKES_REQUEST_SERVICE_IN_BMC_FROM_BSH)
            .and(ALICE_IS_BMC_CONTRACT_OWNER)
            .when(ALICE_INVOKES_APPROVE_SERVICE_IN_BMC)
            .then(SERVICE_SHOULD_BE_ADDED_IN_SERVICES)

        }

        #[runner::test(sandbox)]
        async fn approve_service_unauthorized_fail() {
            Kitten::given(NEW_CONTEXT)
            .and(BMC_CONTRACT_IS_DEPLOYED)
            .and(BSH_CONTRACT_IS_DEPLOYED)
            .and(BOB_IS_BSH_CONTRACT_OWNER)
            .and(SERVICE_NAME_AND_SMARTCONTRACT_ADDRESS_PROVIDED_AS_REQUEST_SERVICE_PARAM)
            .and(BOB_INVOKES_REQUEST_SERVICE_IN_BMC_FROM_BSH)
            .and(CHUCK_IS_NOT_BMC_CONTRACT_OWNER)
            .when(CHUCK_INVOKES_APPROVE_SERVICE_IN_BMC)
            .then(BMC_SHOULD_THROW_UNAUTHORIZED_ERROR)

        }

        #[runner::test(sandbox)]
        async fn remove_service_authorized_success() {
            Kitten::given(NEW_CONTEXT)
            .and(BMC_CONTRACT_IS_DEPLOYED)
            .and(BSH_CONTRACT_IS_DEPLOYED)
            .and(BOB_IS_BSH_CONTRACT_OWNER)
            .and(SERVICE_NAME_AND_SMARTCONTRACT_ADDRESS_PROVIDED_AS_REQUEST_SERVICE_PARAM)
            .and(BOB_INVOKES_REQUEST_SERVICE_IN_BMC_FROM_BSH)
            .and(ALICE_INVOKES_APPROVE_SERVICE_IN_BMC)
            .and(SERVICE_NAME_PROVIDED_AS_REMOVE_REQUEST_PARAM)
            .when(ALICE_INVOKES_REMOVE_REQUEST)
            .then(SERVICE_NOT_BE_PRESENT_IN_SERVICES)
        }

        #[runner::test(sandbox)]
        async fn remove_service_unauthorized_fail() {
            Kitten::given(NEW_CONTEXT)
            .and(BMC_CONTRACT_IS_DEPLOYED)
            .and(BSH_CONTRACT_IS_DEPLOYED)
            .and(BOB_IS_BSH_CONTRACT_OWNER)
            .and(SERVICE_NAME_AND_SMARTCONTRACT_ADDRESS_PROVIDED_AS_REQUEST_SERVICE_PARAM)
            .and(BOB_INVOKES_REQUEST_SERVICE_IN_BMC_FROM_BSH)
            .and(ALICE_INVOKES_APPROVE_SERVICE_IN_BMC)
            .and(CHUCK_IS_NOT_BMC_CONTRACT_OWNER)
            .and(SERVICE_NAME_PROVIDED_AS_REMOVE_REQUEST_PARAM)
            .when(CHUCK_INVOKES_REMOVE_REQUEST)
            .then(SERVICE_NOT_BE_PRESENT_IN_SERVICES)
        }

        #[runner::test(sandbox)]
        async fn remove_non_existing_service_authorized_success() {
            Kitten::given(NEW_CONTEXT)
            .and(BMC_CONTRACT_IS_DEPLOYED)
            .and(BSH_CONTRACT_IS_DEPLOYED)
            .and(BOB_IS_BSH_CONTRACT_OWNER)
            .and(SERVICE_NAME_AND_SMARTCONTRACT_ADDRESS_PROVIDED_AS_REQUEST_SERVICE_PARAM)
            .and(BOB_INVOKES_REQUEST_SERVICE_IN_BMC_FROM_BSH)
            .and(ALICE_IS_BMC_CONTRACT_OWNER)
            .and(ALICE_INVOKES_APPROVE_SERVICE_IN_BMC)
            .and(NONEXISTING_SERVICE_NAME_PROVIDED_AS_REMOVE_REQUEST_PARAM)
            .when(ALICE_INVOKES_REMOVE_REQUEST)
            .then(BMC_SHOULD_THROW_NOTEXIST_ERROR)
        }

    }
}
