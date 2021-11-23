use bmv_icon::BtpMessageVerifier;
use hex::{decode, encode};
use libraries::{rlp, MerklePatriciaTree};
use near_sdk::{testing_env, VMContext};
pub mod accounts;
use accounts::*;
use std::convert::TryFrom;
use std::ops::Deref;

#[cfg(feature = "testable")]
use bmv_icon::types::{RelayMessage, Sha256};

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
#[cfg(feature = "testable")]
fn new_mpt() {
    let context = || get_context(false);
    testing_env!(context());

    let relay_message = RelayMessage::try_from("-Qee-QQ8uQFS-QFPuNT40gKCQKOHBdCqJx0eQ5UAtrV5G-C172cGOzwQuED7gVFNsv2g1tINkHhbtViQXPruQ-FnkmYs_jA0tYrJKbvq3BYO9Rqgt12iGPeeEGlPxvT_hNLnhr9K5nUqvKiB6A_Yc0bfl76g7Z5kTlmy_2VEb189fXfCeFj6z4rrO5aUcNdJnHn5dXz4APgAgLhG-ESgMpG4jtB7-p0Ls3k2EYmLp9RerPVrvKAFveS23xQl2UT4AKBhUsXohogqHXqYEfjsfRAHCiSbbzQfM_H7NFRDIoxzKLh1-HMA4gGgtVIyurXEb0cIjT3frYFFTMfDQ48iwgntObJ1ht6v-RL4TfhLhwXQqicsVs-4QfNbBjk7e16wLiu54BjoVhrFrH41obLbpuP8tVCxRbVLHYoBQyGbXYOYQduPi9KL_jc9iJ284R8Z5uKMJYJvntwB-AC5AVD5AU240vjQAoJApIcF0KonLFbPlQC2tXkb4LXvZwY7PBC4QPuBUU2y_aB_WlGDMMOOnc25Zw71Asy9qm5P1ckuxut4n7eyRa8sX6BkLQ2BfmajCXpSPKQyPbQfknSBaVE5Jh3D-Wyd1gODu6DtnmROWbL_ZURvXz19d8J4WPrPius7lpRw10mcefl1fPgAoNBv8fj4bmAFfa9a8IyYHOK_pHPZt7lJi8AF0Cgkad7_gKbloDKRuI7Qe_qdC7N5NhGJi6fUXqz1a7ygBb3ktt8UJdlE-AD4ALh1-HMA4gGgqw1QRCuG0tcqzCpKDqhHQO09T3_OQMhOHAAgt-D4O-_4TfhLhwXQqic7qGy4QZPydQXTosV0Bw1Q3ntf7-FCxRDMSM7FOnPhP591OxiYUlsWMdK0qfbp-WrnfBJKObfEwNm1dqfX98D9xKLtCVsA-AC5AZH5AY65ARL5AQ8CgkClhwXQqic7qGyVALa1eRvgte9nBjs8ELhA-4FRTbL9oItWxVWVSnoGHZaeON6tuc1mKVrq1Wm18UkpjaplJALMoBbHRfvDVLJUkLGrwX2BHDYbXjXpRT-EcCLGG68iaSdKoO2eZE5Zsv9lRG9fPX13wnhY-s-K6zuWlHDXSZx5-XV8-AD4ALg8CAAgcEAMEg8IhMDEBAhUDg0OhQEgYIiMEEEWjMIisaAENjsEAUIjEXghCkEZQEbjMQhEChETlEglUKgIuEb4RKC_bo8qyH3l705OLCmJ1kMQd9iEV0aw7gStxMPmCTOAffgAoL2i-AmVd_b_p3gkSP5glYl9pIn40AEUMIvqwpHIvkahuHX4cwDiAaCp1buI8W7RsDhZAp69_T6gJBjO_aR00EmjIj3-397EXfhN-EuHBdCqJ0rbgLhB459yJ9MK7TDZ2xQ23jTHbdbiwOCTEcmb5U7olxiUgaFV28sh8nuVzXrjJXuGfUnNureSKSOSXSwvp-aQTixLNAH4APgA-QNauQNX-QNUALkBVPkBUbkBTvkBS4IgALkBRfkBQgCVAZQ5KOt2a33MGDNmJIyCIn-oz8dWgwdSZIMHUmSFAukO3QC49hAAAAAAAAABAAAAAAAAAAAAAAAAAAAAAAAAAAAgQAAAAAAAAAAAAAEAAAAAAAAAAAAAAAAAAAAABAAACAAAAAAAAAAAAAAAACAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAgAAAAAAAAAAAAAAAAAAEAAAAAAAAAAAAAAAAAAAAAAAAIAAAAAACAAAAAAIAAAAEIAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAgAAAAAAACAAAAAAAAAAAAAAAAQAAAAAAAAAQAAAAAAAABAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAIAAAAAAAAAAAPgA-ACgvEWAgjiMdu0pUAxvQcurR0OVNFEmP5ZzhNMkpIVwqPr5Afn5AfYAuQHy-QHvo-IQoMOetu_ZC-YK5vgAe7UsNeBouviaGeP9VQgl1JDY7dRguFP4UaBjTIu0WBCx1UEf8MBqzibUyAkFNsmaQ3gnewIx7LIl96CegRcYwno7-6NZanIbuiC4o_93OVbLE-UByERTjAHFv4CAgICAgICAgICAgICAgLkBc_kBcCC5AWz5AWmVAZwHK-9UWr65aj97ov_fi4YftV3q-FSWTWVzc2FnZShzdHIsaW50LGJ5dGVzKbg6YnRwOi8vMHg1MDEucHJhLzB4NUNDMzA3MjY4YTEzOTNBQjlBNzY0QTIwREFDRTg0OEFCODI3NWM0NgL4-7j5-Pe4PmJ0cDovLzB4NThlYjFjLmljb24vY3g5YzA3MmJlZjU0NWFiZWI5NmEzZjdiYTJmZmRmOGI4NjFmYjU1ZGVhuDpidHA6Ly8weDUwMS5wcmEvMHg1Q0MzMDcyNjhhMTM5M0FCOUE3NjRBMjBEQUNFODQ4QUI4Mjc1YzQ2im5hdGl2ZWNvaW4BuG34awC4aPhmqmh4NDUwMmFhZDc5ODZhZDVhODQ4OTU1MTVmYWY3NmU5MGI1YjQ3ODY1NKoweDE1OEEzOTFGMzUwMEMzMjg4QWIyODY1MzcyMmE2NDU5RTc3MjZCMDHPzoNJQ1iJAIlj3YwsXgAA".to_string()).unwrap();

    let mpt = <MerklePatriciaTree<Sha256>>::try_from(
        relay_message
            .receipt_proofs()
            .last()
            .unwrap()
            .proofs()
            .deref()
            .first()
            .unwrap(),
    )
    .unwrap();

    println!("{:?}", mpt);
}
