use contract_ex::CounterContract;
use kitten::*;
use near_sdk_sim::{call, view};

use near_sdk_sim::{ContractAccount, UserAccount};

use crate::utlis::init;

pub struct Initdata {
    root: UserAccount,
    contract: ContractAccount<CounterContract>,
}

fn contractparam() -> Initdata {
    let (root, contract, _) = init();

    Initdata {
        root: root,
        contract: contract,
    }
}

#[test]
fn some_test_cases() {
    Kitten::given(contractparam)
        .when(viewfunc)
        .then(|answer| assert_eq!(0, answer.clone()));
}

#[test]
fn inc_and_dec() {
    Kitten::given(contractparam)
        .when(increment_decrement)
        .then(|answer| assert_eq!(3, answer.clone()));
}

#[test]
fn inc_and_dec_reset() {
    Kitten::given(contractparam)
        .when(increment_decrement_reset)
        .then(|answer| assert_eq!(0, answer.clone()));
}

fn viewfunc(param: Initdata) -> i8 {
    let contract = param.contract;
    let actual: i8 = view!(contract.get_num()).unwrap_json();

    actual
}

fn increment_decrement(param: Initdata) -> i8 {
    let (root, contract) = (param.root, param.contract);
    for _ in 0..=3 {
        call!(root, contract.increment());
    }
    call!(root, contract.decrement());

    let actual: i8 = view!(contract.get_num()).unwrap_json();

    actual
}

fn increment_decrement_reset(param: Initdata) -> i8 {
    let (root, contract) = (param.root, param.contract);
    for _ in 0..=3 {
        call!(root, contract.increment());
    }
    call!(root, contract.decrement());

    call!(root, contract.reset());

    let actual: i8 = view!(contract.get_num()).unwrap_json();

    actual
}
