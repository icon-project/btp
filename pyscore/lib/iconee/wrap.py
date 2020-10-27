from iconservice import recover_key, create_address_with_key, Address, AddressPrefix, sha3_256
import hashlib
import logging
from coincurve import PublicKey


def log_warning(msg, *args, **kwargs):
    logging.warning(msg, *args, **kwargs)
    pass


def _check_recoverable_error(e: Exception):
    if not isinstance(e, AssertionError) \
            or (isinstance(e, AttributeError) and not str(e) == "'NoneType' object has no attribute 'get_step_cost'"):
        raise e


def log_warning(msg, *args, **kwargs):
    # logging.warning(msg, *args, **kwargs)
    pass


# FIXME  recover_public_key to recover_key
def recover_public_key(msg_hash: bytes, signature: bytes, compressed: bool = True) -> bytes:
    try:
        return recover_key(msg_hash, signature)
    except Exception as e:
        _check_recoverable_error(e)
        log_warning("fail to recover_key, try coincurve")
        if isinstance(msg_hash, bytes) \
                and len(msg_hash) == 32 \
                and isinstance(signature, bytes) \
                and len(signature) == 65:
            return PublicKey.from_signature_and_message(signature, msg_hash, hasher=None).format(compressed)
        return None


# FIXME  remove, copy from iconservice.iconscore.icon_score_base2._convert_key
def _convert_key(public_key: bytes, compressed: bool) -> bytes:
    """Convert key between compressed and uncompressed keys

    :param public_key: compressed or uncompressed key
    :return: the counterpart key of a given public_key
    """
    public_key_object = PublicKey(public_key)
    return public_key_object.format(compressed=not compressed)


# FIXME  address_by_public_key to create_address_with_key
def address_by_public_key(public_key: bytes) -> Address:
    try:
        return create_address_with_key(public_key)
    except Exception as e:
        _check_recoverable_error(e)
        log_warning("fail to create_address_with_key, try coincurve")
        assert isinstance(public_key, bytes)
        assert len(public_key) in (33, 65)

        size = len(public_key)
        prefix: int = public_key[0]

        if size == 33 and prefix in (0x02, 0x03):
            uncompressed_public_key: bytes = _convert_key(public_key, compressed=True)
        elif size == 65 and prefix == 0x04:
            uncompressed_public_key: bytes = public_key
        else:
            raise Exception("not supported public key length")

        body: bytes = hashlib.sha3_256(uncompressed_public_key[1:]).digest()[-20:]
        return Address(AddressPrefix.EOA, body)


# FIXME  get_hash to sha3_256
def get_hash(b: bytes) -> bytes:
    try:
        return sha3_256(b)
    except Exception as e:
        _check_recoverable_error(e)
        log_warning("fail to sha3_256, try hashlib")
        return hashlib.sha3_256(b).digest()
