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
                .and(NEW_OWNER_IS_PROVIDED)
                .when(BMC_CONTRACT_OWNER_INVOKES_ADD_OWNER)
                .then(|_| {
                    assert_eq!(true, true);
                });
        }

        #[test]
        fn add_existing_owner_as_authorized_fail() {
            unimplemented!();
        }

        #[test]
        fn add_new_owner_as_unauthorized_fail() {
            unimplemented!();
        }

        #[test]
        fn remove_owner_as_authorized_success() {
            unimplemented!();
        }

        #[test]
        fn remove_last_owner_as_authorized_fail() {
            unimplemented!();
        }

        #[test]
        fn remove_non_existing_owner_as_authorized_fail() {
            unimplemented!();
        }

        #[test]
        fn remove_owner_as_unauthorized_fail() {
            unimplemented!();
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
