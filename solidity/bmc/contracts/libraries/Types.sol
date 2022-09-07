// SPDX-License-Identifier: Apache-2.0
pragma solidity >=0.8.0 <0.8.5;

library Types {
    /**
     * @Notice List of ALL Structs being used by a BMC contract
     */
    struct VerifierStats {
        uint256 height; // Last verified block height
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
        string btpAddress;
        uint256 rxSeq;
        uint256 txSeq;
        bool isConnected;
    }

    struct LinkStats {
        uint256 rxSeq;
        uint256 txSeq;
        VerifierStats verifier;
        uint256 currentHeight;
    }

    struct RelayStats {
        address addr;
        uint256 blockCount;
        uint256 msgCount;
    }

    struct BTPMessage {
        string src; //  an address of BMC (i.e. btp://1234.PARA/0x1234)
        string dst; //  an address of destination BMC
        string svc; //  service name of BSH
        int256 sn; //  sequence number of BMC
        bytes message; //  serialized Service Message from BSH
    }

    struct ErrorMessage {
        uint256 code;
        string message;
    }

    struct BMCService {
        string serviceType;
        bytes payload;
    }

}
