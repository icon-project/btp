// SPDX-License-Identifier: MIT
pragma solidity >=0.5.0 <0.8.0;

library LibTypes {
    /**
     * @Notice List of ALL Struct being used to Encode and Decode RLP Messages
     */

    //  Result = State Hash + Patch Receipt Hash + Receipt Hash + Extension Data
    struct Result {
        bytes32 stateHash;
        bytes32 patchReceiptHash;
        bytes32 receiptHash;
        bytes extensionData;
    }

    struct BlockHeader {
        bytes32 blockHash;
        uint256 version;
        uint256 height;
        uint256 timestamp;
        bytes proposer;
        bytes32 prevHash;
        bytes32 voteHash;
        bytes32 nextValidatorsHash;
        bytes patchTxHash;
        bytes txHash;
        bytes logsBloom;
        Result result;
        bool isResultEmpty; //  add to check whether SPR is an empty struct
        //  It will not be included in serializing thereafter
    }

    //  TS = Timestamp + Signature
    struct TS {
        uint256 timestamp;
        bytes signature;
    }

    //  BPSI = blockPartSetID
    struct BPSI {
        uint256 n;
        bytes b;
    }

    struct Validators {
        bytes serializedBytes;
        bytes32 validatorsHash;
        address[] validatorAddrs;
        mapping(address => bool) containedValidators;
        mapping(address => bool) checkDuplicateVotes;
    }

    struct Votes {
        uint256 round;
        BPSI blockPartSetID;
        TS[] ts;
    }

    struct BlockWitness {
        uint256 height;
        bytes32[] witnesses;
    }

    struct EventProof {
        uint256 index;
        bytes mptKey;
        bytes[] mptProofs;
    }

    struct BlockUpdate {
        BlockHeader blockHeader;
        Votes votes;
        address[] nextValidators;
        bytes nextValidatorsRlp;
        bytes32 nextValidatorsHash;
    }

    struct Receipt {
        EventLog[] eventLogs;
        bytes32 eventLogHash;
    }

    struct EventLog {
        string addr;
        bytes[] idx;
        bytes[] data;
    }

    struct MessageEvent {
        string nextBmc;
        uint256 seq;
        bytes message;
    }

    struct ReceiptProof {
        uint256 index;
        bytes mptKey;
        bytes[] mptProofs;
        EventProof[] eventProofs;
    }

    struct BlockProof {
        BlockHeader blockHeader;
        BlockWitness blockWitness;
    }

    struct RelayMessage {
        BlockUpdate[] blockUpdates;
        BlockProof blockProof;
        bool isBPEmpty; //  add to check in a case BlockProof is an empty struct
        //  when RLP RelayMessage, this field will not be serialized
    }
}
