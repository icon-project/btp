// SPDX-License-Identifier: Apache-2.0
pragma solidity >=0.8.0;
pragma abicoder v2;

contract TestWeb3jABI {
    mapping(string => uint256[]) private map;

    function setWith2DArray(
        string[] memory _key,
        uint256[][] memory _value
    ) external {
        require(_key.length == _value.length, "invalid length");
        for (uint256 i = 0; i < _value.length; i++) {
            if (_value[i].length > 0) {
                map[_key[i]] = _value[i];
            } else {
                if (map[_key[i]].length > 0) {
                    delete map[_key[i]];
                }
            }
        }
    }

    function get2DArray(
        string[] memory _key
    ) external view returns (
        uint256[][] memory
    ) {
        uint256[][] memory _value = new uint256[][](_key.length);
        for (uint256 i = 0; i < _key.length; i++) {
            if (map[_key[i]].length > 0) {
                _value[i] = map[_key[i]];
            }
        }
        return _value;
    }
}
