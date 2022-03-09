use super::*;
use btp_common::btp_address::Address;
use hex::encode;
use std::ops::Deref;

impl BtpMessageVerifier {
    // * * * * * * * * * * * * * * * * *
    // * * * * * * * * * * * * * * * * *
    // * * * * * * * Utils * * * * * * *
    // * * * * * * * * * * * * * * * * *
    // * * * * * * * * * * * * * * * * *

    pub fn recover_address(hash: &Hash, signature: &[u8]) -> Vec<u8> {
        let mut hash = Hash::new::<Sha256>(&env::ecrecover(
            hash.deref(),
            &signature[..64],
            signature[64].into(),
            0,
        ))[12..].to_vec();

        let mut address_bytes = [0].to_vec();
        address_bytes.append(&mut hash);
        address_bytes
    }

    // TODO: fix cx
    pub fn filter_source_events(event_log: &EventLog, source: &BTPAddress) -> bool {
        let address = event_log.address().get();
        if address.is_ok() && (encode(address.unwrap()) == source.contract_address().unwrap().replace("cx", "01")) {
            true
        } else {
            false
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use hex::{decode, encode};
    use near_sdk::{serde_json::from_str, testing_env, VMContext};

    fn get_context(is_view: bool) -> VMContext {
        VMContext {
            current_account_id: "alice.testnet".to_string(),
            signer_account_id: "robert.testnet".to_string(),
            signer_account_pk: vec![0, 1, 2],
            predecessor_account_id: "jane.testnet".to_string(),
            input: vec![],
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
    fn recover_address() {
        let context = || get_context(false);
        testing_env!(context());
        let message = Hash::from_hash(
            &decode("c5d6c454e4d7a8e8a654f5ef96e8efe41d21a65b171b298925414aa3dc061e37").unwrap(),
        );
        let signature = decode("4011de30c04302a2352400df3d1459d6d8799580dceb259f45db1d99243a8d0c64f548b7776cb93e37579b830fc3efce41e12e0958cda9f8c5fcad682c61079500").unwrap();
        let address = BtpMessageVerifier::recover_address(&message, &signature);

        assert_eq!(
            encode(address),
            "0057b8365292c115d3b72d948272cc4d788fa91f64".to_string()
        );
    }
}
