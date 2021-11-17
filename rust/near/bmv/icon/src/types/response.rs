use near_sdk::serde::{Deserialize, Serialize};

#[derive(Deserialize, Serialize)]
#[serde(crate = "near_sdk::serde")]
pub enum Response {
    Success,
    Failed,
}
