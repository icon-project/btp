#[path = "/Hugobyte/BTP/contract-test/src/tests/test_bmc_management.rs"]
mod test_bmc_management;

fn main(){}

// use kitten::*;
// use near_crypto::{InMemorySigner,KeyType};
// use near_primitives::views::FinalExecutionOutcomeView;
// use runner::*;
// use serde_json::{json,Value};
// use std::path::Path;
// use tokio::time::{sleep, Duration};
// use chrono::Utc;
// use rand::Rng;


// #[runner::main(sandbox)]
// async fn main(){
//     sleep(Duration::from_millis(18000)).await;
//     let (contract_id,signer) = dev_deploy(Path::new("/Hugobyte/BTP/contract-test/res/contract_ex.wasm")).await.unwrap();


//     Kitten::given(a_calculator)
//         .when(adding_1_and_2)
//         .then(the_answer_is_3);
    
    
    

//     let outcome1 = call(
//         &signer,
//         contract_id.clone(),
//         contract_id.clone(),
//         "increment".to_string(),
//         Vec::new(),
//         None,
//     )
//     .await
//     .unwrap();
//     println!(
//         "--------------\n{}",
//         serde_json::to_string_pretty(&outcome1).unwrap()
//     );

//     let result = view(contract_id, "get_num".to_string(), Vec::new().into()).await.expect("could not call into vew function");

//   assert_eq!("1",
//         serde_json::to_string_pretty(&result).unwrap()
//     );

    

// }