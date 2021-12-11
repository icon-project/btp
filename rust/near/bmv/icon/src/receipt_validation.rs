use super::*;
use libraries::types::Math;
use std::convert::TryInto;

impl BtpMessageVerifier {
    pub fn process_receipt_proofs(
        &mut self,
        mut rx_seq: u128,
        source: BTPAddress,
        bmc: BTPAddress,
        receipt_proofs: &Vec<ReceiptProof>,
        last_block_header: &BlockHeader,
        btp_messages: &mut SerializedBtpMessages,
    ) -> Result<(), BmvError> {
        rx_seq.add(1).map_err(|error| BmvError::Unknown {
            message: format!("RxSeq failed: {}", error),
        })?;

        receipt_proofs
            .iter()
            .map(|receipt_proof| -> Result<(), BmvError> {
                let receipt_hash = last_block_header
                    .block_result()
                    .get()
                    .map_err(|message| BmvError::Unknown {
                        message: format!(
                            "Invalid Relay Message, BlockResult: {}",
                            message.to_string()
                        ),
                    })?
                    .receipt_hash()
                    .get()
                    .map_err(|message| BmvError::Unknown {
                        message: format!(
                            "Invalid Relay Message, ReceiptHash: {}",
                            message.to_string()
                        ),
                    })?;

                let receipt = self.prove_receipt_proof(receipt_proof, receipt_hash)?;

                receipt
                    .event_logs()
                    .iter()
                    .filter(|event_log| Self::filter_source_events(event_log, &source))
                    .map(|event_log| -> Result<(), BmvError> {
                        if event_log.is_message() {
                            let message =
                                event_log
                                    .to_message()
                                    .map_err(|error| BmvError::DecodeFailed {
                                        message: error.to_string(),
                                    })?;
                            if message.next() == &bmc {
                                self.ensure_valid_sequence(message.sequence().into(), rx_seq)?;
                                btp_messages.push(
                                    message
                                        .message()
                                        .0
                                        .clone()
                                        .try_into()
                                        .map_err(|error| BmvError::DecodeFailed { message: error })?,
                                );
                                rx_seq.add(1).map_err(|error| BmvError::Unknown {
                                    message: format!("RxSeq failed: {}", error),
                                })?;
                            }
                        }
                        Ok(())
                    })
                    .collect::<Result<(), BmvError>>()?;
                Ok(())
            })
            .collect::<Result<(), BmvError>>()
    }

    fn prove_receipt_proof(
        &self,
        receipt_proof: &ReceiptProof,
        receipt_hash: &Hash,
    ) -> Result<Receipt, BmvError> {
        let proofs =
            receipt_proof
                .proofs()
                .get()
                .map_err(|message| BmvError::InvalidReceiptProof {
                    message: message.to_string(),
                })?;

        let receipt_serialized = <MerklePatriciaTree<Sha256>>::verify_proof(
            receipt_hash,
            &receipt_proof.index_serialized(),
            proofs,
        )
        .map_err(|error| error.to_bmv_error())?;

        let mut receipt: Receipt =
            rlp::decode(&receipt_serialized).map_err(|message| BmvError::InvalidReceipt {
                message: message.to_string(),
            })?;

        receipt_proof
            .event_proofs()
            .iter()
            .map(|event_proof| -> Result<(), BmvError> {
                let event_log_hash = receipt.event_logs_hash().get().map_err(|message| {
                    BmvError::InvalidReceipt {
                        message: message.to_string(),
                    }
                })?;

                let proofs =
                    event_proof
                        .proofs()
                        .get()
                        .map_err(|message| BmvError::InvalidEventProof {
                            message: message.to_string(),
                        })?;

                let event_log_serialized = <MerklePatriciaTree<Sha256>>::verify_proof(
                    event_log_hash,
                    &event_proof.index_serialized(),
                    proofs,
                )
                .map_err(|error| error.to_bmv_error())?;

                let event_log: EventLog =
                    rlp::decode(&event_log_serialized).map_err(|message| {
                        BmvError::InvalidEventLog {
                            message: message.to_string(),
                        }
                    })?;

                receipt.event_logs_mut().push(event_log);

                Ok(())
            })
            .collect::<Result<(), BmvError>>()?;

        Ok(receipt)
    }
}
