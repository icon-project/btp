use near_sdk::borsh::{self, BorshDeserialize, BorshSerialize};
use near_sdk::collections::UnorderedSet;
use std::collections::HashMap;

#[derive(BorshDeserialize, BorshSerialize)]
pub struct BSH {
    pub services: Services,
    pub requests: Requests,
}

impl BSH {
    pub fn new() -> Self {
        Self {
            services: Services::new(),
            requests: Requests::new(),
        }
    }
}

#[derive(BorshDeserialize, BorshSerialize)]
pub struct Services(HashMap<String, String>);

#[derive(BorshDeserialize, BorshSerialize)]
pub struct Requests(HashMap<String, String>);

impl Requests {
    pub fn new() -> Self {
        let requests = HashMap::new();
        Self(requests)
    }
    pub fn add(&mut self, name: String, address: String) -> Result<bool, String> {
        if !self.0.contains_key(&name) {
            self.0.insert(name.clone(), address);
            return Ok(true);
        }
        return Err("BMCRevertAlreadyexist".to_string());
    }
    pub fn remove(&mut self, name: String) -> Result<bool, String> {
        if self.0.contains_key(&name) {
            self.0.remove(&name);
            return Ok(true);
        }
        return Err("BMCRevertnotExist".to_string());
    }

    pub fn get(&self, name: String) -> Result<String, String> {
        if let Some(service) = self.0.get(&name) {
            return Ok(service.to_string());
        }
        return Err("BMCRevertnotExist".to_string());
    }

    pub fn to_vec(&self) -> Result<Vec<(String, String)>, String> {
        if !self.0.is_empty() {
            return Ok(self.0.clone().into_iter().collect());
        }

        return Err("No pending request".to_string());
    }
}

impl Services {
    pub fn new() -> Self {
        let services = HashMap::new();
        Self(services)
    }

    pub fn add(&mut self, name: String, address: String) -> Result<bool, String> {
        if !self.0.contains_key(&name) {
            self.0.insert(name.clone(), address);
            return Ok(true);
        }
        return Err("BMCRevertAlreadyExistBSH".to_string());
    }

    pub fn remove(&mut self, name: String) -> Result<bool, String> {
        if self.0.contains_key(&name) {
            self.0.remove(&name);
            return Ok(true);
        }
        return Err("BMCRevertnotExist".to_string());
    }

    pub fn get(&self, name: String) -> Result<String, String> {
        if let Some(service) = self.0.get(&name) {
            return Ok(service.to_string());
        }
        return Err("BMCRevertnotExist".to_string());
    }

    pub fn to_vec(&self) -> Vec<(String, String)> {
        if !self.0.is_empty() {
            return self.0.clone().into_iter().collect();
        }
        vec![]
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
    fn add_pendingrequest_success() {
        let context = get_context(vec![], false);
        testing_env!(context);
        let name = String::from("addservice");
        let address = String::from("addressservice");
        let mut bsh = BSH::new();

        let result = bsh.requests.add(name.to_string(), address.to_string()).unwrap();

        assert_eq!(result, true);
    }

    #[test]
    fn add_pendingrequest_fail() {
        let context = get_context(vec![], false);
        testing_env!(context);
        let name = String::from("addservice");
        let address = String::from("addressservice");
        let mut bsh = BSH::new();

        bsh.requests.add(name.to_string(), address.to_string()).unwrap();
        let result2 = bsh.requests.add(name.to_string(), address.to_string()).unwrap_err();

        assert_eq!(result2, "BMCRevertAlreadyexist");
    }

    #[test]
    fn remove_pendingrequest_success() {
        let context = get_context(vec![], false);
        testing_env!(context);
        let name = String::from("addservice");
        let address = String::from("addressservice");
        let mut bsh = BSH::new();

        bsh.requests.add(name.to_string(), address.to_string()).unwrap();
        let result = bsh.requests.remove(name.to_string()).unwrap();

        assert_eq!(result, true);
    }
    #[test]
    fn remove_pendingrequest_fail() {
        let context = get_context(vec![], false);
        testing_env!(context);
        let name = String::from("addservice");
        let address = String::from("addressservice");
        let mut bsh = BSH::new();

        bsh.requests.add(name.to_string(), address.to_string()).unwrap();
        bsh.requests.remove(name.to_string()).unwrap();
        let result = bsh.requests.remove(name.to_string()).unwrap_err();

        assert_eq!(result, "BMCRevertnotExist");
    }

    #[test]
    fn get_pendingrequest_success() {
        let context = get_context(vec![], false);
        testing_env!(context);
        let name = String::from("addservice");
        let address = String::from("addressservice");
        let name1 = String::from("addservice1");
        let address1 = String::from("addressservice1");
        let mut bsh = BSH::new();

        bsh.requests.add(name.to_string(), address.to_string()).unwrap();
        bsh.requests.add(name1.to_string(), address1.to_string()).unwrap();
        
        let result = bsh.requests.get(name.to_string()).unwrap();

        assert_eq!(result, address.to_string());
    }
   
    // #[test]
    // fn get_pendingrequests_success() {
    //     let context = get_context(vec![], false);
    //     testing_env!(context);
    //     let name = String::from("addservice");
    //     let address = String::from("addressservice");
    //     let name1 = String::from("addservice1");
    //     let address1 = String::from("addressservice1");
    //     let mut bsh = BSH::new();

    //     bsh.requests.add(name.to_string(), address.to_string()).unwrap();
    //     bsh.requests.add(name1.to_string(), address1.to_string()).unwrap();

    //     let vect = vec![(name1.to_string(), address1.to_string()),(name.to_string(), address.to_string())];
        
    //     let result = bsh.requests.to_vec().unwrap();

    //     assert_eq!(result, vect);
    // }
    
}
