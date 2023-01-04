// SPDX-License-Identifier: Apache-2.0
pragma solidity >=0.8.0;
pragma abicoder v2;

import "../interfaces/IBMCManagement.sol";
import "../interfaces/IOwnerManager.sol";
import "../interfaces/ICCManagement.sol";
import "../interfaces/IBMCPeriphery.sol";
import "../interfaces/ICCPeriphery.sol";
import "../interfaces/ICCService.sol";
import "../libraries/Types.sol";
import "../libraries/Errors.sol";
import "../libraries/BTPAddress.sol";
import "../libraries/Strings.sol";
import "../libraries/RLPEncodeStruct.sol";
import "../libraries/Utils.sol";

import "@openzeppelin/contracts-upgradeable/proxy/utils/Initializable.sol";

contract BMCManagementV2 is IBMCManagement, IOwnerManager, ICCManagement, Initializable {
    using BTPAddress for string;
    using Strings for string;
    using RLPEncodeStruct for Types.BMCMessage;
    using RLPEncodeStruct for Types.ResponseMessage;
    using RLPEncodeStruct for string[];
    using RLPEncodeStruct for string;
    using Utils for string[];
    using Utils for address[];
    using Utils for uint256[];

    mapping(address => bool) private _owners;
    uint256 private numOfOwner;

    mapping(string => address) private bmvMap;//net of bmv => address of BMV
    mapping(string => address) private bshMap;//svc of bsh => address of BSH
    mapping(string => Types.Link) private linkMap; //link => net of link
    mapping(string => address[]) private relayMap; //link => list of address of relay
    mapping(string => Types.RouteInfo) private routeInfoMap;//net of destination => RouteInfo
    mapping(string => uint256[]) private feeMap;//net of destination => list of fee
    string[] private bmvKeyList;//list of net of bmv
    string[] private bshKeyList;//list of svc of bsh
    string[] private linkList;//list of link
    string[] private routeInfoList; //list of destination of routeInfoMap
    address private feeHandler;
    address private bmcPeriphery;
    address private bmcService;

    function requireOwnerAccess(
    ) internal view {
        require(_owners[msg.sender] == true, Errors.BMC_REVERT_UNAUTHORIZED);
    }

    function requireValidAddress(
        address addr
    ) internal pure {
        require(addr != address(0), Errors.BMC_REVERT_INVALID_ARGUMENT);
    }

    function initialize(
    ) public initializer {
        _owners[msg.sender] = true;
        numOfOwner++;
    }

    /**
       @notice Update BMC periphery.
       @dev Caller must be an Owner of BTP network
       @param _addr    Address of a new periphery.
     */
    function setBMCPeriphery(
        address _addr
    ) external {
        requireOwnerAccess();
        requireValidAddress(_addr);
        bmcPeriphery = _addr;
    }

    /**
       @notice Get address of BMC periphery.
       @return address of BMC periphery
     */
    function getBMCPeriphery(
    ) external view returns (
        address
    ) {
        return bmcPeriphery;
    }

    /**
       @notice Update BMC periphery.
       @dev Caller must be an Owner of BTP network
       @param _addr    Address of a new periphery.
     */
    function setBMCService(
        address _addr
    ) external {
        requireOwnerAccess();
        requireValidAddress(_addr);
        bmcService = _addr;
    }

    /**
       @notice Get address of BMC periphery.
       @return address of BMC periphery
     */
    function getBMCService(
    ) external view returns (
        address
    ) {
        return bmcService;
    }

    /*****************************************************************************************
                                        Add Authorized Owner of Contract
        - addOwner(): register additional Owner of this Contract
        - removeOwner(): un-register existing Owner of this Contract. Unable to remove last
        - isOwner(): checking Ownership of an arbitrary address
    *****************************************************************************************/

    function addOwner(
        address _owner
    ) external override {
        requireOwnerAccess();
        requireValidAddress(_owner);
        require(_owners[_owner] == false, Errors.BMC_REVERT_ALREADY_EXISTS_OWNER);
        _owners[_owner] = true;
        numOfOwner++;
    }

    function removeOwner(
        address _owner
    ) external override {
        requireOwnerAccess();
        require(_owners[_owner] == true, Errors.BMC_REVERT_NOT_EXISTS_OWNER);
        require(numOfOwner > 1, Errors.BMC_REVERT_LAST_OWNER);
        delete _owners[_owner];
        numOfOwner--;
    }

    function isOwner(
        address _owner
    ) external view override returns (bool) {
        return _owners[_owner];
    }

    function addService(
        string memory _svc,
        address _addr
    ) external override {
        requireOwnerAccess();
        requireValidAddress(_addr);
        //TODO require(_svc.isAlphaNumeric && _svc != Types.BMC_SERVICE)
        require(!existsService(_svc), Errors.BMC_REVERT_ALREADY_EXISTS_BSH);

        bshMap[_svc] = _addr;
        bshKeyList.push(_svc);
    }

    function removeService(
        string memory _svc
    ) external override {
        requireOwnerAccess();
        requireService(_svc);
        delete bshMap[_svc];
        bshKeyList.removeFromStrings(_svc);
    }

    function getServices(
    ) external view override returns (
        Types.Service[] memory
    ){
        Types.Service[] memory services = new Types.Service[](bshKeyList.length);
        for (uint256 i = 0; i < bshKeyList.length; i++) {
            services[i] = Types.Service(
                bshKeyList[i],
                bshMap[bshKeyList[i]]
            );
        }
        return services;
    }

    function addVerifier(
        string memory _net,
        address _addr
    ) external override {
        requireOwnerAccess();
        requireValidAddress(_addr);
        require(!existsVerifier(_net), Errors.BMC_REVERT_ALREADY_EXISTS_BMV);
        bmvMap[_net] = _addr;
        bmvKeyList.push(_net);
    }

    function removeVerifier(
        string memory _net
    ) external override {
        requireOwnerAccess();
        requireVerifier(_net);
        delete bmvMap[_net];
        bmvKeyList.removeFromStrings(_net);
    }

    function getVerifiers(
    ) external view override returns (
        Types.Verifier[] memory
    ){
        Types.Verifier[] memory verifiers = new Types.Verifier[](bmvKeyList.length);
        for (uint256 i = 0; i < bmvKeyList.length; i++) {
            verifiers[i] = Types.Verifier(
                bmvKeyList[i],
                bmvMap[bmvKeyList[i]]
            );
        }
        return verifiers;
    }

    function addLink(
        string calldata _link
    ) external override {
        requireOwnerAccess();
        string memory net = _link.networkAddress();
        require(!existsLink(net), Errors.BMC_REVERT_ALREADY_EXISTS_LINK);
        requireVerifier(net);

        propagateInternal(
            Types.BMCMessage(Types.BMC_INTERNAL_LINK, _link.encodePropagateMessage())
            .encodeBMCMessage());
        bytes memory initMsg = Types.BMCMessage(
            Types.BMC_INTERNAL_INIT, linkList.encodeInitMessage()
        ).encodeBMCMessage();

        linkMap[_link] = Types.Link(_link, new string[](0));
        linkList.push(_link);
        _addRouteInfo(net, _link, Types.ROUTE_TYPE_LINK);

        sendInternal(_link, initMsg);
    }

    function removeLink(
        string calldata _link
    ) external override {
        requireOwnerAccess();
        requireLink(_link);
        for (uint256 i = 0; i < routeInfoList.length; i++) {
            if (routeInfoMap[routeInfoList[i]].routeType == Types.ROUTE_TYPE_MANUAL &&
                routeInfoMap[routeInfoList[i]].next.compareTo(_link)) {
                revert(Errors.BMC_REVERT_REFERRED_BY_ROUTE);
            }
        }

        _removeRouteInfo(_link.networkAddress(), false, "");

        //remove linkList before _removeRouteInfo(reachable)
        //linkList referred by _resolveNextInReachable in _removeRouteInfo
        linkList.removeFromStrings(_link);

        for (uint256 i = 0; i < linkMap[_link].reachable.length; i++) {
            _removeRouteInfo(linkMap[_link].reachable[i].networkAddress(), true, _link);
        }

        delete linkMap[_link];

        ICCPeriphery(bmcPeriphery).clearSeq(_link);
        delete relayMap[_link];

        propagateInternal(
            Types.BMCMessage(Types.BMC_INTERNAL_UNLINK, _link.encodePropagateMessage())
            .encodeBMCMessage());
    }

    function getLinks(
    ) external view override returns (
        string[] memory
    ) {
        return linkList;
    }

    function propagateInternal(
        bytes memory _msg
    ) private {
        for (uint256 i = 0; i < linkList.length; i++) {
            sendInternal(linkList[i], _msg);
        }
    }

    function sendInternal(
        string memory link,
        bytes memory _msg
    ) private {
        ICCPeriphery(bmcPeriphery).sendInternal(link, _msg);
    }

    function addRelay(
        string memory _link,
        address _addr
    ) external override {
        requireOwnerAccess();
        requireValidAddress(_addr);
        requireLink(_link);
        require(!relayMap[_link].containsFromAddresses(_addr), Errors.BMC_REVERT_ALREADY_EXISTS_BMR);
        relayMap[_link].push(_addr);
    }

    function removeRelay(
        string memory _link,
        address _addr
    ) external override {
        requireOwnerAccess();
        requireLink(_link);
        require(relayMap[_link].containsFromAddresses(_addr), Errors.BMC_REVERT_NOT_EXISTS_BMR);

        //@Notice the order may be changed after remove
        //  arr[index of remove]=arr[last index]
        relayMap[_link].removeFromAddresses(_addr);
    }

    function getRelays(
        string memory _link
    ) external view override returns (
        address[] memory
    ){
        requireLink(_link);
        return relayMap[_link];
    }

    function _addRouteInfo(
        string memory _dst,
        string memory _link,
        uint256 routeType
    ) internal {
        if(routeInfoMap[_dst].routeType == Types.ROUTE_TYPE_NONE) {
            routeInfoMap[_dst] = Types.RouteInfo(_dst, _link, 0, routeType);
            routeInfoList.push(_dst);
        }
        if (routeType == Types.ROUTE_TYPE_REACHABLE) {
            routeInfoMap[_dst].reachable++;
        } else if (routeType == Types.ROUTE_TYPE_MANUAL) {
            routeInfoMap[_dst].routeType = Types.ROUTE_TYPE_MANUAL;
            routeInfoMap[_dst].next = _link;
        }
    }

    function addRoute(
        string memory _dst,
        string memory _link
    ) external override {
        requireOwnerAccess();
        require(!_dst.compareTo(_link), Errors.BMC_REVERT_INVALID_ARGUMENT);
        require(routeInfoMap[_dst].routeType != Types.ROUTE_TYPE_MANUAL, Errors.BMC_REVERT_ALREADY_EXISTS_ROUTE);
        require(existsLink(_link), Errors.BMC_REVERT_NOT_EXISTS_LINK);

        _addRouteInfo(_dst, routeInfoMap[_link].next, Types.ROUTE_TYPE_MANUAL);
        //ignore shortest-path check
        //case : _link is not connected with _dst (3 hop) and other link connected with _dst (2 hop)
    }

    function _resolveNextInReachable(
        string memory _dst
    ) internal view returns (
        string memory
    ) {
        for(uint256 i = 0; i < linkList.length; i++) {
            for(uint256 j = 0; j < linkMap[linkList[i]].reachable.length; j++) {
                if (linkMap[linkList[i]].reachable[j].networkAddress().compareTo(_dst)) {
                    return linkList[i];
                }
            }
        }
        revert(Errors.BMC_REVERT_UNREACHABLE);
    }

    function _removeRouteInfo(
        string memory _dst,
        bool reachable,
        string memory _link
    ) internal {
        if (reachable) {
            routeInfoMap[_dst].reachable--;
        }
        if (routeInfoMap[_dst].reachable > 0) {
            if (!reachable || routeInfoMap[_dst].next.compareTo(_link)) {
                routeInfoMap[_dst].next = _resolveNextInReachable(_dst);
            }
            if (!reachable) {//call by removeRoute
                routeInfoMap[_dst].routeType = Types.ROUTE_TYPE_REACHABLE;
            }
        } else {
            routeInfoList.removeFromStrings(_dst);
            delete routeInfoMap[_dst];
            _removeFee(_dst);
        }
    }

    function removeRoute(
        string memory _dst
    ) external override {
        requireOwnerAccess();
        require(routeInfoMap[_dst].routeType == Types.ROUTE_TYPE_MANUAL, Errors.BMC_REVERT_NOT_EXISTS_ROUTE);
        _removeRouteInfo(_dst, false, "");
    }

    function getRoutes(
    ) external view override returns (
        Types.Route[] memory
    ){
        Types.Route[] memory _routes = new Types.Route[](routeInfoList.length);
        for (uint256 i = 0; i < routeInfoList.length; i++) {
            _routes[i] = Types.Route(routeInfoList[i],
                routeInfoMap[routeInfoList[i]].next.networkAddress());
        }
        return _routes;
    }

    function _removeFee(
        string memory dst
    ) internal {
        if (feeMap[dst].length > 0) {
            delete feeMap[dst];
        }
    }

    function setFeeTable(
        string[] memory _dst,
        uint256[][] memory _value
    ) external override {
        requireOwnerAccess();
        require(_dst.length == _value.length, Errors.BMC_REVERT_INVALID_ARGUMENT);
        for (uint256 i = 0; i < _dst.length; i++) {
            if (_value[i].length > 0) {
                require(_value[i].length % 2 == 0, Errors.BMC_REVERT_LENGTH_MUST_BE_EVEN);
                for (uint256 j = 0; j < _value[i].length; j++) {
                    require(_value[i][j] >= 0, Errors.BMC_REVERT_MUST_BE_POSITIVE);
                }
                if (_value[i].length == 2) {
                    require(existsLink(_dst[i]), Errors.BMC_REVERT_NOT_EXISTS_LINK);
                } else {
                    _resolveNext(_dst[i]);
                }
                feeMap[_dst[i]] = _value[i];
            } else {
                _removeFee(_dst[i]);
            }
        }
    }

    function getFeeTable(
        string[] calldata _dst
    ) external view override returns (
        uint256[][] memory _feeTable
    ) {
        uint256[][] memory ret = new uint256[][](_dst.length);
        for (uint256 i = 0; i < _dst.length; i++) {
            _resolveNext(_dst[i]);
            if (feeMap[_dst[i]].length > 0) {
                ret[i] = feeMap[_dst[i]];
            }
        }
        return ret;
    }

    function getFee(
        string calldata _to,
        bool _response
    ) external view override returns (
        uint256,
        uint256[] memory
    ) {
        uint256 len = feeMap[_to].length;
        if (!_response) {
            len = len / 2;
        }
        uint256 sum = 0;
        uint256[] memory values = new uint256[](len);
        if (len > 0) {
            for (uint256 i = 0; i < len; i++) {
                values[i] = feeMap[_to][i];
                sum += values[i];
            }
        }
        return (sum, values);
    }

    function setFeeHandler(
        address _addr
    ) external override {
        requireOwnerAccess();
        feeHandler = _addr;
    }

    function getFeeHandler(
    ) external view override returns (
        address
    ) {
        return feeHandler;
    }

    function dropMessage(
        string calldata _src,
        uint256 _seq,
        string calldata _svc,
        int256 _sn,
        int256 _nsn,
        string calldata  _feeNetwork,
        uint256[] memory _feeValues
    ) external override {
        requireOwnerAccess();
        string memory next = _resolveNext(_src);
        requireService(_svc);
        require(!((_nsn == 0) || (_nsn > 0 && _sn < 0) || (_nsn < 0 && _sn > 0)),
            Errors.BMC_REVERT_INVALID_SN);

        Types.BTPMessage memory btpMsg = Types.BTPMessage(
            _src,
            "",
            _svc,
            _sn,
            new bytes(0),
            _nsn,
            ICCService(bmcService).handleDropFee(_feeNetwork, _feeValues)
        );
        ICCPeriphery(bmcPeriphery).dropMessage(
            next,
            _seq,
            btpMsg
        );
    }

    function requireService(
        string memory _svc
    ) internal view {
        require(existsService(_svc), Errors.BMC_REVERT_NOT_EXISTS_BSH);
    }

    function existsService(
        string memory _svc
    ) internal view returns (
        bool
    ) {
        return bshMap[_svc] != address(0);
    }

    function getService(
        string memory _svc
    ) external view override returns (
        address
    ){
        requireService(_svc);
        return bshMap[_svc];
    }

    function requireVerifier(
        string memory _net
    ) internal view {
        require(existsVerifier(_net), Errors.BMC_REVERT_NOT_EXISTS_BMV);
    }

    function existsVerifier(
        string memory _net
    ) internal view returns (
        bool
    ){
        return bmvMap[_net] != address(0);
    }

    function getVerifier(
        string memory _net
    ) external view override returns (
        address
    ){
        requireVerifier(_net);
        return bmvMap[_net];
    }

    function requireLink(
        string memory _link
    ) internal view {
        require(bytes(linkMap[_link].btpAddress).length > 0, Errors.BMC_REVERT_NOT_EXISTS_LINK);
    }

    function existsLink(
        string memory net
    ) internal view returns (bool) {
        return routeInfoMap[net].routeType == Types.ROUTE_TYPE_LINK;
    }

    function isLinkRelay(
        string calldata _link,
        address _addr
    ) external view override returns (
        bool
    ){
        requireLink(_link);
        return relayMap[_link].containsFromAddresses(_addr);
    }

    function _resolveNext(
        string memory _dst
    ) internal view returns (
        string memory
    ){
        if (routeInfoMap[_dst].routeType != Types.ROUTE_TYPE_NONE) {
            return routeInfoMap[_dst].next;
        }
        revert(Errors.BMC_REVERT_UNREACHABLE);
    }

    function resolveNext(
        string memory _dst
    ) external view override returns (
        string memory
    ){
        return _resolveNext(_dst);
    }

    function addReachable(
        string memory _from,
        string memory _reachable
    ) external override {
        require(msg.sender == bmcService, Errors.BMC_REVERT_UNAUTHORIZED);
        linkMap[routeInfoMap[_from].next].reachable.push(_reachable);
        _addRouteInfo(_reachable.networkAddress(), routeInfoMap[_from].next, Types.ROUTE_TYPE_REACHABLE);
    }

    function removeReachable(
        string memory _from,
        string memory _reachable
    ) external override {
        require(msg.sender == bmcService, Errors.BMC_REVERT_UNAUTHORIZED);
        linkMap[routeInfoMap[_from].next].reachable.removeFromStrings(_reachable);
        _removeRouteInfo(_reachable.networkAddress(), true, routeInfoMap[_from].next);
    }

    function getHop(
        string memory _dst
    ) external view override returns (
        uint256
    ) {
        return feeMap[_dst].length/2;
    }
}
