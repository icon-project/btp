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

pragma solidity >=0.5.0 <=0.8.5;
pragma experimental ABIEncoderV2;
import "./Libraries/TypesLib.sol";

interface IBMC {
    /**
  @notice Sends the message to the next BMC.
       The relay monitors this event.
       `Message` MUST emit when BTP messages are built.
       `_next` BTP Address of the BMC to handle the message.
       `_seq` Sequence number of the message from current BMC to the next.
       `_msg` Serialized bytes of BTP Message refer to the BTPMessage struct.  
   */

    // struct ErrorMessage {
    //   uint256 code;
    //   string msg;
    // }

    // struct BTPMessage {
    //   string src;
    //   string dst;
    //   string service;
    //   uint256 sn;
    //   bytes serviceMsg;
    // }

    // struct RelayMessage {
    //   bytes blockUpdates;
    //   bytes blockProof;
    //   bytes receiptProof
    // }

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
        string dst;
        string next;
    }

    struct RelayStats {
        address addr;
        uint256 blockCount;
        uint256 msgCount;
    }

    /**
       @notice Verify and decode RelayMessage with BMV, and dispatch BTP Messages to registered BSHs
       @dev Caller must be a registered relayer.     
       @param _prev    BTP Address of the BMC generates the message
       @param _msg     base64 encoded string of serialized bytes of Relay Message refer RelayMessage structure
   */

    function handleRelayMessage(string calldata _prev, bytes calldata _msg)
        external;

    /**
       @notice Send the message to a specific network.
       @dev Caller must be an registered BSH.
       @param _to      Network Address of destination network
       @param _svc     Name of the service
       @param _sn      Serial number of the message, it should be positive
       @param _msg     Serialized bytes of Service Message
   */
    function sendMessage(
        string calldata _to,
        string calldata _svc,
        uint256 _sn,
        bytes calldata _msg
    ) external;

    // /**
    //     @notice Add new owner account to BMC
    //     @dev Caller must be an owner.
    //     @param _addr    Address of new owner
    // */
    // function addOwner(address _addr) external;

    // /**
    //     @notice Remove an owner account from BMC
    //     @dev Caller must be an owner.
    //     Owner accounts can’t remove owner accounts less than 1
    //     @param _addr    Address of the existing owner
    // */
    // function removeOwner(string calldata _svc, address _addr) external;

    /**
       @notice Registers the smart contract for the service.
       @dev Caller must be an operator of BTP network.
       @param _svc     Name of the service
       @param _addr    Address of the smart contract handling the service
   */
    function addService(string calldata _svc, address _addr) external;

    /**
       @notice De-registers the smart contract for the service.  
       @dev Caller must be an operator of BTP network.
       @param _svc     Name of the service
   */
    function removeService(string calldata _svc) external;

    /**
       @notice Registers BMV for the network. 
       @dev Caller must be an operator of BTP network.
       @param _net     Network Address of the blockchain
       @param _addr    Address of BMV
   */
    function addVerifier(string calldata _net, address _addr) external;

    /**
       @notice De-registers BMV for the network.
       @dev Caller must be an operator of BTP network.
       @param _net     Network Address of the blockchain
   */
    function removeVerifier(string calldata _net) external;

    /**
       @notice Initializes status information for the link.
       @dev Caller must be an operator of BTP network.
       @param _link    BTP Address of connected BMC
   */
    function addLink(string calldata _link) external;

    /**
       @notice Removes the link and status information. 
       @dev Caller must be an operator of BTP network.
       @param _link    BTP Address of connected BMC
   */
    function removeLink(string calldata _link) external;

    /**
       @notice Add route to the BMC.
       @dev Caller must be an operator of BTP network.
       @param _dst     BTP Address of the destination BMC
       @param _link    BTP Address of the next BMC for the destination
   */
    function addRoute(string calldata _dst, string calldata _link) external;

    /**
       @notice Remove route to the BMC.
       @dev Caller must be an operator of BTP network.
       @param _dst     BTP Address of the destination BMC
   */
    function removeRoute(string calldata _dst) external;

    /**
       @notice Registers relay for the network.
       @dev Caller must be an operator of BTP network.
       @param _link     BTP Address of connected BMC
       @param _addrs     A list of Relays
   */
    function addRelay(string calldata _link, address[] memory _addrs) external;

    /**
       @notice Unregisters Relay for the network.
       @dev Caller must be an operator of BTP network.
       @param _link     BTP Address of connected BMC
       @param _addrs     A list of Relays
   */
    function removeRelay(string calldata _link, address _addrs) external;

    // /**
    //      @notice Get owner's addresses.
    //      @return _owners A list of owners.
    //   */
    // function getOwners() external view returns (address[] memory _owners);

    /**
       @notice Get registered services.
       @return _servicers   An array of Service.
    */
    function getServices()
        external
        view
        returns (Types.Service[] memory _servicers);

    /**
       @notice Get registered verifiers.
       @return _verifiers   An array of Verifier.
    */
    function getVerifiers()
        external
        view
        returns (Types.Verifier[] memory _verifiers);

    /**
       @notice Get registered links.
       @return _links   An array of links ( BTP Addresses of the BMCs ).
    */
    function getLinks() external view returns (string[] memory _links);

    /**
       @notice Get routing information.
       @return _routes An array of Route.
    */
    function getRoutes() external view returns (Types.Route[] memory _routes);

    /**
       @notice Get registered relays.
       @param _link        BTP Address of the connected BMC.
       @return _relayes A list of relays.
    */
    function getRelays(string calldata _link)
        external
        view
        returns (address[] memory _relayes);

    /*
       @notice Get status of BMC.
       @param _link        BTP Address of the connected BMC.
       @return txSEQ       Next sequence number of the next sending message.
       @return rxSEQ       Next sequence number of the message to receive.
       @return verifierStatus     VerifierStatus is an object that contains status information of the BMV.
       @return relays_stats     a list of relay's stats.
       @return rotation_details     The serial data of rotation infomation.
    */
    function getStatus(string calldata _link)
        external
        view
        returns (
            uint256 txSEQ,
            uint256 rxSEQ,
            VerifierStatus memory verifierStatus,
            RelayStats[] memory relaysStats,
            bytes memory rotationDetails
        );
}
