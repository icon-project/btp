use futures::executor::LocalPool;
use near_crypto::InMemorySigner;
use runner;
use serde_json::Value;

pub fn call(
    signer: &InMemorySigner,
    account_id: &str,
    contract_id: &str,
    method: &str,
    value: &Value,
) {
    let mut pool = LocalPool::new();
    pool.run_until(async {
        runner::call(
            signer,
            account_id.to_owned(),
            contract_id.to_owned(),
            method.to_owned(),
            value.as_str().unwrap().into(),
            None,
        )
        .await
        .unwrap()
    });
}

pub fn view() {

}


mod invoke_macros {
    #[macro_export]
    macro_rules! invoke_call {
        ($self: ident, $context: ident, $method: tt, $param: ident) => {
            use crate::actions::call;
            call(
                $context.signer().signer(),
                $context.signer().account_id(),
                $context.contracts.get($self.name()).account_id(),
                $method,
                $context.$param($method),
            );
        };
    }
}
