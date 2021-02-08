import base64


def urlsafe_b64encode(bs: bytes) -> str:
    v = base64.urlsafe_b64encode(bs)
    if isinstance(v, bytes):
        return v.decode('utf-8')
    return v


def urlsafe_b64decode(s: str) -> bytes:
    return base64.urlsafe_b64decode(s)
