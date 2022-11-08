// SPDX-License-Identifier: Apache-2.0
pragma solidity >=0.8.0 <0.8.5;

import "../interfaces/IBMV.sol";

library Types {
    string internal constant BMC_SERVICE = "bmc";

    uint256 internal constant ECODE_NONE = 0;
    uint256 internal constant ECODE_UNKNOWN = 1;
    uint256 internal constant ECODE_NO_ROUTE = 2;
    uint256 internal constant ECODE_NO_BSH = 3;
    uint256 internal constant ECODE_BSH_REVERT = 4;

    string internal constant BMC_INTERNAL_INIT = "Init";
    string internal constant BMC_INTERNAL_LINK = "Link";
    string internal constant BMC_INTERNAL_UNLINK = "Unlink";
    string internal constant BMC_INTERNAL_CLAIM = "Claim";
    string internal constant BMC_INTERNAL_RESPONSE = "Response";

    uint256 internal constant ROUTE_TYPE_NONE = 0;
    uint256 internal constant ROUTE_TYPE_LINK = 1;
    uint256 internal constant ROUTE_TYPE_REACHABLE = 2;
    uint256 internal constant ROUTE_TYPE_MANUAL = 3;

    string internal constant BTP_EVENT_SEND = "SEND";
    string internal constant BTP_EVENT_ROUTE = "ROUTE";
    string internal constant BTP_EVENT_REPLY = "REPLY";
    string internal constant BTP_EVENT_ERROR = "ERROR";
    string internal constant BTP_EVENT_RECEIVE = "RECEIVE";
    string internal constant BTP_EVENT_DROP = "DROP";

    struct Service {
        string svc;
        address addr;
    }

    struct Verifier {
        string net;
        address addr;
    }

    struct Route {
        string dst; //  Network Address of destination BMC
        string next; //  Network Address of a BMC before reaching dst BMC
    }

    struct RouteInfo {
        string dst; //  Network Address of destination BMC
        string next; //  Network Address of a BMC before reaching dst BMC
        uint256 reachable;
        uint256 routeType;//{0:unregistered, 1:link, 2:reachable, 3:manual}
    }

    struct Link {
        string btpAddress;
        string[] reachable;
    }

    struct LinkStatus {
        uint256 rxSeq;
        uint256 txSeq;
        IBMV.VerifierStatus verifier;
        uint256 currentHeight;
    }

    struct FeeInfo {
        string network;
        uint256[] values;
    }

    struct BTPMessage {
        string src;
        string dst;
        string svc;
        int256 sn;
        bytes message;
        int256 nsn;
        FeeInfo feeInfo;
    }

    struct ResponseMessage {
        uint256 code;
        string message;
    }

    struct BMCMessage {
        string msgType;
        bytes payload;
    }

    struct Request {
        int256 nsn;
        string dst;
        address caller;
        uint256 amount;
    }

    struct ClaimMessage {
        uint256 amount;
        string receiver;
    }

    struct Response {
        int256 nsn;
        FeeInfo feeInfo;
    }
}
