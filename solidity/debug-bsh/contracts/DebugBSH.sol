// SPDX-License-Identifier: MIT
pragma solidity >=0.4.22 <0.9.0;

import "./interfaces/IBSH.sol";
import "./interfaces/IBtpMessageCenter.sol";

contract DebugBSH is IBSH {

    address private bmc;
    string private svc;
    uint private sn;

    event SendMessage(address _bmc, string _svc, string _to, uint _sn, bytes _msg);
    event HandleMessageSuccess(string _from, string _svc, uint _sn, bytes _msg);
    event HandleMessageFailure(string _from, string _svc, uint _sn, uint _code, string _msg);
    event HandleFeeGathering(string _fa, string _svc);

    constructor(address _bmc, string memory _svc) {
        bmc = _bmc;
        svc = _svc;
        sn = 0;
    }

    function sendRawMessage(
        address _bmc,
        string memory _to,
        string memory _svc,
        uint _sn,
        bytes memory _msg
    )
    external
    {
        IBtpMessageCenter ins = IBtpMessageCenter(_bmc);
        ins.sendMessage(_to, _svc, _sn, _msg);
        emit SendMessage(_bmc, _to, _svc, _sn, _msg);
    }

    function sendMessage(string memory _to, bytes memory _msg) external {
        this.sendRawMessage(bmc, _to, svc, sn++, _msg);
    }

    function handleBTPMessage(
        string calldata _from,
        string calldata _svc,
        uint256 _sn,
        bytes calldata /* _msg */
    )
    external
    override
    {
        IBtpMessageCenter ins = IBtpMessageCenter(bmc);
        ins.sendMessage(
            _from,
            _svc,
            _sn,
            "DebugBSH: Success"
        );
    }

    function handleBTPError(
        string calldata _from,
        string calldata _svc,
        uint256 _sn,
        uint256 /* _code */,
        string calldata /* _msg */
    )
    external
    override
    {
        IBtpMessageCenter ins = IBtpMessageCenter(bmc);
        ins.sendMessage(
            _from,
            _svc,
            _sn,
            "DebugBSH: Failure"
        );
    }

    function handleFeeGathering(string calldata _fa, string calldata _svc) external override     {
        emit HandleFeeGathering(_fa, _svc);
    }

}
