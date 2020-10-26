from iconservice import Address


class ConvertException(Exception):
    "Base class for exceptions encountered during convert."
    pass


class UnsupportedTypeException(ConvertException):
    "Object type not supported for convert."
    pass


class InsufficientDataException(ConvertException):
    "Insufficient data to convert the object."
    pass


def parse_signature(val: bytes) -> list:
    signature = []
    s = val.decode('utf-8')
    sidx = s.index("(")
    eidx = s.index(")")
    signature.append(s[0:sidx])
    sl = s[sidx+1:eidx].split(",")
    for ts in sl:
        signature.append(ts.strip())
    return signature


def convert_from_bytes(param_type: str, val: bytes):
    if param_type == "int":
        return to_int(val)
    elif param_type == "str":
        return to_str(val)
    elif param_type == "bool":
        return to_bool(val)
    elif param_type == "bytes":
        return val
    elif param_type == "Address":
        return Address.from_bytes(val)
    else:
        raise UnsupportedTypeException(
            f"{param_type} is not supported type (only int, str, bool, Address, bytes are supported)")


def convert_to_bytes(val) -> bytes:
    if isinstance(val, int):
        n_bytes = ((val + (val < 0)).bit_length() + 8) // 8
        return val.to_bytes(n_bytes, byteorder="big", signed=True)
    elif isinstance(val, str):
        return val.encode('utf-8')
    elif isinstance(val, bool):
        return b'\x01' if val else b'\x00'
    elif isinstance(val, bytes):
        return val
    elif isinstance(val, Address):
        return val.to_bytes()
    else:
        raise UnsupportedTypeException(
            f"{val.__class__.__name__} is not supported type (only int, str, bool, Address, bytes are supported)")


def to_int(val: bytes) -> int:
    return int.from_bytes(val, "big", signed=True)


def to_str(val: bytes) -> str:
    return val.decode('utf-8')


def to_bool(val: bytes) -> bool:
    if val == b'\x00':
        return False
    elif val == b'\x01':
        return True
    else:
        raise InsufficientDataException(f'IllegalBoolBytes{val.hex()})')
