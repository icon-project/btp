use chrono::Utc;
use near_crypto::{InMemorySigner, KeyType, PublicKey};
use rand::Rng;

#[derive(Clone)]
pub struct Signer(InMemorySigner);

impl Default for Signer {
    fn default() -> Signer {
        let mut rng = rand::thread_rng();
        let random_num = rng.gen_range(10000000000000usize..99999999999999);
        let account_id = format!("dev-{}-{}", Utc::now().format("%Y%m%d%H%M%S"), random_num);
        let signer = InMemorySigner::from_seed(&account_id, KeyType::ED25519, "test");
        Signer(signer)
    }
}

impl Signer {
    pub fn new(signer: InMemorySigner) -> Signer {
        Signer(signer)
    }

    pub fn account_id(&self) -> &String {
        &self.0.account_id
    }

    pub fn public_key(&self) -> &PublicKey {
        &self.0.public_key
    }

    pub fn get(&self) -> &InMemorySigner {
        &self.0
    }
}
