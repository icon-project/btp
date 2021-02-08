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
from iconservice import Address

from ..exception import *
from ...lib import BMVException
from ...lib.icon import rlp, base64, Serializable
from ...lib.icon.mta import MerkleTreeAccumulator, MTAException, InvalidWitnessOldException
from ...lib.icon.mpt import MerklePatriciaTree, MPTException
from ...lib.icon.wrap import get_hash, recover_public_key, address_by_public_key


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


class Validators(Serializable):
    def __init__(self, serialized: bytes) -> None:
        self.__bytes = serialized
        self.__addresses = []
        if serialized is not None:
            self.__hash = get_hash(serialized)
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
                    return Validators(base64.urlsafe_b64decode(s))
                else:
                    raise e
        b = rlp.rlp_encode(addresses)
        return Validators(b)

    def from_bytes(self, serialized: bytes) -> 'Validators':
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
        self.__next_validators = Validators(unpacked[2])

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
    class MessageEvent(object):
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

        parsed = EventLog.parse_signature(self.__indexed[0])
        self.__method = parsed[0]
        self.__params = []
        nidx = len(self.__indexed) - 1
        for i in range(len(parsed) - 1):
            if i < nidx:
                val = self.__indexed[i + 1]
            else:
                val = self.__data[i - nidx]
            arg = EventLog.convert_from_bytes(parsed[i + 1], val)
            self.__params.append(arg)

    @staticmethod
    def parse_signature(v: bytes) -> list:
        signature = []
        s = v.decode('utf-8')
        sidx = s.index("(")
        eidx = s.index(")")
        signature.append(s[0:sidx])
        sl = s[sidx+1:eidx].split(",")
        for ts in sl:
            signature.append(ts.strip())
        return signature

    @staticmethod
    def convert_from_bytes(param_type: str, v: bytes):
        if param_type == "int":
            return rlp.from_bytes(v, int)
        elif param_type == "str":
            return rlp.from_bytes(v, str)
        elif param_type == "bool":
            return rlp.from_bytes(v, bool)
        elif param_type == "bytes":
            return v
        elif param_type == "Address":
            return Address.from_bytes(v)
        else:
            raise BMVException(f"{param_type} is not supported type (only int, str, bool, Address, bytes are supported)")

    @property
    def method(self) -> bytes:
        return self.__method

    @property
    def addr(self) -> bytes:
        return self.__addr

    def get_param(self, idx: int):
        return self.__params[idx]

    def to_message_event(self) -> MessageEvent:
        if self.__method == "Message":
            return EventLog.MessageEvent(self.__params)
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

    @property
    def index(self) -> int:
        return self.__index


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

    @property
    def index(self) -> int:
        return self.__index

    @property
    def event_proofs(self) -> list:
        return self.__event_proofs


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
