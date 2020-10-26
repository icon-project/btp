from iconservice import *

from ..lib import BTPAddress, BTPExceptionCode, BMVException, BMCInterfaceForBMV
from ..lib.iconee import rlp, type_converter, base64
from ..lib.iconee.mta import MerkleTreeAccumulator, MTAException, InvalidWitnessOldException
from ..lib.iconee.mpt import MerklePatriciaTree, MPTException
from ..lib.iconee.wrap import get_hash, recover_public_key, address_by_public_key

TAG = 'BTPMessageVerifier'


class BMVExceptionCode(BTPExceptionCode):
    INVALID_MPT = 1
    INVALID_VOTES = 2
    INVALID_SEQUENCE = 3
    INVALID_BLOCK_UPDATE = 4
    INVALID_BLOCK_PROOF = 5
    INVALID_BLOCK_WITNESS = 6
    INVALID_SEQUENCE_HIGHER = 7
    INVALID_BLOCK_UPDATE_HEIGHT_HIGHER = 8
    INVALID_BLOCK_UPDATE_HEIGHT_LOWER = 9
    INVALID_BLOCK_PROOF_HEIGHT_HIGHER = 10
    INVALID_BLOCK_WITNESS_OLD = 11


class InvalidMPTException(BMVException):
    def __init__(self, message: str):
        super().__init__(message, BMVExceptionCode.INVALID_MPT)


class InvalidVotesException(BMVException):
    def __init__(self, message: str):
        super().__init__(message, BMVExceptionCode.INVALID_VOTES)


class InvalidBlockUpdateException(BMVException):
    def __init__(self, message: str):
        super().__init__(message, BMVExceptionCode.INVALID_BLOCK_UPDATE)


class InvalidBlockProofException(BMVException):
    def __init__(self, message: str):
        super().__init__(message, BMVExceptionCode.INVALID_BLOCK_PROOF)


class InvalidBlockWitnessException(BMVException):
    def __init__(self, message: str):
        super().__init__(message, BMVExceptionCode.INVALID_BLOCK_WITNESS)


# ================================================
#  BlockProof
# ================================================
class BlockHeader(object):
    def __init__(self, serialized: bytes) -> None:
        self.__bytes = serialized
        self.__hash = get_hash(serialized)

        unpacked = rlp.rlp_decode(self.__bytes,
                                  [int, int, int, bytes, bytes, bytes, bytes, bytes, bytes, bytes, bytes])

        self.__version = unpacked[0]
        self.__height = unpacked[1]
        self.__timestamp = unpacked[2]
        self.__proposer = unpacked[3]
        self.__prev_hash = unpacked[4]
        self.__vote_hash = unpacked[5]
        self.__next_validators_hash = unpacked[6]
        self.__patch_tx_hash = unpacked[7]
        self.__tx_hash = unpacked[8]
        self.__logs_bloom = unpacked[9]
        if isinstance(unpacked[10], bytes) and len(unpacked[10]) > 0:
            result_unpacked = rlp.rlp_decode(unpacked[10], [bytes, bytes, bytes])
            self.__state_hash = result_unpacked[0]
            self.__patch_receipt_hash = result_unpacked[1]
            self.__receipt_hash = result_unpacked[2]
        else:
            self.__state_hash = None
            self.__patch_receipt_hash = None
            self.__receipt_hash = None

    @property
    def _bytes(self) -> bytes:
        return self.__bytes

    @property
    def height(self) -> int:
        return self.__height

    @property
    def hash(self) -> bytes:
        return self.__hash

    @property
    def next_validators_hash(self) -> bytes:
        return self.__next_validators_hash

    @property
    def receipt_hash(self) -> bytes:
        return self.__receipt_hash

    @staticmethod
    def from_bytes(serialized: bytes) -> 'BlockHeader':
        if not isinstance(serialized, bytes) or len(serialized) < 1:
            return None
        return BlockHeader(serialized)


class Validators(object):
    def __init__(self, serialized: bytes) -> None:
        self.__bytes = serialized
        self.__hash = get_hash(serialized)
        self.__addresses = []
        unpacked = rlp.rlp_decode(self.__bytes, {list: bytes})
        for b in unpacked:
            address = Address.from_bytes(b)
            self.__addresses.append(address)

    @property
    def hash(self) -> bytes:
        return self.__hash

    @property
    def length(self) -> int:
        return len(self.__addresses)

    def contains(self, addr: Address) -> bool:
        for taddr in self.__addresses:
            if taddr == addr:
                return True
        return False

    def __bytes__(self) -> bytes:
        return self.to_bytes()

    def to_bytes(self) -> bytes:
        return self.__bytes

    @staticmethod
    def from_string(s: str) -> 'Validators':
        if not isinstance(s, str) or len(s) < 1:
            return None
        splitted = s.split(",")
        addresses = []
        for a in splitted:
            try:
                addresses.append(Address.from_string(a).to_bytes())
            except BaseException as e:
                if len(splitted) == 1:
                    return Validators.from_bytes(base64.urlsafe_b64decode(s))
                else:
                    raise e
        b = rlp.rlp_encode(addresses)
        return Validators(b)

    @staticmethod
    def from_bytes(serialized: bytes) -> 'Validators':
        if not isinstance(serialized, bytes) or len(serialized) < 1:
            return None
        return Validators(serialized)

    def to_dict(self) -> dict:
        d = {"hash": self.__hash.hex(), "addresses": self.__addresses}
        return d


class Votes(object):
    VOTE_TYPE_PRECOMMIT = 1

    class VoteItem(object):
        def __init__(self, timestamp: int, signature: bytes) -> None:
            self.__timestamp = timestamp
            self.__signature = signature

        @property
        def timestamp(self) -> int:
            return self.__timestamp

        @property
        def signature(self) -> bytes:
            return self.__signature

    def __init__(self, serialized: bytes) -> None:
        self.__bytes = serialized

        unpacked = rlp.rlp_decode(
            self.__bytes, [int, [int, bytes], {list: [int, bytes]}])

        self.__round = unpacked[0]
        self.__block_part_set_id = unpacked[1]
        self.__vote_items = []
        for tvote in unpacked[2]:
            vote_item = Votes.VoteItem(tvote[0], tvote[1])
            self.__vote_items.append(vote_item)

    @property
    def _bytes(self) -> bytes:
        return self.__bytes

    def verify(self, height: int, block_id: bytes, validators: Validators) -> None:
        # validation signature
        vote_msg = []
        vote_msg.append(height)
        vote_msg.append(self.__round)
        vote_msg.append(Votes.VOTE_TYPE_PRECOMMIT)
        vote_msg.append(block_id)
        vote_msg.append(self.__block_part_set_id)

        contained_validators = []
        for vote_item in self.__vote_items:
            vote_msg.append(vote_item.timestamp)
            serialized_vote_msg = rlp.rlp_encode(vote_msg)
            msg_hash = get_hash(serialized_vote_msg)
            public_key = recover_public_key(msg_hash, vote_item.signature)
            addr = address_by_public_key(public_key)
            if not validators.contains(addr):
                raise InvalidVotesException("invalid signature")
            if addr not in contained_validators:
                contained_validators.append(addr)
            else:
                raise InvalidVotesException("duplicated vote")
            vote_msg.pop()

        # has votes +2/3
        if len(contained_validators) <= (validators.length * 2 / 3):
            raise InvalidVotesException("require votes +2/3")

    @staticmethod
    def from_bytes(serialized: bytes) -> 'Votes':
        if not isinstance(serialized, bytes) or len(serialized) < 1:
            return None
        return Votes(serialized)


class BlockUpdate(object):
    def __init__(self, serialized: bytes) -> None:
        self.__bytes = serialized
        # codec
        unpacked = rlp.rlp_decode(self.__bytes, [bytes, bytes, bytes])

        # : BlockUpdate {BlockHeader, Votes, Validators}
        self.__block_header = BlockHeader.from_bytes(unpacked[0])  # required
        self.__votes = Votes.from_bytes(unpacked[1])
        self.__next_validators = Validators.from_bytes(unpacked[2])

    @property
    def block_header(self) -> BlockHeader:
        return self.__block_header

    @property
    def votes(self) -> Votes:
        return self.__votes

    @property
    def next_validators(self) -> Validators:
        return self.__next_validators

    @property
    def height(self) -> int:
        return self.__block_header.height

    @property
    def receipt_hash(self) -> bytes:
        return self.__block_header.receipt_hash

    def verify(self, validators: Validators) -> bool:
        """
        Verify votes with validators, check validators change

        :param: validators: Validators
        :return: Validators change
        """
        if self.__votes is None:
            raise InvalidBlockUpdateException("not exists votes")
        else:
            self.__votes.verify(self.__block_header.height, self.__block_header.hash, validators)
            if self.__block_header.next_validators_hash != validators.hash:
                if self.__next_validators is None:
                    raise InvalidBlockUpdateException("not exists next validator")
                elif self.__next_validators.hash == self.__block_header.next_validators_hash:
                    return True
                else:
                    raise InvalidBlockUpdateException("invalid next validator hash")
            else:
                return False


class BlockWitness(object):
    def __init__(self, height: int, witness: list) -> None:
        self.__height = height  # mta.height when get witness
        self.__witness = witness

    @property
    def height(self) -> int:
        return self.__height

    def verify(self, mta: MerkleTreeAccumulator, _hash: bytes, height: int):
        try:
            mta.verify(self.__witness, _hash, height, self.__height)
        except InvalidWitnessOldException as e:
            raise BMVException(str(e), BMVExceptionCode.INVALID_BLOCK_WITNESS_OLD)
        except MTAException as e:
            raise InvalidBlockWitnessException(str(e))

    @staticmethod
    def from_bytes(serialized: bytes) -> 'BlockWitness':
        if not isinstance(serialized, bytes) or len(serialized) < 1:
            return None
        unpacked = rlp.rlp_decode(serialized, [int, {list: bytes}])
        return BlockWitness(unpacked[0], unpacked[1])


class BlockProof(object):
    def __init__(self, serialized: bytes) -> None:
        self.__bytes = serialized
        # codec
        unpacked = rlp.rlp_decode(self.__bytes, [bytes, [int, {list: bytes}]])

        # : BlockProof {BlockHeader, BlockWitness}
        self.__block_header = BlockHeader.from_bytes(unpacked[0])  # required
        self.__block_witness = BlockWitness(unpacked[1][0], unpacked[1][1])

    @property
    def block_header(self) -> BlockHeader:
        return self.__block_header

    @property
    def block_witness(self) -> BlockWitness:
        return self.__block_witness

    @property
    def height(self) -> int:
        return self.__block_header.height

    @property
    def receipt_hash(self) -> bytes:
        return self.__block_header.receipt_hash

    def verify(self, mta: MerkleTreeAccumulator):
        if self.__block_witness is None:
            raise InvalidBlockProofException("not exists witness")
        else:
            if mta.height < self.__block_header.height:
                raise BMVException(f"given block height is newer {self.__block_header.height} expect:{mta.height}",
                                   BMVExceptionCode.INVALID_BLOCK_PROOF_HEIGHT_HIGHER)
            self.__block_witness.verify(mta, self.__block_header.hash, self.__block_header.height)


# ================================================
#  ReceiptProof
# ================================================
class EventLog(object):
    class BTPMessage(object):
        def __init__(self, params: list) -> None:
            self.next_bmc = params[0]
            self.seq = params[1]
            self.msg = params[2]

    def __init__(self, serialized: bytes) -> None:
        self.__bytes = serialized
        # codec
        unpacked = rlp.rlp_decode(self.__bytes, [bytes, {list: bytes}, {list: bytes}])

        # addr: bytes, indexed: list, data: list
        self.__addr = unpacked[0]
        self.__indexed = []
        for serialized_indexed in unpacked[1]:
            self.__indexed.append(serialized_indexed)
        self.__data = []
        for serialized_data in unpacked[2]:
            self.__data.append(serialized_data)

        parsed = type_converter.parse_signature(self.__indexed[0])
        self.__method = parsed[0]
        self.__params = []
        nidx = len(self.__indexed) - 1
        for i in range(len(parsed) - 1):
            if i < nidx:
                val = self.__indexed[i + 1]
            else:
                val = self.__data[i - nidx]
            arg = type_converter.convert_from_bytes(parsed[i + 1], val)
            self.__params.append(arg)

    @property
    def method(self) -> bytes:
        return self.__method

    @property
    def addr(self) -> bytes:
        return self.__addr

    def get_param(self, idx: int):
        return self.__params[idx]

    def to_btp_message(self) -> BTPMessage:
        if self.__method == "Message":
            return EventLog.BTPMessage(self.__params)
        return None

    @staticmethod
    def from_bytes(serialized: bytes) -> 'EventLog':
        if not isinstance(serialized, bytes) or len(serialized) < 1:
            return None
        return EventLog(serialized)


class Receipt(object):
    def __init__(self, serialized: bytes) -> None:
        self.__bytes = serialized
        unpacked = rlp.rlp_decode(self.__bytes, [int, bytes, bytes, bytes, bytes, bytes, list, bytes, bytes])

        self.__status = unpacked[0]
        self.__to = unpacked[1]  # Address
        self.__cumulative_step_used = unpacked[2]  # HexInt
        self.__step_used = unpacked[3]  # HexInt
        self.__step_price = unpacked[4]  # HexInt
        self.__logs_bloom = unpacked[5]
        self.__event_logs = []
        if unpacked[6] is not None:
            for raw_event_log in unpacked[6]:
                event_log = EventLog(raw_event_log)
                self.__event_logs.append(event_log)
        self.__score_address = unpacked[7]  # Address
        self.__event_logs_hash = unpacked[8]

    @property
    def event_logs(self) -> list:
        return self.__event_logs

    @property
    def event_logs_hash(self) -> bytes:
        return self.__event_logs_hash

    def set_event_logs_with_prove(self, event_proofs: list) -> None:
        event_logs = []
        for event_proof in event_proofs:
            event_log = event_proof.prove(self.__event_logs_hash)
            event_logs.append(event_log)
        self.__event_logs = event_logs

    @staticmethod
    def from_bytes(serialized: bytes) -> 'Receipt':
        if not isinstance(serialized, bytes) or len(serialized) < 1:
            return None
        return Receipt(serialized)


class EventProof(object):
    def __init__(self, serialized: bytes) -> None:
        self.__bytes = serialized
        # codec
        unpacked = rlp.rlp_decode(self.__bytes, [int, bytes])

        # : ReceiptProof {Index, MPTProofs}
        self.__index = unpacked[0]
        self.__mpt_key = rlp.rlp_encode(self.__index)
        self.__mpt_proofs = []
        serialized_mpt_proofs = rlp.rlp_decode(unpacked[1], {list: bytes})
        for serialized_mpt_proof in serialized_mpt_proofs:
            self.__mpt_proofs.append(serialized_mpt_proof)

    def prove(self, event_logs_hash: bytes) -> EventLog:
        try:
            leaf = MerklePatriciaTree.prove(event_logs_hash, self.__mpt_key, self.__mpt_proofs)
            return EventLog.from_bytes(leaf)
        except MPTException as e:
            raise InvalidMPTException(str(e))


class ReceiptProof(object):
    def __init__(self, serialized: bytes) -> None:
        self.__bytes = serialized
        # codec
        unpacked = rlp.rlp_decode(self.__bytes, [int, bytes, list])

        # : ReceiptProof {Index, MPTProofs}
        self.__index = unpacked[0]
        self.__mpt_key = rlp.rlp_encode(self.__index)
        self.__mpt_proofs = []

        serialized_mpt_proofs = rlp.rlp_decode(unpacked[1], {list: bytes})
        for serialized_mpt_proof in serialized_mpt_proofs:
            self.__mpt_proofs.append(serialized_mpt_proof)

        # List of EventProof {Index, MPTProofs}
        self.__event_proofs = []
        for serialized_event_proof in unpacked[2]:
            event_proof = EventProof(serialized_event_proof)
            self.__event_proofs.append(event_proof)

    def prove(self, receipt_hash: bytes) -> Receipt:
        try:
            leaf = MerklePatriciaTree.prove(receipt_hash, self.__mpt_key, self.__mpt_proofs)
            receipt = Receipt.from_bytes(leaf)
        except MPTException as e:
            raise InvalidMPTException(str(e))
        receipt.set_event_logs_with_prove(self.__event_proofs)
        return receipt


# ================================================
#  RelayMessage
# ================================================
class RelayMessage(object):
    def __init__(self, serialized: bytes) -> None:
        self.__bytes = serialized
        # codec
        unpacked = rlp.rlp_decode(self.__bytes, [{list: bytes}, bytes, {list: bytes}])

        # List of BlockUpdate {BlockHeader, Votes, Validators}
        self.__block_updates = []
        for serialized_block_update in unpacked[0]:
            block_update = BlockUpdate(serialized_block_update)
            self.__block_updates.append(block_update)

        # BlockProof {BlockHeader, MTAWitness}
        serialized_block_proof = unpacked[1]
        if isinstance(serialized_block_proof, bytes) and len(serialized_block_proof) > 0:
            self.__block_proof = BlockProof(serialized_block_proof)
        else:
            self.__block_proof = None

        # List of ReceiptProof {Index, MPTProofs}
        self.__receipt_proofs = []
        for serialized_receipt_proof in unpacked[2]:
            receipt_proof = ReceiptProof(serialized_receipt_proof)
            self.__receipt_proofs.append(receipt_proof)

    @property
    def block_updates(self) -> list:
        return self.__block_updates

    @property
    def block_proof(self) -> BlockProof:
        return self.__block_proof

    @property
    def receipt_proofs(self) -> list:
        return self.__receipt_proofs

    @staticmethod
    def from_bytes(serialized: bytes) -> 'RelayMessage':
        if not isinstance(serialized, bytes) or len(serialized) < 1:
            return None
        return RelayMessage(serialized)


class Properties(object):
    _PROPERTIES = 'properties'
    _BMC = 'bmc'
    _NET = 'net'
    _VALIDATORS = 'validators'
    _MTA = 'mta'
    _LAST_HEIGHT = 'last_height'

    def __init__(self, db: IconScoreDatabase, key: str = _PROPERTIES) -> None:
        self.__db = DictDB(key, db, value_type=bytes)
        self.__bmc = None
        self.__net_addr = None
        self.__validators = None
        self.__mta = None
        self.__last_height = None

    @property
    def bmc(self) -> Address:
        if self.__bmc is None:
            self.__bmc = Address.from_bytes(self.__db[self._BMC])
        return self.__bmc

    @bmc.setter
    def bmc(self, bmc: Address) -> None:
        self.__db[self._BMC] = bmc.to_bytes()
        self.__bmc = bmc

    @property
    def net_addr(self) -> str:
        if self.__net_addr is None:
            net_addr = self.__db[self._NET]
            self.__net_addr = net_addr.decode('utf-8')
        return self.__net_addr

    @net_addr.setter
    def net_addr(self, net_addr: str) -> None:
        self.__db[self._NET] = bytes(net_addr, 'utf-8')
        self.__net_addr = net_addr

    @property
    def validators(self) -> Validators:
        if self.__validators is None:
            self.__validators = Validators.from_bytes(self.__db[self._VALIDATORS])
        return self.__validators

    @validators.setter
    def validators(self, validators: Validators) -> None:
        self.__db[self._VALIDATORS] = bytes(validators)
        self.__validators = validators

    @property
    def mta(self) -> MerkleTreeAccumulator:
        if self.__mta is None:
            self.__mta = MerkleTreeAccumulator.from_bytes(self.__db[self._MTA])
        return self.__mta

    @mta.setter
    def mta(self, mta: MerkleTreeAccumulator) -> None:
        self.__db[self._MTA] = bytes(mta)
        self.__mta = mta

    @property
    def last_height(self) -> int:
        if self.__last_height is None:
            last_height = self.__db[self._LAST_HEIGHT]
            self.__last_height = int.from_bytes(last_height, "big", signed=True)
        return self.__last_height

    @last_height.setter
    def last_height(self, last_height: int) -> None:
        n_bytes = ((last_height + (last_height < 0)).bit_length() + 8) // 8
        self.__db[self._LAST_HEIGHT] = last_height.to_bytes(n_bytes, byteorder="big", signed=True)
        self.__net_addr = last_height


class BTPMessageVerifier(IconScoreBase):

    def __init__(self, db: IconScoreDatabase) -> None:
        super().__init__(db)

        # Initialize here
        self.__properties = Properties(db)
        self.__mta_updated = False

    def on_install(self, _bmc: Address, _net: str, _validators: str, _offset: int) -> None:
        super().on_install()

        self.__properties.bmc = _bmc
        self.__properties.net_addr = _net
        validators = Validators.from_string(_validators)
        if validators is None:
            raise BMVException(f"invalid validators {_validators}")
        self.__properties.validators = validators
        self.__properties.mta = MerkleTreeAccumulator(_offset)
        self.__properties.last_height = _offset
        bmc_score = self.create_interface_score(_bmc, BMCInterfaceForBMV)
        bmc_score.addVerifier(_net, self.address)

    def on_update(self) -> None:
        super().on_update()

    def _check_accessible(self, cur_addr: BTPAddress, from_addr: BTPAddress) -> None:
        if self.__properties.net_addr != from_addr.net:
            raise BMVException("not acceptable from")
        # TODO caller access control for self.__bmc_addrs = ArrayDB
        # self.msg.sender
        if self.msg.sender != self.__properties.bmc:
            raise BMVException("not acceptable bmc")
        if Address.from_string(cur_addr.contract) != self.__properties.bmc:
            raise BMVException("not acceptable bmc")

    def _last_receipt_hash(self, relay_msg: RelayMessage) -> tuple:
        receipt_hash = bytes
        last_height = 0
        for block_update in relay_msg.block_updates:
            next_height = self.__properties.mta.height + 1
            if next_height == block_update.height:
                if block_update.verify(self.__properties.validators):
                    self.__properties.validators = block_update.next_validators
                self.__properties.mta.add(block_update.block_header.hash)
                self.__mta_updated = True
                receipt_hash = block_update.receipt_hash
                last_height = block_update.height
            elif next_height < block_update.height:
                raise BMVException(f'invalid blockUpdate height {block_update.height} expected:{next_height}',
                                   BMVExceptionCode.INVALID_BLOCK_UPDATE_HEIGHT_HIGHER)
            else:
                raise BMVException(f'invalid blockUpdate height {block_update.height} expected:{next_height}',
                                   BMVExceptionCode.INVALID_BLOCK_UPDATE_HEIGHT_LOWER)
                # raise InvalidBlockUpdateException(
                #     f'invalid blockUpdate height {block_update.height} expected:{next_height}')

        block_proof = relay_msg.block_proof
        if block_proof is not None:
            block_proof.verify(self.__properties.mta)
            receipt_hash = block_proof.receipt_hash
            last_height = block_proof.height
        return receipt_hash, last_height

    @external(readonly=True)
    def mta(self) -> str:
        return base64.urlsafe_b64encode(self.__properties.mta.to_bytes())
    
    @external(readonly=True)
    def bmc(self) -> Address:
        return self.__properties.bmc

    @external(readonly=True)
    def netAddress(self) -> str:
        return self.__properties.net_addr

    @external(readonly=True)
    def validators(self) -> dict:
        return self.__properties.validators.to_dict()

    @external(readonly=True)
    def getStatus(self) -> dict:
        """
        Get status of BMV.
        Used by the relay to resolve next BTP Message to send.
        Called by BMC.

        :return: The object contains followings fields.

            +-------------+---------+--------------------------------------------------+
            | Field       | Type    | Description                                      |
            +=============+=========+==================================================+
            | height      | Integer | height of MerkleTreeAccumulator                  |
            +-------------+---------+--------------------------------------------------+
            | offset      | Integer | offset of MerkleTreeAccumulator                  |
            +-------------+---------+--------------------------------------------------+
            | last_height | Integer | block height of last relayed BTP Message         |
            +-------------+---------+--------------------------------------------------+
        """
        d = self.__properties.mta.get_status()
        d["last_height"] = self.__properties.last_height
        return d

    @external
    def handleRelayMessage(self, _bmc: str, _prev: str, _seq: int, _msg: str) -> list:
        """
        - Decodes Relay Messages and process BTP Messages
        - If there is an error, then it sends a BTP Message containing the Error Message
        - BTP Messages with old sequence numbers are ignored. A BTP Message contains future sequence number will fail.

        :param _bmc: String ( BTP Address of the BMC handling the message )
        :param _prev: String ( BTP Address of the previous BMC )
        :param _seq: Integer ( next sequence number to get a message )
        :param _msg: Bytes ( serialized bytes of Relay Message )
        :return: List of serialized bytes of a BTP Message
        """
        cur_addr = BTPAddress.from_string(_bmc)
        prev_addr = BTPAddress.from_string(_prev)
        self._check_accessible(cur_addr, prev_addr)

        serialized_msg = base64.urlsafe_b64decode(_msg)
        relay_msg = RelayMessage(serialized_msg)
        if len(relay_msg.receipt_proofs) > 0 and len(relay_msg.block_updates) == 0 and relay_msg.block_proof is None:
            raise BMVException("invalid RelayMessage not exists BlockUpdate or BlockProof")

        receipt_hash, last_height = self._last_receipt_hash(relay_msg)

        next_seq = _seq + 1
        btp_msgs = []
        for receipt_proof in relay_msg.receipt_proofs:
            receipt = receipt_proof.prove(receipt_hash)
            for event_log in receipt.event_logs:
                if str(Address.from_bytes(event_log.addr)) != prev_addr.contract:
                    continue
                btp_msg = event_log.to_btp_message()
                if btp_msg is not None:
                    if btp_msg.seq > next_seq:
                        raise BMVException(f'invalid sequence {btp_msg.seq} expected:{next_seq}',
                                           BMVExceptionCode.INVALID_SEQUENCE_HIGHER)
                    elif btp_msg.seq < next_seq:
                        raise BMVException(f'invalid sequence {btp_msg.seq} expected:{next_seq}',
                                           BMVExceptionCode.INVALID_SEQUENCE)
                    elif btp_msg.next_bmc == _bmc:
                        btp_msgs.append(btp_msg.msg)
                        next_seq += 1

        if self.__mta_updated:
            self.__properties.mta = self.__properties.mta

        if len(btp_msgs) > 0:
            self.__properties.last_height = last_height
        return btp_msgs
