# Copyright 2021 ICON Foundation
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

from iconservice import *

from .irc31_basic import IRC31Basic
from ..util import require


class IRC31MintBurn(IRC31Basic):

    def __init__(self, db: 'IconScoreDatabase') -> None:
        super().__init__(db)
        # id ==> creator
        self._creators = DictDB('creators', db, value_type=Address)

    @external
    def mint(self, _id: int, _supply: int, _uri: str):
        """
        Creates a new token type and assigns _supply to creator
        """
        require(self._creators[_id] is None, "Token is already minted")
        require(_supply > 0, "Supply should be positive")
        require(len(_uri) > 0, "Uri should be set")

        self._creators[_id] = self.msg.sender
        super()._mint(self.msg.sender, _id, _supply, _uri)

    @external
    def burn(self, _id: int, _amount: int):
        """
        Destroys tokens for a given amount
        """
        require(self._creators[_id] is not None, "Invalid token id")
        require(_amount > 0, "Amount should be positive")

        super()._burn(self.msg.sender, _id, _amount)

    @external
    def setTokenURI(self, _id: int, _uri: str):
        """
        Updates the given token URI
        """
        require(self._creators[_id] == self.msg.sender, "Not token creator")
        require(len(_uri) > 0, "Uri should be set")

        super()._setTokenURI(_id, _uri)
