mod steps;

#[cfg(test)]
mod manage_verifers {
    use super::*;
    use kitten::*;
    use steps::*;

    mod bmc {
        use super::*;

        #[runner::test(sandbox)]
        async fn add_link_as_authorized_success(){
            Kitten::given(NEW_CONTEXT)
            .and(BMC_CONTRACT_IS_DEPLOYED)
            .when(ADD_LINK_INVOKED_BY_BMC_OWNER)
            .then(LINKS_ARE_QURIED_IN_BMC)
            .and(ADDED_LINK_SHOULD_BE_IN_LIST)
        }

        #[runner::test(sandbox)]
        async fn add_link_as_unauthorized_fail(){
            Kitten::given(NEW_CONTEXT)
            .and(BMC_CONTRACT_IS_DEPLOYED)
            .when(ADD_LINK_INVOKED_BY_NON_BMC_OWNER)
            .then(BMC_SHOULD_THROW_UNAUTHORIZED_ERROR)
        }

        #[runner::test(sandbox)]
        async fn add_link_with_invalid_address_authorized_fail(){
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED)
                .when(ADD_LINK_WITH_INVALID_ADDRESS_INVOKED_BY_BMC_OWNER)
                .then(BMC_SHOULD_THROW_NOTEXIST_ERROR)
        }

        #[runner::test(sandbox)]
        async fn add_existing_link_as_authorized_fail(){
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED)
                .when(ADD_LINK_INVOKED_WITH_EXISTING_ADDRESS_BY_BMC_OWNER)
                .then(BMC_SHOULD_THROW_ALREADY_EXIST_ERROR)

        }

        #[runner::test(sandbox)]
        async fn remove_link_as_authorized_success(){
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED)
                .when(REMOVE_LINK_INVKED_BY_BMC_OWNER)
                .then(LINKS_ARE_QURIED_IN_BMC)
                .and(REMOVED_LINK_SHOULD_NOT_BE_PRESENT)

        }

        #[runner::test(sandbox)]
        async fn remove_link_as_unauthorized_fail(){
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED)
                .when(REMOVE_LINK_INVKED_BY_NON_BMC_OWNER)
                .then(BMC_SHOULD_THROW_UNAUTHORIZED_ERROR)

        }

        #[runner::test(sandbox)]
        async fn remove_non_existing_link_as_authorized_fail(){
            Kitten::given(NEW_CONTEXT)
                .and(BMC_CONTRACT_IS_DEPLOYED)
                .when(REMOVE_NON_EXISTING_LINK_BY_BMC_OWNER)
                .then(BMC_SHOULD_THROW_NOTEXIST_ERROR)
        }

        #[runner::test(sandbox)]
        async fn set_link_as_authorized_success(){
            unimplemented!()
        }

        #[runner::test(sandbox)]
        async fn set_link_as_unauthorized_fail(){
            unimplemented!()
        }

        #[runner::test(sandbox)]
        async fn set_link_for_non_existing_link_authorized_fail(){
            unimplemented!()
        }


    }

}
