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

from ..lib import BMCException
from ..lib.icon import StringDictDB


class Permission(object):
    _APIS = [
        "addPermission", "removePermission",
        "addService", "removeService",
        "addVerifier", "removeVerifier",
        "addLink", "removeLink",
        "addRoute", "removeRoute",
        "addRelayer", "removeRelayer",
        "addRelay", "removeRelay",
        "renewRelay",
        "sendMessage"
    ]

    def __init__(self, apis: str = None):
        if apis is None:
            self.apis = []
        else:
            self.apis = apis.split(",")

    def __str__(self):
        return ",".join(self.apis)

    def has(self, api: str) -> bool:
        if api in self.apis:
            return True
        else:
            return False

    def is_valid(self) -> bool:
        if len(self.apis) == 0:
            return False
        for api in self.apis:
            if api not in self._APIS:
                return False
        return True

    def merge(self, other: 'Permission'):
        for api in other.apis:
            if api not in self.apis:
                self.apis.append(api)

    def remove(self, other: 'Permission'):
        for api in other.apis:
            if api in self.apis:
                self.apis.remove(api)

    def __iter__(self):
        return self.apis.__iter__()

    def __len__(self):
        return len(self.apis)

    @staticmethod
    def from_string(s: str) -> 'Permission':
        if s is None or len(s) == 0:
            return None
        return Permission(s)

    @staticmethod
    def from_string_with_validation(s: str) -> 'Permission':
        v = Permission.from_string(s)
        if v is None or not v.is_valid():
            raise BMCException("invalid Permission")
        return v


class Permissions(StringDictDB):
    def __getitem__(self, key: object) -> Permission:
        return super().__getitem__(key)

    def str_to_object(self, s: str):
        return Permission.from_string(s)