pragma solidity >=0.5.0 <=0.8.0;
pragma experimental ABIEncoderV2;

import "../../../icondao/Interfaces/IBMC.sol";
import "../../../icondao/Libraries/ParseAddressLib.sol";
import "../../../icondao/Libraries/RLPEncodeStructLib.sol";
import "../../../icondao/Libraries/StringsLib.sol";
import "../../../icondao/Libraries/Owner.sol";
import "../../../icondao/Libraries/EncodeBase64Lib.sol";
import "../../../icondao/Libraries/DecodeBase64Lib.sol";

contract BMC is IBMC, Owner {
    using ParseAddress for address;
    using ParseAddress for string;
    using RLPEncodeStruct for Types.BMCMessage;
    using RLPEncodeStruct for Types.EventMessage;
    using Strings for string;

    uint256 internal constant RC_OK = 0;
    uint256 internal constant RC_ERR = 1;

    event Message(
        string indexed _next, //  an address of the next BMC (it could be a destination BMC)
        uint256 _seq, //  a sequence number of BMC (NOT sequence number of BSH)
        bytes _msg
    );

    //  emit EVENT to announce link/unlink service
    event Event(string _next, uint256 _seq, bytes _msg);

    mapping(string => address) private bshServices;
    mapping(string => address) private bmvServices;
    // mapping(address => string) Relays;
    mapping(string => string) private routes;
    mapping(string => Types.Link) private links;

    string[] private listBMVNames;
    string[] private listBSHNames;
    string[] private listRouteKeys;
    string[] private listLinkNames;
    // address[] list_Relays;
    address[] private addrs;

    string private bmcAddress; // a network address BMV, i.e. btp://1234.pra
    uint256 private serialNo;
    uint256 private numOfBMVService;
    uint256 private numOfBSHService;
    uint256 private numOfLinks;
    uint256 private numOfRoutes;

    // uint numOfRelays;
    //  must call Owner()
    constructor(string memory _network) {
        bmcAddress = _network.concat("/").concat(address(this).toString());
    }

    function handleRelayMessage(string calldata _prev, bytes memory _msg)
        public
        override
    {}

    /**
       @notice Send the message to a specific network.
       @dev Caller must be an registered BSH.
       @param _to      Network Address of destination network
       @param _svc     Name of the service
       @param _sn      Serial number of the message, it should be positive
       @param _msg     Serialized bytes of Service Message
    */
    function sendMessage(
        string memory _to,
        string memory _svc,
        uint256 _sn,
        bytes memory _msg
    ) external override {
        require(
            msg.sender == address(this) || bshServices[_svc] == msg.sender,
            "No permission"
        );
        //  In case BSH sends a REQUEST_COIN_TRANSFER,
        //  but '_to' is a network which is not supported by BMC
        //  revert
        if (bmvServices[_to] == address(0)) {
            revert("Invalid network");
        }
        bytes memory _rlp =
            Types
                .BMCMessage(bmcAddress, _to, _svc, _sn, _msg)
                .encodeBMCMessage();
        if (_svc.compareTo("EVENT")) {
            emit Event(_to, serialNo, _rlp);
        } else {
            emit Message(_to, serialNo, _rlp);
        }
        serialNo++;
    }

    /**
       @notice Registers the smart contract for the service.
       @dev Caller must be an operator of BTP network.
       @param _svc     Name of the service
       @param _addr    Address of the smart contract handling the service
   */
    function addService(string memory _svc, address _addr)
        public
        override
        owner
    {
        require(bshServices[_svc] == address(0), "BSH service existed");
        bshServices[_svc] = _addr;
        listBSHNames.push(_svc);
        numOfBSHService++;
    }

    /**
       @notice Unregisters the smart contract for the service.  
       @dev Caller must be an operator of BTP network.
       @param _svc     Name of the service
   */
    function removeService(string memory _svc) public override owner {
        require(bshServices[_svc] != address(0), "BSH service not existed");
        delete bshServices[_svc];
        numOfBSHService--;
    }

    /**
       @notice Get registered services.
       @return _servicers   An array of Service.
    */
    function getServices()
        public
        view
        override
        returns (Types.Service[] memory)
    {
        Types.Service[] memory services = new Types.Service[](numOfBSHService);
        uint256 temp = 0;
        for (uint256 i = 0; i < listBSHNames.length; i++) {
            if (bshServices[listBSHNames[i]] != address(0)) {
                services[temp] = Types.Service(
                    listBSHNames[i],
                    bshServices[listBSHNames[i]]
                );
                temp++;
            }
        }
        return services;
    }

    /**
       @notice Registers BMV for the network. 
       @dev Caller must be an operator of BTP network.
       @param _net     Network Address of the blockchain
       @param _addr    Address of BMV
   */
    function addVerifier(string memory _net, address _addr)
        public
        override
        owner
    {
        require(bmvServices[_net] == address(0), "BMV service existed");
        bmvServices[_net] = _addr;
        listBMVNames.push(_net);
        numOfBMVService++;
    }

    /**
       @notice Unregisters BMV for the network.
       @dev Caller must be an operator of BTP network.
       @param _net     Network Address of the blockchain
   */
    function removeVerifier(string memory _net) public override owner {
        require(bmvServices[_net] != address(0), "BMV service not existed");
        delete bmvServices[_net];
        numOfBMVService--;
    }

    /**
       @notice Get registered verifiers.
       @return _verifiers   An array of Verifier.
    */
    function getVerifiers()
        public
        view
        override
        returns (Types.Verifier[] memory)
    {
        Types.Verifier[] memory verifiers =
            new Types.Verifier[](numOfBMVService);
        uint256 temp = 0;
        for (uint256 i = 0; i < listBMVNames.length; i++) {
            if (bmvServices[listBMVNames[i]] != address(0)) {
                verifiers[temp] = Types.Verifier(
                    listBMVNames[i],
                    bmvServices[listBMVNames[i]]
                );
                temp++;
            }
        }
        return verifiers;
    }

    function getVerifier(string memory _net)
        public
        view
        returns (address _addr)
    {
        return bmvServices[_net];
    }

    /**
       @notice Initializes status information for the link.
       @dev Caller must be an operator of BTP network.
       @param _link    BTP Address of connected BMC
   */
    function addLink(string memory _link) public override owner {
        string memory _net;
        string memory _bmc;
        (_net, _bmc) = _link.splitBTPAddress();
        require(bmvServices[_net] != address(0), "Verifier not existed");
        require(links[_link].isConnected == false, "Link existed");
        links[_link] = Types.Link(new address[](0), "", true);
        listLinkNames.push(_link);
        numOfLinks++;

        //  propagate an event "LINK"
        this.sendMessage(
            _net,
            "EVENT",
            0,
            prepareEventMessage(Types.EventType.LINK, bmcAddress, _link)
        );
    }

    /**
       @notice Removes the link and status information. 
       @dev Caller must be an operator of BTP network.
       @param _link    BTP Address of connected BMC
   */
    function removeLink(string memory _link) public override owner {
        require(links[_link].isConnected == true, "Link not existed");
        delete links[_link];
        numOfLinks--;

        string memory _net;
        string memory _bmc;
        (_net, _bmc) = _link.splitBTPAddress();
        //  propagate an event "UNLINK"
        this.sendMessage(
            _net,
            "EVENT",
            0,
            prepareEventMessage(Types.EventType.UNLINK, bmcAddress, _link)
        );
    }

    /**
       @notice Get registered links.
       @return _links   An array of links ( BTP Addresses of the BMCs ).
    */
    function getLinks() public view override returns (string[] memory) {
        string[] memory res = new string[](numOfLinks);
        uint256 temp;
        for (uint256 i = 0; i < listLinkNames.length; i++) {
            if (links[listLinkNames[i]].isConnected) {
                res[temp] = listLinkNames[i];
                temp++;
            }
        }
        return res;
    }

    function getLink(string memory _link)
        public
        view
        returns (Types.Link memory)
    {
        return links[_link];
    }

    function prepareEventMessage(
        Types.EventType eventType,
        string memory _from,
        string memory _to
    ) private returns (bytes memory) {
        return Types.EventMessage(eventType, _from, _to).encodeEventMessage();
    }

    /**
       @notice Add route to the BMC.
       @dev Caller must be an operator of BTP network.
       @param _dst     BTP Address of the destination BMC
       @param _link    BTP Address of the next BMC for the destination
   */
    function addRoute(string memory _dst, string memory _link)
        public
        override
        owner
    {
        require(bytes(routes[_dst]).length == 0, "Route existed");
        //  Verify _dst and _link format address
        //  these two strings must follow BTP format address
        //  If one of these is failed, revert()
        _dst.splitBTPAddress();
        _link.splitBTPAddress();

        routes[_dst] = _link; //  map _dst to _link
        listRouteKeys.push(_dst); //  push _dst key into an array of route keys
        numOfRoutes++; //  increase a number of routes
    }

    /**
       @notice Remove route to the BMC.
       @dev Caller must be an operator of BTP network.
       @param _dst     BTP Address of the destination BMC
   */
    function removeRoute(string memory _dst) public override owner {
        //  @dev No need to check if _dst is a valid BTP format address
        //  since it was checked when adding route at the beginning
        //  If _dst does not match, revert()
        require(bytes(routes[_dst]).length != 0, "Route not existed");
        delete routes[_dst];
        numOfRoutes--; // decrease a number of routes
    }

    /**
       @notice Get routing information.
       @return _routes An array of Route.
    */
    function getRoutes() public view override returns (Types.Route[] memory) {
        Types.Route[] memory _routes = new Types.Route[](numOfRoutes);
        uint256 temp;
        for (uint256 i = 0; i < listRouteKeys.length; i++) {
            if (bytes(routes[listRouteKeys[i]]).length != 0) {
                _routes[temp] = Types.Route(
                    listRouteKeys[i],
                    routes[listRouteKeys[i]]
                );
                temp++;
            }
        }
        return _routes;
    }

    /**
       @notice Registers relay for the network.
       @dev Called by the Relay-Operator to manage the BTP network.
       @param _link     BTP Address of connected BMC
       @param _addr     the address of Relay
    */
    function addRelay(string memory _link, address[] memory _addr)
        public
        override
        owner
    {
        require(links[_link].isConnected == true, "Link not existed");
        // if (links[_link].relays.length != 0) {
        //     numOfRelays += _addr.length - links[_link].relays.length;
        // }else {
        //     numOfRelays += _addr.length;
        // }
        links[_link].relays = _addr;
    }

    /**
       @notice Unregisters Relay for the network.
       @dev Called by the Relay-Operator to manage the BTP network.
       @param _link     BTP Address of connected BMC
       @param _addr     the address of Relay
    */
    function removeRelay(string memory _link, address _addr)
        public
        override
        owner
    {
        require(
            links[_link].isConnected == true && links[_link].relays.length != 0,
            "Link/Relay not existed"
        );
        for (uint256 i = 0; i < links[_link].relays.length; i++) {
            if (links[_link].relays[i] != _addr) {
                addrs.push(links[_link].relays[i]);
            }
        }
        links[_link].relays = addrs;
        delete addrs;
    }

    /**
       @notice Get registered relays.
       @param _link        BTP Address of the connected BMC.
       @return _relayes A list of relays.
    */

    function getRelays(string memory _link)
        public
        view
        override
        returns (address[] memory)
    {
        return links[_link].relays;
    }

    event MessageEvent(string _next, uint256 _seq);

    /*
       @notice Get status of BMC.
       @param _link        BTP Address of the connected BMC.
       @return tx_seq       Next sequence number of the next sending message.
       @return rx_seq       Next sequence number of the message to receive.
       @return verifier     VerifierStatus Object contains status information of the BMV.
    */
    function getStatus(string calldata _link)
        public
        view
        override
        returns (
            uint256 txSEQ,
            uint256 rxSEQ,
            VerifierStatus memory verifierStatus,
            RelayStats[] memory relaysStats,
            bytes memory rotationDetails
        )
    {
        uint256 heightMTA = 0; // MTA = Merkle Trie Accumulator
        uint256 offsetMTA = 0;
        uint256 lastHeight = 0;
        Types.Link memory link = getLink(_link);
        // Blockcount and message count are from Relay go struct, and added during handleRelayMessage
        //relay.block_count = relay.block_count + status["height"] - prev_status["height"]
        //relay.msg_count = relay.msg_count + len(serialized_msgs)
        uint256 length = link.relays.length;
        RelayStats[] memory relayStatus = new RelayStats[](length);
        for (uint256 i = 0; i < link.relays.length; i++) {
            relayStatus[i] = RelayStats(link.relays[i], 0, 0);
        }
        string memory _net;
        string memory _bmc;
        (_net, _bmc) = _link.splitBTPAddress();
        address verifier = getVerifier(_net);
        // create verifier interface Object with getstatus implemented
        // verifier.getStatus return height,offset from BMV- MTA.getStatus()
        // & lastheight= block_proof.blockHeight

        //relaysStats[0] = RelayStats(address(0), 0, 0); // Relay Address, blockCount, msgCount
        txSEQ = serialNo;
        rxSEQ =  0;//link.rx_seq = link.rx_seq + len(serialized_msgs) in handleRelayMessage*/
        verifierStatus =
            VerifierStatus(heightMTA, offsetMTA, lastHeight, new bytes(0));
        return (txSEQ, rxSEQ, verifierStatus, relayStatus, new bytes(0));
    }
}
