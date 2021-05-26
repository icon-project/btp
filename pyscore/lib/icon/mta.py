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

from . import rlp, Serializable


class MTAException(BaseException):
    def __init__(self, message: str):
        super().__init__(message)


class InvalidWitnessOldException(MTAException):
    def __init__(self, message: str):
        super().__init__(message)


class InvalidWitnessNewerException(MTAException):
    def __init__(self, message: str):
        super().__init__(message)


class MerkleTreeAccumulator(Serializable):
    def __init__(self, serialized: bytes = None) -> None:
        self.__bytes = serialized

        if serialized is not None:
            unpacked = rlp.rlp_decode(serialized, list)
        else:
            unpacked = []
        self.__height = rlp.rlp_decode(unpacked[0], int) if len(unpacked) > 0 else 0
        self.__roots = []
        if len(unpacked) > 1:
            serialized_roots = rlp.rlp_decode(unpacked[1], {list: bytes})
            for serialized_root in serialized_roots:
                self.__roots.append(serialized_root)
        self.__offset = rlp.rlp_decode(unpacked[2], int) if len(unpacked) > 2 else 0
        self.__roots_size = rlp.rlp_decode(unpacked[3], int) if len(unpacked) > 3 else 0
        self.__cache_size = rlp.rlp_decode(unpacked[4], int) if len(unpacked) > 4 else 0
        self.__cache = []
        if len(unpacked) > 5:
            serialized_caches = rlp.rlp_decode(unpacked[5], {list: bytes})
            for serialized_cache in serialized_caches:
                self.__cache.append(serialized_cache)
        self.__is_allow_newer_witness = rlp.rlp_decode(unpacked[6], bool) if len(unpacked) > 6 else False

        if self.__height == 0 and self.__offset > 0:
            self.__height = self.__offset

    @property
    def height(self) -> int:
        return self.__height

    @property
    def offset(self) -> int:
        return self.__offset

    @offset.setter
    def offset(self, offset: int):
        self.__offset = offset
        if self.__height == 0 and self.__offset > 0:
            self.__height = self.__offset

    @property
    def roots_size(self) -> int:
        return self.__roots_size

    @property
    def cache_size(self) -> int:
        return self.__cache_size

    @property
    def is_allow_newer_witness(self) -> bool:
        return self.__is_allow_newer_witness

    def get_root(self, idx: int) -> bytes:
        if 0 <= idx < len(self.__roots):
            return self.__roots[idx]
        else:
            raise MTAException("root idx is out of range")

    # def set_offset(self, offset: int):
    #     self.__offset = offset

    def set_roots_size(self, size: int):
        if size < 0:
            size = 0
        self.__roots_size = size
        if self.__roots_size > 0:
            while len(self.__roots) > self.__roots_size:
                self.__roots.pop(0)

    def set_cache_size(self, size: int):
        if size < 0:
            size = 0
        self.__cache_size = size
        while len(self.__cache) > self.__cache_size:
            self.__cache.pop(0)

    def set_allow_newer_witness(self, allow: bool):
        self.__is_allow_newer_witness = allow

    def has_cache(self, _hash: bytes) -> bool:
        if _hash is None or len(_hash) == 0:
            return False
        for node in self.__cache:
            if node == _hash:
                return True
        return False

    def _put_cache(self, _hash: bytes) -> None:
        if self.__cache_size > 0:
            self.__cache.append(_hash)
        while len(self.__cache) > self.__cache_size:
            self.__cache.pop(0)

    def add(self, _hash: bytes):
        self._put_cache(_hash)
        if self.__height == 0:
            self.__roots.append(_hash)
        elif len(self.__roots) == 0:
            self.__roots.append(_hash)
        else:
            root = None
            for idx in range(len(self.__roots)):
                if self.__roots[idx] is None:
                    root = _hash
                    self.__roots[idx] = root
                    break
                else:
                    if 0 < self.__roots_size <= (idx + 1):
                        root = _hash
                        self.__roots[idx] = root
                        offset = pow(2, idx)
                        self.__offset += offset
                        # self.__height -= offset
                        break
                    else:
                        _hash = sha3_256(self.__roots[idx] + _hash)
                        self.__roots[idx] = None
            if root is None:
                root = _hash
                self.__roots.append(root)
        self.__height += 1
        if self.__height < 0:
            raise Exception(f'invalid height {self.__height}')

    def get_root_idx_by_height(self, height: int) -> int:
        idx = height - 1 - self.__offset
        root_idx = 0
        i = len(self.__roots)
        while i > 0:
            if idx < 0:
                raise MTAException("given height is out of range")
            i -= 1
            if self.__roots[i] is None:
                continue
            bit_flag = 1 << i
            if idx < bit_flag:
                root_idx = i
                break
            idx -= bit_flag
        return root_idx

    @staticmethod
    def _verify(witness: list, root: bytes, _hash: bytes, idx: int):
        for w in witness:
            if idx % 2 == 0:
                _hash = sha3_256(_hash + w)  # right
            else:
                _hash = sha3_256(w + _hash)  # left
            idx = int(idx/2)

        if _hash != root:
            raise MTAException("invalid witness")

    def verify(self, witness: list, _hash: bytes, height: int, at: int):
        if self.__height == at:
            root = self.get_root(len(witness))
            self._verify(witness, root, _hash, height - 1 - self.__offset)
        elif self.__height < at:
            # acc: old, wit: new
            if not self.__is_allow_newer_witness:
                raise InvalidWitnessNewerException("not allowed newer witness")

            if self.__height < height:
                raise MTAException("given witness for newer node")

            root_idx = self.get_root_idx_by_height(height)
            root = self.get_root(root_idx)
            MerkleTreeAccumulator._verify(witness[:root_idx], root, _hash, height)
        else:
            # acc: new, wit: old
            # rebuild witness is not supported, but able to verify by cache if enabled
            if (self.__height - height - 1) < self.__cache_size:
                if not self.has_cache(_hash):
                    raise MTAException("invalid old witness")
            else:
                raise InvalidWitnessOldException("not allowed old witness")

    def __bytes__(self):
        return self.to_bytes()

    def to_bytes(self) -> bytes:
        # TODO [TBD] change roots, height, cache every BlockUpdate
        #  self.__cache as Ring buffer with offset
        pack = []
        pack.append(self.__height)
        pack.append(self.__roots)
        pack.append(self.__offset)
        pack.append(self.__roots_size)
        pack.append(self.__cache_size)
        pack.append(self.__cache)
        pack.append(self.__is_allow_newer_witness)
        return rlp.rlp_encode(pack)

    def get_status(self) -> dict:
        return {"height": self.__height, "offset": self.__offset}

    def dump(self):
        print(f'height:{self.__height}, offset:{self.__offset}')
        for i in range(len(self.__roots)):
            root = self.__roots[i]
            if root is None:
                print(f'root[{i}]:{root}')
            else:
                print(f'root[{i}]:{root.hex()}')

    def from_bytes(self, serialized: bytes) -> 'MerkleTreeAccumulator':
        if not isinstance(serialized, bytes) or len(serialized) < 1:
            return None
        return MerkleTreeAccumulator(serialized)
