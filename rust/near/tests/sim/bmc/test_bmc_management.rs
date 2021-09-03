use kitten::*;
use near_sdk_sim::{call, view};

#[path = "../traits.rs"]
mod traits;
use traits::BMC;

pub struct Contract {}

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
        let r = traits::Verifier {
            addr: "".to_string(),
            net: "".to_string(),
        };
        vec![r]
    }

    fn add_link(&mut self, link: String) -> Result<(), String> {
        Ok(())
    }
    fn remove_link(&mut self, link: String) -> Result<(), String> {
        Ok(())
    }
    fn get_links(&self) -> &Vec<String> {
        
        return vec!["".to_string()];
    }

    fn set_link(
        &mut self,
        link: String,
        block_interval: u128,
        max_agg: u128,
        delay_limit: u128,
    ) -> Result<(), String> {
        Ok(())
    }

    fn add_route(&mut self, dst: String, link: String) -> Result<(), String>{
        Ok(())
    }
    
    fn remove_route(&mut self, dst: String) -> Result<(), String>{

        Ok(())
    }

    fn get_routes(&self) -> Vec<traits::Route> {

        vec![traits::Route{}]

    }

    fn add_owner(&mut self, owner: String) -> Result<(),String>{

        Ok(())
    }

    fn remove_owner(&mut self, owner: String) -> Result<(), String>{
        Ok(())
    }

    fn is_owner(&self, owner: String) -> bool{

        true
    }

    
}
//Manage BSH Services
#[test]
fn add_service_sucess() {
    //add service sucessfully
    let mut contract = Contract{
        //contract deployed - initializes and adds account_id to owner 
        //contract owner can add services
        
    };
    let err = contract
        .add_service("TokenA".to_string(), "bsh1address".to_string())
        .unwrap_err();
    assert_eq!("", err);
}

#[test]
fn add_service_fails() {
    //add service fails due to service associated with diffrent BSH address.
    let mut contract = Contract{
        //contract deployed - initializes and adds account_id to owner 
        //contract owner can add services 
    };
    let err = contract
        .add_service("TokenA".to_string(), "bsh2address".to_string())
        .unwrap_err();
    assert_eq!("BMCRevertAlreadyExistsBSH", err)
}
#[test]
fn add_service_fails_2() {
    //add service fails due to method invoked my non bmc owner

    let mut contract = Contract{
        //contract deployed - initializes and adds account_id to owner 
        //contract owner can add services 
    };
    let err = contract
        .add_service("TokenA".to_string(), "bsh2address".to_string())
        .unwrap_err();
    assert_eq!("BMCRevertUnauthorized", err)
}

#[test]
fn add_service_fails_address_invalid() {
    //add service fails due to invalid service address
    let mut contract = Contract{
        //contract deployed - initializes and adds account_id to owner 
        //contract owner can add services 
    };
    let err = contract
        .add_service(
            "TokenA".to_string(),
            "0x0000000000000000000000000000000000000000".to_string(),
        )
        .unwrap_err();
    assert_eq!("BMCRevertInvalidAddress", err);
}

#[test]
fn remove_service_fails() {
    //remove service fails due to non contract owner
    let mut contract = Contract{
        //contract deployed - initializes and adds account_id to owner 
        //contract owner can add services 
    };
    let err = contract.remove_service("TokenA".to_string()).unwrap_err();
    assert_eq!("BMCRevertUnauthorized", err);
}
#[test]
fn remove_service_not_exist_fails() {
    //remove service fails due to service not exists
    let mut contract = Contract{
                //contract deployed - initializes and adds account_id to owner 
        //contract owner can add services 
    };

    let err = contract.remove_service("TokenA".to_string()).unwrap_err();

    assert_eq!("BMCRevertNotExistsBSH", err)
}

#[test]
fn remove_service_sucess() {
    //remove service when caller is contract owner

    let mut contract = Contract{
         //contract deployed - initializes and adds account_id to owner 
        //contract owner can add services 
    };

    let err = contract.remove_service("TokenA".to_string()).unwrap_err();

    assert_eq!("", err);
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
fn add_verifier_sucess() {
    //add verifier sucessfully -> by contract owner

    let mut contract = Contract{

        //contract deployed - initializes and adds account_id to owner 
       //contract owner can add services 
   };
    let netaddr = String::from("0x03.icon");
    let addr = String::from("addressb1");
    let _ = contract.add_verifier(netaddr, addr).unwrap();

    let verifier = contract.get_verifiers();

    assert_eq!("", "hello")
}

#[test]
fn add_verifer_fails() {
    //Fail to add verifier if caller is not a contract owner

    let mut contract = Contract{
        //contract deployed - initializes and adds account_id to owner 
       
    };

    //calling add verifier as non contract owner
    let netaddr = String::from("0x03.icon");
    let addr = String::from("addressb1");
    let err = contract.add_verifier(netaddr, addr).unwrap_err();

    assert_eq!("BMCRevertUnauthorized", err);
}

#[test]
fn add_verfier_fails_toadd() {
    // Fail to add berifier if verifier is already registered
    let mut contract = Contract{
        //contract deployed - initializes and adds account_id to owner 
       
    };

    //calling add verifier as non contract owner
    let netaddr = String::from("0x03.icon");
    let addr = String::from("addressb1");
    let err = contract.add_verifier(netaddr, addr).unwrap_err();
    assert_eq!("BMCRevertAlreadyExistsBMV", err);
}

#[test]
fn remove_verifer_fails() {
    //Fail to add verifier if caller is not a contract owner

    let mut contract = Contract{
        //contract deployed - initializes and adds account_id to owner 
       
    };

    //calling add verifier as non contract owner
    let netaddr = String::from("0x03.icon");

    let err = contract.remove_verifier(netaddr).unwrap_err();

    assert_eq!("BMCRevertUnauthorized", err);
}

#[test]
fn remove_verifer_fails_notregistered() {
    //Fail to remove verifier if caller if verifier is not regsitered
    let mut contract = Contract{
        //contract deployed - initializes and adds account_id to owner 
       
    };
    let netaddr = String::from("0x04.xyz");

    let err = contract.remove_verifier(netaddr).unwrap_err();

    assert_eq!("BMCRevertNotExistsBMV", err);
}

#[test]
fn remove_verfier_sucess() {
    let mut contract = Contract{
        //contract deployed - initializes and adds account_id to owner 
       
    };
    let netaddr = String::from("0x04.xyz");

    let _ = contract.remove_verifier(netaddr).unwrap();

    let _ = contract.get_verifiers();

    assert_eq!("", "verifer_address");
}

//Manage Links

#[test]
fn add_link_sucess() {

    //Add link sucessfullyy if caller is contract owner

    let mut contract = Contract{
        //contract deployed - initializes and adds account_id to owner 
       
    };
    //BMV1 is registered on BMC with address B1 and binded to “0x03.icon” 
    //Previous BMC BTP address: “btp://0x03.icon/cxf3d12e9baef523c5a2d03c67b2792f3548926cef”

    let _ = contract.add_link(String::from("btp://0x03.icon/cxf3d12e9baef523c5a2d03c67b2792f3548926cef")).unwrap();

    let links = contract.get_links();

    let mut link = String::new();

    for _link in links{

        if _link == "btp://0x03.icon/cxf3d12e9baef523c5a2d03c67b2792f3548926cef"{
            link = String::from("btp://0x03.icon/cxf3d12e9baef523c5a2d03c67b2792f3548926cef");
        }
    }

    assert_eq!("btp://0x03.icon/cxf3d12e9baef523c5a2d03c67b2792f3548926cef",links);


}

#[test]
fn add_link_fails(){
    let mut contract = Contract{
        //contract deployed - initializes and adds account_id to owner 
       
    };
    //BMV1 is registered on BMC with address B1 and binded to “0x03.icon” 
    //Previous BMC BTP address: “btp://0x03.icon/cxf3d12e9baef523c5a2d03c67b2792f3548926cef”

    let err = contract.add_link(String::from("btp://0x03.icon/cxf3d12e9baef523c5a2d03c67b2792f3548926cef")).unwrap_err();

    assert_eq!("BMCRevertUnauthorized",err)
}

#[test]
fn add_link_verifier_fails(){
    let mut contract = Contract{
        //contract deployed - initializes and adds account_id to owner 
       
    };

    //fails when bmv not exists
    let err = contract.add_link(String::from("btp://0x03.icon/cxf3d12e9baef523c5a2d03c67b2792f3548926cef")).unwrap_err();

    assert_eq!("BMCRevertNotExistsBMV",err)

}

#[test]
fn add_link_fails_invbtp(){
    let mut contract = Contract{
        //contract deployed - initializes and adds account_id to owner 
       
    };

    //fails when bmv not exists
    let err = contract.add_link(String::from("btp://0x03.icon/cxf3d12e9baef523c5a2d03c67b2792f3548926cef")).unwrap_err();

    assert_eq!("Invalid Opcode",err)

}

#[test]
fn add_link_fails_ifpresent(){
    let mut contract = Contract{
        //contract deployed - initializes and adds account_id to owner 
       
    };

    //fails when bmv not exists
    let err = contract.add_link(String::from("btp://0x03.icon/cxf3d12e9baef523c5a2d03c67b2792f3548926cef")).unwrap_err();

    assert_eq!("BMCRevertAlreadyExistsLink",err)
}

#[test]
fn remove_link_fails(){

    //remove link fails due to non owner call
    let mut contract = Contract{
        //contract deployed - initializes and adds account_id to owner 
       
    };

    //fails when bmv not exists
    let err = contract.remove_link(String::from("btp://0x03.icon/cxf3d12e9baef523c5a2d03c67b2792f3548926cef")).unwrap_err();

    assert_eq!("BMCRevertUnauthorized",err)

}

#[test]
fn remove_link_fails_dnexist(){
    // Fail to remove link if link does not exist

    let mut contract = Contract{
        //contract deployed - initializes and adds account_id to owner 
       
    };

    //fails when bmv not exists
    let err = contract.remove_link(String::from("btp://0x03.icon/cxf3d12e9baef523c5a2d03c67b2792f3548926cef")).unwrap_err();

    assert_eq!("BMCRevertNotExistsLink",err)

}

#[test]
fn remove_link_success(){
    let mut contract = Contract{
        //contract deployed - initializes and adds account_id to owner 
       
    };

    //fails when bmv not exists
    let _ = contract.remove_link(String::from("btp://0x03.icon/cxf3d12e9baef523c5a2d03c67b2792f3548926cef")).unwrap();

    let links = contract.get_links();
    let value  = false;

    for _link in links{

        if _link == "btp://0x03.icon/cxf3d12e9baef523c5a2d03c67b2792f3548926cef"{
            value= true;
        }
    }

    assert_eq!(true, value)

}


//Configure Link  set  link {link status}

#[test]
fn set_link_fail(){
    //set link status fails due to non contract owner call

    let mut contract = Contract{
       //contract deployed - initializes and adds account_id to owner 
    };
    let link = String::from("btp://0x04.icon/cxf3d12e9baef523c5a2d03c67b2792f3548926cef");
    let block_interval:u128 = 3000;
    let max_agg :u128 = 5;
    let delay_limit:u128 = 3;

    let _ = contract.add_link(link.clone()).unwrap();

    let err = contract.set_link(link.clone(), block_interval, max_agg, delay_limit).unwrap_err();

    assert_eq!("BMCRevertUnauthorized",err);

}
#[test]
fn set_link_fail_notexist(){

    //set link status fails due to non existed link 

    let mut contract = Contract{
        //contract deployed - initializes and adds account_id to owner 
     };
     let link = String::from("btp://0x05.icon/cxf3d12e9baef523c5a2d03c67b2792f3548926cef");
     let block_interval:u128 = 3000;
     let max_agg :u128 = 5;
     let delay_limit:u128 = 3;

     let _ = contract.add_link(String::from("btp://0x04.icon/cxf3d12e9baef523c5a2d03c67b2792f3548926cef")).unwrap();

    let err = contract.set_link(link, block_interval, max_agg, delay_limit).unwrap_err();

    assert_eq!("BMCRevertNotExistsLink",err);

}

#[test]
fn set_link_fail_mxagg(){

    //set link status fails when mx_agg set to 0
    let mut contract = Contract{
        //contract deployed - initializes and adds account_id to owner 
     };
     let link = String::from("btp://0x04.icon/cxf3d12e9baef523c5a2d03c67b2792f3548926cef");
     let block_interval:u128 = 3000;
     let max_agg :u128 = 0;
     let delay_limit:u128 = 3;
 
     let _ = contract.add_link(link.clone()).unwrap();
 
     let err = contract.set_link(link.clone(), block_interval, max_agg, delay_limit).unwrap_err();
 
     assert_eq!("BMCRevertInvalidParam",err);


}

#[test]
fn set_link_fail_invalid_delay(){
    //set link status fails when delay_limit is invalid
    let mut contract = Contract{
        //contract deployed - initializes and adds account_id to owner 
     };
     let link = String::from("btp://0x04.icon/cxf3d12e9baef523c5a2d03c67b2792f3548926cef");
     let block_interval:u128 = 3000;
     let max_agg :u128 = 5;
     let delay_limit:u128 = 0;
 
     let _ = contract.add_link(link.clone()).unwrap();
 
     let err = contract.set_link(link.clone(), block_interval, max_agg, delay_limit).unwrap_err();
 
     assert_eq!("BMCRevertInvalidParam",err);
}

#[test]
fn set_link_success(){


    //set link status success
    let mut contract = Contract{
        //contract deployed - initializes and adds account_id to owner 
     };
     let link = String::from("btp://0x04.icon/cxf3d12e9baef523c5a2d03c67b2792f3548926cef");
     let block_interval:u128 = 3000;
     let max_agg :u128 = 5;
     let delay_limit:u128 = 0;
 
     let _ = contract.add_link(link.clone()).unwrap();
 
     let _ = contract.set_link(link.clone(), block_interval, max_agg, delay_limit).unwrap();

     let linkstatus = contract.get_links(); // need to change

     assert_eq!("",linkstatus);

}

//Manage Routes

#[test]
fn add_route_success(){

    //add route by contract owner

    //Destined BMC: “btp://0x05.pra/0xb6F2B9415fc599130084b7F20B84738aCBB15930” 
    //Linked BMC: “btp://0x03.icon/cxf3d12e9baef523c5a2d03c67b2792f3548926cef”

    let mut contract = Contract{
        //contract deployed - initializes and adds account_id to owner 
     };

    //Add link 

    let _ = contract.add_link(String::from("btp://0x03.icon/cxf3d12e9baef523c5a2d03c67b2792f3548926cef")).unwrap();

   let err =  contract.add_route(String::from("btp://0x05.pra/0xb6F2B9415fc599130084b7F20B84738aCBB15930"),String::from("btp://0x03.icon/cxf3d12e9baef523c5a2d03c67b2792f3548926cef")).unwrap_err();

   assert_eq!("",err);
}

#[test]
fn add_route_fail_nonowner(){

    //add route by non contract owner

    //Destined BMC: “btp://0x05.pra/0xb6F2B9415fc599130084b7F20B84738aCBB15930” 
    //Linked BMC: “btp://0x03.icon/cxf3d12e9baef523c5a2d03c67b2792f3548926cef”

    let mut contract = Contract{
        //contract deployed - initializes and adds account_id to owner 
     };
     //Add link 

    let _ = contract.add_link(String::from("btp://0x03.icon/cxf3d12e9baef523c5a2d03c67b2792f3548926cef")).unwrap();

    let err =  contract.add_route(String::from("btp://0x05.pra/0xb6F2B9415fc599130084b7F20B84738aCBB15930"),String::from("btp://0x03.icon/cxf3d12e9baef523c5a2d03c67b2792f3548926cef")).unwrap_err();
 
    assert_eq!("BMCRevertUnauthorized",err);

}

#[test]

fn add_route_fail_alreadyexist(){

    //add route fail already exists

    //Destined BMC: “btp://0x05.pra/0xb6F2B9415fc599130084b7F20B84738aCBB15930” 
    //Linked BMC: “btp://0x03.icon/cxf3d12e9baef523c5a2d03c67b2792f3548926cef”

    let mut contract = Contract{
        //contract deployed - initializes and adds account_id to owner 
     };
     //Add link 

    let _ = contract.add_link(String::from("btp://0x03.icon/cxf3d12e9baef523c5a2d03c67b2792f3548926cef")).unwrap();

    let err =  contract.add_route(String::from("btp://0x05.pra/0xb6F2B9415fc599130084b7F20B84738aCBB15930"),String::from("btp://0x03.icon/cxf3d12e9baef523c5a2d03c67b2792f3548926cef")).unwrap_err();
 
    assert_eq!("BTPRevertAlreadyExistRoute",err);
}

#[test]
fn add_route_fails_invalidlink(){

    //add route fail already exists

    //Destined BMC: “btp://0x05.pra/0xb6F2B9415fc599130084b7F20B84738aCBB15930” 
    //Linked BMC: “btp://0x03.icon/cxf3d12e9baef523c5a2d03c67b2792f3548926cef”

    let mut contract = Contract{
        //contract deployed - initializes and adds account_id to owner 
     };
     //Add link 

    let _ = contract.add_link(String::from("btp://0x03.icon/cxf3d12e9baef523c5a2d03c67b2792f3548926cef")).unwrap();

    let err =  contract.add_route(String::from("btp://0x050xb6F2B9415fc599130084b7F20B84738aCBB15930"),String::from("btp://0x03.cxf3d12e9baef523c5a2d03c67b2792f3548926cef")).unwrap_err();
 
    assert_eq!("nvalid opcode",err);


}

#[test]

fn remove_route_fails(){

    //remove fails - non contract owner call 

   //Linked BMC: “btp://0x03.icon/cxf3d12e9baef523c5a2d03c67b2792f3548926cef”

   let mut contract = Contract{
    //contract deployed - initializes and adds account_id to owner 
 };

//Add link 

let _ = contract.add_link(String::from("btp://0x03.icon/cxf3d12e9baef523c5a2d03c67b2792f3548926cef")).unwrap();

let _ =  contract.add_route(String::from("btp://0x05.pra/0xb6F2B9415fc599130084b7F20B84738aCBB15930"),String::from("btp://0x03.icon/cxf3d12e9baef523c5a2d03c67b2792f3548926cef")).unwrap_err();

let err = contract.remove_route(String::from("btp://0x05.pra/0xb6F2B9415fc599130084b7F20B84738aCBB15930")).unwrap_err();

assert_eq!("BMCRevertUnauthorized",err);
}

#[test]
fn remove_route_notexists(){

 //Linked BMC: “btp://0x03.icon/cxf3d12e9baef523c5a2d03c67b2792f3548926cef”

 let mut contract = Contract{
    //contract deployed - initializes and adds account_id to owner 
 };

//Add link 

let _ = contract.add_link(String::from("btp://0x03.icon/cxf3d12e9baef523c5a2d03c67b2792f3548926cef")).unwrap();

let _ =  contract.add_route(String::from("btp://0x05.pra/0xb6F2B9415fc599130084b7F20B84738aCBB15930"),String::from("btp://0x03.icon/cxf3d12e9baef523c5a2d03c67b2792f3548926cef")).unwrap_err();

let err = contract.remove_route(String::from("btp://0x05.pra/0xb6F2B9415fc599130084b7F20B84738aCBB15930")).unwrap_err();

assert_eq!("BTPRevertNotExistRoute",err);    
}

#[test]
fn remove_route_sucess(){


 //Linked BMC: “btp://0x03.icon/cxf3d12e9baef523c5a2d03c67b2792f3548926cef”

 let mut contract = Contract{
    //contract deployed - initializes and adds account_id to owner 
 };

//Add link 

let _ = contract.add_link(String::from("btp://0x03.icon/cxf3d12e9baef523c5a2d03c67b2792f3548926cef")).unwrap();

let _ =  contract.add_route(String::from("btp://0x05.pra/0xb6F2B9415fc599130084b7F20B84738aCBB15930"),String::from("btp://0x03.icon/cxf3d12e9baef523c5a2d03c67b2792f3548926cef")).unwrap_err();

let err = contract.remove_route(String::from("btp://0x05.pra/0xb6F2B9415fc599130084b7F20B84738aCBB15930")).unwrap_err();

assert_eq!("_",err);  
}

//Manage Owner Accounts
#[test]
fn add_owner_success(){

    // add owner - by contract owner

    let addr = "0xb6F2B9415fc599130084b7F20B84738aCBB15930".to_string() ;

    let mut contract = Contract{

    };

    let _ = contract.add_owner(addr).unwrap();

    let owner = contract.is_owner(addr);

    assert_eq!(true,owner);
    
}

#[test]
fn add_owner_fails(){

    // add owner by non contract owner 
    let addr = "0xb6F2B9415fc599130084b7F20B84738aCBB15930".to_string() ;

    let mut contract = Contract{

    };
    let err = contract.add_owner(addr).unwrap_err();

    assert_eq!("BMCRevertUnauthorized",err);

}
#[test]
fn remove_owner_fails(){

    // remove owner fails -> { only one owner present}
    let mut contract = Contract{

    };

    let err = contract.remove_owner("0xb6F2B9415fc599130084b7F20B84738aCBB15930".to_string()).unwrap_err();

    assert_eq!("BMCRevertLastOwner", err);
    
}

#[test]
fn remove_onwer_fails_nonowner(){

    let addr = "0xb6F2B9415fc599130084b7F20B84738aCBB15930".to_string() ;

    let mut contract = Contract{

    };

    let _ = contract.add_owner(addr).unwrap();

    let err = contract.remove_owner(addr).unwrap_err();
    assert_eq!("BMCRevertUnauthorized", err);

}

#[test]
fn remove_owner_success(){

    let addr = "0xb6F2B9415fc599130084b7F20B84738aCBB15930".to_string() ;

    let mut contract = Contract{

    };

    let _ = contract.add_owner(addr).unwrap();

    let _ = contract.remove_owner(addr).unwrap();

    let is_owner = contract.is_owner(addr);

    assert_eq!(true,is_owner)
    


}