// SPDX-License-Identifier: Apache-2.0
pragma solidity >=0.8.0 <0.8.5;
pragma abicoder v2;

import "./interfaces/IBMV.sol";
import "./interfaces/IMockBMV.sol";
import "./libraries/RLPEncode.sol";
import "./libraries/RLPDecode.sol";
import "./libraries/Integers.sol";
import "./libraries/Strings.sol";

contract MockBMV is IBMV, IMockBMV {
    uint internal constant BMV_REVERT_OFFSET = 25;

    using Integers for uint;
    using Strings for string;

    uint256 private height;
    uint256 private offset;
    uint256 private lastHeight;

    function handleRelayMessage(
        string memory,
        string memory,
        uint256,
        bytes calldata _msg
    ) external override returns (
        bytes[] memory
    ) {
        MockRelayMessage memory rm = decodeMockRelayMessage(_msg);
        if (!rm.revertCode.isNull) {
            uint revertCode = BMV_REVERT_OFFSET + uint(rm.revertCode.value);
            revert(revertCode.toString().concat(":").concat(rm.revertMessage));
        } else {
            if (!rm.offset.isNull) {
                offset = uint256(rm.offset.value);
            }
            if (!rm.height.isNull) {
                height = uint256(rm.height.value);
            }
            if (!rm.lastHeight.isNull) {
                lastHeight = uint256(rm.lastHeight.value);
            }
            emit HandleRelayMessage(encodeBytesArray(rm.btpMessages));
            return rm.btpMessages;
        }
    }

    function getStatus(
    ) external view override returns (
        IBMV.VerifierStatus memory
    ) {
        return IBMV.VerifierStatus(height, encodeExtra(offset, lastHeight));
    }

    function setHeight(
        uint256 _height
    ) external override {
        height = _height;
    }

    function setOffset(
        uint256 _offset
    ) external override {
        offset = _offset;
    }

    function setLastHeight(
        uint256 _lastHeight
    ) external override {
        lastHeight = _lastHeight;
    }

    struct NullableInteger {
        int256 value;
        bool isNull;
    }

    function decodeNullableInteger(
        RLPDecode.RLPItem memory item
    ) internal pure returns (
        NullableInteger memory
    ) {
        if (RLPDecode.isNull(item)) {
            return NullableInteger(0, true);
        } else {
            return NullableInteger(RLPDecode.toInt(item), false);
        }
    }

    struct MockRelayMessage {
        NullableInteger offset;
        NullableInteger height;
        NullableInteger lastHeight;
        bytes[] btpMessages;
        NullableInteger revertCode;
        string revertMessage;
    }

    function decodeMockRelayMessage(
        bytes memory _rlp
    ) internal pure returns (
        MockRelayMessage memory
    ) {
        RLPDecode.RLPItem[] memory l = RLPDecode.toList(RLPDecode.toRlpItem(_rlp));
        return MockRelayMessage(
            decodeNullableInteger(l[0]),
            decodeNullableInteger(l[1]),
            decodeNullableInteger(l[2]),
            decodeBytesArray(l[3]),
            decodeNullableInteger(l[4]),
            string(RLPDecode.toBytes(l[5]))
        );
    }

    function decodeBytesArray(
        RLPDecode.RLPItem memory item
    ) internal pure returns (
        bytes[] memory
    ) {
        RLPDecode.RLPItem[] memory l = RLPDecode.toList(item);
        bytes[] memory bytesArray = new bytes[](l.length);
        for (uint256 i=0; i<l.length; i++) {
            bytesArray[i] = RLPDecode.toBytes(l[i]);
        }
        return bytesArray;
    }

    function encodeBytesArray(
        bytes[] memory bytesArray
    ) internal pure returns (
        bytes memory
    ) {
        bytes[] memory l = new bytes[](bytesArray.length);
        for (uint256 i = 0; i < l.length; i++) {
            l[i] = RLPEncode.encodeBytes(bytesArray[i]);
        }
        return RLPEncode.encodeList(l);
    }

    function encodeExtra(
        uint256 _offset,
        uint256 _lastHeight
    ) internal pure returns (
        bytes memory
    ) {
        bytes[] memory l = new bytes[](2);
        l[0] = RLPEncode.encodeUint(_offset);
        l[1] = RLPEncode.encodeUint(_lastHeight);
        return RLPEncode.encodeList(l);
    }

}
