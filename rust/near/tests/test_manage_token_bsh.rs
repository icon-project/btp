// mod steps;

// #[cfg(test)]
// mod manage_verifers {
//     use super::*;
//     use kitten::*;
//     use steps::*;

//     mod bmc {
//         use super::*;

//         #[runner::test(sandbox)]
//         async fn register_new_token_as_authorized_success(){
//             Kitten::given(NEW_CONTEXT)
//             .and(BSH_CONTRACT_IS_DEPLOYED)
//             .when(BSH_OWNER_REGISTERS_NEW_COIN)
//             .then(COIN_NAMES_ARE_QURIED_IN_BSH)
//             .and(COIN_REGISTERED_SHOULD_BE_PRESENT)

//         }

//         #[runner::test(sandbox)]
//         async fn register_new_token_as_unauthorized_fail(){
//             Kitten::given(NEW_CONTEXT)
//             .and(BSH_CONTRACT_IS_DEPLOYED)
//             .when(NON_BSH_OWNER_REGISTERS_NEW_COIN)
//             .then(BSH_SHOULD_THROW_UNAUTHORIZED_ERROR)
//         }

//         #[runner::test(sandbox)]
//         async fn register_existing_token_as_authorized_fail(){
//             Kitten::given(NEW_CONTEXT)
//             .and(BSH_CONTRACT_IS_DEPLOYED)
//             .when(BSH_OWNER_REGISTERS_EXISTING_COIN)
//             .then(BSH_SHOULD_THROW_ALREADY_EXIST_ERRROR)
//         }

//         #[runner::test(sandbox)]
//         async fn transfer_native_coin_to_valid_btp_address_success(){
//             Kitten::given(NEW_CONTEXT)
//             .and(BSH_CONTRACT_IS_DEPLOYED)
//             .when(BSH_OWNER_INVOKES_NATIVE_COIN_TRANSFER_TO_VALID_ADDRESS)
//             .then(COIN_BALANCES_ARE_QURIED)
//             .and(COIN_VALUE_SENT_SHOULD_BE_EQUAL_TO_COIN_VALUE_DEDUCTED)
//         }

//         #[runner::test(sandbox)]
//         async fn transfer_native_coin_to_invlaid_btp_address_fail(){
//             Kitten::given(NEW_CONTEXT)
//             .and(BSH_CONTRACT_IS_DEPLOYED)
//             .when(BSH_OWNER_INVOKES_NATIVE_COIN_TRANSFER_TO_INVALID_ADDRESS)
//             .then(BMC_SHOULD_THROW_INVALIDADDRESS_ERROR)
//         }

//         #[runner::test(sandbox)]
//         async fn transfer_native_coin_of_zero_value_to_valid_btp_address_fail(){
//             Kitten::given(NEW_CONTEXT)
//             .and(BSH_CONTRACT_IS_DEPLOYED)
//             .when(BSH_OWNER_INVOKES_NATIVE_COIN_WITH_NOVALUE)
//             .then(BSH_SHOULD_THROW_FAIL_TO_TRANSFER_ERROR)
//         }

//         #[runner::test(sandbox)]
//         async fn transfer_native_coin_to_network_not_supported(){
//             Kitten::given(NEW_CONTEXT)
//             .and(BSH_CONTRACT_IS_DEPLOYED)
//             .when(BSH_OWNER_INVOKES_NATIVE_COIN_TRANSFER_TO_UNSUPPORTED_NETWORK)
//             .then(BMC_SHOULD_THROW_INVALIDADDRESS_ERROR)
//         }

//         //bsh owner management

//         #[runner::test(sandbox)]
//         async fn add_an_owner_to_bsh_authorized_success(){
//             Kitten::given(NEW_CONTEXT)
//             .and(BSH_CONTRACT_IS_DEPLOYED)
//             .when(BSH_OWNER_ADDS_NEW_OWNER)
//             .then(ONWERS_ARE_QURIED_IN_BSH)
//             .and(ADDED_OWNER_SHOULD_BE_PRESENT)
//         }

//         #[runner::test(sandbox)]
//         async fn add_an_exsisting_owner_as_authorized_success(){
//             Kitten::given(NEW_CONTEXT)
//             .and(BSH_CONTRACT_IS_DEPLOYED)
//             .when(BSH_OWNER_ADD_EXSISTING_OWNER)
//             .then(BMC_SHOULD_THROW_ALREADY_EXIST_ERROR)
//         }

//         #[runner::test(sandbox)]
//         async fn add_an_owner_unauthorized_fail(){
//             Kitten::given(NEW_CONTEXT)
//             .and(BSH_CONTRACT_IS_DEPLOYED)
//             .when(NON_BSH_OWNER_INVOKES_ADD_OWNER)
//             .then(BSH_SHOULD_THROW_UNAUTHORIZED_ERROR)
//         }
//         #[runner::test(sandbox)]
//         async fn remove_an_owner_as_authorized_success(){
//             Kitten::given(NEW_CONTEXT)
//             .and(BSH_CONTRACT_IS_DEPLOYED)
//             .when(NON_BSH_OWNER_INVOKES_REMOVE_OWNER)
//             .then(BSH_SHOULD_THROW_UNAUTHORIZED_ERROR)
//         }

//         #[runner::test(sandbox)]
//         async fn remove_non_exsisting_as_authorized_fail(){
//             Kitten::given(NEW_CONTEXT)
//             .and(BSH_CONTRACT_IS_DEPLOYED)
//             .when(BSH_OWNER_TRIES_TO_REMOVE_NON_EXSISTING_OWNER)
//             .then(BMC_SHOULD_THROW_NOTEXIST_ERROR)
//         }

//         #[runner::test(sandbox)]
//         async fn remove_owner_authorized_success(){
//             Kitten::given(NEW_CONTEXT)
//             .and(BSH_CONTRACT_IS_DEPLOYED)
//             .when(BSH_OWNER_INVOKES_REMOVE_OWNER)
//             .then(ONWERS_ARE_QURIED_IN_BSH)
//             .and(REMOVE_ONWER_SHOULD_NOT_BE_PRESENT)
//         }

//         #[runner::test(sandbox)]
//         async fn update_bsh_periphery_as_authorized_success(){
//             Kitten::given(NEW_CONTEXT)
//             .and(BSH_CONTRACT_IS_DEPLOYED)
//             .when(UPDATE_BSH_PERIPHERY_INVOKED_BY_BSH_OWNER)
//             .then(BSH_PERIPHERY_ADDRESS_SHOULD_BE_UPDATED)
//         }

//         #[runner::test(sandbox)]
//         async fn update_bsh_periphery_as_unauthorized_fail(){
//             Kitten::given(NEW_CONTEXT)
//             .and(BSH_CONTRACT_IS_DEPLOYED)
//             .when(UPDATE_BSH_PERIPHERY_INVOKED_BY_NON_BSH_OWNER)
//             .then(BSH_SHOULD_THROW_UNAUTHORIZED_ERROR)
//         }

//         #[runner::test(sandbox)]
//         async fn update_uri_as_authorized_success(){
//             Kitten::given(NEW_CONTEXT)
//             .and(BSH_CONTRACT_IS_DEPLOYED)
//             .when(UPDATE_URI_INVOKED_BY_BSH_OWNER)
//             .then(BSH_TOKEN_URI_IS_UPDATED)
//         }

//         #[runner::test(sandbox)]
//         async fn update_uri_as_unauthorized_fail(){
//             Kitten::given(NEW_CONTEXT)
//             .and(BSH_CONTRACT_IS_DEPLOYED)
//             .when(UPDATE_URI_INVOKED_BY_NON_BSH_OWNER)
//             .then(BSH_SHOULD_THROW_UNAUTHORIZED_ERROR)
//         }

//         #[runner::test(sandbox)]
//         async fn set_fee_ratio_as_authorized_success(){
//             Kitten::given(NEW_CONTEXT)
//             .and(BSH_CONTRACT_IS_DEPLOYED)
//             .when(SET_FEE_RATIO_INVOKED_BY_BSH_OWNER)
//             .then(FEE_NUMERATOR_IS_UPDATED)
//         }

//         #[runner::test(sandbox)]
//         async fn set_fee_ratio_as_unauthorized_fail(){
//             Kitten::given(NEW_CONTEXT)
//             .and(BSH_CONTRACT_IS_DEPLOYED)
//             .when(SET_FEE_RATIO_INVOKED_BY_NON_BSH_OWNER)
//             .then(BSH_SHOULD_THROW_UNAUTHORIZED_ERROR)        }

//         #[runner::test(sandbox)]
//         async fn set_fixed_fee_as_authorized_success(){
//             Kitten::given(NEW_CONTEXT)
//             .and(BSH_CONTRACT_IS_DEPLOYED)
//             .when(SET_FIXED_FEE_INVOKED_BY_BSH_OWNER)
//             .then(FIXED_FEE_IS_UPDATED)
//         }

//         #[runner::test(sandbox)]
//         async fn set_fixed_fee_as_unauthorized_fail(){
//             Kitten::given(NEW_CONTEXT)
//             .and(BSH_CONTRACT_IS_DEPLOYED)
//             .when(SET_FIXED_FEE_INVOKED_BY_NON_BSH_OWNER)
//             .then(BSH_SHOULD_THROW_UNAUTHORIZED_ERROR)
//         }
//     }
// }