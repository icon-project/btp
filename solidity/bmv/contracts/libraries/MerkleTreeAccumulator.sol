// SPDX-License-Identifier: Apache-2.0
pragma solidity >=0.5.0 <0.8.0;
pragma experimental ABIEncoderV2;

import "./RLPDecode.sol";
import "./RLPEncode.sol";
import "./Bytes.sol";
import "./Hash.sol";

library MerkleTreeAccumulator {
    using RLPEncode for bytes;
    using RLPEncode for uint256;
    using RLPEncode for bool;

    using RLPDecode for RLPDecode.RLPItem;
    using RLPDecode for bytes;

    using Bytes for bytes;

    using Hash for bytes;

    struct MTA {
        uint256 height;
        bytes32[] roots;
        uint256 offset;
        uint256 rootsSize;
        uint256 cacheSize;
        bytes32[] cache;
        bool isAllowNewerWitness;
    }

    function initFromSerialized(MTA storage mta, bytes memory rlpBytes)
        internal
    {
        RLPDecode.RLPItem[] memory unpacked;
        RLPDecode.RLPItem[] memory serializedRoots;

        if (rlpBytes.length != 0) {
            unpacked = rlpBytes.toRlpItem().toList();
        }

        if (unpacked.length > 0) mta.height = unpacked[0].toUint();

        if (unpacked.length > 1) {
            serializedRoots = unpacked[1].toList();
            for (uint256 i = 0; i < serializedRoots.length; i++) {
                mta.roots.push(serializedRoots[i].toBytes().bytesToBytes32());
            }
        }

        if (unpacked.length > 2) mta.offset = unpacked[2].toUint();

        if (unpacked.length > 3) mta.rootsSize = unpacked[3].toUint();

        if (unpacked.length > 4) mta.cacheSize = unpacked[4].toUint();

        if (unpacked.length > 5) {
            delete serializedRoots;
            serializedRoots = unpacked[5].toList();
            for (uint256 i = 0; i < serializedRoots.length; i++) {
                mta.cache.push(serializedRoots[i].toBytes().bytesToBytes32());
            }
        }

        if (unpacked.length > 6)
            mta.isAllowNewerWitness = unpacked[6].toBoolean();

        if (mta.height == 0 && mta.offset == 0) mta.height = mta.offset;
    }

    function setOffset(MTA storage mta, uint256 offset) internal {
        mta.offset = offset;
        //TODO: NOTE: comment here if needed to update MTA manually for testing
        if (mta.height == 0 && mta.offset > 0) mta.height = mta.offset;
    }

    function getRoot(MTA storage mta, uint256 idx)
        internal
        view
        returns (bytes32 root)
    {
        require(
            0 <= idx && idx < mta.roots.length,
            "BMVRevertInvalidBlockWitness: root index is out of range"
        );
        return mta.roots[idx];
    }

    function doesIncludeCache(MTA storage mta, bytes32 _hash)
        internal
        view
        returns (bool)
    {
        if (_hash == 0 && _hash.length == 0) return false;
        for (uint256 i = 0; i < mta.cache.length; i++)
            if (mta.cache[i] == _hash) return true;
        return false;
    }

    function putCache(MTA storage mta, bytes32 _hash) internal {
        if (mta.cacheSize > 0) mta.cache.push(_hash);
        if (mta.cache.length > mta.cacheSize) {
            bytes32[] memory newCache = new bytes32[](mta.cacheSize);
            for (uint256 i = 0; i < mta.cacheSize; i++)
                newCache[i] = mta.cache[i + mta.cache.length - mta.cacheSize];
            delete mta.cache;
            mta.cache = newCache;
        }
    }

    function add(MTA storage mta, bytes32 _hash) internal {
        putCache(mta, _hash);
        if (mta.height == 0) mta.roots.push(_hash);
        else if (mta.roots.length == 0) mta.roots.push(_hash);
        else {
            bytes32 root;
            for (uint256 i = 0; i < mta.roots.length; i++) {
                if (mta.roots[i] == 0) {
                    root = _hash;
                    mta.roots[i] = root;
                    break;
                } else {
                    if (0 < mta.rootsSize && mta.rootsSize <= i + 1) {
                        root = _hash;
                        mta.roots[i] = root;
                        mta.offset += 2**i;
                        break;
                    } else {
                        _hash = abi
                            .encodePacked(mta.roots[i], _hash)
                            .sha3FIPS256();
                        delete mta.roots[i];
                    }
                }
            }

            if (root == 0) mta.roots.push(_hash);
        }
        mta.height += 1;
    }

    function getRootIndexByHeight(MTA storage mta, uint256 height)
        internal
        view
        returns (uint256)
    {
        uint256 idx = height - 1 - mta.offset;
        uint256 rootIdx = 0;
        uint256 i = mta.roots.length;
        uint256 bitFlag;
        while (i > 0) {
            require(
                idx >= 0,
                "BMVRevertInvalidBlockWitness: given height is out of range"
            );
            i -= 1;
            if (mta.roots[i] == 0) continue;
            bitFlag = 1 << i;
            if (idx < bitFlag) {
                rootIdx = i;
                break;
            }
            idx -= bitFlag;
        }
        return rootIdx;
    }

    function verify(
        bytes32[] memory witness,
        bytes32 root,
        bytes32 leaf,
        uint256 index
    ) internal {
        bytes32 hash = leaf;
        for (uint256 i = 0; i < witness.length; i++) {
            if (index % 2 == 0) {
                hash = abi.encodePacked(hash, witness[i]).sha3FIPS256();
            } else {
                hash = abi.encodePacked(witness[i], hash).sha3FIPS256();
            }

            index = index / 2;
        }

        require(hash == root, "BMVRevertInvalidBlockWitness: invalid witness");
    }

    function verify(
        MTA storage mta,
        bytes32[] memory proof,
        bytes32 leaf,
        uint256 height,
        uint256 at
    ) internal {
        bytes32 root;
        uint256 rootIdx;

        if (mta.height == at) {
            root = getRoot(mta, proof.length);
            verify(proof, root, leaf, height - 1 - mta.offset);
        } else if (mta.height < at) {
            require(
                mta.isAllowNewerWitness,
                "BMVRevertInvalidBlockWitness: not allowed newer witness"
            );
            require(
                mta.height >= height,
                "BMVRevertInvalidBlockWitness: given witness for newer node"
            );
            rootIdx = getRootIndexByHeight(mta, height);
            root = getRoot(mta, rootIdx);
            bytes32[] memory sliceRoots = new bytes32[](rootIdx);
            for (uint256 i = 0; i < rootIdx; i++) sliceRoots[i] = proof[i];
            verify(sliceRoots, root, leaf, height - 1 - mta.offset);
        } else {
            if (int256(mta.height - height - 1) < int256(mta.cacheSize))
                require(
                    doesIncludeCache(mta, leaf),
                    "BMVRevertInvalidBlockWitness: invalid old witness"
                );
            else {
                revert("BMVRevertInvalidBlockWitnessOld");
            }
        }
    }

    function toBytes(MTA storage mta) internal view returns (bytes memory) {
        bytes memory rlpBytes;
        bytes memory rlpTemp;

        // RLP encode roots[]
        if (mta.roots.length == 0)
            rlpTemp = abi.encodePacked(RLPEncode.LIST_SHORT_START);
        else {
            for (uint256 i = 0; i < mta.roots.length; i++) {
                rlpTemp = abi.encodePacked(
                    rlpTemp,
                    abi.encodePacked(mta.roots[i]).encodeBytes()
                );
            }
            rlpTemp = abi.encodePacked(
                rlpTemp.length.addLength(false),
                rlpTemp
            );
        }

        // RLP encode height, roots[], offset, rootsSize, cacheSize
        rlpBytes = abi.encodePacked(
            rlpBytes,
            mta.height.encodeUint(),
            rlpTemp,
            mta.offset.encodeUint(),
            mta.rootsSize.encodeUint(),
            mta.cacheSize.encodeUint()
        );
        delete rlpTemp;

        // RLP encode cache
        if (mta.cache.length == 0)
            rlpTemp = abi.encodePacked(RLPEncode.LIST_SHORT_START);
        else {
            for (uint256 i = 0; i < mta.cache.length; i++) {
                rlpTemp = abi.encodePacked(
                    rlpTemp,
                    abi.encodePacked(mta.cache[i]).encodeBytes()
                );
            }
            rlpTemp = abi.encodePacked(
                rlpTemp.length.addLength(false),
                rlpTemp
            );
        }
        rlpBytes = abi.encodePacked(rlpBytes, rlpTemp);

        // RLP encode isAllowNewerWitness
        rlpBytes = abi.encodePacked(
            rlpBytes,
            mta.isAllowNewerWitness.encodeBool()
        );

        // Add rlp bytes length header
        rlpBytes = abi.encodePacked(rlpBytes.length.addLength(false), rlpBytes);
        return rlpBytes;
    }
}
