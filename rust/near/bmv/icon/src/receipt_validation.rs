use super::*;
use libraries::{mpt::Prove, rlp::Encodable, types::Math};
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

        receipt_proofs
            .iter()
            .map(|receipt_proof| -> Result<(), BmvError> {
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
                                    message.message().0.clone().try_into().map_err(|error| {
                                        BmvError::DecodeFailed { message: error }
                                    })?,
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
        let mut receipt: Receipt = receipt_proof
            .prove(receipt_hash)
            .map_err(|error| error.to_bmv_error())?;

        receipt_proof
            .event_proofs()
            .iter()
            .map(|event_proof| -> Result<(), BmvError> {
                let event_log_hash = receipt.event_logs_hash().get().map_err(|message| {
                    BmvError::InvalidReceipt {
                        message: message.to_string(),
                    }
                })?;

                let event_log: EventLog = event_proof
                    .prove(event_log_hash)
                    .map_err(|error| error.to_bmv_error())?;

                receipt.event_logs_mut().push(event_log);

                Ok(())
            })
            .collect::<Result<(), BmvError>>()?;

        Ok(receipt)
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use libraries::types::messages::BtpMessage;
    use libraries::types::messages::SerializedMessage;
    use libraries::types::Message;
    use libraries::types::WrappedI128;
    use near_sdk::AccountId;
    use near_sdk::{testing_env, VMContext};
    use std::convert::TryFrom;

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
    fn test_prove_receipt_proof() {
        let context = || get_context(false);
        testing_env!(context());

        let relay_message = RelayMessage::try_from("-Qee-QQ8uQFS-QFPuNT40gKCQKOHBdCqJx0eQ5UAtrV5G-C172cGOzwQuED7gVFNsv2g1tINkHhbtViQXPruQ-FnkmYs_jA0tYrJKbvq3BYO9Rqgt12iGPeeEGlPxvT_hNLnhr9K5nUqvKiB6A_Yc0bfl76g7Z5kTlmy_2VEb189fXfCeFj6z4rrO5aUcNdJnHn5dXz4APgAgLhG-ESgMpG4jtB7-p0Ls3k2EYmLp9RerPVrvKAFveS23xQl2UT4AKBhUsXohogqHXqYEfjsfRAHCiSbbzQfM_H7NFRDIoxzKLh1-HMA4gGgtVIyurXEb0cIjT3frYFFTMfDQ48iwgntObJ1ht6v-RL4TfhLhwXQqicsVs-4QfNbBjk7e16wLiu54BjoVhrFrH41obLbpuP8tVCxRbVLHYoBQyGbXYOYQduPi9KL_jc9iJ284R8Z5uKMJYJvntwB-AC5AVD5AU240vjQAoJApIcF0KonLFbPlQC2tXkb4LXvZwY7PBC4QPuBUU2y_aB_WlGDMMOOnc25Zw71Asy9qm5P1ckuxut4n7eyRa8sX6BkLQ2BfmajCXpSPKQyPbQfknSBaVE5Jh3D-Wyd1gODu6DtnmROWbL_ZURvXz19d8J4WPrPius7lpRw10mcefl1fPgAoNBv8fj4bmAFfa9a8IyYHOK_pHPZt7lJi8AF0Cgkad7_gKbloDKRuI7Qe_qdC7N5NhGJi6fUXqz1a7ygBb3ktt8UJdlE-AD4ALh1-HMA4gGgqw1QRCuG0tcqzCpKDqhHQO09T3_OQMhOHAAgt-D4O-_4TfhLhwXQqic7qGy4QZPydQXTosV0Bw1Q3ntf7-FCxRDMSM7FOnPhP591OxiYUlsWMdK0qfbp-WrnfBJKObfEwNm1dqfX98D9xKLtCVsA-AC5AZH5AY65ARL5AQ8CgkClhwXQqic7qGyVALa1eRvgte9nBjs8ELhA-4FRTbL9oItWxVWVSnoGHZaeON6tuc1mKVrq1Wm18UkpjaplJALMoBbHRfvDVLJUkLGrwX2BHDYbXjXpRT-EcCLGG68iaSdKoO2eZE5Zsv9lRG9fPX13wnhY-s-K6zuWlHDXSZx5-XV8-AD4ALg8CAAgcEAMEg8IhMDEBAhUDg0OhQEgYIiMEEEWjMIisaAENjsEAUIjEXghCkEZQEbjMQhEChETlEglUKgIuEb4RKC_bo8qyH3l705OLCmJ1kMQd9iEV0aw7gStxMPmCTOAffgAoL2i-AmVd_b_p3gkSP5glYl9pIn40AEUMIvqwpHIvkahuHX4cwDiAaCp1buI8W7RsDhZAp69_T6gJBjO_aR00EmjIj3-397EXfhN-EuHBdCqJ0rbgLhB459yJ9MK7TDZ2xQ23jTHbdbiwOCTEcmb5U7olxiUgaFV28sh8nuVzXrjJXuGfUnNureSKSOSXSwvp-aQTixLNAH4APgA-QNauQNX-QNUALkBVPkBUbkBTvkBS4IgALkBRfkBQgCVAZQ5KOt2a33MGDNmJIyCIn-oz8dWgwdSZIMHUmSFAukO3QC49hAAAAAAAAABAAAAAAAAAAAAAAAAAAAAAAAAAAAgQAAAAAAAAAAAAAEAAAAAAAAAAAAAAAAAAAAABAAACAAAAAAAAAAAAAAAACAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAgAAAAAAAAAAAAAAAAAAEAAAAAAAAAAAAAAAAAAAAAAAAIAAAAAACAAAAAAIAAAAEIAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAgAAAAAAACAAAAAAAAAAAAAAAAQAAAAAAAAAQAAAAAAAABAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAIAAAAAAAAAAAPgA-ACgvEWAgjiMdu0pUAxvQcurR0OVNFEmP5ZzhNMkpIVwqPr5Afn5AfYAuQHy-QHvo-IQoMOetu_ZC-YK5vgAe7UsNeBouviaGeP9VQgl1JDY7dRguFP4UaBjTIu0WBCx1UEf8MBqzibUyAkFNsmaQ3gnewIx7LIl96CegRcYwno7-6NZanIbuiC4o_93OVbLE-UByERTjAHFv4CAgICAgICAgICAgICAgLkBc_kBcCC5AWz5AWmVAZwHK-9UWr65aj97ov_fi4YftV3q-FSWTWVzc2FnZShzdHIsaW50LGJ5dGVzKbg6YnRwOi8vMHg1MDEucHJhLzB4NUNDMzA3MjY4YTEzOTNBQjlBNzY0QTIwREFDRTg0OEFCODI3NWM0NgL4-7j5-Pe4PmJ0cDovLzB4NThlYjFjLmljb24vY3g5YzA3MmJlZjU0NWFiZWI5NmEzZjdiYTJmZmRmOGI4NjFmYjU1ZGVhuDpidHA6Ly8weDUwMS5wcmEvMHg1Q0MzMDcyNjhhMTM5M0FCOUE3NjRBMjBEQUNFODQ4QUI4Mjc1YzQ2im5hdGl2ZWNvaW4BuG34awC4aPhmqmh4NDUwMmFhZDc5ODZhZDVhODQ4OTU1MTVmYWY3NmU5MGI1YjQ3ODY1NKoweDE1OEEzOTFGMzUwMEMzMjg4QWIyODY1MzcyMmE2NDU5RTc3MjZCMDHPzoNJQ1iJAIlj3YwsXgAA".to_string()).unwrap();

        let receipt_proof = relay_message.receipt_proofs().last().unwrap();

        let receipt_hash = relay_message
            .clone()
            .block_updates()
            .last()
            .unwrap()
            .block_header()
            .block_result()
            .get()
            .unwrap()
            .receipt_hash()
            .get()
            .unwrap()
            .clone();

        let mut contract = BtpMessageVerifier::new(
            "alice.testnet".parse::<AccountId>().unwrap(),
            "0x1.icon".to_string(),
            Validators::from(&vec![]),
            U64(16546),
            Hash::try_from("0xc5d6c454e4d7a8e8a654f5ef96e8efe41d21a65b171b298925414aa3dc061e37".to_string()).unwrap()
        );

        let receipt = contract
            .prove_receipt_proof(receipt_proof, &receipt_hash)
            .unwrap();

        let messages = receipt
            .event_logs()
            .iter()
            .map(
                |event_log| -> Result<BtpMessage<SerializedMessage>, BmvError> {
                    if event_log.is_message() {
                        return event_log
                            .to_message()
                            .map_err(|error| BmvError::DecodeFailed {
                                message: error.to_string(),
                            })?
                            .message()
                            .0
                            .clone()
                            .try_into()
                            .map_err(|error| BmvError::DecodeFailed { message: error });
                    }
                    Err(BmvError::Unknown {
                        message: "Not Message".to_string(),
                    })
                },
            )
            .collect::<Result<SerializedBtpMessages, BmvError>>()
            .unwrap();

        assert_eq!(
            messages,
            vec![BtpMessage::new(
                BTPAddress::new(
                    "btp://0x58eb1c.icon/cx9c072bef545abeb96a3f7ba2ffdf8b861fb55dea".to_string()
                ),
                BTPAddress::new(
                    "btp://0x501.pra/0x5CC307268a1393AB9A764A20DACE848AB8275c46".to_string()
                ),
                "nativecoin".to_string(),
                WrappedI128::new(1),
                vec![
                    248, 107, 0, 184, 104, 248, 102, 170, 104, 120, 52, 53, 48, 50, 97, 97, 100,
                    55, 57, 56, 54, 97, 100, 53, 97, 56, 52, 56, 57, 53, 53, 49, 53, 102, 97, 102,
                    55, 54, 101, 57, 48, 98, 53, 98, 52, 55, 56, 54, 53, 52, 170, 48, 120, 49, 53,
                    56, 65, 51, 57, 49, 70, 51, 53, 48, 48, 67, 51, 50, 56, 56, 65, 98, 50, 56, 54,
                    53, 51, 55, 50, 50, 97, 54, 52, 53, 57, 69, 55, 55, 50, 54, 66, 48, 49, 207,
                    206, 131, 73, 67, 88, 137, 0, 137, 99, 221, 140, 44, 94, 0, 0
                ],
                None
            )]
        );
    }
}
