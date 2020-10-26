from iconservice import recover_key, create_address_with_key, Address, AddressPrefix, sha3_256
import hashlib
import secp256k1
import logging

_public_key = secp256k1.PublicKey(None, False, secp256k1.ALL_FLAGS)


def log_warning(msg, *args, **kwargs):
    # logging.warning(msg, *args, **kwargs)
    pass


# FIXME  recover_public_key to recover_key
def recover_public_key(msg_hash: bytes, signature: bytes, compressed: bool = True) -> bytes:
    try:
        return recover_key(msg_hash, signature)
    except AssertionError:  # assert context
        log_warning("fail to recover_key, try secp256k1")
        recover_sig = _public_key.ecdsa_recoverable_deserialize(signature[:64], signature[64])
        internal_pubkey = _public_key.ecdsa_recover(msg_hash, recover_sig, True, None)
        public_key = secp256k1.PublicKey(internal_pubkey, False, secp256k1.FLAG_VERIFY, _public_key.ctx)
        return public_key.serialize(compressed)


# FIXME  remove, copy from iconservice.iconscore.icon_score_base2._convert_key
def convert_public_key(public_key: bytes, compressed: bool) -> bytes:
    """Convert key between compressed and uncompressed keys

    :param public_key: compressed or uncompressed key
    :return: the counterpart key of a given public_key
    """
    public_key = secp256k1.PublicKey(public_key, raw=True, flags=secp256k1.NO_FLAGS, ctx=_public_key.ctx)
    return public_key.serialize(compressed=not compressed)


# FIXME  address_by_public_key to create_address_with_key
def address_by_public_key(public_key: bytes) -> Address:
    try:
        return create_address_with_key(public_key)
    except AssertionError:  # assert context
        log_warning("fail to create_address_with_key, try secp256k1")
        assert isinstance(public_key, bytes)
        assert len(public_key) in (33, 65)

        size = len(public_key)
        prefix: int = public_key[0]

        if size == 33 and prefix in (0x02, 0x03):
            uncompressed_public_key: bytes = convert_public_key(public_key, compressed=True)
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
    except AssertionError:  # assert context
        log_warning("fail to sha3_256, try hashlib")
        return hashlib.sha3_256(b).digest()
