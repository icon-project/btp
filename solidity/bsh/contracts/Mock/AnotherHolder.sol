pragma solidity >=0.5.0 <0.8.0;
pragma experimental ABIEncoderV2;

import "@openzeppelin/contracts/token/ERC1155/ERC1155Holder.sol";
import "./BSHPeripheryV1.sol";
import "./BSHCoreV1.sol";

contract AnotherHolder is ERC1155Holder {
    BSHPeripheryV1 private bshs;
    BSHCoreV1 private bshc;

    function addBSHContract(address _bshs, address _bshc) external {
        bshs = BSHPeripheryV1(_bshs);
        bshc = BSHCoreV1(_bshc);
    }

    function setApprove(address _operator) external {
        bshc.setApprovalForAll(_operator, true);
    }

    function callTransfer(
        string calldata _coinName,
        uint256 _value,
        string calldata _to
    ) external {
        bshc.transfer(_coinName, _value, _to);
    }
}
