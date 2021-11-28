mod steps;

#[cfg(test)]
mod manage_bsh_services {
    use super::*;
    use kitten::*;
    use steps::*;

    mod bmc {
        use super::*;

        #[ignore]
        #[workspaces::test(sandbox)]
        async fn request_service_as_bsh_contract_owner_success() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED)
                .and(TOKEN_BSH_CONTRACT_IS_DEPLOYED)
                .and(BOB_IS_BSH_CONTRACT_OWNER)
                .and(SERVICE_NAME_AND_SMARTCONTRACT_ADDRESS_PROVIDED_AS_REQUEST_SERVICE_PARAM)
                .when(BOB_INVOKES_REQUEST_SERVICE_IN_BMC_FROM_BSH)
                .then(REQUEST_IN_BMC_ARE_QUERIED)
                .and(NEWLY_ADDED_REQUEST_SHOULD_BE_IN_BMC_REQUESTS)
        }

        #[ignore]
        #[workspaces::test(sandbox)]
        async fn request_service_unauthorized_fail() {
            Kitten::given(NEW_CONTEXT)
            .and(CONTRACTS_ARE_DEPLOYED)
            .and(CHUCK_IS_NOT_A_BSH_OWNER)
            .and(SERVICE_NAME_AND_SMARTCONTRACT_ADDRESS_PROVIDED_AS_REQUEST_SERVICE_PARAM)
            .when(CHUCK_INVOKES_REQUEST_SERVICE_IN_BMC_FROM_BSH)
            .then(BSH_SHOULD_THROW_UNAUTHORIZED_ERROR)

        }

        #[ignore]
        #[workspaces::test(sandbox)]
        async fn request_service_of_invalid_contract_address_authorized_fail() {
            Kitten::given(NEW_CONTEXT)
            .and(BMC_CONTRACT_IS_DEPLOYED)
            .and(SERVICE_NAME_AND_INVALID_SMARTCONTRACT_ADDRESS_PROVIDED_AS_REQUEST_SERVICE_PARAM)
            .when(CHUCK_INVOKES_REQUEST_SERVICE_IN_BMC_FROM_BSH)
            .then(BMC_SHOULD_THROW_INVALIDADDRESS_ERROR) //TODO: Confirm InvalidAddress Message   
        }

        #[ignore]
        #[workspaces::test(sandbox)]
        async fn request_service_of_existing_service_authorized_fail() {
            Kitten::given(NEW_CONTEXT)
            .and(BSH_IS_AN_EXISTING_SERVICE)
            .when(BOB_INVOKES_REQUEST_SERVICE_IN_BMC_FROM_BSH_AGAIN)
            .then(BSH_SHOULD_THROW_REQUESTEXIST_ERROR)
        }

        #[ignore]
        #[workspaces::test(sandbox)]
        async fn approve_service_authorized_success() {
            Kitten::given(NEW_CONTEXT)
            .and(BSH_SERVICE_ALREADY_EXISTS)
            .when(ALICE_INVOKES_APPROVE_SERVICE_IN_BMC)
            .then(BSH_SHOULD_BE_ADDED_IN_BMC_SERVICES)
            .and(BSH_REQUEST_SHOULD_BE_REMOVED_FROM_BMC_REQUESTS)

        }

        #[ignore]
        #[workspaces::test(sandbox)]
        async fn approve_service_unauthorized_fail() {
            Kitten::given(NEW_CONTEXT)
            .and(BSH_SERVICE_ALREADY_EXISTS)
            .and(CHUCK_IS_NOT_A_BMC_OWNER)
            .when(CHUCK_INVOKES_APPROVE_SERVICE_IN_BMC)
            .then(BMC_SHOULD_THROW_UNAUTHORIZED_ERROR)//

        }

        #[ignore]
        #[workspaces::test(sandbox)]
        async fn remove_service_authorized_success() {
            Kitten::given(NEW_CONTEXT)
            .and(APPROVED_BSH_SERVICE)
            .and(SERVICE_NAME_PROVIDED_AS_REMOVE_REQUEST_PARAM)
            .when(ALICE_INVOKES_REMOVE_REQUEST)
            .then(BSH_SERVICE_REMOVED_FROM_SERVICES)
        }

        #[ignore]
        #[workspaces::test(sandbox)]
        async fn remove_service_unauthorized_fail() {
            Kitten::given(NEW_CONTEXT)
            .and(APPROVED_BSH_SERVICE)
            .and(CHUCK_IS_NOT_A_BMC_OWNER)
            .and(SERVICE_NAME_PROVIDED_AS_REMOVE_REQUEST_PARAM)
            .when(CHUCK_INVOKES_REMOVE_REQUEST)
            .then(BMC_SHOULD_THROW_UNAUTHORIZED_ERROR)
        }

        #[ignore]
        #[workspaces::test(sandbox)]
        async fn remove_non_existing_service_authorized_success() {
            Kitten::given(NEW_CONTEXT)
            .and(CONTRACTS_ARE_DEPLOYED)
            .and(NONEXISTING_SERVICE_NAME_PROVIDED_AS_REMOVE_REQUEST_PARAM)
            .when(ALICE_INVOKES_REMOVE_REQUEST)
            .then(BMC_SHOULD_THROW_NOTEXIST_ERROR)
        }

    }
}
