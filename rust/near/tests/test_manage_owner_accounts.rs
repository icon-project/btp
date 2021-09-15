mod steps;

#[cfg(test)]
mod manage_owner_accounts {
    use super::*;
    use kitten::*;
    use steps::*;

    mod bmc {
        use super::*;

        #[runner::test(sandbox)]
        async fn add_new_owner_as_authorized_success() {
            Kitten::given(new_context)
                .and(bmc_contract_deployed)
                .when(|hello_world| Some(hello_world))
                .then(|_| {
                    assert_eq!(true, true); // we assert on our Option<&str>
                });
            // Deploy Smart Contracts
            // deploy_bmc_contract
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
