# Copyright 2021 ICON Foundation
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

from iconservice import *


def rlp_encode_list(ls: List[int]) -> bytes:
    if not isinstance(ls, list):
        raise Exception("RLP: not a list")
    bs = b''
    for e in ls:
        bs += rlp_encode_bytes(int_to_bytes(e))
    return rlp_encode_length(len(bs), 0xc0) + bs


def rlp_encode_bytes(bs: bytes) -> bytes:
    size = len(bs)
    if size == 1 and bs[0] < 0x80:
        return bs
    return rlp_encode_length(size, 0x80) + bs


def rlp_encode_length(size: int, offset: int) -> bytes:
    if size <= 55:
        return bytes([size + offset])
    len_bytes = int_to_bytes(size)
    return bytes([len(len_bytes) + offset + 55]) + len_bytes


def int_to_bytes(x: int) -> bytes:
    if x == 0:
        return b''
    else:
        return int_to_bytes(int(x / 256)) + bytes([x % 256])
