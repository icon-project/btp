use near_crypto::InMemorySigner;
use runner::dev_deploy;
use std::path::Path;
use tokio::time::{sleep, Duration};

pub async fn deploy_contract(path: &str) -> Result<(String, InMemorySigner), String> {
    sleep(Duration::from_millis(18000)).await;
    dev_deploy(Path::new(path)).await
}