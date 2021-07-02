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

from iconservice import *

from .message import *
from ...lib import BTPAddress, BMVException
from ...lib.icon import base64, PropertiesDB
from ...lib.icon.mta import MerkleTreeAccumulator

TAG = 'BTPMessageVerifier'


class BTPMessageVerifier(IconScoreBase):

    class Props(PropertiesDB):
        def __init__(self, prefix, db: IconScoreDatabase) -> None:
            self.bmc: Address = None
            self.net_addr = ""
            self.last_height = 0
            self.validators = Validators
            self.mta = MerkleTreeAccumulator
            super().__init__(prefix, db)

        @property
        def validators(self) -> Validators:
            return super().get("validators")

        @validators.setter
        def validators(self, v: Validators):
            pass

        @property
        def mta(self) -> MerkleTreeAccumulator:
            return super().get("mta")

        @mta.setter
        def mta(self, v: MerkleTreeAccumulator):
            pass

    def __init__(self, db: IconScoreDatabase) -> None:
        super().__init__(db)

        # Initialize here
        self._props = BTPMessageVerifier.Props("props", db)
        self._mta_updated = False

    def on_install(self, _bmc: Address, _net: str, _validators: str, _offset: int) -> None:
        super().on_install()

        self._props.bmc = _bmc
        self._props.net_addr = _net
        self._props.validators = Validators.from_string(_validators)
        if self._props.validators is None:
            raise BMVException(f"invalid validators {_validators}")
        mta = MerkleTreeAccumulator()
        mta.offset = _offset
        self._props.mta = mta
        self._props.last_height = _offset

    def on_update(self) -> None:
        super().on_update()

    def _check_accessible(self, cur_addr: BTPAddress, from_addr: BTPAddress) -> None:
        if self._props.net_addr != from_addr.net:
            raise BMVException("not acceptable from")
        if self.msg.sender != self._props.bmc:
            raise BMVException("not acceptable bmc")
        if Address.from_string(cur_addr.contract) != self._props.bmc:
            raise BMVException("not acceptable bmc")

    def _last_receipt_hash(self, relay_msg: RelayMessage) -> tuple:
        receipt_hash = bytes
        last_height = 0
        for block_update in relay_msg.block_updates:
            next_height = self._props.mta.height + 1
            if next_height == block_update.height:
                if block_update.verify(self._props.validators):
                    self._props.validators = block_update.next_validators
                self._props.mta.add(block_update.block_header.hash)
                self._mta_updated = True
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
            block_proof.verify(self._props.mta)
            receipt_hash = block_proof.receipt_hash
            last_height = block_proof.height
        return receipt_hash, last_height

    @external(readonly=True)
    def mta(self) -> str:
        return base64.urlsafe_b64encode(self._props.mta.to_bytes())
    
    @external(readonly=True)
    def bmc(self) -> Address:
        return self._props.bmc

    @external(readonly=True)
    def netAddress(self) -> str:
        return self._props.net_addr

    @external(readonly=True)
    def validators(self) -> dict:
        return self._props.validators.to_dict()

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
        d = self._props.mta.get_status()
        d["last_height"] = self._props.last_height
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
        if len(relay_msg.block_updates) == 0 and relay_msg.block_proof is None:
            raise BMVException("invalid RelayMessage not exists BlockUpdate or BlockProof")

        receipt_hash, last_height = self._last_receipt_hash(relay_msg)

        next_seq = _seq + 1
        msgs = []
        for receipt_proof in relay_msg.receipt_proofs:
            receipt = receipt_proof.prove(receipt_hash)
            for event_log in receipt.event_logs:
                if str(Address.from_bytes(event_log.addr)) != prev_addr.contract:
                    continue
                msg_evt = event_log.to_message_event()
                if msg_evt is not None:
                    if msg_evt.seq > next_seq:
                        raise BMVException(f'invalid sequence {msg_evt.seq} expected:{next_seq}',
                                           BMVExceptionCode.INVALID_SEQUENCE_HIGHER)
                    elif msg_evt.seq < next_seq:
                        raise BMVException(f'invalid sequence {msg_evt.seq} expected:{next_seq}',
                                           BMVExceptionCode.INVALID_SEQUENCE)
                    elif msg_evt.next_bmc == _bmc:
                        msgs.append(msg_evt.msg)
                        next_seq += 1

        if self._mta_updated:
            self._props.mta = self._props.mta

        if len(msgs) > 0:
            self._props.last_height = last_height
        return msgs
