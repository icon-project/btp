// SPDX-License-Identifier: Apache-2.0
pragma solidity >=0.8.0;

/**
   BTPAddress 'btp://NETWORK_ADDRESS/ACCOUNT_ADDRESS'
*/
library BTPAddress {
    bytes internal constant PREFIX = bytes("btp://");
    string internal constant REVERT = "invalidBTPAddress";
    bytes internal constant DELIMITER = bytes("/");

    /**
       @notice Parse BTP address
       @param _str (String) BTP address
       @return (String) network address
       @return (String) account address
    */
    function parseBTPAddress(
        string memory _str
    ) internal pure returns (
        string memory,
        string memory
    ) {
        uint256 offset = _validate(_str);
        return (_slice(_str, 6, offset),
        _slice(_str, offset+1, bytes(_str).length));
    }

    /**
       @notice Gets network address of BTP address
       @param _str (String) BTP address
       @return (String) network address
    */
    function networkAddress(
        string memory _str
    ) internal pure returns (
        string memory
    ) {
        return _slice(_str, 6, _validate(_str));
    }

    function _validate(
        string memory _str
    ) private pure returns (
        uint256 offset
    ){
        bytes memory _bytes = bytes(_str);

        uint256 i = 0;
        for (; i < 6; i++) {
            if (_bytes[i] != PREFIX[i]) {
                revert(REVERT);
            }
        }
        for (; i < _bytes.length; i++) {
            if (_bytes[i] == DELIMITER[0]) {
                require(i > 6 && i < (_bytes.length -1), REVERT);
                return i;
            }
        }
        revert(REVERT);
    }

    function _slice(
        string memory _str,
        uint256 _from,
        uint256 _to
    ) private pure returns (
        string memory
    ) {
        //If _str is calldata, could use slice
        //        return string(bytes(_str)[_from:_to]);
        bytes memory _bytes = bytes(_str);
        bytes memory _ret = new bytes(_to - _from);
        uint256 j = _from;
        for (uint256 i = 0; i < _ret.length; i++) {
            _ret[i] = _bytes[j++];
        }
        return string(_ret);
    }

    /**
       @notice Create BTP address by network address and account address
       @param _net (String) network address
       @param _addr (String) account address
       @return (String) BTP address
    */
    function btpAddress(
        string memory _net,
        string memory _addr
    ) internal pure returns (
        string memory
    ) {
        return string(abi.encodePacked(PREFIX, _net, DELIMITER, _addr));
    }

}
