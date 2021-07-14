def urlsafe_b64encode(bs: bytes) -> str:
    return encode(bs, True).decode('utf-8')


def urlsafe_b64decode(s: str) -> bytes:
    return base64url_decode(s)



_to_base64 = [
    'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M',
    'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
    'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm',
    'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
    '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '+', '/'
]

_to_base64_url = [
    'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M',
    'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
    'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm',
    'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
    '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '-', '_'
]


def _from_base64(base64: list) -> list:
    result = [-1] * 256
    for i, v in enumerate(base64):
        result[ord(v)] = i
    result[ord('=')] = -2
    return result


def encode(s, is_url: bool = False) -> bytes:
    base64 = _to_base64_url if is_url else _to_base64
    if isinstance(s, bytes):
        src = s
    elif isinstance(s, str):
        src = bytes(s, 'utf-8')

    dst = bytearray(_encode_length(len(src)))

    start = 0
    end = len(src)
    slen = int(end / 3) * 3

    dp = 0
    sl0 = slen
    while start < slen:
        sp0 = start
        dp0 = dp
        while sp0 < sl0:
            bits = (src[sp0] & 0xff) << 16 | (src[sp0+1] & 0xff) << 8 | (src[sp0+2] & 0xff)
            sp0 = sp0 + 3
            dst[dp0] = ord(base64[(bits >> 18) & 0x3f])
            dst[dp0+1] = ord(base64[(bits >> 12) & 0x3f])
            dst[dp0+2] = ord(base64[(bits >> 6) & 0x3f])
            dst[dp0+3] = ord(base64[bits & 0x3f])
            dp0 = dp0 + 4

        dlen = int((sl0 - start) / 3) * 4
        dp += dlen
        start = sl0

    if start < end:
        b0 = src[start] & 0xff
        start = start + 1
        dst[dp] = ord(base64[b0 >> 2])
        dp = dp + 1
        if start == end:
            dst[dp] = ord(base64[b0 << 4 & 0x3f])
            dst[dp+1] = ord('=')
            dst[dp+2] = ord('=')
        else:
            b1 = src[start] & 0xff
            dst[dp] = ord(base64[b0 << 4 & 0x3f | (b1 >> 4)])
            dst[dp+1] = ord(base64[b1 << 2 & 0x3f])
            dst[dp+2] = ord('=')

    return bytes(dst)


def decode(src, is_url: bool = False) -> bytes:
    if isinstance(src, str):
        src = bytes(src, 'utf-8')

    dst = bytearray(_decode_length(src))

    sp = 0
    sl = len(src)

    dp = 0
    bits = 0
    shiftto = 18

    while sp < sl:
        b = src[sp] & 0xff
        sp = sp + 1
        base64 = _to_base64_url if is_url else _to_base64
        b = _from_base64(base64)[b]
        if b < 0:
            if b == -2:
                if shiftto == 6 and (sp == sl or src[sp] != ord('=')) or shiftto == 18:
                    raise Exception('Input byte array has wrong 4-byte ending unit')
                sp = sp + 1
                break

            raise Exception(f'Illegal base64 character {chr(src[sp - 1])}')

        bits = bits | (b << shiftto)
        shiftto -= 6
        if shiftto < 0:
            dst[dp] = (bits >> 16 & 0xff)
            dst[dp+1] = (bits >> 8 & 0xff)
            dst[dp+2] = bits & 0xff
            dp = dp + 3
            shiftto = 18
            bits = 0

    if shiftto == 6:
        dst[dp] = (bits >> 16 & 0xff)
    elif shiftto == 0:
        dst[dp] = (bits >> 16 & 0xff)
        dst[dp+1] = (bits >> 8 & 0xff)
    elif shiftto == 12:
        raise Exception('Last unit does not have enough valid bits')

    if sp < sl:
        raise Exception(f'Input byte array has incorrect ending byte at {sp}')

    return bytes(dst)


def urlsafe_encode(s: str) -> bytes:
    return encode(s, True)


def urlsafe_decode(s) -> bytes:
    return decode(s, True)


def _encode_length(l: int) -> int:
    return 4 * int((l + 2) / 3)


def _decode_length(s: bytes) -> int:
    paddings = 0
    start = 0
    sl = len(s)
    l = sl - start
    if l == 0:
        return 0
    if l < 2:
        raise Exception('Input byte[] should at least have 2 bytes for base64 bytes')

    if s[sl - 1] == ord('='):
        paddings = paddings + 1
    if s[sl - 2] == ord('='):
        paddings = paddings + 1

    if paddings == 0 and l & 0x3 != 0:
        paddings = 4 - (l & 0x3)
    return 3 * (int((l + 3) / 4)) - paddings


def base64url_decode(s) -> bytes:
    if isinstance(s, str):
        s = s.encode('ascii')

    rem = len(s) % 4
    if rem > 0:
        s += b'=' * (4 - rem)
    return urlsafe_decode(s)

