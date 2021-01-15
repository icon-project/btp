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

from ..lib.iconee import StringDictDB, BTPAddress


class BTPRoutes(StringDictDB):
    def __getitem__(self, key) -> BTPAddress:
        return super().__getitem__(key)

    def str_to_object(self, s: str):
        return BTPAddress.from_string(s)

    def resolve(self, dst_net: str) -> BTPAddress:
        if self.__contains__(dst_net):
            return self.__getitem__(dst_net)
        else:
            for key in self.keys():
                dst = BTPAddress.from_string(key)
                if dst is not None and dst.net == dst_net:
                    return self.__getitem__(key)
            return None
