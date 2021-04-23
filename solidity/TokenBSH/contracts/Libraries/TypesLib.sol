// SPDX-License-Identifier: Apache-2.0

/*
 * Copyright 2021 ICON Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

pragma solidity >=0.4.22 <0.8.5;

library Types {
    /**
     * @Notice List of ALL Struct being used to Encode and Decode RLP Messages
     */

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

    ////////
    enum ServiceType {
        REQUEST_TOKEN_TRANSFER,
        REQUEST_TOKEN_REGISTER,
        RESPONSE_HANDLE_SERVICE,
        RESPONSE_UNKNOWN
    }

    struct TransferToken {
        string from;
        string to;
        string tokenName;
        uint256 value;
    }

    struct Record {
        TransferToken request;
        Response response;
        bool isResolved;
    }

    struct ServiceMessage {
        ServiceType serviceType;
        bytes data;
    }

    struct Response {
        uint256 code;
        string message;
    }

    struct Balance {
        uint256 lockedBalance;
        uint256 refundableBalance;
    }

    struct Service {
        string svc;
        address addr;
    }
}
