use chrono::Utc;
use near_crypto::{InMemorySigner, KeyType};
use rand::Rng;

#[derive(Clone)]
pub struct Signer {
    account_id: String,
    signer: InMemorySigner,
}

impl Default for Signer {
    fn default() -> Signer {
        let mut rng = rand::thread_rng();
        let random_num = rng.gen_range(10000000000000usize..99999999999999);
        let account_id = format!("dev-{}-{}", Utc::now().format("%Y%m%d%H%M%S"), random_num);
        let signer = InMemorySigner::from_seed(&account_id, KeyType::ED25519, "test");
        Signer { signer, account_id }
    }
}

impl Signer {
    pub fn new(account_id: String, signer: InMemorySigner) -> Signer {
        Signer { signer, account_id }
    }

    pub fn account_id(&self) -> &String {
        &self.account_id
    }

    pub fn signer(&self) -> &InMemorySigner {
        &self.signer
    }
}
