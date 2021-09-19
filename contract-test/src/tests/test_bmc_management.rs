#![cfg(test)]
use kitten::*;
use near_crypto::{InMemorySigner,KeyType};
use near_primitives::views::FinalExecutionOutcomeView;
use runner::*;
use serde_json::{json,Value};
use std::path::Path;
use tokio::time::{sleep, Duration};
use chrono::Utc;
use rand::Rng;

pub struct Signer {
    contract: String,
    signer: InMemorySigner,
    signer_id: String,
}

impl Signer {
    fn new(contract: String, signer: InMemorySigner, signer_id: String) -> Signer {
        Signer {
            contract,
            signer,
            signer_id,
        }
    }

    fn new_generated(contract: String) -> Signer {
        let mut rng = rand::thread_rng();
        let random_num = rng.gen_range(10000000000000usize..99999999999999);
        let signer_id = format!("dev-{}-{}", Utc::now().format("%Y%m%d%H%M%S"), random_num);

        let signer = InMemorySigner::from_seed(&signer_id, KeyType::ED25519, "test");

        Signer { contract,signer,signer_id }
    }
}

async fn deploy_contract() -> Result<(String, InMemorySigner), String> {
    sleep(Duration::from_millis(18000)).await;
    dev_deploy(Path::new(
        "/Hugobyte/BTP/contract-test/res/contract_ex.wasm",
    ))
    .await
}

async fn add_owner(
    signer:&Signer,params :&Value
) -> Result<FinalExecutionOutcomeView, String> {
    call(
        &signer,
        contract_id.clone(),
        contract_id.clone(),
        "add_owner".to_string(),
        params
        .to_string()
        .into_bytes(),
        None,
    )
    .await
}
async fn remove_owner(
    contract_id: String,
    signer: InMemorySigner,
    owner: String,
) -> Result<FinalExecutionOutcomeView, String> {
    call(
        &signer,
        contract_id.clone(),
        contract_id.clone(),
        "remove_owner".to_string(),
        json!({
            "owner": owner,
        })
        .to_string()
        .into_bytes(),
        None,
    )
    .await
}

async fn is_owner(
    contract_id: String,
    signer: InMemorySigner,
    owner: String,
) -> Result<FinalExecutionOutcomeView, String> {
    call(
        &signer,
        contract_id.clone(),
        contract_id.clone(),
        "is_owner".to_string(),
        json!({
            "owner": owner,
        })
        .to_string()
        .into_bytes(),
        None,
    )
    .await
}

async fn add_service(
    contract_id: String,
    signer: InMemorySigner,
    service_name: String,
    address: String,
) -> Result<FinalExecutionOutcomeView, String> {
    call(
        &signer,
        contract_id.clone(),
        contract_id.clone(),
        "add_service".to_string(),
        json!({
            "svc": service_name,
            "add":address,
        })
        .to_string()
        .into_bytes(),
        None,
    )
    .await
}

async fn remove_service(
    contract_id: String,
    signer: InMemorySigner,
    service_name: String,
) -> Result<FinalExecutionOutcomeView, String> {
    call(
        &signer,
        contract_id.clone(),
        contract_id.clone(),
        "add_service".to_string(),
        json!({
            "svc": service_name,
        })
        .to_string()
        .into_bytes(),
        None,
    )
    .await
}

async fn get_service(contract_id: String) -> Result<serde_json::Value, String> {
    view(contract_id, "get_services".to_string(), Vec::new().into()).await
}

#[runner::test(sandbox)]
async fn add_owner_sucess() {
    //deploy contract
    let (contract_id, signer) = deploy_contract().await.unwrap();
    let owner = String::from("address");

    let _ = add_owner(contract_id, signer, owner).await.unwrap();

    assert_eq!(
        true,
        serde_json::to_string_pretty(&is_owner(contract_id, signer, owner).await.unwrap()).unwrap()
    );
}

#[runner::test(sandbox)]
async fn add_owner_failure() {

    //add owner fails due to random account calls
}

async fn service_management() {
    //Test Add services

    //add_service- success
    let (contract, signer) = deploy_contract().await.unwrap();

    let _outcome1 = add_service(contract, signer, "Token".to_string(), "address".to_string())
        .await
        .unwrap();

    assert_eq!("TOken", get_service(contract))

    //add service fails

    // add service fails non bmc owner

    // add service fails - invalid service  address

    //Test Remove Services
}
