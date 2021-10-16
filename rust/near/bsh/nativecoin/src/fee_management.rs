use super::*;

#[near_bindgen]
impl NativeCoinService {
    // * * * * * * * * * * * * * * * * *
    // * * * * * * * * * * * * * * * * *
    // * * * * Fee Management  * * * * *
    // * * * * * * * * * * * * * * * * *
    // * * * * * * * * * * * * * * * * *

    pub fn accumulated_fees(&self) -> Vec<AccumulatedAssetFees> {
        self.tokens
            .to_vec()
            .iter()
            .map(|token| {
                let token_id = Self::hash_token_id(&token.name);
                let token_fee = self.token_fees.get(&token_id).unwrap();
                let token = self.tokens.get(&token_id).unwrap();
                AccumulatedAssetFees {
                    name: token.name().clone(),
                    network: token.network().clone(),
                    accumulated_fees: *token_fee,
                }
            })
            .collect()
    }
}

impl NativeCoinService {
    pub fn transfer_fees(&mut self, fee_aggregator: &BTPAddress) {
        let sender_id = env::current_account_id();
        let assets = self
            .tokens
            .to_vec()
            .iter()
            .filter_map(|token| {
                let token_id = Self::hash_token_id(&token.name);
                if let Some(token_fee) = self.token_fees.clone().get(&token_id) {
                    self.token_fees.set(&token_id, 0);
                    
                    Some(
                        self.process_external_transfer(&token_id, &sender_id, *token_fee)
                            .unwrap(),
                    )
                } else {
                    None
                }
            })
            .collect::<Vec<Asset>>();

        self.send_request(sender_id, fee_aggregator.clone(), assets);
    }
}
