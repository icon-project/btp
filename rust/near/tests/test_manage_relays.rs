mod steps;

#[cfg(test)]
mod manage_verifers {
    use super::*;
    use kitten::*;
    use steps::*;

    mod bmc {
        use super::*;

        #[workspaces::test(sandbox)]
        async fn bmc_owner_can_add_bmr_as_list_and_overwrite_an_existing_bmr() {
          Kitten::given(NEW_CONTEXT)
            .and(BMC_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
            .and(BMV_CONTRACT_IS_DEPLOYED_AND_INITIALIZED)
            .and(BMC_CONTRACT_IS_OWNED_BY_ALICE)
            .and(ICON_LINK_IS_PRESENT_IN_BMC)
            .and(RELAY_1_IS_REGISTERED)
            .and(RELAY_2_ACCOUNT_IS_CREATED)
            .and(ICON_LINK_ADDRESS_AND_RELAY_1_ACCOUNT_ID_AND_RELAY_2_ACCOUNT_ID_ARE_PROVIDED_AS_ADD_RELAYS_PARAM)
            .when(ALICE_INVOKES_ADD_RELAY_IN_BMC)
            .then(ADDED_RELAYS_SHOULD_BE_IN_BMC_RELAY_LIST)
        }
    }
}