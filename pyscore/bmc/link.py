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

import math

from iconservice import Address, IconScoreDatabase

from .relay import BTPRelays, BTPRelay
from ..lib import BTPAddress, BTPException
from ..lib.icon import PropertiesDB, IterableDictDB, remove_from_array_db


class BTPLink(PropertiesDB):
    def __init__(self, prefix, db: IconScoreDatabase, addr: BTPAddress) -> None:
        self.addr = addr  # primary key
        self.rx_seq = 0
        self.tx_seq = 0

        self.block_interval_src = 0
        self.block_interval_dst = 0
        self.max_aggregation = 10  # TODO define default max_aggregation
        self.delay_limit = 3  # if delay_limit < 3, too sensitive

        self.relay_idx = 0
        self.rotate_height = 0
        self.rx_height = 0  # initialize with BMC.block_height
        self.rx_height_src = 0  # initialize with BMV._offset

        # next-relays
        self.next_relays_height = 0
        self.next_relays_term = 0  # 0: disable next-relays, 43200: number of block daily

        # SACK
        self.sack_term = 0  # 0: disable sack
        self.sack_next = 0
        self.sack_height = 0
        self.sack_seq = 0

        if prefix is None:
            prefix = str(addr)
        super().__init__(prefix, db)
        self.relays = BTPRelays(self._sub_prefix("relays"), self._base_db)
        self.reachable = super()._array_db("reachable", str)

        self.next_relays_add = super()._array_db("next_relays_add", Address)
        self.next_relays_remove = super()._array_db("next_relays_remove", Address)

    def _scale(self) -> float:
        if self.block_interval_src < 1 or self.block_interval_dst < 1:
            return 0
        return self.block_interval_src / self.block_interval_dst

    def rotate_term(self) -> int:
        scale = self._scale()
        if scale > 0:
            return math.ceil(self.max_aggregation / self._scale())
        else:
            return 0

    def _rotate(self, rotate_count: int, base_height: int) -> BTPRelay:
        rotate_term = self.rotate_term()
        if rotate_term > 0 and rotate_count > 0:
            self.rotate_height = base_height + rotate_term
            self.relay_idx += rotate_count
            if self.relay_idx >= len(self.relays):
                self.relay_idx = self.relay_idx % len(self.relays)
        return self.relays.at(self.relay_idx)

    def rotate(self, current_height: int, relay_msg_height: int, has_msg: bool = False) -> BTPRelay:
        rotate_term = self.rotate_term()
        if rotate_term > 0:
            if has_msg:
                guess_height = self.rx_height + math.ceil((relay_msg_height - self.rx_height_src) / self._scale()) - 1
                if guess_height > current_height:
                    guess_height = current_height
                rotate_count = math.ceil((guess_height - self.rotate_height)/rotate_term)
                if rotate_count < 0:
                    rotate_count = 0
                base_height = self.rotate_height + ((rotate_count-1) * rotate_term)
                skip_count = math.ceil((current_height - guess_height)/self.delay_limit) - 1
                if skip_count > 0:
                    rotate_count += skip_count
                    base_height = current_height
                self.rx_height = current_height
                self.rx_height_src = relay_msg_height
                return self._rotate(rotate_count, base_height)
            else:
                rotate_count = math.ceil((current_height - self.rotate_height)/rotate_term)
                base_height = self.rotate_height + ((rotate_count-1) * rotate_term)
                return self._rotate(rotate_count, base_height)
        else:
            return None

    def add_relay(self, addr: Address):
        if self.next_relays_term > 0:
            if addr in self.relays or addr in self.next_relays_add:
                raise BTPException("already registered relay")
            if remove_from_array_db(self.next_relays_remove, addr) < 0:
                self.next_relays_add.put(addr)
        else:
            if addr in self.relays:
                raise BTPException("already registered relay")
            self.relays[addr] = self.relays.new_relay(addr)

    def remove_relay(self, addr: Address, current_height: int):
        if self.next_relays_term > 0:
            if addr not in self.relays:
                raise BTPException("not registered relay")
            if addr in self.next_relays_remove:
                raise BTPException("already unregistered relay")
            if remove_from_array_db(self.next_relays_add, addr) < 0:
                self.next_relays_remove.put(addr)
        else:
            if addr not in self.relays:
                raise BTPException("not registered relay")
            self.relays.remove(addr)
            if 0 < len(self.relays) <= self.relay_idx:
                self._rotate(1, current_height)

    def get_next_relays(self) -> list:
        relays = []
        remove_relays = []
        for relay in self.next_relays_remove:
            remove_relays.append(relay)
        for relay in self.relays.keys():
            if relay not in remove_relays:
                relays.append(relay)
        for relay in self.next_relays_add:
            relays.append(relay)
        return relays

    def next_relays(self, current_height: int):
        # TODO how to trigger next_relays?
        if current_height >= self.next_relays_height:
            for i in range(len(self.next_relays_remove)):
                self.relays.remove(self.next_relays_remove.pop())
            for i in range(len(self.next_relays_add)):
                relay = self.next_relays_add.pop()
                self.relays[relay] = self.relays.new_relay(relay)
            self.next_relays_height = self.next_relays_height + self.next_relays_term


class BTPLinks(IterableDictDB):

    def __init__(self, key, db: IconScoreDatabase):
        super().__init__(key, db, BTPLink)

    def raw_to_object(self, raw):
        return self.new_btp_link(BTPAddress.from_string(raw))

    def object_to_raw(self, obj):
        return obj

    def __getitem__(self, key: object) -> BTPLink:
        return super().__getitem__(key)

    def remove(self, key) -> None:
        v = self.__getitem__(key)
        if v is not None:
            v.remove_all()
            super().remove(key)

    def new_btp_link(self, addr: BTPAddress) -> BTPLink:
        return BTPLink(self._sub_prefix(str(addr)), self.base_db, addr)
