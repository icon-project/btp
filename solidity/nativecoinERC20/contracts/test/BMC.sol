// SPDX-License-Identifier: Apache-2.0
pragma solidity >=0.5.0 <0.8.0;
pragma experimental ABIEncoderV2;
import "../interfaces/IBSH.sol";
import "../interfaces/IBMCPeriphery.sol";
import "../interfaces/IBMV.sol";
import "../libraries/ParseAddress.sol";
import "../libraries/RLPEncodeStruct.sol";
import "../libraries/RLPDecodeStruct.sol";
import "../libraries/String.sol";
import "../libraries/EncodeBase64.sol";
import "../libraries/DecodeBase64.sol";
import "../libraries/Types.sol";

contract BMC is IBMCPeriphery {
    using ParseAddress for address;
    using ParseAddress for string;
    using RLPDecodeStruct for bytes;
    using RLPEncodeStruct for Types.BMCMessage;
    using RLPEncodeStruct for Types.ServiceMessage;
    using RLPEncodeStruct for Types.EventMessage;
    using RLPEncodeStruct for Types.Response;
    using String for string;

    uint256 internal constant RC_OK = 0;
    uint256 internal constant RC_ERR = 1;

    event Message(
        string _next, //  an address of the next BMC (it could be a destination BMC)
        uint256 _seq, //  a sequence number of BMC (NOT sequence number of BSH)
        bytes _msg
    );

    //  emit EVENT to announce link/unlink service
    event Event(string _next, uint256 _seq, bytes _msg);

    event ErrorOnBTPError(
        string _svc,
        int256 _sn,
        uint256 _code,
        string _errMsg,
        uint256 _svcErrCode,
        string _svcErrMsg
    );

    mapping(address => bool) private _owners;
    uint256 private numOfOwner;

    mapping(string => address) internal bshServices;
    mapping(string => address) private bmvServices;
    mapping(string => string) private connectedBMC;
    mapping(address => Types.RelayStats) private relayStats;
    mapping(string => string) private routes;
    mapping(string => Types.Link) internal links; // should be private, temporarily set internal for testing
    mapping(string => string[]) private reachable;
    string[] private listBMVNames;
    string[] private listBSHNames;
    string[] private listRouteKeys;
    string[] private listLinkNames;
    address[] private addrs;

    string public bmcAddress; // a network address BMV, i.e. btp://1234.pra
    uint256 private numOfBMVService;
    uint256 private numOfBSHService;
    uint256 private numOfLinks;
    uint256 private numOfRoutes;

    uint256 private constant BLOCK_INTERVAL_MSEC = 1000;
    uint256 internal constant UNKNOWN_ERR = 0;
    uint256 internal constant BMC_ERR = 10;
    uint256 internal constant BMV_ERR = 25;
    uint256 internal constant BSH_ERR = 40;
    uint256 private constant DECIMAL_PRECISION = 10**6;

    modifier owner {
        require(_owners[msg.sender] == true, "BMCRevertUnauthorized");
        _;
    }

    constructor(string memory _network) {
        bmcAddress = string("btp://").concat(_network).concat("/").concat(
            address(this).toString()
        );
        _owners[msg.sender] = true;
        numOfOwner++;
    }

    /*****************************************************************************************

    *****************************************************************************************/
    function getBmcBtpAddress() external view override returns (string memory) {
        return bmcAddress;
    }

    function handleRelayMessage(string calldata _prev, string calldata _msg)
        external
        override
    {}

    function handleMessage(string calldata _prev, Types.BMCMessage memory _msg)
        internal
    {
        if (_msg.svc.compareTo("bmc")) {
            Types.BMCService memory _sm;
            try this.tryDecodeBMCService(_msg.message) returns (
                Types.BMCService memory ret
            ) {
                _sm = ret;
            } catch {
                _sendError(_prev, _msg, BMC_ERR, "BMCRevertParseFailure");
            }
            if (_sm.serviceType.compareTo("FeeGathering")) {
                Types.GatherFeeMessage memory _gatherFee;
                try this.tryDecodeGatherFeeMessage(_sm.payload) returns (
                    Types.GatherFeeMessage memory ret
                ) {
                    _gatherFee = ret;
                } catch {
                    _sendError(_prev, _msg, BMC_ERR, "BMCRevertParseFailure");
                }
                for (uint256 i = 0; i < _gatherFee.svcs.length; i++) {
                    //  If 'svc' not found, ignore
                    if (bshServices[_gatherFee.svcs[i]] != address(0)) {
                        try
                            IBSH(bshServices[_gatherFee.svcs[i]])
                                .handleFeeGathering(
                                _gatherFee.fa,
                                _gatherFee.svcs[i]
                            )
                        {} catch {
                            //  If BSH contract throws a revert error, ignore and continue
                        }
                    }
                }
            }
        } else {
            if (bshServices[_msg.svc] == address(0)) {
                _sendError(_prev, _msg, BMC_ERR, "BMCRevertNotExistsBSH");
                return;
            }

            if (_msg.sn >= 0) {
                (string memory _net, ) = _msg.src.splitBTPAddress();
                try
                    IBSH(bshServices[_msg.svc]).handleBTPMessage(
                        _net,
                        _msg.svc,
                        uint256(_msg.sn),
                        _msg.message
                    )
                {} catch Error(string memory _error) {
                    _sendError(_prev, _msg, BSH_ERR, _error);
                }
            } else {
                Types.Response memory _errMsg = _msg.message.decodeResponse();
                try
                    IBSH(bshServices[_msg.svc]).handleBTPError(
                        _msg.src,
                        _msg.svc,
                        uint256(int256(_msg.sn) * -1),
                        _errMsg.code,
                        _errMsg.message
                    )
                {} catch Error(string memory _error) {
                    emit ErrorOnBTPError(
                        _msg.svc,
                        int256(_msg.sn) * -1,
                        _errMsg.code,
                        _errMsg.message,
                        BSH_ERR,
                        _error
                    );
                } catch (bytes memory _error) {
                    emit ErrorOnBTPError(
                        _msg.svc,
                        int256(_msg.sn) * -1,
                        _errMsg.code,
                        _errMsg.message,
                        UNKNOWN_ERR,
                        string(_error)
                    );
                }
            }
        }
    }

    //  @dev Solidity does not allow using try_catch with internal function
    //  Thus, work-around solution is the followings
    //  If there is any error throwing, BMC contract can catch it, then reply back a RC_ERR Response
    function tryDecodeBMCService(bytes calldata _msg)
        external
        pure
        returns (Types.BMCService memory)
    {
        return _msg.decodeBMCService();
    }

    function tryDecodeGatherFeeMessage(bytes calldata _msg)
        external
        pure
        returns (Types.GatherFeeMessage memory)
    {
        return _msg.decodeGatherFeeMessage();
    }

    function _sendMessage(string memory _to, bytes memory _serializedMsg)
        internal
    {
        links[_to].txSeq += 1;
        emit Message(_to, links[_to].txSeq, _serializedMsg);
    }

    function _sendError(
        string calldata _prev,
        Types.BMCMessage memory _message,
        uint256 _errCode,
        string memory _errMsg
    ) internal {
        if (_message.sn > 0) {
            bytes memory _serializedMsg =
                Types
                    .BMCMessage(
                    bmcAddress,
                    _message
                        .src,
                    _message
                        .svc,
                    int256(_message.sn) * -1,
                    Types
                        .ServiceMessage(
                        Types
                            .ServiceType
                            .REPONSE_HANDLE_SERVICE,
                        Types.Response(_errCode, _errMsg).encodeResponse()
                    )
                        .encodeServiceMessage()
                )
                    .encodeBMCMessage();
            _sendMessage(_prev, _serializedMsg);
        }
    }

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
        string memory _toBMC = connectedBMC[_to];
        bytes memory _rlp =
            Types
                .BMCMessage(bmcAddress, _toBMC, _svc, int256(_sn), _msg)
                .encodeBMCMessage();
        if (_svc.compareTo("_EVENT")) {
            links[_toBMC].txSeq += 1;
            emit Event(_toBMC, links[_toBMC].txSeq, _rlp);
        } else {
            links[_toBMC].txSeq += 1;
            emit Message(_toBMC, links[_toBMC].txSeq, _rlp);
        }
    }

    /**
       @notice Add the smart contract for the service.
       @dev Caller must be an operator of BTP network.
       @param _svc     Name of the service
       @param _addr    Service's contract address
     */
    function addService(string memory _svc, address _addr) external owner {
        require(_addr != address(0), "BMCRevertInvalidAddress");
        require(bshServices[_svc] == address(0), "BMCRevertAlreadyExistsBSH");
        bshServices[_svc] = _addr;
        listBSHNames.push(_svc);
    }

    /**
       @notice Registers BMV for the network. 
       @dev Caller must be an operator of BTP network.
       @param _net     Network Address of the blockchain
       @param _addr    Address of BMV
   */
    function addVerifier(string memory _net, address _addr) external owner {
        require(bmvServices[_net] == address(0), "BMCRevertAlreadyExistsBMV");
        bmvServices[_net] = _addr;
        listBMVNames.push(_net);
        numOfBMVService++;
    }

    /**
       @notice Initializes status information for the link.
       @dev Caller must be an operator of BTP network.
       @param _link    BTP Address of connected BMC
   */
    function addLink(string calldata _link) external owner {
        string memory _net;
        (_net, ) = _link.splitBTPAddress();
        require(bmvServices[_net] != address(0), "BMCRevertNotExistsBMV");
        require(
            links[_link].isConnected == false,
            "BMCRevertAlreadyExistsLink"
        );
        links[_link] = Types.Link(
            new address[](0),
            0,
            0,
            BLOCK_INTERVAL_MSEC,
            0,
            10,
            3,
            0,
            0,
            0,
            0,
            true
        );
        listLinkNames.push(_link);
        connectedBMC[_net] = _link;
        numOfLinks++;

        //  propagate an event "LINK"
        propagateEvent("Link", _link);
    }

    function propagateEvent(string memory _eventType, string calldata _link)
        private
    {}

    /*
       @notice Get status of BMC.
       @param _link        BTP Address of the connected BMC.
       @return tx_seq       Next sequence number of the next sending message.
       @return rx_seq       Next sequence number of the message to receive.
       @return verifier     VerifierStatus Object contains status information of the BMV.
    */
    function getStatus(string calldata _link)
        external
        view
        override
        returns (Types.LinkStats memory _linkStats)
    {
        _linkStats.txSeq = links[_link].txSeq;
    }
}
