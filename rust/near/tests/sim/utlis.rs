near_sdk_sim::lazy_static_include::lazy_static_include_bytes! {
  CONTRACT_EX_WASM => "target/wasm32-unknown-unknown/release/contract_ex.wasm",
}

use near_sdk_sim::{UserAccount,ContractAccount,deploy};

use contract_ex::CounterContract;
use near_sdk_sim::{init_simulator, to_yocto};

const CONTRACT_ID: &str = "contract";



pub fn init() -> (UserAccount, ContractAccount<CounterContract>, UserAccount) {
    // Use `None` for default genesis configuration; more info below
    let root = init_simulator(None);

    let contract = deploy!(
        contract: CounterContract,
       
        contract_id: CONTRACT_ID,
        bytes: &CONTRACT_EX_WASM,
        signer_account: root
    );

    let alice = root.create_user(
        "alice".parse().unwrap(),
        to_yocto("100"), // initial balance
    
    );

    (root, contract, alice)
}
