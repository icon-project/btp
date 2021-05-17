#  Copyright 2021 ICON Foundation
#
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.

from .rlp import rlp_decode


class MPTException(BaseException):
    def __init__(self, message: str):
        super().__init__(message)


class MerklePatriciaTree(object):

    class Node(object):
        def __init__(self, _hash: bytes, serialized: bytes = None) -> None:
            self.__hash = _hash
            if 0 < len(_hash) < 32:
                raise MPTException("invalid hash length")

            self.__children = []
            self.__nibbles = bytes(0)
            if serialized is None:
                self.__bytes = bytes(0)
            else:
                self.__bytes = serialized
                unpacked = rlp_decode(serialized, list)
                if len(unpacked) == 2:
                    header = rlp_decode(unpacked[0])
                    self.__prefix = header[0] & 0xF0
                    nibbles = []
                    # is_odd
                    if (self.__prefix & 0x10) != 0:
                        nibbles.append(header[0] & 0x0F)
                    self.__nibbles = MerklePatriciaTree.bytes_to_nibbles(header[1:], nibbles)
                    # is_leaf
                    if (self.__prefix & 0x20) != 0:
                        self.__data = rlp_decode(unpacked[1])
                    else:
                        _hash = rlp_decode(unpacked[1])
                        node = MerklePatriciaTree.Node(_hash)
                        self.__children.append(node)
                elif len(unpacked) == 17:
                    for i in range(16):
                        if unpacked[i][0] >= 0xC0:
                            node = MerklePatriciaTree.Node(bytes(0), unpacked[i])
                        else:
                            _hash = rlp_decode(unpacked[i])
                            if len(_hash) > 0:
                                node = MerklePatriciaTree.Node(_hash)
                            else:
                                node = None
                        self.__children.append(node)
                    self.__data = unpacked[16]
                else:
                    raise MPTException("invalid list length")

        def is_hash(self) -> bool:
            return len(self.__hash) > 0 and len(self.__bytes) == 0

        def is_extension(self) -> bool:
            return len(self.__children) == 1

        def is_branch(self) -> bool:
            return len(self.__children) == 16

        def prove(self, nibbles: bytes, proofs: list) -> bytes:
            if self.is_hash():
                serialized = proofs.pop(0)
                _hash = sha3_256(serialized)
                if self.__hash != _hash:
                    raise MPTException("mismatch hash")
                node = MerklePatriciaTree.Node(_hash, serialized)
                return node.prove(nibbles, proofs)
            elif self.is_extension():
                cnt = MerklePatriciaTree.match_nibbles(self.__nibbles, nibbles)
                if cnt < len(self.__nibbles):
                    raise MPTException("mismatch nibbles on extension")
                return self.__children[0].prove(nibbles[cnt:], proofs)
            elif self.is_branch():
                if len(nibbles) == 0:
                    return self.__data
                else:
                    node = self.__children[nibbles[0]]
                    return node.prove(nibbles[1:], proofs)
            else:
                cnt = MerklePatriciaTree.match_nibbles(self.__nibbles, nibbles)
                if cnt < len(nibbles):
                    raise MPTException("mismatch nibbles on leaf")
                return self.__data

    @staticmethod
    def bytes_to_nibbles(bs: bytes, nibbles: list = None) -> bytes:
        if nibbles is None:
            nibbles = []
        for b in bs:
            nibbles.append((b >> 4) & 0x0f)
            nibbles.append(b & 0x0f)
        return bytes(nibbles)

    @staticmethod
    def match_nibbles(src: bytes, dst: bytes) -> int:
        len_src = len(src)
        if len(dst) < len_src:
            len_src = len(dst)
        for i in range(len_src):
            if src[i] != dst[i]:
                return i
        return len_src

    @staticmethod
    def prove(root: bytes, key: bytes, proofs: list) -> bytes:
        nibbles = MerklePatriciaTree.bytes_to_nibbles(key)
        node = MerklePatriciaTree.Node(root)
        return node.prove(nibbles, proofs)
