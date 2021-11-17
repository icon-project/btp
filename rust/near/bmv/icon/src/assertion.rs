use super::*;

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

    pub fn assert_have_block_updates_or_block_proof(&self, relay_message: &RelayMessage) {
        require!(
            !relay_message.block_updates().is_empty() || relay_message.block_proof().is_some(),
            format!("{}", BmvError::Unknown)
        )
    }

    pub fn assert_have_block_witness(&self, block_proof: &BlockProof) {
        require!(
            block_proof.block_witness().is_some()
                && !block_proof.block_witness().get().witnesses().is_empty(),
            format!("{}", BmvError::Unknown)
        )
    }

    pub fn ensure_have_block_witness(&self, block_proof: &BlockProof) -> Result<(), BmvError> {
        if block_proof.block_witness().is_some()
            && !block_proof.block_witness().get().witnesses().is_empty()
        {
            Ok(())
        } else {
            Err(BmvError::InvalidBlockProof {
                message: "not exists witness",
            })
        }
    }

    pub fn ensure_have_valid_block_height(
        &self,
        next_height: u128,
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
                message: "not exists next validator",
            });
        };

        if &Hash::new(&<Vec<u8>>::from(block_update.next_validators()))
            != block_update.block_header().next_validator_hash()
        {
            return Err(BmvError::InvalidBlockUpdate {
                message: "invalid next validator hash",
            });
        };

        Ok(())
    }

    pub fn ensure_validator_is_valid(&self, address: &Vec<u8>) -> Result<(), BmvError> {
        if !self.validators.contains(&address) {
            return Err(BmvError::InvalidVotes {
                message: "invalid signature",
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
                message: "duplicated vote",
            });
        };

        Ok(())
    }

    pub fn ensure_minimum_votes(&self, addresses: &Vec<Vec<u8>>) -> Result<(), BmvError> {
        if addresses.len() <= (self.validators.len() * 2 / 3) {
            return Err(BmvError::InvalidVotes {
                message: "require votes +2/3",
            });
        };

        Ok(())
    }

    pub fn ensure_block_proof_height_is_valid(&self, block_proof: &BlockProof) -> Result<(), BmvError> {
        if self.mta.height() >= block_proof.block_header().height() {
            Ok(())
        } else {
            Err(BmvError::InvalidBlockProofHeightHigher {
                expected: self.mta.height(),
                actual: block_proof.block_header().height()
            })
        }
    }
}
