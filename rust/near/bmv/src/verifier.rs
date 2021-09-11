use byteorder::{BigEndian, ByteOrder};
use libraries::bmv_types::*;
use merkle_tree_accumulator::{hash::Hash, mta::MerkleTreeAccumulator};
use near_sdk::{
    borsh::{BorshDeserialize, BorshSerialize},
    env,
};
use rlp::{self, encode_list};
use vrf::openssl::{CipherSuite, ECVRF};
use vrf::VRF;

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
        validators: &mut Validators,
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
        votes: &Votes,
        block_height: u128,
        block_hash: &Hash,
        validators: &mut Validators,
        is_next_validators_updated: bool,
    ) -> Result<(), &str> {
        // Calculate RLP of vote item
        // [block height, vote.round, vote_type precommit = 1, block hash, vote.bpsi]
        let mut block_part_set_id: Vec<Vec<u8>> = Vec::with_capacity(2);
        block_part_set_id[0] = u64::to_le_bytes(votes.block_part_set_id.n).to_vec();
        block_part_set_id[1] = votes.block_part_set_id.b.clone();

        let serialized_vote_msg = encode_list(&[
            block_height,
            votes.round as u128,
            1,
            BigEndian::read_u128(&block_hash.0),
            BigEndian::read_u128(&votes.block_part_set_id.b),
        ])
        .to_vec();

        let mut msg_hash: Vec<u8>;
        let mut encoded_vote_msg: Vec<u8>;

        for i in 0..votes.ts.len() {
            encoded_vote_msg = encode_list(&[
                BigEndian::read_u128(&serialized_vote_msg),
                votes.ts[i].timestamp as u128,
            ])
            .to_vec();
            encoded_vote_msg = encode_list(&[
                encoded_vote_msg.len() as u128,
                BigEndian::read_u128(&encoded_vote_msg),
            ])
            .to_vec();

            msg_hash = env::sha256(&encoded_vote_msg);
            let public_key = &votes.ts[i].signature;
            let hash_key = encode_list(&[
                BigEndian::read_u128(&msg_hash),
                BigEndian::read_u128(public_key.as_slice()),
            ])
            .to_vec();
            let address = String::try_from_slice(&hash_key[12..32])
                .expect("Failed to convert slice to string");

            if !validators.contained_validators.contains_key(&address) {
                return Err("BMVRevertInvalidVotes: Invalid signature");
            }

            if validators.check_duplicate_votes.contains_key(&address) {
                let _ = validators
                    .check_duplicate_votes
                    .insert(address.clone(), true);
                return Err("BMVRevertInvalidVotes: Duplicate votes");
            }
        }
        if votes.ts.len() > (validators.validator_addrs.len() * 2) / 3 {
            return Err("BMVRevertInvalidVotes: Require votes > 2/3");
        }

        for addr in &validators.validator_addrs {
            let _ = validators.check_duplicate_votes.remove(addr);
            if is_next_validators_updated {
                let _ = validators.contained_validators.remove(addr);
            }
        }

        Ok(())
    }

    fn verify_mpt_proof(receipt_proof: &ReceiptProof, receipt_hash: &Hash) -> Receipt {
        let receipt_proof_vec = receipt_proof
            .try_to_vec()
            .expect("Failed to convert receipt proof to vec");
        let mut vrf = ECVRF::from_suite(CipherSuite::SECP256K1_SHA256_TAI).unwrap();
        let leaf = vrf.prove(&receipt_hash.0, &receipt_proof_vec).unwrap();

        let mut receipt = Receipt {
            event_logs: vec![],
            event_log_hash: Hash::default(),
        };
        receipt.event_log_hash = Hash::new(&leaf);
        receipt.event_logs = Vec::with_capacity(receipt_proof.event_proofs.len());
        let mut serialized_event_logs: Vec<Vec<u8>> =
            Vec::with_capacity(receipt_proof.event_proofs.len());

        for i in 0..receipt_proof.event_proofs.len() {
            let event_proof_vec = receipt_proof.event_proofs[i].clone().try_to_vec().unwrap();
            serialized_event_logs.push(vrf.prove(&receipt_hash.0, &event_proof_vec).unwrap());
            receipt
                .event_logs
                .push(EventLog::try_from_slice(&serialized_event_logs[i].clone()).unwrap());
        }
        receipt
    }

    fn to_message_event(&mut self) -> MessageEvent {
        todo!()
    }
}
