// use byteorder::{BigEndian, ByteOrder};
use libraries::bmv_types::*;
use merkle_tree_accumulator::{hash::Hash, mta::MerkleTreeAccumulator};
// use near_sdk::{bs58::encode, env};
// use rlp::{self, encode_list};

pub trait Verifier {
    fn verify_mta_proof(
        &mut self,
        block_proof: BlockProof,
        mut mta: MerkleTreeAccumulator,
    ) -> Result<(), &str> {
        if block_proof.block_witness.witnesses.is_empty() {
            return Err("BMVRevertInvalidBlockWitness");
        }
        if mta.height < block_proof.block_header.height {
            return Err("BMVRevertInvalidBlockProofHigher");
        }
        mta.verify(
            block_proof.block_witness.witnesses.as_slice(),
            &block_proof.block_header.block_hash,
            block_proof.block_header.height,
            block_proof.block_witness.height,
        )
        .expect("Failed to verify MTA proof");
        Ok(())
    }

    fn verify_validators(
        &mut self,
        block_update: &BlockUpdate,
        validators: &Validators,
    ) -> Result<bool, &str> {
        if block_update.votes.ts.is_empty() {
            return Err("BMVRevertInvalidBlockUpdate: Not exists votes");
        }

        self.verify_votes(
            &block_update.votes,
            block_update.block_header.height,
            &block_update.block_header.block_hash,
            validators,
            block_update.block_header.next_validator_hash != validators.validator_hash,
        )
        .expect("Failed to verify votes");

        if block_update.block_header.next_validator_hash != validators.validator_hash
            && block_update.next_validators.is_empty()
        {
            return Err("BMVRevertInvalidBlockUpdate: Not exists next validators");
        } else if block_update.next_validators_hash == block_update.block_header.next_validator_hash
        {
            return Ok(true);
        } else {
            return Err("BMVRevertInvalidBlockUpdate: Invalid next validator hash");
        }
    }

    #[allow(clippy::too_many_arguments)]
    fn verify_votes(
        &mut self,
        _votes: &Votes,
        _block_height: u128,
        _block_hash: &Hash,
        _validators: &Validators,
        _is_next_validators_updated: bool,
    ) -> Result<(), &str> {
        // // Calculate RLP of vote item
        // // [block height, vote.round, vote_type precommit = 1, block hash, vote.bpsi]
        // let mut serialized_vote_msg: Vec<u8>;
        // let mut block_part_set_id: Vec<Vec<u8>> = Vec::with_capacity(2);
        // block_part_set_id[0] = u64::to_le_bytes(votes.block_part_set_id.n).to_vec();
        // block_part_set_id[1] = votes.block_part_set_id.b.clone();

        // serialized_vote_msg = encode_list(&[
        //     block_height,
        //     votes.round as u128,
        //     1,
        //     BigEndian::read_u128(&block_hash.0),
        //     BigEndian::read_u128(&votes.block_part_set_id.b),
        // ])
        // .to_vec();

        // let mut msg_hash: &Hash;
        // let mut encoded_vote_msg: Vec<u8>;

        // for i in 0..votes.ts.len() {
        //     encoded_vote_msg = encode_list(&[
        //         BigEndian::read_u64(&serialized_vote_msg),
        //         votes.ts[i].timestamp,
        //     ])
        //     .to_vec();
        // }

        // Ok(())
        todo!()
    }

    fn verify_mpt_proof(&mut self, _receipt_hash: &Hash) -> Receipt {
        todo!()
    }

    fn to_message_event(&mut self) -> MessageEvent {
        todo!()
    }
}
