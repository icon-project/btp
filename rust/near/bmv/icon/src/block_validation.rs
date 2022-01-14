use super::*;

impl BtpMessageVerifier {
    pub fn process_block_updates(
        &mut self,
        block_updates: &Vec<BlockUpdate>,
        last_block_header: &mut BlockHeader,
        state_changes: &mut StateChanges,
    ) -> Result<(), BmvError> {
        let mut validator_hash = Hash::new::<Sha256>(&<Vec<u8>>::from(self.validators.as_ref()));

        block_updates
            .iter()
            .enumerate()
            .map(|(index, block_update)| -> Result<(), BmvError> {
                let next_height = self.mta.height() + 1;
                self.ensure_have_valid_block_height(next_height, &block_update)?;

                let block_hash = Hash::new::<Sha256>(&<Vec<u8>>::from(block_update.block_header()));

                self.verify_votes(
                    block_update.votes(),
                    block_update.block_header().height(),
                    &block_hash,
                )?;

                if &validator_hash != block_update.block_header().next_validator_hash() {
                    self.ensure_have_valid_next_validators(&block_update)?;

                    validator_hash.clone_from(&block_update.block_header().next_validator_hash());
                    state_changes.push(StateChange::SetValidators {
                        validators: block_update.next_validators().get().clone(),
                    });
                }

                state_changes.push(StateChange::MtaAddBlockHash { block_hash });
                if block_updates.len() - index == 1 {
                    last_block_header.clone_from(block_update.block_header());
                }
                Ok(())
            })
            .collect()
    }

    pub fn process_block_proof(
        &mut self,
        block_proof: &BlockProof,
        last_block_header: &mut BlockHeader,
    ) -> Result<(), BmvError> {
        self.ensure_have_block_witness(&block_proof)?;
        self.ensure_block_proof_height_is_valid(&block_proof)?;

        let block_hash = Hash::new::<Sha256>(&<Vec<u8>>::from(block_proof.block_header()));
        self.mta
            .verify::<Sha256>(
                block_proof
                    .block_witness()
                    .get()
                    .map_err(|message| BmvError::InvalidBlockProof { message: message.to_string() })?
                    .witnesses(),
                &block_hash,
                block_proof.block_header().height(),
                block_proof
                    .block_witness()
                    .get()
                    .map_err(|message| BmvError::InvalidBlockProof { message: message.to_string() })?
                    .height(),
            )
            .map_err(|error| error.to_bmv_error())?;
        last_block_header.clone_from(block_proof.block_header());

        Ok(())
    }

    fn verify_votes(
        &self,
        votes: &Votes,
        block_height: u64,
        block_hash: &Hash,
    ) -> Result<(), BmvError> {
        let mut vote_message = VoteMessage::new(
            block_height,
            votes.round(),
            VOTE_TYPE_PRECOMMIT,
            block_hash.clone(),
            votes.part_set_id().clone(),
        );
        let mut addresses: Vec<Vec<u8>> = Vec::new();

        votes
            .iter()
            .map(|vote| -> Result<(), BmvError> {
                vote_message.timestamp_mut().clone_from(&&vote.timestamp());
                let vote_message_hash = Hash::new::<Sha256>(&<Vec<u8>>::from(vote_message.as_ref()));
                let address = Self::recover_address(&vote_message_hash, vote.signature());

                self.ensure_validator_is_valid(&address)?;
                self.ensure_vote_is_unique(&addresses, &address)?;
                addresses.push(address);

                Ok(())
            })
            .collect::<Result<(), BmvError>>()?;

        self.ensure_minimum_votes(&addresses)?;
        Ok(())
    }
}
