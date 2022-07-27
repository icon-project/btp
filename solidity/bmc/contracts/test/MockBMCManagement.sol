// SPDX-License-Identifier: Apache-2.0
pragma solidity >=0.8.0 <0.8.5;
pragma abicoder v2;

import "../BMCManagement.sol";
import "../libraries/Utils.sol";

contract MockBMCManagement is BMCManagement {
    using Utils for uint256;

    struct RelayInfo {
        address r;
        uint256 cb;
        uint256 rh;
    }

    RelayInfo private relay;

    function _rotateRelay(
        string memory _link,
        uint256 _currentHeight,
        uint256 _relayMsgHeight,
        bool _hasMsg
    ) internal returns (address) {
        Types.Link memory link = links[_link];
        uint256 _scale = link.blockIntervalSrc.getScale(link.blockIntervalDst);
        uint256 _rotateTerm = link.maxAggregation.getRotateTerm(_scale);
        uint256 _baseHeight;
        uint256 _rotateCount;
        if (_rotateTerm > 0) {
            if (_hasMsg) {
                uint256 _guessHeight =
                    link.rxHeight +
                        uint256((_relayMsgHeight - link.rxHeightSrc) * 10**6)
                            .ceilDiv(_scale) -
                        1;

                if (_guessHeight > _currentHeight) {
                    _guessHeight = _currentHeight;
                }

                if (_guessHeight < link.rotateHeight) {
                    _rotateCount =
                        (link.rotateHeight - _guessHeight).ceilDiv(
                            _rotateTerm
                        ) -
                        1;
                } else {
                    _rotateCount = (_guessHeight - link.rotateHeight).ceilDiv(
                        _rotateTerm
                    );
                }

                _baseHeight =
                    link.rotateHeight +
                    ((_rotateCount - 1) * _rotateTerm);

                uint256 _skipCount =
                    (_currentHeight - _guessHeight).ceilDiv(link.delayLimit);

                if (_skipCount > 0) {
                    _skipCount = _skipCount - 1;
                    _rotateCount += _skipCount;
                    _baseHeight = _currentHeight;
                }
                link.rxHeight = _currentHeight;
                link.rxHeightSrc = _relayMsgHeight;
                links[_link] = link;
            } else {
                if (_currentHeight < link.rotateHeight) {
                    _rotateCount =
                        (link.rotateHeight - _currentHeight).ceilDiv(
                            _rotateTerm
                        ) -
                        1;
                } else {
                    _rotateCount = (_currentHeight - link.rotateHeight).ceilDiv(
                        _rotateTerm
                    );
                }
                _baseHeight =
                    link.rotateHeight +
                    ((_rotateCount - 1) * _rotateTerm);
            }
            return rotate(_link, _rotateTerm, _rotateCount, _baseHeight);
        }
        return address(0);
    }

    function relayRotation(
        string memory _link,
        uint256 _relayMsgHeight,
        bool hasMsg
    ) external {
        address r = _rotateRelay(_link, block.number, _relayMsgHeight, hasMsg);
        Types.Link memory link = links[_link];
        relay = RelayInfo(r, block.number, link.rotateHeight);
    }

    function getRelay() external view returns (RelayInfo memory) {
        return relay;
    }

    function mineOneBlock() external {}
}
