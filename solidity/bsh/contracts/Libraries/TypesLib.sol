pragma solidity >=0.5.0 <=0.8.0;

library Types {
    /**
     * @Notice List of ALL Struct being used to Encode and Decode RLP Messages
     */

    //  SPR = State Hash + Pathch Receipt Hash + Receipt Hash
    struct SPR {
        bytes stateHash;
        bytes patchReceiptHash;
        bytes receiptHash;
    }

    struct BlockHeader {
        uint256 version;
        uint256 height;
        uint256 timestamp;
        bytes proposer;
        bytes prevHash;
        bytes voteHash;
        bytes nextValidators;
        bytes patchTxHash;
        bytes txHash;
        bytes logsBloom;
        SPR spr;
        bool isSPREmpty; //  add to check whether SPR is an empty struct
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

    struct Votes {
        uint256 round;
        BPSI blockPartSetID;
        TS[] _ts;
    }

    struct BlockWitness {
        uint256 height;
        bytes[] witnesses;
    }

    struct EventProof {
        uint256 index;
        bytes[] eventMptNode;
    }

    struct BlockUpdate {
        BlockHeader _bh;
        Votes _votes;
        bytes[] _validators;
    }

    struct ReceiptProof {
        uint256 index;
        bytes[] txReceipts;
        EventProof[] _ep;
    }

    struct BlockProof {
        BlockHeader _bh;
        BlockWitness _bw;
    }

    struct RelayMessage {
        BlockUpdate[] _buArray;
        BlockProof _bp;
        bool isBPEmpty; //  add to check in a case BlockProof is an empty struct
        //  when RLP RelayMessage, this field will not be serialized
        ReceiptProof[] _rp;
        bool isRPEmpty; //  add to check in a case ReceiptProof is an empty struct
        //  when RLP RelayMessage, this field will not be serialized
    }

    /**
     * @Notice List of ALL Structs being used by a BSH contract
     */
    enum ServiceType {
        REQUEST_COIN_TRANSFER,
        REQUEST_COIN_REGISTER,
        REPONSE_HANDLE_SERVICE,
        UNKNOWN_TYPE
    }

    struct TransferCoin {
        string from;
        string to;
        string coinName;
        uint256 value;
    }

    struct RegisterCoin {
        string coinName;
        uint256 id;
        string symbol;
    }

    struct Response {
        uint256 code;
        string message;
    }

    struct Record {
        TransferCoin request;
        Response response;
        bool isResolved;
    }

    struct ServiceMessage {
        ServiceType serviceType;
        bytes data;
    }

    struct Coin {
        uint256 id;
        string symbol;
        uint256 decimals;
    }

    struct Balance {
        uint256 lockedBalance;
        uint256 refundableBalance;
    }

    /**
     * @Notice List of ALL Structs being used by a BMC contract
     */

    enum EventType {LINK, UNLINK}

    struct VerifierStatus {
        uint256 heightMTA; // MTA = Merkle Trie Accumulator
        uint256 offsetMTA;
        uint256 lastHeight; // Block height of last verified message which is BTP-Message contained
        bytes extra;
    }

    struct Service {
        string svc;
        address addr;
    }

    struct Verifier {
        string net;
        address addr;
    }

    struct Route {
        string dst; //  BTP Address of destination BMC
        string next; //  BTP Address of a BMC before reaching dst BMC
    }

    struct Link {
        address[] relays; //  Address of multiple Relays handle for this link network
        string reachable; //  A BTP Address of the next BMC that can be reach using this link
        bool isConnected;
    }

    struct RelayStats {
        address addr;
        uint256 blockCount;
        uint256 msgCount;
    }

    struct BMCMessage {
        string src; //  an address of BMC (i.e. btp://1234.PARA/0x1234)
        string dst; //  an address of destination BMC
        string svc; //  service name of BSH
        uint256 sn; //  sequence number of BMC
        bytes message; //  serializef Service Message from BSH
    }

    struct EventMessage {
        Types.EventType eventType;
        string src;
        string dst;
    }

    // bytes4(keccak256("onERC1155Received(address,address,uint256,uint256,bytes)"))
    bytes4 internal constant ERC1155_ACCEPTED = 0xf23a6e61;

    // bytes4(keccak256("onERC1155BatchReceived(address,address,uint256[],uint256[],bytes)"))
    bytes4 internal constant ERC1155_BATCH_ACCEPTED = 0xbc197c81;
}
