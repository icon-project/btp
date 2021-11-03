mod steps;

#[cfg(test)]
mod manage_verifers {
    use super::*;
    use kitten::*;
    use steps::*;

    mod bmc {
        use super::*;

        #[runner::test(sandbox)]
        async fn add_verifier_authorized_success(){
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED)
                .and(BMV_CONTRACT_IS_DEPLOYED)
                .when(VERIFIER_IS_ADDED_BY_BMC_CONTRACT_OWNER)
                .then(VERIFIERS_IN_BMC_ARE_QUERIED)
                .and(ADDED_VERIFIER_SHOULD_BE_IN_LIST_OF_VERIFIERS)


        }
        #[runner::test(sandbox)]
        async fn add_verifier_unauthorized_fail(){
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED)
                .and(BMV_CONTRACT_IS_DEPLOYED)
                .when(VERIFIER_IS_ADDED_BY_NON_BMC_OWNER)
                .then(BMC_SHOULD_THROW_UNAUTHORIZED_ERROR)

        }
        #[runner::test(sandbox)]
        async fn add_existing_verifier_authorized_fail(){
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED)
                .and(BMV_CONTRACT_IS_DEPLOYED)
                .when(EXISTING_VERIFIER_IS_ADDED_AGAIN_BY_BMC_OWNER)
                .then(BMC_SHOULD_THROW_ALREADY_EXIST_ERROR)


        }
        #[runner::test(sandbox)]
        async fn add_invalid_verifier_authorized_fail(){
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED)
                .and(BMV_CONTRACT_IS_DEPLOYED)
                .when(ADD_VERIFIER_INVOKED_WITH_INVALID_VERIFIER_ADDRESS)
                .then(BMC_SHOULD_THROW_INVALIDADDRESS_ERROR)

        }

        #[runner::test(sandbox)]
        async fn remove_verifier_authorized_success(){
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED)
                .and(BMV_CONTRACT_IS_DEPLOYED)
                .when(REMOVE_VERIFER_INOKED_BY_BMC_OWNER)
                .then(VERIFIER_DELETED_SHOULD_NOT_BE_IN_LIST_OF_VERIFIERS)

        }

        #[runner::test(sandbox)]
        async fn remove_verifier_unauthorized_fail(){
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED)
                .and(BMV_CONTRACT_IS_DEPLOYED)
                .when(REMOVE_VERIFER_INOKED_BY_NON_BMC_OWNER)
                .then(BMC_SHOULD_THROW_UNAUTHORIZED_ERROR)

        }

        #[runner::test(sandbox)]
        async fn remove_nonexisting_verifer_authorized_fail(){
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED)
                .and(BMV_CONTRACT_IS_DEPLOYED)
                .and(REMOVE_VERIFER_INOKED_BY_BMC_OWNER)
                .when(REMOVE_VERIFER_INOKED_BY_BMC_OWNER)
                .then(BMC_SHOULD_THROW_NOTEXIST_ERROR)

        }
    }
}
