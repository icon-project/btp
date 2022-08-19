// SPDX-License-Identifier: Apache-2.0
pragma solidity >=0.8.0 <0.8.5;

/**
 * @notice List of ALL Struct being used to Encode and Decode RLP Messages
 */
library Types {
    int constant CS_REQUEST = 1;
    int constant CS_RESPONSE = 2;

    struct CallRequest {
        address from;
        string to;
        bytes rollback;
        bool enabled;
    }

    struct CSMessage {
        int msgType;
        bytes payload;
    }

    struct CSMessageRequest {
        string from;
        string to;
        uint256 sn;
        bool rollback;
        bytes data;
    }

    int constant CS_RESP_SUCCESS = 0;
    int constant CS_RESP_FAILURE = -1;
    int constant CS_RESP_BTP_ERROR = -2;

    struct CSMessageResponse {
        uint256 sn;
        int code;
        string msg;
    }

    string constant FEE_RELAY = "relay";
    string constant FEE_PROTOCOL = "protocol";

    struct FeeConfig {
        uint256 relay;
        uint256 protocol;
        bool exists;//only for null check
    }

}
