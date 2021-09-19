use crate::types::Signer;
use futures::executor::LocalPool;
use runner::create_tla_account;

pub fn create_account(signer: &Signer) {
    let mut pool = LocalPool::new();
    pool.run_until(async {
        create_tla_account(
            signer.account_id().to_owned(),
            signer.public_key().to_owned(),
        )
        .await
        .unwrap()
    });
}
