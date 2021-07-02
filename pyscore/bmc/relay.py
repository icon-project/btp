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

from iconservice import *

from ..lib.icon import PropertiesDB, IterableDictDB


class BTPRelay(PropertiesDB):
    def __init__(self, prefix, db: IconScoreDatabase, addr: Address) -> None:
        self.addr = addr  # primary key
        # TODO define statistics of relay
        self.block_count = 0
        self.msg_count = 0

        if prefix is None:
            prefix = str(addr)
        super().__init__(prefix, db)


class BTPRelays(IterableDictDB):

    def __init__(self, key, db: IconScoreDatabase):
        super().__init__(key, db, BTPRelay)

    def raw_to_object(self, raw):
        return self.new_relay(Address.from_string(raw))

    def object_to_raw(self, obj):
        return obj

    def __getitem__(self, key: object) -> BTPRelay:
        return super().__getitem__(key)

    def remove(self, key) -> None:
        v = self.__getitem__(key)
        if v is not None:
            v.remove_all()
            super().remove(key)

    def new_relay(self, addr: Address) -> BTPRelay:
        return BTPRelay(self._sub_prefix(str(addr)), self.base_db, addr)
