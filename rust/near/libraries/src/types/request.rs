use crate::types::messages::SerializedMessage;
use near_sdk::borsh::{self, BorshDeserialize, BorshSerialize};
use near_sdk::collections::UnorderedMap;

#[derive(BorshDeserialize, BorshSerialize)]
pub struct Requests(UnorderedMap<u128, SerializedMessage>);

impl Requests {
    pub fn new() -> Self {
        Self(UnorderedMap::new(b"requests".to_vec()))
    }

    pub fn add(&mut self, serial_no: u128, message: &SerializedMessage) {
        self.0.insert(&serial_no, message);
    }

    pub fn remove(&mut self, serial_no: u128) {
        self.0.remove(&serial_no);
    }

    pub fn get(&self, serial_no: u128) -> Option<SerializedMessage> {
        if let Some(message) = self.0.get(&serial_no) {
            return Some(message);
        }
        None
    }

    pub fn contains(&self, serial_no: u128) -> bool {
        return self.0.get(&serial_no).is_some();
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::types::{
        messages::{NativeCoinServiceMessage, NativeCoinServiceType},
        Asset,
    };
    use near_sdk::{testing_env, VMContext};

    fn get_context(input: Vec<u8>, is_view: bool) -> VMContext {
        VMContext {
            current_account_id: "alice.testnet".to_string(),
            signer_account_id: "robert.testnet".to_string(),
            signer_account_pk: vec![0, 1, 2],
            predecessor_account_id: "jane.testnet".to_string(),
            input,
            block_index: 0,
            block_timestamp: 0,
            account_balance: 0,
            account_locked_balance: 0,
            storage_usage: 0,
            attached_deposit: 0,
            prepaid_gas: 10u64.pow(18),
            random_seed: vec![0, 1, 2],
            is_view,
            output_data_receivers: vec![],
            epoch_height: 19,
        }
    }

    #[test]
    fn add_request() {
        let context = get_context(vec![], false);
        testing_env!(context);
        let mut requests = Requests::new();
        let native_coin_message =
            NativeCoinServiceMessage::new(NativeCoinServiceType::RequestCoinTransfer {
                source: "88bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4"
                    .to_string(),
                destination: "78bd0675686be0a5df7da33b6f1089eghea3769b19dbb2477fe0cd6e0f12667"
                    .to_string(),
                assets: vec![Asset::new("ABC".to_string(), 100, 1)],
            });
        requests.add(1, &(native_coin_message.clone().into()));
        let result = requests.get(1).unwrap();
        assert_eq!(result, native_coin_message.into());
    }

    #[test]
    fn add_request_existing() {
        let context = get_context(vec![], false);
        testing_env!(context);
        let mut requests = Requests::new();
        let native_coin_message =
            NativeCoinServiceMessage::new(NativeCoinServiceType::RequestCoinTransfer {
                source: "88bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4"
                    .to_string(),
                destination: "78bd0675686be0a5df7da33b6f1089eghea3769b19dbb2477fe0cd6e0f12667"
                    .to_string(),
                assets: vec![Asset::new("ABC".to_string(), 100, 1)],
            });
        requests.add(1, &(native_coin_message.clone().into()));
        requests.add(1, &(native_coin_message.clone().into()));
        let result = requests.get(1).unwrap();
        assert_eq!(result, native_coin_message.into());
    }

    #[test]
    fn remove_request() {
        let context = get_context(vec![], false);
        testing_env!(context);
        let mut requests = Requests::new();
        let native_coin_message =
            NativeCoinServiceMessage::new(NativeCoinServiceType::RequestCoinTransfer {
                source: "88bd05442686be0a5df7da33b6f1089ebfea3769b19dbb2477fe0cd6e0f126e4"
                    .to_string(),
                destination: "78bd0675686be0a5df7da33b6f1089eghea3769b19dbb2477fe0cd6e0f12667"
                    .to_string(),
                assets: vec![Asset::new("ABC".to_string(), 100, 1)],
            });
        requests.add(1, &(native_coin_message.clone().into()));
        requests.remove(1);
        let result = requests.get(1);
        assert_eq!(result, None);
    }

    #[test]
    fn remove_request_non_existing() {
        let context = get_context(vec![], false);
        testing_env!(context);
        let mut requests = Requests::new();
        requests.remove(1);
        let result = requests.get(1);
        assert_eq!(result, None);
    }
}
