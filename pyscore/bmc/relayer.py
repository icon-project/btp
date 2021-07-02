#  Copyright 2020 ICON Foundation
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#      http://www.apache.org/licenses/LICENSE-2.0
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
from iconservice import *

from ..lib import BTPException, BTPAddress
from ..lib.icon import IterableDictDB, PropertiesDB


class BTPRelayer(PropertiesDB):
    def __init__(self, prefix, db: IconScoreDatabase, addr: Address) -> None:
        self.addr = addr
        self.desc = ""

        if prefix is None:
            prefix = str(addr)
        super().__init__(prefix, db)
        # TODO BTPRelayer.relays => rx_relays, tx_relays
        #   compositKey(link,addressOfRelay) for identifier
        self.relays = super()._address_dict_db("relays")

    def add_relay(self, link: BTPAddress, addr: Address):
        if link in self.relays:
            raise BTPException("already exists relay")
        self.relays[link] = addr

    def remove_relay(self, link: BTPAddress, addr: Address):
        if link not in self.relays:
            raise BTPException("not found relay")
        if addr != self.relays[link]:
            raise BTPException("invalid address")
        else:
            self.relays.remove(link)


class BTPRelayers(IterableDictDB):

    def __init__(self, key, db: IconScoreDatabase):
        super().__init__(key, db, BTPRelayer)

    def raw_to_object(self, raw):
        return self.new_relayer(Address.from_string(raw))

    def object_to_raw(self, obj):
        return obj

    def __getitem__(self, key: object) -> BTPRelayer:
        return super().__getitem__(key)

    def remove(self, key) -> None:
        v = self.__getitem__(key)
        if v is not None:
            v.remove_all()
            super().remove(key)

    def new_relayer(self, addr: Address) -> BTPRelayer:
        return BTPRelayer(self._sub_prefix(str(addr)), self.base_db, addr)
