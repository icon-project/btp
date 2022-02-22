use super::*;

#[near_bindgen]
impl NativeCoinService {
    // * * * * * * * * * * * * * * * * *
    // * * * * * * * * * * * * * * * * *
    // * * * * Fee Management  * * * * *
    // * * * * * * * * * * * * * * * * *
    // * * * * * * * * * * * * * * * * *

    pub fn accumulated_fees(&self) -> Vec<AccumulatedAssetFees> {
        self.coins
            .to_vec()
            .iter()
            .map(|coin| {
                let coin_id = Self::hash_coin_id(&coin.name);
                let coin_fee = self.coin_fees.get(&coin_id).unwrap();
                let coin = self.coins.get(&coin_id).unwrap();
                AccumulatedAssetFees {
                    name: coin.name().clone(),
                    network: coin.network().clone(),
                    accumulated_fees: *coin_fee,
                }
            })
            .collect()
    }

    pub fn handle_fee_gathering(&mut self, fee_aggregator: BTPAddress, service: String) {
        self.assert_predecessor_is_bmc();
        self.assert_valid_service(&service);
        self.transfer_fees(&fee_aggregator);
    }

    pub fn set_fee_ratio(&mut self, fee_numerator: U128) {
        self.assert_have_permission();
        self.assert_valid_fee_ratio(fee_numerator.into());
        self.fee_numerator.clone_from(&fee_numerator.into());
    }

    pub fn calculate_coin_transfer_fee(&self, amount: U128) -> u128 {
        let fee = (u128::from(amount) * self.fee_numerator) / FEE_DENOMINATOR;
        fee
    }
}

impl NativeCoinService {
    pub fn transfer_fees(&mut self, fee_aggregator: &BTPAddress) {
        let sender_id = env::current_account_id();
        let assets = self
            .coins
            .to_vec()
            .iter()
            .filter_map(|coin| {
                let coin_id = Self::hash_coin_id(&coin.name);
                let coin_fee = self.coin_fees.get(&coin_id).unwrap().clone();

                if coin_fee > 0 {
                    self.coin_fees.set(&coin_id, 0);

                    Some(
                        self.process_external_transfer(&coin_id, &sender_id, coin_fee)
                            .unwrap(),
                    )
                } else {
                    None
                }
            })
            .collect::<Vec<TransferableAsset>>();

        self.send_request(sender_id, fee_aggregator.clone(), assets);
    }
}
