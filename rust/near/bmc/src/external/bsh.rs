use near_sdk::{ ext_contract };
use crate::BTPAddress;

#[ext_contract(bsh_contract)]
pub trait BshContract {
    fn gather_fees(fee_aggregator: BTPAddress, service: String);
}