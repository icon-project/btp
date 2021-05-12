// SPDX-License-Identifier: MIT
pragma solidity >=0.5.0 <0.8.0;
pragma experimental ABIEncoderV2;
import "../NativeCoinBSH.sol";
import "../Interfaces/IBMC.sol";
import "../Interfaces/IBMV.sol";
import "../Libraries/ParseAddressLib.sol";
import "../Libraries/RLPEncodeStructLib.sol";
import "../Libraries/StringsLib.sol";
import "../Libraries/Owner.sol";
import "../Libraries/EncodeBase64Lib.sol";
import "../Libraries/DecodeBase64Lib.sol";
import "../Libraries/TypesLib.sol";

contract BMC is IBMC {
    using ParseAddress for address;
    using ParseAddress for string;
    using RLPEncodeStruct for Types.BMCMessage;
    using RLPEncodeStruct for Types.EventMessage;
    using Strings for string;

    uint256 internal constant RC_OK = 0;
    uint256 internal constant RC_ERR = 1;

    event Message(
        string _next, //  an address of the next BMC (it could be a destination BMC)
        uint256 _seq, //  a sequence number of BMC (NOT sequence number of BSH)
        bytes _msg
    );

    mapping(address => bool) private _owners;
    uint256 private numOfOwner;

    //  emit EVENT to announce link/unlink service
    event Event(string _next, uint256 _seq, bytes _msg);

    mapping(uint => Types.Request[]) private pendingReq;
    mapping(string => address) private bshServices;
    mapping(string => address) private bmvServices;
    mapping(address => Types.RelayStats) private relayStats;
    mapping(string => string) private routes;
    mapping(string => Types.Link) internal links; // should be private, temporarily set internal for testing
    string[] private listBMVNames;
    string[] private listBSHNames;
    string[] private listRouteKeys;
    string[] private listLinkNames;
    address[] private addrs;

    string private bmcAddress; // a network address BMV, i.e. btp://1234.pra
    uint256 private serialNo;
    uint256 private numOfBMVService;
    uint256 private numOfBSHService;
    uint256 private numOfLinks;
    uint256 private numOfRoutes;
    
    uint private constant BLOCK_INTERVAL_MSEC = 1000;

    modifier owner {
        require(_owners[msg.sender] == true, "BMCRevertUnauthorized");
        _;
    }

    constructor(string memory _network) {
        bmcAddress = _network.concat("/").concat(address(this).toString());
        _owners[msg.sender] = true;
        numOfOwner++;
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
    function addOwner(address _owner) external owner {
        _owners[_owner] = true;
        numOfOwner++;
    }

    /**
       @notice Removing an existing Owner.
       @dev Caller must be an Owner of BTP network
       @dev If only one Owner left, unable to remove the last Owner
       @param _owner    Address of an Owner to be removed.
   */
    function removeOwner(address _owner) external owner {
        require(numOfOwner > 1, "BMCRevertLastOwner");
        require(_owners[_owner] == true, "BMCRevertNotExistsPermission");
        delete _owners[_owner];
        numOfOwner--;
    }

    /**
       @notice Checking whether one specific address has Owner role.
       @dev Caller can be ANY
       @param _owner    Address needs to verify.
    */
    function isOwner(address _owner) external view returns (bool) {
        return _owners[_owner];
    }

    /*****************************************************************************************

    *****************************************************************************************/

    function requestAddService(string memory _serviceName, address _addr) external override {
        require(bshServices[_serviceName] == address(0), "BMCRevertAlreadyExistsBSH");
        for (uint i = 0; i < pendingReq[0].length; i++) {
            if (pendingReq[0][i].serviceName.compareTo(_serviceName)) {
                revert("BMCRevertRequestPending");
            }
        }
        pendingReq[0].push(
            Types.Request(
                _serviceName,
                _addr
            )
        );
    }

    function getPendingRequest() external view returns (Types.Request[] memory) {
        return pendingReq[0];
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
            "BMCRevertUnauthorized"
        );
        require(_sn >= 0, "BMCRevertInvalidSN");
        //  In case BSH sends a REQUEST_COIN_TRANSFER,
        //  but '_to' is a network which is not supported by BMC
        //  revert() therein
        if (bmvServices[_to] == address(0)) {
            revert("BMCRevertNotExistsBMV");
        }
        bytes memory _rlp =
            Types
                .BMCMessage(bmcAddress, _to, _svc, _sn, _msg)
                .encodeBMCMessage();
        if (_svc.compareTo("_EVENT")) {
            emit Event(_to, serialNo, _rlp);
        } else {
            emit Message(_to, serialNo, _rlp);
        }
        serialNo++;
    }

     /**
       @notice Registers the smart contract for the service.
       @dev Caller must be an operator of BTP network.
       @dev Service being approved must be in the pending request list
       @param _svc     Name of the service
   */
    function approveService(string memory _svc)
        public
        override
        owner
    {
        require(bshServices[_svc] == address(0), "BMCRevertAlreadyExistsBSH");
        bool foundReq = false;
        Types.Request[] memory temp = new Types.Request[](pendingReq[0].length);
        temp = pendingReq[0];
        delete pendingReq[0];
        address _addr;
        for (uint i = 0; i < temp.length; i++) {
            if (!temp[i].serviceName.compareTo(_svc)) {
                pendingReq[0].push(temp[i]);
            }else {
                foundReq = true;
                _addr = temp[i].bsh;
            }
        }
        //  If service not existed in a pending request list,
        //  then revert()
        require(foundReq == true, "BMCRevertNotExistRequest");

        bshServices[_svc] = _addr;
        listBSHNames.push(_svc);
        numOfBSHService++;
    }

    /**
       @notice Registers the smart contract for the service.
       @dev Caller must be an operator of BTP network.
       @param _svc     Name of the service
       @param _addr    Address of the smart contract handling the service
   */
    // function addService(string memory _svc, address _addr)
    //     public
    //     override
    //     owner
    // {
    //     require(bshServices[_svc] == address(0), "BSH service existed");
    //     bshServices[_svc] = _addr;
    //     listBSHNames.push(_svc);
    //     numOfBSHService++;
    // }

    /**
       @notice Unregisters the smart contract for the service.  
       @dev Caller must be an operator of BTP network.
       @param _svc     Name of the service
   */
    function removeService(string memory _svc) public override owner {
        require(bshServices[_svc] != address(0), "BMCRevertNotExistsBSH");
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
        require(bmvServices[_net] == address(0), "BMCRevertAlreadyExistsBMV");
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
        require(bmvServices[_net] != address(0), "BMCRevertNotExistsBMV");
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

    /**
       @notice Initializes status information for the link.
       @dev Caller must be an operator of BTP network.
       @param _link    BTP Address of connected BMC
   */
    function addLink(string calldata _link) public override owner {
        string memory _net;
        (_net, ) = _link.splitBTPAddress();
        require(bmvServices[_net] != address(0), "BMCRevertNotExistsBMV");
        require(links[_link].isConnected == false, "BMCRevertAlreadyExistsLink");
        links[_link] = Types.Link(new address[](0), new string[](0),0, 0, BLOCK_INTERVAL_MSEC, 0, 10, 3, 0, 0, 0, 0, true);
        listLinkNames.push(_link);
        numOfLinks++;

        //  propagate an event "LINK"
        propagateEvent("Link", _link);
    }

    /**
       @notice Removes the link and status information. 
       @dev Caller must be an operator of BTP network.
       @param _link    BTP Address of connected BMC
   */
    function removeLink(string calldata _link) public override owner {
        require(links[_link].isConnected == true, "BMCRevertNotExistsLink");
        delete links[_link];
        numOfLinks--;
        propagateEvent("Unlink", _link);
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

    function setLink(
        string memory _link,
        uint _blockInterval,
        uint _maxAggregation,
        uint _delayLimit
    ) 
        external
        override
        owner 
    {
        require(links[_link].isConnected == true, "BMCRevertNotExistsLink");
        require(_maxAggregation >= 1 && _delayLimit >= 1, "BMCRevertInvalidParam");
        Types.Link memory link = links[_link];
        uint _scale = getScale(link.blockIntervalSrc, link.blockIntervalDst);
        bool resetRotateHeight = false;
        if (getRotateTerm(link.maxAggregation, _scale) == 0) {
            resetRotateHeight = true;
        }
        link.blockIntervalDst = _blockInterval;
        link.maxAggregation = _maxAggregation;
        link.delayLimit = _delayLimit;

        _scale = getScale(link.blockIntervalSrc, _blockInterval);
        uint _rotateTerm = getRotateTerm(_maxAggregation, _scale);
        if (resetRotateHeight && _rotateTerm > 0) {
            link.rotateHeight = block.number + _rotateTerm;
            link.rxHeight = block.number;
            string memory _net;
            (_net, ) = _link.splitBTPAddress();
            (link.rxHeightSrc, , ) = IBMV(bmvServices[_net]).getStatus();
        }
        links[_link] = link;
    }

    function rotateRelay(
        string memory _link,
        uint _currentHeight,
        uint _relayMsgHeight,
        bool _hasMsg
    )
        internal
        returns (address) 
    {
        /*
            @dev Solidity does not support calculate rational numbers/floating numbers
            thus, a division of _blockIntervalSrc and _blockIntervalDst should be
            scaled by 10^6 to minimize proportional error
        */
        Types.Link memory link = links[_link];
        uint _scale = getScale(link.blockIntervalSrc, link.blockIntervalDst); 
        uint _rotateTerm = getRotateTerm(link.maxAggregation, _scale);
        uint _baseHeight; uint _rotateCount;
        if (_rotateTerm > 0) {
            if (_hasMsg) {
                //  Note that, Relay has to relay this event immediately to BMC
                //  upon receiving this event. However, Relay is allowed to hold
                //  no later than 'delay_limit'. Thus, guessHeight comes up
                //  Arrival time of BTP Message identified by a block height
                //  BMC starts guessing when an event of 'RelayMessage' was thrown by another BMC
                //  which is 'guessHeight' and the time BMC receiving this event is 'currentHeight'
                //  If there is any delay, 'guessHeight' is likely less than 'currentHeight'
                uint _guessHeight = link.rxHeight + ceilDiv(
                    (_relayMsgHeight - link.rxHeightSrc) * 10**6, _scale
                ) - 1;

                if (_guessHeight > _currentHeight) {
                    _guessHeight = _currentHeight;
                }
                //  Python implementation as:
                //  rotate_count = math.ceil((guess_height - self.rotate_height)/rotate_term)
                //  the following code re-write it with using unsigned integer
                if (_guessHeight < link.rotateHeight) {
                    _rotateCount = ceilDiv(
                        (link.rotateHeight - _guessHeight), _rotateTerm
                    ) - 1;
                }else {
                    _rotateCount = ceilDiv(
                        (_guessHeight - link.rotateHeight), _rotateTerm
                    );
                }
                //  No need to check this if using unsigned integer as above
                // if (_rotateCount < 0) {
                //     _rotateCount = 0;
                // }

                _baseHeight = link.rotateHeight + (
                    (_rotateCount - 1) * _rotateTerm
                );
                /*  Python implementation as:
                //  skip_count = math.ceil((current_height - guess_height)/self.delay_limit) - 1
                //  In case that 'current_height' = 'guess_height'
                //  it might have an error calculation if using unsigned integer
                //  Thus, 'skipCount - 1' is moved into if_statement
                //  For example:
                //     + 'currentHeight' = 'guessHeight' 
                //        => skipCount = 0 
                //        => no delay
                //     + 'currentHeight' > 'guessHeight' and 'currentHeight' - 'guessHeight' <= 'delay_limit' 
                //        => ceil(('currentHeight' - 'guessHeight') / 'delay_limit') = 1
                //        => skipCount = skipCount - 1 = 0
                //        => not out of 'delay_limit'
                //        => accepted
                //     + 'currentHeight' > 'guessHeight' and 'currentHeight' - 'guessHeight' > 'delay_limit'
                //        => ceil(('currentHeight' - 'guessHeight') / 'delay_limit') = 2
                //        => skipCount = skipCount - 1 = 1
                //        => out of 'delay_limit'
                //        => rejected and move to next Relay
                */
                uint _skipCount = ceilDiv(
                    (_currentHeight - _guessHeight), link.delayLimit
                );
  
                if (_skipCount > 0) {
                    _skipCount = _skipCount - 1;
                    _rotateCount += _skipCount;
                    _baseHeight = _currentHeight;
                }
                link.rxHeight = _currentHeight;
                link.rxHeightSrc = _relayMsgHeight;
                links[_link] = link;
            } else {
                if (_currentHeight < link.rotateHeight) {
                    _rotateCount = ceilDiv(
                        (link.rotateHeight - _currentHeight), _rotateTerm
                    ) - 1;
                } else {
                    _rotateCount = ceilDiv(
                        (_currentHeight - link.rotateHeight), _rotateTerm
                    );
                }
                _baseHeight = link.rotateHeight + (
                    (_rotateCount - 1) * _rotateTerm
                );
            }
            return rotate(
                _link,
                _rotateTerm,
                _rotateCount,
                _baseHeight
            );
        }
        return address(0);
    }

    function getScale(
        uint _blockIntervalSrc,
        uint _blockIntervalDst
    )
        private
        pure
        returns (uint) 
    {
        if (_blockIntervalSrc < 1 || _blockIntervalDst < 1) {
            return 0;
        }
        return ceilDiv(_blockIntervalSrc * 10**6, _blockIntervalDst);
    }

    function getRotateTerm(
        uint _maxAggregation,
        uint _scale
    )   
        private
        pure
        returns (uint) 
    {
        if (_scale > 0) {
            return ceilDiv(_maxAggregation * 10**6, _scale);
        }
        return 0;
    }

    function rotate(
        string memory _link,
        uint _rotateTerm,
        uint _rotateCount,
        uint _baseHeight
    )
        private
        returns (address) 
    {
        Types.Link memory link = links[_link];
        if (_rotateTerm > 0 && _rotateCount > 0) {
            link.rotateHeight = _baseHeight + _rotateTerm;
            link.relayIdx = link.relayIdx + _rotateCount;
            if (link.relayIdx >= link.relays.length) {
                link.relayIdx = link.relayIdx % link.relays.length;
            }
            links[_link] = link;
        }
        return link.relays[link.relayIdx];
    }

    /**
    @notice this function return a ceiling value of division
    @dev No need to check validity of num2 (num2 != 0)
    @dev It is checked before calling this function
    */
    function ceilDiv(uint num1, uint num2) private pure returns (uint) {
        if (num1 % num2 == 0) {
            return num1 / num2;
        }
        return (num1 / num2) + 1;
    }

   function propagateEvent(string memory _eventType, string calldata _link) private {
        string memory _net;
        for (uint i = 0; i < listLinkNames.length; i++) {
            if (links[listLinkNames[i]].isConnected) {
                (_net, ) = listLinkNames[i].splitBTPAddress();
                this.sendMessage(
                    _net,
                    "_EVENT",
                    0,
                    Types.EventMessage(
                        _eventType, 
                        Types.Connection(
                            bmcAddress,
                            _link
                        )
                    ).encodeEventMessage()
                );
            }
        }
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
        require(bytes(routes[_dst]).length == 0, "BTPRevertAlreadyExistRoute");
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
        require(bytes(routes[_dst]).length != 0, "BTPRevertNotExistRoute");
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
        require(links[_link].isConnected == true, "BMCRevertNotExistsLink");
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
            "BMCRevertUnauthorized"
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
            // uint256 txSEQ,
            // uint256 rxSEQ,
            // VerifierStatus memory verifierStatus,
            // RelayStats[] memory relaysStats,
            // bytes memory rotationDetails
            Types.LinkStats memory _linkStats
        )
    {
        require(links[_link].isConnected == true, "BMCRevertNotExistsLink");
        Types.Link memory link = links[_link];
        Types.RelayStats[] memory _relays = new Types.RelayStats[](link.relays.length);
        for (uint i = 0; i < link.relays.length; i++) {
            _relays[i] = relayStats[link.relays[i]];
        }
        string memory _net;
        (_net, ) = _link.splitBTPAddress();
        uint _height; uint _offset; uint _lastHeight;
        (_height, _offset, _lastHeight) = IBMV(bmvServices[_net]).getStatus();
        uint _rotateTerm = getRotateTerm(
            link.maxAggregation,
            getScale(link.blockIntervalSrc, link.blockIntervalDst)
        );
        return Types.LinkStats (
            link.rxSeq,
            link.txSeq,
            Types.VerifierStats(
                _height,
                _offset,
                _lastHeight,
                ""
            ),
            _relays,
            link.relayIdx,
            link.rotateHeight,
            _rotateTerm,
            link.delayLimit,
            link.maxAggregation,
            link.rxHeightSrc,
            link.rxHeight,
            link.blockIntervalSrc,
            link.blockIntervalDst,
            block.number
        );
    }
}
