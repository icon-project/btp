use super::*;

impl NativeCoinService {
    // * * * * * * * * * * * * * * * * *
    // * * * * * * * * * * * * * * * * *
    // * * * * * * * Utils * * * * * * *
    // * * * * * * * * * * * * * * * * *
    // * * * * * * * * * * * * * * * * *

    pub fn hash_token_id(token_name: &String) -> TokenId {
        env::sha256(token_name.as_bytes())
    }

    pub fn calculate_token_transfer_fee(
        token: &Token<NativeCoin>,
        mut amount: u128,
    ) -> Result<u128, String> {
        Ok(*(amount
            .mul(token.fee_numerator())?
            .div(token.denominator())?))
    }
}
