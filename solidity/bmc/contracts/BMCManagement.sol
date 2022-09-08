// SPDX-License-Identifier: Apache-2.0
pragma solidity >=0.8.0 <0.8.5;
pragma abicoder v2;

import "./interfaces/IBMCManagement.sol";
import "./interfaces/IBMCPeriphery.sol";
import "./interfaces/IBMV.sol";
import "./libraries/ParseAddress.sol";
import "./libraries/RLPEncode.sol";
import "./libraries/RLPEncodeStruct.sol";
import "./libraries/Strings.sol";
import "./libraries/Types.sol";
import "./libraries/Utils.sol";

import "@openzeppelin/contracts-upgradeable/proxy/utils/Initializable.sol";

contract BMCManagement is IBMCManagement, Initializable {
    using ParseAddress for address;
    using ParseAddress for string;
    using RLPEncode for bytes;
    using RLPEncode for string;
    using RLPEncodeStruct for uint256;
    using RLPEncodeStruct for Types.BMCService;
    using RLPEncodeStruct for string[];
    using RLPEncodeStruct for string;
    using Strings for string;
    using Utils for string[];
    using Utils for address[];
    using Utils for uint256[];

    string internal constant BMCRevertInvalidAddress = "InvalidAddress";
    string internal constant BMCRevertAlreadyExistsBMV = "13:AlreadyExistsBMV";
    string internal constant BMCRevertNotExistsBMV = "14:NotExistsBMV";
    string internal constant BMCRevertAlreadyExistsBSH = "15:AlreadyExistsBSH";
    string internal constant BMCRevertNotExistsBSH = "16:NotExistsBSH";
    string internal constant BMCRevertAlreadyExistsLink = "17:AlreadyExistsLink";
    string internal constant BMCRevertNotExistsLink = "18:NotExistsLink";
    string internal constant BMCRevertAlreadyExistsRoute = "10:AlreadyExistRoute";
    string internal constant BMCRevertNotExistsRoute = "10:NotExistsRoute";
    string internal constant BMCRevertAlreadyExistsBMR = "19:AlreadyExistsBMR";
    string internal constant BMCRevertNotExistsBMR = "20:NotExistsBMR";
    string internal constant BMCRevertUnauthorized = "11:Unauthorized";
    string internal constant BMCRevertUnreachable = "21:Unreachable";
    string internal constant BMCRevertInvalidSn = "12:InvalidSn";
    string internal constant BMCRevertInvalidSeq = "10:InvalidSeq";

    mapping(address => bool) private _owners;
    uint256 private numOfOwner;

    mapping(string => address) private bshMap;//svc of bsh => address of BSH
    mapping(string => address) private bmvMap;//net of bmv => address of BMV
    mapping(string => Types.Link) private linkMap; //net of link => Types.Link
    mapping(string => string[]) private reachableMap;//net of link => list of reachable of link
    mapping(string => Types.Route) private routeMap;//net of destination => Types.Route
    mapping(string => address[]) private relayMap; //net => list of address of relay
    mapping(string => mapping(address => Types.RelayStats)) private relayStats; //link => address of relay => Types.RelayStats
    string[] private bmvKeyList;//list of net of bmv
    string[] private bshKeyList;//list of svc of bsh
    string[] private routeList;//list of destination of route
    string[] private linkList;//list of link
    address private bmcPeriphery;

    modifier hasPermission() {
        require(_owners[msg.sender] == true, "11:Unauthorized");
        _;
    }

    modifier onlyBMCPeriphery() {
        require(msg.sender == bmcPeriphery, "onlyBMCPeriphery");
        _;
    }

    function requireLink(
        string memory _link
    ) internal view returns (string memory) {
        (string memory net, ) = _link.splitBTPAddress();
        require(linkMap[net].isConnected == true, BMCRevertNotExistsLink);
        return net;
    }

    function requireValidAddress(
        address addr
    ) internal pure {
        require(addr != address(0), BMCRevertInvalidAddress);
    }

    function initialize() public initializer {
        _owners[msg.sender] = true;
        numOfOwner++;
    }

    function setBMCPeriphery(address _addr) external override hasPermission {
        requireValidAddress(_addr);
        bmcPeriphery = _addr;
    }

    function getBMCPeriphery() external view override returns (address) {
        return bmcPeriphery;
    }

    /*****************************************************************************************
                                        Add Authorized Owner of Contract
        - addOwner(): register additional Owner of this Contract
        - removeOwner(): un-register existing Owner of this Contract. Unable to remove last
        - isOwner(): checking Ownership of an arbitrary address
    *****************************************************************************************/

    /**
       @notice Adding another Onwer.
       @dev Caller must be an Onwer of BTP network
       @param _owner    Address of a new Onwer.
   */
    function addOwner(address _owner) external override hasPermission {
        requireValidAddress(_owner);
        require(_owners[_owner] == false, "10:AlreadyExists");
        _owners[_owner] = true;
        numOfOwner++;
    }

    /**
       @notice Removing an existing Owner.
       @dev Caller must be an Owner of BTP network
       @dev If only one Owner left, unable to remove the last Owner
       @param _owner    Address of an Owner to be removed.
     */
    function removeOwner(address _owner) external override hasPermission {
        require(_owners[_owner] == true, "10:NotExists");
        require(numOfOwner > 1, "LastOwner");
        delete _owners[_owner];
        numOfOwner--;
    }

    /**
       @notice Checking whether one specific address has Owner role.
       @dev Caller can be ANY
       @param _owner    Address needs to verify.
    */
    function isOwner(address _owner) external view override returns (bool) {
        return _owners[_owner];
    }

    /**
       @notice Add the smart contract for the service.
       @dev Caller must be an operator of BTP network.
       @param _svc     Name of the service
       @param _addr    Service's contract address
     */
    function addService(string memory _svc, address _addr)
        external
        override
        hasPermission
    {
        requireValidAddress(_addr);
        require(bshMap[_svc] == address(0), BMCRevertAlreadyExistsBSH);
        bshMap[_svc] = _addr;
        bshKeyList.push(_svc);
    }

    /**
       @notice Unregisters the smart contract for the service.
       @dev Caller must be an operator of BTP network.
       @param _svc     Name of the service
   */
    function removeService(string memory _svc) external override hasPermission {
        require(bshMap[_svc] != address(0), BMCRevertNotExistsBSH);
        delete bshMap[_svc];
        bshKeyList.removeFromStrings(_svc);
    }

    /**
       @notice Get registered services.
       @return _servicers   An array of Service.
    */
    function getServices()
        external
        view
        override
        returns (Types.Service[] memory)
    {
        Types.Service[] memory services = new Types.Service[](
            bshKeyList.length
        );
        for (uint256 i = 0; i < bshKeyList.length; i++) {
            services[i] = Types.Service(
                bshKeyList[i],
                bshMap[bshKeyList[i]]
            );
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
        external
        override
        hasPermission
    {
        requireValidAddress(_addr);
        require(bmvMap[_net] == address(0), BMCRevertAlreadyExistsBMV);
        bmvMap[_net] = _addr;
        bmvKeyList.push(_net);
    }

    /**
       @notice Unregisters BMV for the network.
       @dev Caller must be an operator of BTP network.
       @param _net     Network Address of the blockchain
     */
    function removeVerifier(string memory _net)
        external
        override
        hasPermission
    {
        require(bmvMap[_net] != address(0), BMCRevertNotExistsBMV);
        delete bmvMap[_net];
        bmvKeyList.removeFromStrings(_net);
    }

    /**
       @notice Get registered verifiers.
       @return _verifiers   An array of Verifier.
     */
    function getVerifiers()
        external
        view
        override
        returns (Types.Verifier[] memory)
    {
        Types.Verifier[] memory verifiers =
            new Types.Verifier[](bmvKeyList.length);

        for (uint256 i = 0; i < bmvKeyList.length; i++) {
            verifiers[i] = Types.Verifier(
                bmvKeyList[i],
                bmvMap[bmvKeyList[i]]
            );
        }
        return verifiers;
    }

    /**
       @notice Initializes status information for the link.
       @dev Caller must be an operator of BTP network.
       @param _link    BTP Address of connected BMC
   */
    function addLink(string calldata _link) external override hasPermission {
        (string memory net, ) = _link.splitBTPAddress();
        require(bmvMap[net] != address(0), BMCRevertNotExistsBMV);
        require(linkMap[net].isConnected == false,BMCRevertAlreadyExistsLink);

        // propagate an event "Link"
        propagateInternal(
            Types.BMCService("Link", _link.encodePropagateMessage())
            .encodeBMCService());
        bytes memory initMsg = Types.BMCService("Init", linkList.encodeInitMessage())
            .encodeBMCService();
        // store link
        linkMap[net] = Types.Link(
            _link,
            0,
            0,
            true
        );
        linkList.push(_link);
        // init link
        sendInternal(_link, net, initMsg);
    }

    /**
       @notice Removes the link and status information.
       @dev Caller must be an operator of BTP network.
       @param _link    BTP Address of connected BMC
   */
    function removeLink(string calldata _link) external override hasPermission {
        string memory net = requireLink(_link);

        for (uint256 i = 0; i < routeList.length; i++) {
            (string memory dstNet, ) = routeList[i].splitBTPAddress();
            if (routeMap[dstNet].next.compareTo(_link)) {
                revert("10:referred by route");
            }
        }
        propagateInternal(
            Types.BMCService("Unlink", _link.encodePropagateMessage())
            .encodeBMCService());

        for (uint256 i = 0; i < relayMap[net].length; i++) {
            delete relayStats[_link][relayMap[net][i]];
        }
        delete relayMap[net];
        delete reachableMap[net];
        delete linkMap[net];
        linkList.removeFromStrings(_link);
    }

    /**
       @notice Get registered links.
       @return _links   An array of links ( BTP Addresses of the BMCs ).
    */
    function getLinks() external view override returns (string[] memory) {
        return linkList;
    }

    function propagateInternal(
        bytes memory serializedMsg
    ) private {
        for (uint256 i = 0; i < linkList.length; i++) {
            (string memory net, ) = linkList[i].splitBTPAddress();
            sendInternal(linkList[i], net, serializedMsg);
        }
    }

    function sendInternal(
        string memory link,
        string memory linkNet,
        bytes memory serializedMsg
    ) private {
        linkMap[linkNet].txSeq++;
        IBMCPeriphery(bmcPeriphery).sendInternal(
            link, linkMap[linkNet].txSeq, serializedMsg);
    }

    /**
       @notice Add route to the BMC.
       @dev Caller must be an operator of BTP network.
       @param _dst     BTP Address of the destination BMC
       @param _link    BTP Address of the next BMC for the destination
   */
    function addRoute(string memory _dst, string memory _link)
        external
        override
        hasPermission
    {
        (string memory dstNet, ) = _dst.splitBTPAddress();
        require(bytes(routeMap[dstNet].dst).length == 0, BMCRevertAlreadyExistsRoute);
        requireLink(_link);

        routeMap[dstNet] = Types.Route(_dst, _link);
        routeList.push(_dst);
    }

    /**
       @notice Remove route to the BMC.
       @dev Caller must be an operator of BTP network.
       @param _dst     BTP Address of the destination BMC
    */
    function removeRoute(string memory _dst) external override hasPermission {
        (string memory dstNet, ) = _dst.splitBTPAddress();
        require(bytes(routeMap[dstNet].dst).length > 0, BMCRevertNotExistsRoute);
        delete routeMap[dstNet];
        routeList.removeFromStrings(_dst);
    }

    /**
       @notice Get routing information.
       @return _routes An array of Route.
    */
    function getRoutes() external view override returns (Types.Route[] memory) {
        Types.Route[] memory _routes = new Types.Route[](routeList.length);
        for (uint256 i = 0; i < routeList.length; i++) {
            (string memory dstNet, ) = routeList[i].splitBTPAddress();
            _routes[i] = routeMap[dstNet];
        }
        return _routes;
    }

    /**
       @notice Registers relay for the network.
       @dev Called by the Relay-Operator to manage the BTP network.
       @param _link     BTP Address of connected BMC
       @param _addr     the address of Relay
    */
    function addRelay(string memory _link, address _addr)
        external
        override
        hasPermission
    {
        requireValidAddress(_addr);
        string memory net = requireLink(_link);

        require(relayStats[_link][_addr].addr == address(0), BMCRevertAlreadyExistsBMR);
        relayStats[_link][_addr] = Types.RelayStats(_addr, 0, 0);

        relayMap[net].push(_addr);
    }

    /**
       @notice Unregisters Relay for the network.
       @dev Called by the Relay-Operator to manage the BTP network.
       @param _link     BTP Address of connected BMC
       @param _addr     the address of Relay
    */
    function removeRelay(string memory _link, address _addr)
        external
        override
        hasPermission
    {
        string memory net = requireLink(_link);

        require(relayStats[_link][_addr].addr == _addr, BMCRevertNotExistsBMR);
        delete relayStats[_link][_addr];
        //@Notice the order may be changed after remove
        //  arr[index of remove]=arr[last index]
        relayMap[net].removeFromAddresses(_addr);
    }

    /**
       @notice Get relays status by link.
       @param _link        BTP Address of the connected BMC.
       @return _relays Relay status of all relays
    */
    function getRelays(string memory _link)
        external
        view
        override
        returns (Types.RelayStats[] memory _relays)
    {
        string memory net = requireLink(_link);

        _relays = new Types.RelayStats[](relayMap[net].length);
        for (uint256 i = 0; i < _relays.length; i++) {
            _relays[i] = relayStats[_link][relayMap[net][i]];
        }
    }

    /**
        @notice Drop the next message that to be relayed from a specific network
        @dev Called by the operator to manage the BTP network.
        @param _src  String ( BTP Address of source BMC )
        @param _seq  Integer ( number of the message from connected BMC )
        @param _svc  String ( number of the message from connected BMC )
        @param _sn   Integer ( serial number of the message, must be positive )
     */
    function dropMessage(
        string calldata _src,
        uint256 _seq,
        string calldata _svc,
        uint256 _sn
    ) external override hasPermission {
        (string memory srcNet, ) = _src.splitBTPAddress();
        (string memory next, ) = resolveNext(srcNet);

        string memory net = requireLink(next);
        require(_seq == (linkMap[net].rxSeq + 1), BMCRevertInvalidSeq);
        require(bshMap[_svc] != address(0), BMCRevertNotExistsBSH);
        require(_sn > 0, BMCRevertInvalidSn);

        linkMap[net].rxSeq++;
        linkMap[net].txSeq++;
        IBMCPeriphery(bmcPeriphery).dropMessage(
            _src, next, linkMap[net].rxSeq, _svc, _sn, linkMap[net].txSeq);
    }

    /******************************* Use for BMC Service *************************************/
    function getBshServiceByName(string memory _serviceName)
        external
        view
        override
        returns (address)
    {
        return bshMap[_serviceName];
    }

    function getBmvServiceByNet(string memory _net)
        external
        view
        override
        returns (address)
    {
        return bmvMap[_net];
    }

    function getLink(string memory _to)
        external
        view
        override
        returns (Types.Link memory)
    {
        (string memory _net, ) = _to.splitBTPAddress();
        return linkMap[_net];
    }

    function isLinkRelay(string calldata _prev, address _addr)
        external
        view
        override
        returns (bool)
    {
        return relayStats[_prev][_addr].addr != address(0);
    }

    function updateLinkRxSeq(string memory net, uint256 _val)
        external
        override
        onlyBMCPeriphery
    {
        linkMap[net].rxSeq += _val;
    }

    function updateLinkTxSeq(string memory net)
        external
        override
        onlyBMCPeriphery
        returns (uint256)
    {
        linkMap[net].txSeq++;
        return linkMap[net].txSeq;
    }

    function setLinkReachable(string memory _net, string[] memory _reachable)
    external
    override
    onlyBMCPeriphery
    {
        reachableMap[_net] = _reachable;
    }

    function updateLinkReachable(string memory _net, string memory _reachable, bool _remove)
        external
        override
        onlyBMCPeriphery
    {
        if (_remove) {
            reachableMap[_net].removeFromStrings(_reachable);
        } else {
            if (!reachableMap[_net].containsFromStrings(_reachable)) {
                reachableMap[_net].push(_reachable);
            }
        }
    }

    function updateRelayStats(
        string memory _prev,
        address _addr,
        uint256 _blockCountVal,
        uint256 _msgCountVal
    ) external override onlyBMCPeriphery {
        relayStats[_prev][_addr].blockCount += _blockCountVal;
        relayStats[_prev][_addr].msgCount += _msgCountVal;
    }

    function resolveNext(string memory _dstNet)
    internal
    view
    returns (string memory, string memory)
    {
        // search in routeMap
        if (bytes(routeMap[_dstNet].dst).length > 0) {
            return (routeMap[_dstNet].next, routeMap[_dstNet].dst);
        }

        // search in linkMap
        if (linkMap[_dstNet].isConnected) {
            return (linkMap[_dstNet].btpAddress, linkMap[_dstNet].btpAddress);
        }

        // search in reachableMap
        for (uint i =0 ; i < linkList.length; i++) {
            (string memory net, ) = linkList[i].splitBTPAddress();
            for (uint j=0; j < reachableMap[net].length; j++) {
                (string memory rnet, ) = reachableMap[net][j].splitBTPAddress();
                if (rnet.compareTo(_dstNet)) {
                    return (linkList[i], reachableMap[net][j]);
                }
            }
        }
        revert(BMCRevertUnreachable);
    }

    function resolveRoute(string memory _dstNet)
        external
        view
        override
        onlyBMCPeriphery
        returns (string memory next, string memory dst)
    {
        return resolveNext(_dstNet);
    }
    /*******************************************************************************************/
}
