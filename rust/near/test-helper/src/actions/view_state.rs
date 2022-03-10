use crate::types::{Bmc, Context, Contract};
use base64::{encode_config,encode,URL_SAFE};
use duplicate::duplicate;
use near_primitives::types::StoreKey;
use tokio::runtime::Handle;

impl Contract<'_, Bmc> {
    pub fn view_state(&self, context: Context, key: String) -> Vec<u8> {
        let handle = Handle::current();
        let state_key = key.clone();
        let state = tokio::task::block_in_place(move || {
            handle.block_on(async {
                context
                    .worker()
                    .view_state(
                        context.contracts().get(self.name()).id(),
                        Some(StoreKey::from(encode_config(state_key,URL_SAFE).as_bytes().to_vec())),
                    )
                    .await
                    .unwrap()
            })
        });
        state.get(&key).unwrap().to_owned()
    }
}
