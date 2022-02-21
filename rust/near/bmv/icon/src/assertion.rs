use super::*;
use btp_common::btp_address::Address;
use libraries::types::Account;

impl BtpMessageVerifier {
    // * * * * * * * * * * * * * * * * *
    // * * * * * * * * * * * * * * * * *
    // * * * * Internal Validations  * *
    // * * * * * * * * * * * * * * * * *
    // * * * * * * * * * * * * * * * * *

    pub fn assert_predecessor_is_bmc(&self) {
        require!(
            env::predecessor_account_id() == *self.bmc(),
            format!("{}", BmvError::NotBmc)
        )
    }

    pub fn assert_bmc_is_valid(&self, bmc: &BTPAddress) {
        require!(
            bmc.account_id() == *self.bmc(),
            format!("{}", BmvError::NotBmc)
        )
    }

    pub fn assert_source_is_valid(&self, source: &BTPAddress) {
        require!(
            self.network == source.network_address().unwrap(),
            format!(
                "{}",
                BmvError::Unknown {
                    message: format!("not acceptable from {}", source)
                }
            )
        )
    }

    pub fn assert_have_block_updates_or_block_proof(&self, relay_message: &RelayMessage) {
        require!(
            !relay_message.block_updates().is_empty() || relay_message.block_proof().is_some(),
            format!(
                "{}",
                BmvError::Unknown {
                    message: "does not have block updates or block proof".to_string()
                }
            )
        )
    }

    pub fn ensure_have_block_witness(&self, block_proof: &BlockProof) -> Result<(), BmvError> {
        if block_proof.block_witness().is_some()
            && !block_proof
                .block_witness()
                .get()
                .map_err(|message| BmvError::InvalidBlockProof {
                    message: message.to_string(),
                })?
                .witnesses()
                .is_empty()
        {
            Ok(())
        } else {
            Err(BmvError::InvalidBlockProof {
                message: "not exists witness".to_string(),
            })
        }
    }

    pub fn ensure_have_valid_block_height(
        &self,
        next_height: u64,
        block_update: &BlockUpdate,
    ) -> Result<(), BmvError> {
        if next_height > block_update.block_header().height() {
            return Err(BmvError::InvalidBlockUpdateHeightLower {
                expected: next_height,
                actual: block_update.block_header().height(),
            });
        };

        if next_height < block_update.block_header().height() {
            return Err(BmvError::InvalidBlockUpdateHeightHigher {
                expected: next_height,
                actual: block_update.block_header().height(),
            });
        };

        Ok(())
    }

    pub fn ensure_have_valid_next_validators(
        &self,
        block_update: &BlockUpdate,
    ) -> Result<(), BmvError> {
        if block_update.next_validators().is_empty() {
            return Err(BmvError::InvalidBlockUpdate {
                message: "not exists next validator".to_string(),
            });
        };

        if &Hash::new::<Sha256>(&<Vec<u8>>::from(block_update.next_validators()))
            != block_update
                .block_header()
                .next_validator_hash()
                .get()
                .map_err(|message| BmvError::InvalidBlockUpdate {
                    message: message.to_string(),
                })?
        {
            return Err(BmvError::InvalidBlockUpdate {
                message: "invalid next validator hash".to_string(),
            });
        };

        Ok(())
    }

    pub fn ensure_validator_is_valid(&self, address: &Vec<u8>) -> Result<(), BmvError> {
        if !self.validators.contains(&address) {
            return Err(BmvError::InvalidVotes {
                message: "invalid signature".to_string(),
            });
        };

        Ok(())
    }

    pub fn ensure_vote_is_unique(
        &self,
        addresses: &Vec<Vec<u8>>,
        address: &Vec<u8>,
    ) -> Result<(), BmvError> {
        if addresses.contains(&address) {
            return Err(BmvError::InvalidVotes {
                message: "duplicated vote".to_string(),
            });
        };

        Ok(())
    }

    pub fn ensure_minimum_votes(&self, addresses: &Vec<Vec<u8>>) -> Result<(), BmvError> {
        if addresses.len() <= (self.validators.len() * 2 / 3) {
            return Err(BmvError::InvalidVotes {
                message: "require votes +2/3".to_string(),
            });
        };

        Ok(())
    }

    pub fn ensure_block_proof_height_is_valid(
        &self,
        block_proof: &BlockProof,
    ) -> Result<(), BmvError> {
        if self.mta.height() >= block_proof.block_header().height() {
            Ok(())
        } else {
            Err(BmvError::InvalidBlockProofHeightHigher {
                expected: self.mta.height(),
                actual: block_proof.block_header().height(),
            })
        }
    }

    pub fn ensure_valid_sequence(&self, actual: u128, expected: u128) -> Result<(), BmvError> {
        if actual == expected {
            Ok(())
        } else if actual > expected {
            Err(BmvError::InvalidSequenceHigher { actual, expected })
        } else {
            Err(BmvError::InvalidSequence { actual, expected })
        }
    }
}