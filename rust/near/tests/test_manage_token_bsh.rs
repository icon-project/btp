mod steps;

#[cfg(test)]
mod manage_verifers {
    use super::*;
    use kitten::*;
    use steps::*;

    mod bmc {
        use super::*;

        #[runner::test(sandbox)]
        async fn register_new_token_as_authorized_success(){
            Kitten::given(NEW_CONTEXT)
            .and(BSH_CONTRACT_IS_DEPLOYED)
            .when(BSH_OWNER_REGISTERS_NEW_COIN)
            .then(COIN_NAMES_ARE_QURIED_IN_BSH)
            .and(COIN_REGISTERED_SHOULD_BE_PRESENT)

        }

        #[runner::test(sandbox)]
        async fn register_new_token_as_unauthorized_fail(){
            Kitten::given(NEW_CONTEXT)
            .and(BSH_CONTRACT_IS_DEPLOYED)
            .when(NON_BSH_OWNER_REGISTERS_NEW_COIN)
            .then(BSH_SHOULD_THROW_UNAUTHORIZED_ERROR)
        }

        #[runner::test(sandbox)]
        async fn register_existing_token_as_authorized_fail(){
            Kitten::given(NEW_CONTEXT)
            .and(BSH_CONTRACT_IS_DEPLOYED)
            .when(BSH_OWNER_REGISTERS_EXISTING_COIN)
            .then(BSH_SHOULD_THROW_ALREADY_EXIST_ERRROR)
        }

        #[runner::test(sandbox)]
        async fn transfer_native_coin_to_valid_btp_address_success(){
            Kitten::given(NEW_CONTEXT)
            .and(BSH_CONTRACT_IS_DEPLOYED)
            .when(BSH_OWNER_INVOKES_NATIVE_COIN_TRANSFER_TO_VALID_ADDRESS)
            .then(COIN_BALANCES_ARE_QURIED)
            .and(COIN_VALUE_SENT_SHOULD_BE_EQUAL_TO_COIN_VALUE_DEDUCTED)
        }

        #[runner::test(sandbox)]
        async fn transfer_native_coin_to_invlaid_btp_address_fail(){
            Kitten::given(NEW_CONTEXT)
            .and(BSH_CONTRACT_IS_DEPLOYED)
            .when(BSH_OWNER_INVOKES_NATIVE_COIN_TRANSFER_TO_INVALID_ADDRESS)
            .then(BSH_SHOULD_THROW_INVALID_ADDRESS)
        }

        #[runner::test(sandbox)]
        async fn transfer_native_coin_of_zero_value_to_valid_btp_address_fail(){
            Kitten::given(NEW_CONTEXT)
            .and(BSH_CONTRACT_IS_DEPLOYED)
            .when(BSH_OWNER_INVOKES_NATIVE_COIN_WITH_NOVALUE)
            .then(BSH_SHOULD_THROW_FAIL_TO_TRANSFER_ERROR)
        }

        #[runner::test(sandbox)]
        async fn transfer_native_coin_to_network_not_supported(){
            Kitten::given(NEW_CONTEXT)
            .and(BSH_CONTRACT_IS_DEPLOYED)
            .when(BSH_OWNER_INVOKES_NATIVE_COIN_TRANSFER_TO_UNSUPPORTED_NETWORK)
            .then(BSH_SHOULD_THROW_INVALID_ADDRESS)
        }


    }
}