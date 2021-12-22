// SPDX-License-Identifier: Apache-2.0
pragma solidity >=0.5.0 <0.8.0;
pragma experimental ABIEncoderV2;

import "@openzeppelin/contracts/token/ERC1155/ERC1155Holder.sol";
import "../interfaces/IBSHPeriphery.sol";
import "../interfaces/IBSHCore.sol";
import "../libraries/String.sol";

contract Holder is ERC1155Holder {
    IBSHPeriphery private bshp;
    IBSHCore private bshc;
    using String for string;

    function deposit() external payable {}

    function addBSHContract(address _bshp, address _bshc) external {
        bshp = IBSHPeriphery(_bshp);
        bshc = IBSHCore(_bshc);
    }

    function callTransfer(
        string calldata _coinName,
        uint256 _value,
        string calldata _to
    ) external {
        bshc.transferWrappedCoin(_coinName, _value, _to);
    }

    // function isSendingNative(string[] memory _coinNames)
    //     private
    //     pure
    //     returns (int256)
    // {
    //     for (uint256 i = 0; i < _coinNames.length; i++) {
    //         if (_coinNames[i].compareTo("PARA")) {
    //             return int256(i);
    //         }
    //     }
    //     return -1;
    // }
}
