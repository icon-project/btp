pub struct Verifier {
    pub net: String,
    pub addr: String,
}
pub trait BMC {
    fn add_service(&mut self, svc: String, add: String) -> Result<(), String>;
    fn remove_service(&mut self, svc: String) -> Result<(), String>;
    fn add_relay(&mut self, link: String, addrs: Vec<String>) -> Result<(), String>;
    fn remove_relay(&mut self, link: String, addr: String) -> Result<(), String>;
    fn get_relays(&self, link: String) -> Vec<String>;
    fn add_verifier(&mut self, net: String, addr: String) -> Result<(), String>;
    fn remove_verifier(&mut self, net: String) -> Result<(), String>;
    fn get_verifiers(&self) -> Vec<Verifier> ;
}
