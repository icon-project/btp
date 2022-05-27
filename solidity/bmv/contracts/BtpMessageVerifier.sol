// SPDX-License-Identifier: MIT
pragma solidity >=0.4.22 <0.9.0;

import "./interfaces/IBtpMessageVerifier.sol";
import "./RelayMessageLib.sol";

contract BtpMessageVerifier is IBtpMessageVerifier {

    using MessageProofLib for MessageProofLib.MessageProof;
    using RelayMessageLib for RelayMessageLib.RelayMessage;

    address public immutable bmc;
    address[] private validators;
    bytes private prevNetworkSectionHash;

    constructor(address _bmc, address[] memory _validators) {
        bmc = _bmc;
        for (uint i = 0; i < _validators.length; i++) {
            validators.push(_validators[i]);
        }
    }

    modifier onlyBmc() {
        require(
            msg.sender == bmc,
            "Function must be called through known bmc"
        );
        _;
    }

    function getStatus() external pure returns (uint256) {
        return 1;
    }

    function handleRelayMessage(
        string memory _bmc,
        string memory _prev,
        uint256 _seq,
        bytes memory _msg
    ) external returns (bytes[] memory) {
        RelayMessageLib.RelayMessage[] memory rms = RelayMessageLib.decode(_msg);
        for (uint i = 0; i < rms.length; i++) {
            if (rms[i].typ == RelayMessageLib.TypeBlockUpdate) {
                BlockUpdateLib.BlockUpdate memory bu = rms[i].toBlockUpdate();
                // TODO
            } else if (rms[i].typ == RelayMessageLib.TypeMessageProof) {
                MessageProofLib.MessageProof memory mp = rms[i].toMessageProof();
                // TODO
            }
        }
    }

}

