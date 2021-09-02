pub struct Verifier {
    pub net: String,
    pub addr: String,
}
pub struct Route{}
pub trait BMC {
    fn add_service(&mut self, svc: String, add: String) -> Result<(), String>;
    fn remove_service(&mut self, svc: String) -> Result<(), String>;
    fn add_relay(&mut self, link: String, addrs: Vec<String>) -> Result<(), String>;
    fn remove_relay(&mut self, link: String, addr: String) -> Result<(), String>;
    fn get_relays(&self, link: String) -> Vec<String>;
    fn add_verifier(&mut self, net: String, addr: String) -> Result<(), String>;
    fn remove_verifier(&mut self, net: String) -> Result<(), String>;
    fn get_verifiers(&self) -> Vec<Verifier>;
    fn add_link(&mut self, link: String) -> Result<(), String>;
    fn remove_link(&mut self, link: String) -> Result<(), String>;
    fn get_links(&self) -> &Vec<String>;
    fn set_link(
        &mut self,
        link: String,
        block_interval: u128,
        max_agg: u128,
        delay_limit: u128,
    ) -> Result<(), String>;

    fn add_route(&mut self, dst:String, link: String) -> Result<(), String>;
    fn remove_route(&mut self, dst: String) -> Result<(), String>;
    fn get_routes(&self) -> Vec<Route>;

}
