use btp_common::BTPAddress;
use near_sdk::borsh::{self, BorshDeserialize, BorshSerialize};
use near_sdk::collections::{UnorderedMap, UnorderedSet};


#[derive(BorshDeserialize, BorshSerialize)] 
pub struct Relays(pub Vec<String>);

impl Relays {
    pub fn new(link: &BTPAddress) -> Self {
        link.to_string().push_str("_relay");
        let relays = Vec::new();
        // let relays: UnorderedSet<String> = UnorderedSet::new(link.to_string().into_bytes());
        Self(relays)
    }
}

// pub trait Relay {
//     // fn add_relay(&mut self,address: String) -> Result<bool, String>;
//     // fn remove_relay(&mut self,link:&BTPAddress , address : String) -> Result<bool,String>;
//     // fn get_relays(&self,link:&BTPAddress)-> Result<Relays,String>;
// }

impl Relays {
   pub fn add_relay(&mut self, address: String) -> Result<bool, String> {
        // if !self.0.contains(&address) { 
        //     self.0.insert(&address);

           

        //     return Ok(true);
        // }
        // return Err("relay not added".to_string());


        if !self.0.contains(&address){

            self.0.push(address);
            return Ok(true);
        }

        return  Err("Failed to add".to_string()) ;
    }

    pub fn reset(&mut self){

        self.0.clear()
    }

    pub fn remove_relay(&mut self,address:String)->Result<bool,String>{

        if !self.0.contains(&address){

            
            return Ok(false);
        }
        let index = self.0.iter().position(|x| *x == address).unwrap();
        self.0.remove(index);
        return  Ok(true);


    }

    pub fn get_relays(&self) ->Result<Vec<String>,String>{

        if !self.0.is_empty(){
            
            let result = self.0.to_vec();

            return  Ok(result);
            println!("{:?} nothing",self.0.to_vec());
        }

        

           
        return Err("Not found".to_string());   
    }
}

#[cfg(test)]
mod tests {
    
   use super::*;

    #[test]

    fn add_relay(){
        let relay = Relays::new();
    }

}
