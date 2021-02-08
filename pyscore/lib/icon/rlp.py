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

def rlp_encode(obj) -> bytes:
    # param obj: Union[bytes, int, str, bool, list]
    if obj is None:
        return b'\xF8\x00'
    elif isinstance(obj, bytes):
        return rlp_encode_bytes(obj)
    elif isinstance(obj, int):
        n_bytes = ((obj + (obj < 0)).bit_length() + 8) // 8
        return rlp_encode_bytes(obj.to_bytes(n_bytes, byteorder="big", signed=True))
    elif isinstance(obj, str):
        return rlp_encode_bytes(obj.encode('utf-8'))
    elif isinstance(obj, bool):
        return rlp_encode_bytes(b'\x01' if obj else b'\x00')
    elif isinstance(obj, list):
        el = []
        for e in obj:
            eb = rlp_encode(e)
            el.append(eb)
        return rlp_encode_list_with_encoded(el)
    else:
        raise Exception(f"{obj.__class__.__name__} is not supported type")


def rlp_encode_bytes(bs: bytes) -> bytes:
    blen = len(bs)
    if blen == 1 and bs[0] < 0x80:
        return bs
    if blen <= 55:
        buf = (0x80+blen).to_bytes(1, 'big')  # max 0x80+0x37=0xB7
        return buf + bs

    slen = _rlp_length_size(blen)
    buf = []
    # buf[0] = 0x80+55+slen & 0xFF
    buf.insert(0, 0xB7+slen & 0xFF)
    while slen > 0:
        # buf[slen] = blen & 0xFF
        buf.insert(slen, blen & 0xFF)
        blen >>= 8
        slen -= 1
    return bytes(buf) + bs


def rlp_encode_list_with_encoded(l: list) -> bytes:
    # param l: List[bytes]
    blen = 0
    for b in l:
        blen += len(b)

    if blen <= 55:
        buf = (0xC0+blen).to_bytes(1, 'big')  # max 0xC0+0x37=0xF7
        for b in l:
            buf += b
        return buf

    slen = _rlp_length_size(blen)
    buf = []
    # buf[0] = 0xC0+55+slen & 0xFF
    buf.insert(0, 0xF7+slen & 0xFF)
    while slen > 0:
        buf.insert(slen, blen & 0xFF)
        blen >>= 8
        slen -= 1

    bb = bytes(buf)
    for b in l:
        bb += b
    return bb


def _rlp_length_size(blen: int) -> int:
    cnt = 0
    while blen > 0:
        blen >>= 8
        cnt += 1
    return cnt


def from_bytes(val, v_type: type):
    if val is None:
        return None
    if isinstance(val, bytes):
        if v_type == bytes:
            return val
        elif v_type == int:
            return int.from_bytes(val, "big", signed=True)
        elif v_type == str:
            return val.decode('utf-8')
        elif v_type == bool:
            if val == b'\x00':
                return False
            elif val == b'\x01':
                return True
            else:
                raise Exception(f'IllegalBoolBytes{val.hex()})')
        else:
            raise Exception(f"{v_type} is not supported type (only int, str, bool, bytes are supported)")
    elif isinstance(val, list) and v_type == list:
        return val
    else:
        raise Exception(f"InvalidArgument val:{val.__class__.__name__} type:{v_type}")


def rlp_decode(bs: bytes, v_type: any = bytes):
    # v_type: Union[type, list, dict]

    obj, remain = rlp_decode_part(bs)
    if len(remain) > 0:
        raise Exception("Remaining bytes")

    if isinstance(v_type, list):  # for object
        if isinstance(obj, list):
            el = []
            if len(v_type) < len(obj):
                raise Exception("InvalidArgument: v_type:list invalid length")
            for i in range(len(obj)):
                el.append(rlp_decode(obj[i], v_type[i]))
            return el
        else:
            raise Exception(f"InvalidArgument: v_type:list mismatch v:{obj}")
    elif isinstance(v_type, dict):  # for generic
        if isinstance(obj, list):
            if len(v_type) != 1:
                raise Exception("InvalidArgument: v_type:dict invalid length")
            g_type = v_type.popitem()
            el = []
            for i in range(len(obj)):
                el.append(rlp_decode(obj[i], g_type[1]))
            return el
        else:
            raise Exception(f"InvalidArgument: v_type:dict mismatch v:{obj}")
    elif isinstance(v_type, type):  # for single value
        return from_bytes(obj, v_type)
    else:
        raise Exception(f"InvalidArgument: v_type:{v_type.__class__.__name__} must be list or dict or type")


def rlp_decode_header(bs: bytes) -> tuple:
    # return Tuple[bool, int, int]
    if len(bs) < 1:
        raise Exception("Not enough bytes")

    is_list = False
    b = bs[0]
    if b < 0x80:
        ts = 0
        size = 1
    elif b < 0xB8:
        ts = 1
        size = b - 0x80
        if size == 1 and len(bs) > 1 and b < 128:
            raise Exception('Invalid encoding')
    elif b < 0xC0:
        ts = b - 0xB7 + 1
        size = int.from_bytes(bs[1:ts], byteorder="big", signed=False)
    elif b < 0xF8:
        is_list = True
        ts = 1
        size = b - 0xC0
    else:
        is_list = True
        ts = b - 0xF7 + 1
        size = int.from_bytes(bs[1:ts], byteorder="big", signed=False)

    if len(bs) < ts + size:
        raise Exception("Not enough bytes for list")
    return is_list, ts, size


def rlp_decode_part(bs: bytes) -> tuple:
    # return Tuple[Union[bytes, list], bytes]
    is_list, ts, size = rlp_decode_header(bs)
    if is_list:
        if ts == 2 and size == 0:
            return None, bs[ts + size:]
        # l: List[bytes] = []
        l: list = []
        c = bs[ts:ts + size]
        while len(c) > 0:
            _, ets, esize = rlp_decode_header(c)
            l.append(c[:ets+esize])
            c = c[ets+esize:]
        return l, bs[ts + size:]
    else:
        return bs[ts: ts + size], bs[ts + size:]
