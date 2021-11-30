mod steps;

#[cfg(test)]
mod manage_verifers {
    use super::*;
    use kitten::*;
    use steps::*;

    mod bmc {
        use super::*;

        #[workspaces::test(sandbox)]
        async fn add_link_as_authorized_success(){
            Kitten::given(NEW_CONTEXT)
            .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
            .and(ALICE_IS_BMC_CONTRACT_OWNER)
            .and(VERIFIER_FOR_ICON_IS_ADDED)
            .and(ICON_LINK_ADDRESS_IS_PROVIDED_AS_ADD_LINK_PARAM)
            .when(ALICE_INVOKES_ADD_LINK_IN_BMC)
            .then(LINKS_ARE_QURIED_IN_BMC)
            .and(ADDED_LINK_SHOULD_BE_IN_LIST)
        }

        #[workspaces::test(sandbox)]
        async fn add_link_with_existing_10_links_as_authorized_success(){
            Kitten::given(NEW_CONTEXT)
            .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
            .and(ALICE_IS_BMC_CONTRACT_OWNER)
            .and(ALREADY_HAVE_10_EXISTING_LINKS)
            .and(VERIFIER_FOR_ICON_IS_ADDED)
            .and(ICON_LINK_ADDRESS_IS_PROVIDED_AS_ADD_LINK_PARAM)
            .when(ALICE_INVOKES_ADD_LINK_IN_BMC)
            .then(LINKS_ARE_QURIED_IN_BMC)
            .and(LEN_OF_LINKS_SHOULD_BE_11)
        }

        #[ignore = "Scalability issue due to max gas limit 300000000000000, Max link with this approach is 12 Links"]
        #[workspaces::test(sandbox)]
        async fn add_link_with_existing_12_links_as_authorized_success(){
            Kitten::given(NEW_CONTEXT)
            .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
            .and(ALICE_IS_BMC_CONTRACT_OWNER)
            .and(ALREADY_HAVE_12_EXISTING_LINKS)
            .and(VERIFIER_FOR_ICON_IS_ADDED)
            .and(ICON_LINK_ADDRESS_IS_PROVIDED_AS_ADD_LINK_PARAM)
            .when(ALICE_INVOKES_ADD_LINK_IN_BMC)
            .then(LINKS_ARE_QURIED_IN_BMC)
            .and(LEN_OF_LINKS_SHOULD_BE_13)
        }

        #[ignore]
        #[workspaces::test(sandbox)]
        async fn add_link_as_unauthorized_fail(){
            Kitten::given(NEW_CONTEXT)
            .and(BMC_CONTRACT_IS_DEPLOYED)
            .when(ADD_LINK_INVOKED_BY_NON_BMC_OWNER)
            .then(BMC_SHOULD_THROW_UNAUTHORIZED_ERROR)
        }

        #[ignore]
        #[workspaces::test(sandbox)]
        async fn add_link_with_invalid_address_authorized_fail(){
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED)
                .when(ADD_LINK_WITH_INVALID_ADDRESS_INVOKED_BY_BMC_OWNER)
                .then(BMC_SHOULD_THROW_NOTEXIST_ERROR)
        }

        #[ignore]
        #[workspaces::test(sandbox)]
        async fn add_existing_link_as_authorized_fail(){
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED)
                .when(ADD_LINK_INVOKED_WITH_EXISTING_ADDRESS_BY_BMC_OWNER)
                .then(BMC_SHOULD_THROW_ALREADY_EXIST_ERROR)

        }

        #[ignore]
        #[workspaces::test(sandbox)]
        async fn remove_link_as_authorized_success(){
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED)
                .when(REMOVE_LINK_INVKED_BY_BMC_OWNER)
                .then(LINKS_ARE_QURIED_IN_BMC)
                .and(REMOVED_LINK_SHOULD_NOT_BE_PRESENT)

        }

        #[ignore]
        #[workspaces::test(sandbox)]
        async fn remove_link_as_unauthorized_fail(){
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED)
                .when(REMOVE_LINK_INVKED_BY_NON_BMC_OWNER)
                .then(BMC_SHOULD_THROW_UNAUTHORIZED_ERROR)

        }

        #[ignore]
        #[workspaces::test(sandbox)]
        async fn remove_non_existing_link_as_authorized_fail(){
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED)
                .when(REMOVE_NON_EXISTING_LINK_BY_BMC_OWNER)
                .then(BMC_SHOULD_THROW_NOTEXIST_ERROR)
        }

        #[ignore]
        #[workspaces::test(sandbox)]
        async fn set_link_as_authorized_success(){
            unimplemented!()
        }

        #[ignore]
        #[workspaces::test(sandbox)]
        async fn set_link_as_unauthorized_fail(){
            unimplemented!()
        }

        #[ignore]
        #[workspaces::test(sandbox)]
        async fn set_link_for_non_existing_link_authorized_fail(){
            unimplemented!()
        }


    }

}
