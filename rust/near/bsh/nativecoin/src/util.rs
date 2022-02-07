use super::*;

impl NativeCoinService {
    // * * * * * * * * * * * * * * * * *
    // * * * * * * * * * * * * * * * * *
    // * * * * * * * Utils * * * * * * *
    // * * * * * * * * * * * * * * * * *
    // * * * * * * * * * * * * * * * * *

    pub fn hash_token_id(token_name: &String) -> TokenId {
        let mut sha3 = Sha3::v256();
        let mut output = [0u8; 32];
        sha3.update(token_name.as_bytes());
        sha3.finalize(&mut output);
        output.to_vec()
    }
}
