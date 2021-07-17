use lazy_static::lazy_static;
use near_sdk::borsh::{self, BorshDeserialize, BorshSerialize};
use std::collections::HashMap;
use std::sync::Mutex;

lazy_static! {
    static ref PENDINGREQUESTS: Mutex<Vec<String>> = Mutex::new(Vec::new());
    static ref BSHNAMESLIST: Mutex<Vec<String>> = Mutex::new(Vec::new());
}

pub fn getPendingRequest() -> Vec<String> {
    return PENDINGREQUESTS.lock().unwrap().to_vec();
}

pub fn updatePendingRequest(service: String) {
    PENDINGREQUESTS.lock().unwrap().push(service);
}

pub fn removePendingRequest(service: String) {
    if let Some(pos) = PENDINGREQUESTS
        .lock()
        .unwrap()
        .iter()
        .position(|x| x == &service)
    {
        PENDINGREQUESTS.lock().unwrap().remove(pos);
    }
}

#[derive(BorshDeserialize, BorshSerialize)]
pub struct BSHServices(HashMap<String, String>);

impl BSHServices {
    pub fn new() -> Self {
        let bshservices = HashMap::new();

        Self(bshservices)
    }
    pub fn contains(&self, key: String) -> bool {
        return self.0.contains_key(&key);
    }
    pub fn add(&mut self, service: String, pendingrq: String) -> Result<bool, String> {
        if !self.0.contains_key(&service) {
            self.0.insert(service.clone(), pendingrq);
            BSHNAMESLIST.lock().unwrap().push(service.clone());

            return Ok(true);
        }

        return Err("BMCRevertAlreadyExistBSH".to_string());
    }
    pub fn remove(&mut self, service: String) -> Result<bool, String> {
        self.0.remove(&service);
        if let Some(pos) = BSHNAMESLIST
            .lock()
            .unwrap()
            .iter()
            .position(|x| x == &service)
        {
            BSHNAMESLIST.lock().unwrap().remove(pos);
        }

        return Ok(true);
    }

    pub fn get(&self) -> Result<HashMap<String, String>, String> {
        if !self.0.is_empty() {
            return Ok(self.0.clone());
        }

        return Err("Services Empty".to_string());
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use near_sdk::MockedBlockchain;
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
    fn updaterelay() {
        let context = get_context(vec![], false);
        testing_env!(context);

        updatePendingRequest("helloworld".to_string());

        assert_eq!(getPendingRequest(), vec!["helloworld".to_string()]);
    }

    #[test]
    fn getrelay() {
        let context = get_context(vec![], false);
        testing_env!(context);

        assert_eq!(getPendingRequest(), vec!["helloworld".to_string()]);
    }
}
