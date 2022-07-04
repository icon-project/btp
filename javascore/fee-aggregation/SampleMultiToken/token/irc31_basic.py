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

from .irc31_receiver import IRC31ReceiverInterface
from ..util import ZERO_ADDRESS, require
from ..util.rlp import rlp_encode_list


class IRC31Basic(IconScoreBase):

    def __init__(self, db: 'IconScoreDatabase') -> None:
        super().__init__(db)
        # id => (owner => balance)
        self._balances = DictDB('balances', db, value_type=int, depth=2)
        # owner => (operator => approved)
        self._operatorApproval = DictDB('approval', db, value_type=bool, depth=2)
        # id => token URI
        self._tokenURIs = DictDB('token_uri', db, value_type=str)

    def on_install(self) -> None:
        super().on_install()

    def on_update(self) -> None:
        super().on_update()

    @external(readonly=True)
    def balanceOf(self, _owner: Address, _id: int) -> int:
        """
        Returns the balance of the owner's tokens.

        :param _owner: the address of the token holder
        :param _id: ID of the token
        :return: the _owner's balance of the token type requested
        """
        return self._balances[_id][_owner]

    @external(readonly=True)
    def balanceOfBatch(self, _owners: List[Address], _ids: List[int]) -> List[int]:
        """
        Returns the balance of multiple owner/id pairs.

        :param _owners: the addresses of the token holders
        :param _ids: IDs of the tokens
        :return: the list of balance (i.e. balance for each owner/id pair)
        """
        require(len(_owners) == len(_ids), "owner/id pairs mismatch")

        balances = []
        for i in range(len(_owners)):
            balances.append(self._balances[_ids[i]][_owners[i]])
        return balances

    @external(readonly=True)
    def tokenURI(self, _id: int) -> str:
        """
        Returns an URI for a given token ID.

        :param _id: ID of the token
        :return: the URI string
        """
        return self._tokenURIs[_id]

    @external
    def transferFrom(self, _from: Address, _to: Address, _id: int, _value: int, _data: bytes = None):
        """
        Transfers `_value` amount of an token `_id` from one address to another address,
        and must emit `TransferSingle` event to reflect the balance change.
        When the transfer is complete, this method must invoke `onIRC31Received(Address,Address,int,int,bytes)` in `_to`,
        if `_to` is a contract. If the `onIRC31Received` method is not implemented in `_to` (receiver contract),
        then the transaction must fail and the transfer of tokens should not occur.
        If `_to` is an externally owned address, then the transaction must be sent without trying to execute
        `onIRC31Received` in `_to`.
        Additional `_data` can be attached to this token transaction, and it should be sent unaltered in call
        to `onIRC31Received` in `_to`. `_data` can be empty.
        Throws unless the caller is the current token holder or the approved address for the token ID.
        Throws if `_from` does not have enough amount to transfer for the token ID.
        Throws if `_to` is the zero address.

        :param _from: source address
        :param _to: target address
        :param _id: ID of the token
        :param _value: the amount of transfer
        :param _data: additional data that should be sent unaltered in call to `_to`
        """
        require(_to != ZERO_ADDRESS, "_to must be non-zero")
        require(_from == self.msg.sender or self.isApprovedForAll(_from, self.msg.sender),
                "Need operator approval for 3rd party transfers")
        require(0 <= _value <= self._balances[_id][_from], "Insufficient funds")

        # transfer funds
        self._balances[_id][_from] = self._balances[_id][_from] - _value
        self._balances[_id][_to] = self._balances[_id][_to] + _value

        # emit event
        self.TransferSingle(self.msg.sender, _from, _to, _id, _value)

        if _to.is_contract:
            # call `onIRC31Received` if the recipient is a contract
            recipient_score = self.create_interface_score(_to, IRC31ReceiverInterface)
            recipient_score.onIRC31Received(self.msg.sender, _from, _id, _value,
                                            b'' if _data is None else _data)

    @external
    def transferFromBatch(self, _from: Address, _to: Address, _ids: List[int], _values: List[int], _data: bytes = None):
        """
        Transfers `_values` amount(s) of token(s) `_ids` from one address to another address,
        and must emit `TransferSingle` or `TransferBatch` event(s) to reflect all the balance changes.
        When all the transfers are complete, this method must invoke `onIRC31Received(Address,Address,int,int,bytes)` or
        `onIRC31BatchReceived(Address,Address,int[],int[],bytes)` in `_to`,
        if `_to` is a contract. If the `onIRC31Received` method is not implemented in `_to` (receiver contract),
        then the transaction must fail and the transfers of tokens should not occur.
        If `_to` is an externally owned address, then the transaction must be sent without trying to execute
        `onIRC31Received` in `_to`.
        Additional `_data` can be attached to this token transaction, and it should be sent unaltered in call
        to `onIRC31Received` in `_to`. `_data` can be empty.
        Throws unless the caller is the current token holder or the approved address for the token IDs.
        Throws if length of `_ids` is not the same as length of `_values`.
        Throws if `_from` does not have enough amount to transfer for any of the token IDs.
        Throws if `_to` is the zero address.

        :param _from: source address
        :param _to: target address
        :param _ids: IDs of the tokens (order and length must match `_values` list)
        :param _values: transfer amounts per token (order and length must match `_ids` list)
        :param _data: additional data that should be sent unaltered in call to `_to`
        """
        require(_to != ZERO_ADDRESS, "_to must be non-zero")
        require(len(_ids) == len(_values), "id/value pairs mismatch")
        require(_from == self.msg.sender or self.isApprovedForAll(_from, self.msg.sender),
                "Need operator approval for 3rd party transfers.")

        for i in range(len(_ids)):
            _id = _ids[i]
            _value = _values[i]
            require(0 <= _value <= self._balances[_id][_from], "Insufficient funds")

            # transfer funds
            self._balances[_id][_from] = self._balances[_id][_from] - _value
            self._balances[_id][_to] = self._balances[_id][_to] + _value

        # emit event
        self.TransferBatch(self.msg.sender, _from, _to, rlp_encode_list(_ids), rlp_encode_list(_values))

        if _to.is_contract:
            # call `onIRC31BatchReceived` if the recipient is a contract
            recipient_score = self.create_interface_score(_to, IRC31ReceiverInterface)
            recipient_score.onIRC31BatchReceived(self.msg.sender, _from, _ids, _values,
                                                 b'' if _data is None else _data)

    @external
    def setApprovalForAll(self, _operator: Address, _approved: bool):
        """
        Enables or disables approval for a third party ("operator") to manage all of the caller's tokens,
        and must emit `ApprovalForAll` event on success.

        :param _operator: address to add to the set of authorized operators
        :param _approved: true if the operator is approved, false to revoke approval
        """
        self._operatorApproval[self.msg.sender][_operator] = _approved
        self.ApprovalForAll(self.msg.sender, _operator, _approved)

    @external(readonly=True)
    def isApprovedForAll(self, _owner: Address, _operator: Address) -> bool:
        """
        Returns the approval status of an operator for a given owner.

        :param _owner: the owner of the tokens
        :param _operator: the address of authorized operator
        :return: true if the operator is approved, false otherwise
        """
        return self._operatorApproval[_owner][_operator]

    @eventlog(indexed=3)
    def TransferSingle(self, _operator: Address, _from: Address, _to: Address, _id: int, _value: int):
        """
        Must trigger on any successful token transfers, including zero value transfers as well as minting or burning.
        When minting/creating tokens, the `_from` must be set to zero address.
        When burning/destroying tokens, the `_to` must be set to zero address.

        :param _operator: the address of an account/contract that is approved to make the transfer
        :param _from: the address of the token holder whose balance is decreased
        :param _to: the address of the recipient whose balance is increased
        :param _id: ID of the token
        :param _value: the amount of transfer
        """
        pass

    @eventlog(indexed=3)
    def TransferBatch(self, _operator: Address, _from: Address, _to: Address, _ids: bytes, _values: bytes):
        """
        Must trigger on any successful token transfers, including zero value transfers as well as minting or burning.
        When minting/creating tokens, the `_from` must be set to zero address.
        When burning/destroying tokens, the `_to` must be set to zero address.

        :param _operator: the address of an account/contract that is approved to make the transfer
        :param _from: the address of the token holder whose balance is decreased
        :param _to: the address of the recipient whose balance is increased
        :param _ids: serialized bytes of list for token IDs (order and length must match `_values`)
        :param _values: serialized bytes of list for transfer amounts per token (order and length must match `_ids`)

        NOTE: RLP (Recursive Length Prefix) would be used for the serialized bytes to represent list type.
        """
        pass

    @eventlog(indexed=2)
    def ApprovalForAll(self, _owner: Address, _operator: Address, _approved: bool):
        """
        Must trigger on any successful approval (either enabled or disabled) for a third party/operator address
        to manage all tokens for the `_owner` address.

        :param _owner: the address of the token holder
        :param _operator: the address of authorized operator
        :param _approved: true if the operator is approved, false to revoke approval
        """
        pass

    @eventlog(indexed=1)
    def URI(self, _id: int, _value: str):
        """
        Must trigger on any successful URI updates for a token ID.
        URIs are defined in RFC 3986.
        The URI must point to a JSON file that conforms to the "ERC-1155 Metadata URI JSON Schema".

        :param _id: ID of the token
        :param _value: the updated URI string
        """
        pass

    # ===============================================================================================
    # Internal methods
    # ===============================================================================================

    def _mint(self, _owner: Address, _id: int, _supply: int, _uri: str):
        self._balances[_id][_owner] = _supply

        # emit transfer event for Mint semantic
        self.TransferSingle(_owner, ZERO_ADDRESS, _owner, _id, _supply)

        # set token URI and emit event
        self._setTokenURI(_id, _uri)

    def _burn(self, _owner: Address, _id: int, _amount: int):
        require(0 <= _amount <= self._balances[_id][_owner], "Not an owner or invalid amount")
        self._balances[_id][_owner] -= _amount

        # emit transfer event for Burn semantic
        self.TransferSingle(_owner, _owner, ZERO_ADDRESS, _id, _amount)

    def _setTokenURI(self, _id: int, _uri: str):
        self._tokenURIs[_id] = _uri
        self.URI(_id, _uri)
