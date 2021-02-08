#  Copyright 2020 ICON Foundation
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

from ..lib import BTPAddress
from ..lib.icon import rlp


class BTPMessage(object):
    def __init__(self, src: BTPAddress, dst: BTPAddress, svc: str, sn: int, payload: bytes) -> None:
        self.src = src
        self.dst = dst
        self.svc = svc
        self.sn = sn
        self.payload = payload

    def to_bytes(self) -> bytes:
        msg = [str(self.src), str(self.dst), self.svc, self.sn, self.payload]
        return rlp.rlp_encode(msg)

    @staticmethod
    def from_bytes(bs: bytes) -> 'BTPMessage':
        unpacked = rlp.rlp_decode(bs, [str, str, str, int, bytes])
        src = BTPAddress.from_string(unpacked[0])
        dst = BTPAddress.from_string(unpacked[1])
        svc = unpacked[2]
        sn = unpacked[3]
        payload = unpacked[4]
        return BTPMessage(src, dst, svc, sn, payload)


class ErrorMessage(object):
    def __init__(self, code: int, msg: str) -> None:
        self.code = code
        self.msg = msg

    def to_bytes(self) -> bytes:
        msg = [self.code, self.msg]
        return rlp.rlp_encode(msg)

    @staticmethod
    def from_bytes(bs: bytes) -> 'ErrorMessage':
        unpacked = rlp.rlp_decode(bs, [int, str])
        code = unpacked[0]
        msg = unpacked[1]
        return ErrorMessage(code, msg)


class EventMessage(object):
    def __init__(self, evt: str, values: list) -> None:
        self.evt = evt
        self.values = values

    def to_bytes(self) -> bytes:
        msg = [self.evt, self.values]
        return rlp.rlp_encode(msg)

    @staticmethod
    def from_bytes(bs: bytes) -> 'EventMessage':
        unpacked = rlp.rlp_decode(bs, [str, {list: str}])
        evt = unpacked[0]
        values = unpacked[1]
        return EventMessage(evt, values)


class SACKMessage(object):
    def __init__(self, height: int, seq: int) -> None:
        self.height = height
        self.seq = seq

    def to_bytes(self) -> bytes:
        msg = [self.height, self.seq]
        return rlp.rlp_encode(msg)

    @staticmethod
    def from_bytes(bs: bytes) -> 'SACKMessage':
        unpacked = rlp.rlp_decode(bs, [int, int])
        height = unpacked[0]
        seq = unpacked[1]
        return SACKMessage(height, seq)
