pragma solidity >=0.5.0 <0.8.0;
pragma experimental ABIEncoderV2;

import "@openzeppelin/contracts/token/ERC1155/ERC1155Holder.sol";
import "../NativeCoinBSH.sol";

contract Holder is ERC1155Holder {
    NativeCoinBSH private bsh;

    /**
     * @dev See {IERC165-supportsInterface}.
     */
    // function supportsInterface(bytes4 interfaceId)
    //     public
    //     view
    //     virtual
    //     override(ERC1155Receiver)
    //     returns (bool)
    // {
    //     return ERC1155Receiver.supportsInterface(interfaceId);
    // }

    function addBSHContract(address _bsh) external {
        bsh = NativeCoinBSH(_bsh);
    }

    function setApprove(address _operator) external {
        bsh.setApprovalForAll(_operator, true);
    }

    function callTransfer(
        string calldata _coinName,
        uint256 _value,
        string calldata _to
    ) external {
        bsh.transfer(_coinName, _value, _to);
    }
}
