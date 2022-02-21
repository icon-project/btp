use super::*;

impl NativeCoinService {
    // * * * * * * * * * * * * * * * * *
    // * * * * * * * * * * * * * * * * *
    // * * * * * * * Utils * * * * * * *
    // * * * * * * * * * * * * * * * * *
    // * * * * * * * * * * * * * * * * *

    pub fn hash_coin_id(coin_name: &String) -> CoinId {
        env::sha256(coin_name.as_bytes())
    }
}
