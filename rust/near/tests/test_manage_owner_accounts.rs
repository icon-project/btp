mod steps;

#[cfg(test)]
mod manage_owner_accounts {
    use super::*;
    use kitten::*;
    use steps::*;

    mod bmc {
        use super::*;

        #[runner::test(sandbox)]
        async fn add_new_owner_as_bmc_contract_owner_success() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED)
                .and(ALICE_IS_BMC_CONTRACT_OWNER)
                .and(CHARLES_ACCOUNT_CREATED_AND_PASSED_AS_ADD_OWNER_PARAM)
                .when(ALICE_INVOKES_ADD_OWNER_IN_BMC)
                .then(OWNERS_IN_BMC_ARE_QUERIED)
                .and(CHARLIES_ACCOUNT_ID_SHOULD_BE_IN_OWNERS_LIST);
        }

        #[runner::test(sandbox)]
        async fn add_existing_owner_as_authorized_fail() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED)
                .and(ALICE_IS_BMC_CONTRACT_OWNER)
                .and(CHARLIES_ACCOUNT_IS_CREATED)
                .and(CHARLIE_IS_AN_EXISITNG_OWNER_IN_BMC)
                .and(CHARLIES_ACCOUNT_ID_IS_PROVIDED_AS_ADD_OWNER_PARAM)
                .when(ALICE_INVOKES_ADD_OWNER_IN_BMC)
                .then(OWNERS_IN_BMC_ARE_QUERIED)
                .and(BMC_SHOULD_THROW_ALREADY_EXIST_ERROR);
        }

        #[runner::test(sandbox)]
        async fn add_new_owner_as_unauthorized_fail() {
           Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED)
                .and(CHUCKS_ACCOUNT_IS_CREATED)
                .and(CHUCK_IS_NOT_A_BMC_OWNER)
                .and(CHARLIES_ACCOUNT_IS_CREATED)
                .and(CHARLIES_ACCOUNT_ID_IS_PROVIDED_AS_ADD_OWNER_PARAM)
                .when(CHUCK_INVOKES_ADD_OWNER_IN_BMC)
                .then(BMC_SHOULD_THROW_UNAUTHORIZED_ERROR);
                
        }

        #[runner::test(sandbox)]
        async fn remove_owner_as_authorized_success() {
            Kitten::given(NEW_CONTEXT)
            .and(BMC_CONTRACT_IS_DEPLOYED)
            .and(ALICE_IS_BMC_CONTRACT_OWNER)
            .and(CHARLIES_ACCOUNT_IS_CREATED)
            .and(CHARLIE_IS_AN_EXISITNG_OWNER_IN_BMC)
            .and(CHARLIES_ACCOUNT_ID_IS_PROVIDED_AS_REMOVE_OWNER_PARAM)
            .when(ALICE_INVOKES_REMOVE_OWNER_IN_BMC)
            .then(OWNERS_IN_BMC_ARE_QUERIED)
            .and(CHARLIES_ACCOUNT_ID_SHOULD_NOT_BE_IN_OWNERS_LIST);
        }

        #[runner::test(sandbox)]
        async fn remove_last_owner_as_authorized_fail() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED)
                .and(ALICE_IS_BMC_CONTRACT_OWNER)
                .and(ALICE_ACCOUNT_ID_IS_PROVIDED_AS_REMOVE_OWNER_PARAM)
                .when(ALICE_INVOKES_REMOVE_OWNER_IN_BMC)
                .then(BMC_SHOULD_THROW_LASTOWNER_ERROR)

        }
        #[runner::test(sandbox)]
        async fn remove_non_existing_owner_as_authorized_fail() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED)
                .and(ALICE_IS_BMC_CONTRACT_OWNER)
                .and(CHARLIES_ACCOUNT_IS_CREATED)
                .and(CHARLIES_ACCOUNT_ID_IS_PROVIDED_AS_REMOVE_OWNER_PARAM)
                .when(ALICE_INVOKES_REMOVE_OWNER_IN_BMC)
                .then(BMC_SHOULD_THROW_NOTEXIST_ERROR);
        }

        #[runner::test(sandbox)]
        async fn remove_owner_as_unauthorized_fail() {
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED)
                .and(CHUCKS_ACCOUNT_IS_CREATED)
                .and(CHUCK_IS_NOT_A_BMC_OWNER)
                .and(CHARLIES_ACCOUNT_IS_CREATED)
                .and(CHARLIES_ACCOUNT_ID_IS_PROVIDED_AS_REMOVE_OWNER_PARAM)
                .when(CHUCK_INVOKES_REMOVE_OWNER_IN_BMC)
                .then(BMC_SHOULD_THROW_UNAUTHORIZED_ERROR);
        }
    }

    mod bsh {
        #[ignore]
        #[test]
        fn it_works() {
            unimplemented!();
        }
    }
}

