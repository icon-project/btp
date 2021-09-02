use kitten::*;
use near_sdk_sim::{call, view};

#[path ="../traits.rs"]
mod traits;
use traits::BMC;

pub struct Contract{}

impl BMC for Contract {
    fn add_service(&mut self, svc: String, add: String) -> Result<(), String> {
        Ok(())
    }
    fn remove_service(&mut self, svc: String) -> Result<(), String> {
        Ok(())
    }
    fn add_relay(&mut self, link: String, addrs: Vec<String>) -> Result<(), String> {
        Ok(())
    }
    fn remove_relay(&mut self, link: String, addr: String) -> Result<(), String> {
        Ok(())
    }
    fn get_relays(&self, link: String) -> Vec<String> {
        vec!["relay1Addr".to_string(), "relay2Addr".to_string()]
    }
    fn add_verifier(&mut self, net: String, addr: String) -> Result<(), String> {
        Ok(())
    }
    fn remove_verifier(&mut self, net: String) -> Result<(), String> {
        Ok(())
    }
    fn get_verifiers(&self) -> Vec<traits::Verifier> {
        
       let r =  traits::Verifier{
           addr:"".to_string(),
           net:"".to_string()
       };
       vec![r]
    }
}
//Manage BSH Services
#[test]
fn add_service_sucess(){

    //add service sucessfully
    let mut contract = Contract{
        //contract deployed - initializes and adds account_id to owner 
        //contract owner can add services 
        
        
    };
    let err = contract.add_service("TokenA".to_string(), "bsh1address".to_string()).unwrap_err();
    assert_eq!("",err);
}

#[test]
fn add_service_fails(){
    //add service fails due to service associated with diffrent BSH address.
    let mut contract = Contract{

        //contract deployed - initializes and adds account_id to owner 
        //contract owner can add services 
    };
    let err = contract.add_service("TokenA".to_string(), "bsh2address".to_string()).unwrap_err();
    assert_eq!("BMCRevertAlreadyExistsBSH",err)
}
#[test]
fn add_service_fails_2(){

    //add service fails due to method invoked my non bmc owner

    let mut contract = Contract{

        //contract deployed - initializes and adds account_id to owner 
        //contract owner can add services 
    };
    let err = contract.add_service("TokenA".to_string(), "bsh2address".to_string()).unwrap_err();
    assert_eq!("BMCRevertUnauthorized",err)

}

#[test]
fn add_service_fails_address_invalid(){

    //add service fails due to invalid service address
    let mut contract = Contract{

        //contract deployed - initializes and adds account_id to owner 
        //contract owner can add services 
    };
    let err = contract.add_service("TokenA".to_string(), "0x0000000000000000000000000000000000000000".to_string()).unwrap_err();
    assert_eq!("BMCRevertInvalidAddress",err);

}

#[test]
fn remove_service_fails(){

    //remove service fails due to non contract owner
    let mut contract = Contract{

        //contract deployed - initializes and adds account_id to owner 
        //contract owner can add services 
    };
    let err= contract.remove_service("TokenA".to_string()).unwrap_err();
    assert_eq!("BMCRevertUnauthorized",err);
}
#[test]
fn remove_service_not_exist_fails(){

    //remove service fails due to service not exists
    let mut contract = Contract{
                //contract deployed - initializes and adds account_id to owner 
        //contract owner can add services 
    };

    let err = contract.remove_service("TokenA".to_string()).unwrap_err();

    assert_eq!("BMCRevertNotExistsBSH",err)
}

#[test]
fn remove_service_sucess(){
    //remove service when caller is contract owner

    let mut contract = Contract{
         //contract deployed - initializes and adds account_id to owner 
        //contract owner can add services 
    };

    let err = contract.remove_service("TokenA".to_string()).unwrap_err();

    assert_eq!("",err);

}
//Manage BMR relays
#[test]

fn add_relays_fails() {
    //add realys fails due to non contract owner
    let mut contract = Contract{

         //contract deployed - initializes and adds account_id to owner 
        //contract owner can add services 
    };

    //add relays with random account

    let relays = vec!["relay1Addr".to_string(), "relay2Addr".to_string()];
    let link = String::from("btp://0x04.icon/cxf3d12e9baef523c5a2d03c67b2792f3548926cef");

    let err = contract.add_relay(link, relays).unwrap_err();

    assert_eq!("BMCRevertUnauthorized", err)
}
#[test]
fn add_relays_fail_link() {
    //add relays fails due not existed link

    let mut contract = Contract{

        //contract deployed - initializes and adds account_id to owner 
       //contract owner can add services 
   };

    //add relays with link not registered with bmc

    let relays = vec!["relay1Addr".to_string(), "relay2Addr".to_string()];
    let link = String::from("btp://0x04.icon/cxf3d12e9baef523c5a2d03c67b2792f3548926cef");

    let err = contract.add_relay(link, relays).unwrap_err();

    assert_eq!("BMCRevertNotExistsLink", err)
}

#[test]
fn add_relays_sucess() {
    //add relays sucess

    let mut contract = Contract{

        //contract deployed - initializes and adds account_id to owner 
       //contract owner can add services 
   };
    let relays = vec!["relay1Addr".to_string(), "relay2Addr".to_string()];
    let link = String::from("btp://0x04.icon/cxf3d12e9baef523c5a2d03c67b2792f3548926cef");
    let err = contract
        .add_relay(link.clone(), relays.clone())
        .unwrap_err();

    let relayadded = contract.get_relays(link.clone());

    assert_eq!(relays, relayadded)
}

#[test]
fn overwrite_relays() {
    //overwrite relays with new one -{old :["relays2addr"]  with  new :["relay1addr","relay3addr"]}

    let mut contract = Contract{

        //contract deployed - initializes and adds account_id to owner 
       //contract owner can add services 
   };
    let relays = vec!["relay1Addr".to_string(), "relay3Addr".to_string()];
    let link = String::from("btp://0x04.icon/cxf3d12e9baef523c5a2d03c67b2792f3548926cef");
    let err = contract
        .add_relay(link.clone(), relays.clone())
        .unwrap_err();

    let relayadded = contract.get_relays(link.clone()); //verify the updated relays

    assert_eq!(relays, relayadded)
}

#[test]
fn remove_relays_fails() {
    //remove relays fails due to non owner call
    let mut contract = Contract{

        //contract deployed - initializes and adds account_id to owner 
       //contract owner can add services 
   };
    //assume relayaddress regestred with the below link is ["relay1addr",relay2addr]

    let link = String::from("btp://0x04.icon/cxf3d12e9baef523c5a2d03c67b2792f3548926cef");
    let err = contract
        .remove_relay(link.clone(), "relay2addr".to_string())
        .unwrap_err();

    assert_eq!("BMCRevertUnauthorized", err);
}

#[test]
fn remove_relays_fails_link() {
    //remove relays fails due to non existed link
    let mut contract = Contract{

    //contract deployed - initializes and adds account_id to owner 
   //contract owner can add services 
};

    let link = String::from("btp://0x04.icon/cxf3d12e9baef523c5a2d03c67b2792f3548926cef"); //not a valid link
    let err = contract
        .remove_relay(
            link.clone(),
            "0xb6F2B9415fc599130084b7F20B84738aCBB15930".to_string(),
        )
        .unwrap_err();
    assert_eq!("BMCRevertUnauthorized", err);
}

#[test]
fn remove_relays_fail_invalid_relayaddr() {
    //remove relays fails due non existed relay add
    let mut contract = Contract{

        //contract deployed - initializes and adds account_id to owner 
       //contract owner can add services 
   };
    //assume relayaddress regestred with the below link is ["relay1addr",relay2addr]

    let link = String::from("btp://0x04.icon/cxf3d12e9baef523c5a2d03c67b2792f3548926cef");
    let err = contract
        .remove_relay(link.clone(), "relay3addr".to_string())
        .unwrap_err(); //removing adddress which not exists

    assert_eq!("BMCRevertUnauthorized", err);
}

#[test]
fn remove_relays_sucess() {
    //remove relays sucesss
    let mut contract = Contract{

        //contract deployed - initializes and adds account_id to owner 
       //contract owner can add services 
   };
    //assume relayaddress regestred with the below link is ["relay1addr",relay2addr]

    let link = String::from("btp://0x04.icon/cxf3d12e9baef523c5a2d03c67b2792f3548926cef");
    let _ = contract
        .remove_relay(link.clone(), "relay2addr".to_string())
        .unwrap();

    let relays = contract.get_relays(link.clone());

    assert_eq!(vec!["realay1addr".to_string()], relays);
}
//manage BMV Verifiers

#[test]
fn add_verifier_sucess(){

    //add verifier sucessfully -> by contract owner

 let mut contract = Contract{

        //contract deployed - initializes and adds account_id to owner 
       //contract owner can add services 
   };

}

