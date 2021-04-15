pragma solidity >=0.5.0 <=0.8.0;

import "../NativeCoinBSH.sol";
import "./BMC.sol";

contract Mock is NativeCoinBSH, BMC {
    using RLPEncodeStruct for Types.ServiceMessage;
    using RLPEncodeStruct for Types.TransferCoin;
    using ParseAddress for address;

    constructor(
        string memory _network,
        string memory _serviceName,
        string memory _nativeCoinName,
        string memory _symbol,
        uint256 _decimal
    )
        NativeCoinBSH(
            address(this),
            _serviceName,
            _nativeCoinName,
            _symbol,
            _decimal
        )
        BMC(_network)
    {}

    function sendBTPMessage(
        string memory _from,
        string memory _svc,
        uint256 _sn,
        bytes memory _msg
    ) internal {
        this.handleBTPMessage(_from, _svc, _sn, _msg);
    }

    function setBalance(
        address _tokenContract,
        uint256 _id,
        uint256 _amt
    ) external {
        _mint(_tokenContract, _id, _amt, "");
    }

    function deposit() public payable {}

    function getBalance(address _addr) external view returns (uint256) {
        return _addr.balance;
    }

    function transferRequestWithAddress(
        string memory _net,
        string memory _svc,
        string memory _from,
        address _to,
        string memory _coinName,
        uint256 _value
    ) external {
        // string memory _net = "1234.iconee";
        // string memory _svc = "Token";
        sendBTPMessage(
            _net,
            _svc,
            0,
            Types
                .ServiceMessage(
                Types
                    .ServiceType
                    .REQUEST_COIN_TRANSFER,
                Types
                    .TransferCoin(_from, _to.toString(), _coinName, _value)
                    .encodeData()
            )
                .encodeServiceMessage()
        );
    }

    function transferRequestWithStringAddress(
        string memory _net,
        string memory _svc,
        string memory _from,
        string memory _to,
        string memory _coinName,
        uint256 _value
    ) external {
        // string memory _net = "1234.iconee";
        // string memory _svc = "Token";
        sendBTPMessage(
            _net,
            _svc,
            0,
            Types
                .ServiceMessage(
                Types
                    .ServiceType
                    .REQUEST_COIN_TRANSFER,
                Types.TransferCoin(_from, _to, _coinName, _value).encodeData()
            )
                .encodeServiceMessage()
        );
    }

    function hashCoinName(string memory _coinName)
        external
        view
        returns (uint256)
    {
        return uint256(keccak256(abi.encodePacked(_coinName)));
    }
}
