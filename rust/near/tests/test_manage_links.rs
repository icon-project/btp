mod steps;

#[cfg(test)]
mod manage_links {
    use super::*;
    use kitten::*;
    use steps::*;

    mod bmc {
        use super::*;

        #[workspaces::test(sandbox)]
        async fn add_link_as_authorized_success(){
            Kitten::given(NEW_CONTEXT)
            .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
            .and(BMV_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
            .and(BMC_CONTRACT_IS_OWNED_BY_ALICE)
            .and(VERIFIER_FOR_ICON_IS_ADDED)
            .and(ICON_LINK_ADDRESS_IS_PROVIDED_AS_ADD_LINK_PARAM)
            .when(ALICE_INVOKES_ADD_LINK_IN_BMC)
            .then(ICON_LINK_SHOULD_BE_ADDED_TO_LIST);
        }

        #[workspaces::test(sandbox)]
        async fn add_link_as_unauthorized_fail(){
            Kitten::given(NEW_CONTEXT)
            .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
            .and(BMV_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
            .and(BMC_CONTRACT_IS_NOT_OWNED_BY_CHUCK)
            .and(CHUCKS_ACCOUNT_IS_CREATED)
            .and(VERIFIER_FOR_ICON_IS_ADDED)
            .and(ICON_LINK_ADDRESS_IS_PROVIDED_AS_ADD_LINK_PARAM)
            .when(CHUCK_INVOKES_ADD_LINK_IN_BMC)
            .then(BMC_SHOULD_THROW_UNAUTHORIZED_ERROR_ON_ADD_LINK);
        }

        #[workspaces::test(sandbox)]
        async fn set_link_as_authorized_success(){
            Kitten::given(NEW_CONTEXT)
            .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
            .and(BMV_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
            .and(BMC_CONTRACT_IS_OWNED_BY_ALICE)
            .and(ICON_LINK_IS_ADDED)
            .and(ICON_LINK_ADDRESS_IS_PROVIDED_AS_SET_LINK_PARAM)
            .when(ALICE_INVOKES_SET_LINK_IN_BMC)
            .then(ICON_LINK_STATUS_SHOULD_BE_UPDATED)
        }
    }
}
