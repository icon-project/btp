// SPDX-License-Identifier: Apache-2.0
pragma solidity >=0.8.0 <0.8.5;
pragma abicoder v2;

import "./interfaces/IBMCManagement.sol";
import "./interfaces/IBMCPeriphery.sol";
import "./interfaces/IBMV.sol";
import "./libraries/ParseAddress.sol";
import "./libraries/RLPEncode.sol";
import "./libraries/RLPEncodeStruct.sol";
import "./libraries/String.sol";
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
    using String for string;
    using Utils for uint256;
    using Utils for string[];

    mapping(address => bool) private _owners;
    uint256 private numOfOwner;

    mapping(string => address) private bshServices;
    mapping(string => address) private bmvServices;
    mapping(address => Types.RelayStats) private relayStats;
    mapping(string => string) private routes;
    mapping(string => Types.Link) internal links; // should be private, temporarily set internal for testing
    string[] private listBMVNames;
    string[] private listBSHNames;
    string[] private listRouteKeys;
    string[] private listLinkNames;
    address private bmcPeriphery;

    uint256 public serialNo;

    address[] private addrs;

    // Use to search by substring
    mapping(string => string) private getRouteDstFromNet;
    mapping(string => string) private getLinkFromNet;
    mapping(string => Types.Tuple) private getLinkFromReachableNet;

    uint256 private constant BLOCK_INTERVAL_MSEC = 1000;

    modifier hasPermission() {
        require(_owners[msg.sender] == true, "Unauthorized");
        _;
    }

    modifier onlyBMCPeriphery() {
        require(msg.sender == bmcPeriphery, "Unauthorized");
        _;
    }

    function initialize() public initializer {
        _owners[msg.sender] = true;
        numOfOwner++;
    }

    function setBMCPeriphery(address _addr) external override hasPermission {
        require(_addr != address(0), "InvalidAddress");
        require(_addr != bmcPeriphery, "AlreadyExistsBMCPeriphery");
        bmcPeriphery = _addr;
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
        require(numOfOwner > 1, "LastOwner");
        require(_owners[_owner] == true, "NotExistsPermission");
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
        require(_addr != address(0), "InvalidAddress");
        require(bshServices[_svc] == address(0), "AlreadyExistsBSH");
        bshServices[_svc] = _addr;
        listBSHNames.push(_svc);
    }

    /**
       @notice Unregisters the smart contract for the service.
       @dev Caller must be an operator of BTP network.
       @param _svc     Name of the service
   */
    function removeService(string memory _svc) external override hasPermission {
        require(bshServices[_svc] != address(0), "NotExistsBSH");
        delete bshServices[_svc];
        listBSHNames.remove(_svc);
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
            listBSHNames.length
        );
        for (uint256 i = 0; i < listBSHNames.length; i++) {
            services[i] = Types.Service(
                listBSHNames[i],
                bshServices[listBSHNames[i]]
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
        require(bmvServices[_net] == address(0), "AlreadyExistsBMV");
        bmvServices[_net] = _addr;
        listBMVNames.push(_net);
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
        require(bmvServices[_net] != address(0), "NotExistsBMV");
        delete bmvServices[_net];
        listBMVNames.remove(_net);
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
            new Types.Verifier[](listBMVNames.length);

        for (uint256 i = 0; i < listBMVNames.length; i++) {
            verifiers[i] = Types.Verifier(
                listBMVNames[i],
                bmvServices[listBMVNames[i]]
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
        (string memory _net, ) = _link.splitBTPAddress();
        require(bmvServices[_net] != address(0), "BMCRevertNotExistsBMV");
        require(
            links[_link].isConnected == false,
            "AlreadyExistsLink"
        );
        links[_link] = Types.Link(
            new address[](0),
            new string[](0),
            0,
            0,
            true
        );

        // propagate an event "LINK"
        propagateInternal("Link", _link);

        string[] memory _links = listLinkNames;

        listLinkNames.push(_link);
        getLinkFromNet[_net] = _link;

        // init link
        sendInternal(_link, "Init", _links);
    }

    /**
       @notice Removes the link and status information.
       @dev Caller must be an operator of BTP network.
       @param _link    BTP Address of connected BMC
   */
    function removeLink(string calldata _link) external override hasPermission {
        require(links[_link].isConnected == true, "BMCRevertNotExistsLink");
        delete links[_link];
        (string memory _net, ) = _link.splitBTPAddress();
        delete getLinkFromNet[_net];
        propagateInternal("Unlink", _link);
        listLinkNames.remove(_link);
    }

    /**
       @notice Get registered links.
       @return _links   An array of links ( BTP Addresses of the BMCs ).
    */
    function getLinks() external view override returns (string[] memory) {
        return listLinkNames;
    }

    function propagateInternal(
        string memory _serviceType,
        string calldata _link
    ) private {
        bytes memory _rlpBytes;
        _rlpBytes = abi.encodePacked(_rlpBytes, _link.encodeString());

        _rlpBytes = abi.encodePacked(
            _rlpBytes.length.addLength(false),
            _rlpBytes
        );

        // encode payload
        _rlpBytes = abi
            .encodePacked(_rlpBytes.length.addLength(false), _rlpBytes)
            .encodeBytes();

        for (uint256 i = 0; i < listLinkNames.length; i++) {
            if (links[listLinkNames[i]].isConnected) {
                (string memory _net, ) = listLinkNames[i].splitBTPAddress();
                IBMCPeriphery(bmcPeriphery).sendMessage(
                    _net,
                    "bmc",
                    0,
                    Types.BMCService(_serviceType, _rlpBytes).encodeBMCService()
                );
            }
        }
    }

    function sendInternal(
        string memory _target,
        string memory _serviceType,
        string[] memory _links
    ) private {
        bytes memory _rlpBytes;
        if (_links.length == 0) {
            _rlpBytes = abi.encodePacked(RLPEncodeStruct.LIST_SHORT_START);
        } else {
            for (uint256 i = 0; i < _links.length; i++)
                _rlpBytes = abi.encodePacked(
                    _rlpBytes,
                    _links[i].encodeString()
                );
            // encode target's reachable list
            _rlpBytes = abi.encodePacked(
                _rlpBytes.length.addLength(false),
                _rlpBytes
            );
        }
        // encode payload
        _rlpBytes = abi
            .encodePacked(_rlpBytes.length.addLength(false), _rlpBytes)
            .encodeBytes();

        (string memory _net, ) = _target.splitBTPAddress();
        IBMCPeriphery(bmcPeriphery).sendMessage(
            _net,
            "bmc",
            0,
            Types.BMCService(_serviceType, _rlpBytes).encodeBMCService()
        );
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
        require(bytes(routes[_dst]).length == 0, "AlreadyExistRoute");
        //  Verify _dst and _link format address
        //  these two strings must follow BTP format address
        //  If one of these is failed, revert()
        (string memory _net, ) = _dst.splitBTPAddress();
        _link.splitBTPAddress();

        routes[_dst] = _link; //  map _dst to _link
        listRouteKeys.push(_dst); //  push _dst key into an array of route keys
        getRouteDstFromNet[_net] = _dst;
    }

    /**
       @notice Remove route to the BMC.
       @dev Caller must be an operator of BTP network.
       @param _dst     BTP Address of the destination BMC
    */
    function removeRoute(string memory _dst) external override hasPermission {
        //  @dev No need to check if _dst is a valid BTP format address
        //  since it was checked when adding route at the beginning
        //  If _dst does not match, revert()
        require(bytes(routes[_dst]).length != 0, "NotExistRoute");
        delete routes[_dst];
        (string memory _net, ) = _dst.splitBTPAddress();
        delete getRouteDstFromNet[_net];
        listRouteKeys.remove(_dst);
    }

    /**
       @notice Get routing information.
       @return _routes An array of Route.
    */
    function getRoutes() external view override returns (Types.Route[] memory) {
        Types.Route[] memory _routes = new Types.Route[](listRouteKeys.length);
        for (uint256 i = 0; i < listRouteKeys.length; i++) {
            _routes[i] = Types.Route(
                listRouteKeys[i],
                routes[listRouteKeys[i]]
            );
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
        external
        override
        hasPermission
    {
        require(links[_link].isConnected == true, "NotExistsLink");
        links[_link].relays = _addr;
        for (uint256 i = 0; i < _addr.length; i++)
            relayStats[_addr[i]] = Types.RelayStats(_addr[i], 0, 0);
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
        require(
            links[_link].isConnected == true && links[_link].relays.length != 0,
            "Unauthorized"
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
        external
        view
        override
        returns (address[] memory)
    {
        return links[_link].relays;
    }

    /******************************* Use for BMC Service *************************************/
    function getBshServiceByName(string memory _serviceName)
        external
        view
        override
        returns (address)
    {
        return bshServices[_serviceName];
    }

    function getBmvServiceByNet(string memory _net)
        external
        view
        override
        returns (address)
    {
        return bmvServices[_net];
    }

    function getLink(string memory _to)
        external
        view
        override
        returns (Types.Link memory)
    {
        return links[_to];
    }

    function getLinkRxSeq(string calldata _prev)
        external
        view
        override
        returns (uint256)
    {
        return links[_prev].rxSeq;
    }

    function getLinkTxSeq(string calldata _prev)
        external
        view
        override
        returns (uint256)
    {
        return links[_prev].txSeq;
    }

    function getLinkRelays(string calldata _prev)
        external
        view
        override
        returns (address[] memory)
    {
        return links[_prev].relays;
    }

    function isLinkRelay(string calldata _prev, address _addr)
        external
        view
        override
        returns (bool)
    {
        address[] memory relays = links[_prev].relays;
        for (uint256 i = 0; i < relays.length; i++) {
            if (_addr == relays[i]) {
                return true;
            }
        }
        return false;
    }

    function getRelayStatusByLink(string memory _prev)
        external
        view
        override
        returns (Types.RelayStats[] memory _relays)
    {
        _relays = new Types.RelayStats[](links[_prev].relays.length);
        for (uint256 i = 0; i < links[_prev].relays.length; i++) {
            _relays[i] = relayStats[links[_prev].relays[i]];
        }
    }

    //todo: commented temp         //onlyBMCPeriphery
    function updateLinkRxSeq(string calldata _prev, uint256 _val)
        external
        override
        onlyBMCPeriphery
    {
        links[_prev].rxSeq += _val;
    }

    function updateLinkTxSeq(string memory _prev)
        external
        override
        onlyBMCPeriphery
    {
        links[_prev].txSeq++;
    }

    function updateLinkReachable(string memory _prev, string[] memory _to)
        external
        override
        onlyBMCPeriphery
    {
        for (uint256 i = 0; i < _to.length; i++) {
            links[_prev].reachable.push(_to[i]);
            (string memory _net, ) = _to[i].splitBTPAddress();
            getLinkFromReachableNet[_net] = Types.Tuple(_prev, _to[i]);
        }
    }

    function deleteLinkReachable(string memory _prev, uint256 _index)
        external
        override
        onlyBMCPeriphery
    {
        (string memory _net, ) = links[_prev]
            .reachable[_index]
            .splitBTPAddress();
        delete getLinkFromReachableNet[_net];
        delete links[_prev].reachable[_index];
        links[_prev].reachable[_index] = links[_prev].reachable[
            links[_prev].reachable.length - 1
        ];
        links[_prev].reachable.pop();
    }

    function updateRelayStats(
        address relay,
        uint256 _blockCountVal,
        uint256 _msgCountVal
    ) external override onlyBMCPeriphery {
        relayStats[relay].blockCount += _blockCountVal;
        relayStats[relay].msgCount += _msgCountVal;
    }

    function resolveRoute(string memory _dstNet)
        external
        view
        override
        onlyBMCPeriphery
        returns (string memory, string memory)
    {
        // search in routes
        string memory _dst = getRouteDstFromNet[_dstNet];
        if (bytes(_dst).length != 0) return (routes[_dst], _dst);

        // search in links
        _dst = getLinkFromNet[_dstNet];
        if (bytes(_dst).length != 0) return (_dst, _dst);

        // search link by reachable net
        Types.Tuple memory res = getLinkFromReachableNet[_dstNet];

        require(
            bytes(res._to).length > 0,
            string("Unreachable: ").concat(_dstNet).concat(
                " is unreachable"
            )
        );
        return (res._prev, res._to);
    }
    /*******************************************************************************************/
}
