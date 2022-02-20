mod steps;

#[cfg(test)]
mod manage_nep141{
    use super::*;
    use kitten::*;
    use steps::*;

    mod nep141 {
        use super::*;

        #[tokio::test(flavor = "multi_thread")]
        async fn user_can_mint_coins() {
            Kitten::given(NEW_CONTEXT)
                .and(CHARLIES_ACCOUNT_IS_CREATED)
                .and(WRAPPED_ICX_COIN_IS_REGESITERED)
                .when(CHARLIE_INVOKES_MINT_IN_BMC)
                .then(AMOUNT_SHOULD_BE_MINTED);
        }

        #[tokio::test(flavor = "multi_thread")]
        async fn user_can_burn_coins() {
            Kitten::given(NEW_CONTEXT)
                .and(CHARLIES_ACCOUNT_IS_CREATED)
                .and(WRAPPED_ICX_COIN_IS_REGESITERED)
                .and(CHARLIE_INVOKES_MINT_IN_BMC)
                .when(CHARLIE_INVOKES_BURN_IN_BMC)
                .then(AMOUNT_SHOULD_BE_BURNED);
        }
    }
}
