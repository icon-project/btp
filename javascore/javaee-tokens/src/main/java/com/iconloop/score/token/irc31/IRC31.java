/*
 * Copyright 2021 ICON Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.iconloop.score.token.irc31;

import score.Address;
import score.ByteArrayObjectWriter;
import score.Context;
import score.annotation.Optional;

import java.math.BigInteger;

public interface IRC31 {
    /**
     * Returns the balance of the owner's tokens.
     *
     * @param _owner the address of the token holder
     * @param _id ID of the token
     * @return the _owner's balance of the token type requested
     */
    BigInteger balanceOf(Address _owner, BigInteger _id);

    /**
     * Returns the balance of multiple owner/id pairs.
     *
     * @param _owners the addresses of the token holders
     * @param _ids IDs of the tokens
     * @return the list of balance (i.e. balance for each owner/id pair)
     */
    BigInteger[] balanceOfBatch(Address[] _owners, BigInteger[] _ids);

    /**
     * Transfers `_value` amount of an token `_id` from one address to another address,
     *     and must emit `TransferSingle` event to reflect the balance change.
     *     When the transfer is complete, this method must invoke `onIRC31Received(Address,Address,int,int,bytes)` in `_to`,
     *     if `_to` is a contract. If the `onIRC31Received` method is not implemented in `_to` (receiver contract),
     *     then the transaction must fail and the transfer of tokens should not occur.
     *     If `_to` is an externally owned address, then the transaction must be sent without trying to execute
     *     `onIRC31Received` in `_to`.
     *     Additional `_data` can be attached to this token transaction, and it should be sent unaltered in call
     *     to `onIRC31Received` in `_to`. `_data` can be empty.
     *     Throws unless the caller is the current token holder or the approved address for the token ID.
     *     Throws if `_from` does not have enough amount to transfer for the token ID.
     *     Throws if `_to` is the zero address.
     *
     * @param _from source address
     * @param _to target address
     * @param _id ID of the token
     * @param _value the amount of transfer
     * @param _data additional data that should be sent unaltered in call to `_to`
     */
    void transferFrom(Address _from, Address _to, BigInteger _id, BigInteger _value, @Optional  byte[] _data);

    /**
     * Transfers `_values` amount(s) of token(s) `_ids` from one address to another address,
     *     and must emit `TransferSingle` or `TransferBatch` event(s) to reflect all the balance changes.
     *     When all the transfers are complete, this method must invoke `onIRC31Received(Address,Address,int,int,bytes)` or
     *     `onIRC31BatchReceived(Address,Address,int[],int[],bytes)` in `_to`,
     *     if `_to` is a contract. If the `onIRC31Received` method is not implemented in `_to` (receiver contract),
     *     then the transaction must fail and the transfers of tokens should not occur.
     *     If `_to` is an externally owned address, then the transaction must be sent without trying to execute
     *     `onIRC31Received` in `_to`.
     *     Additional `_data` can be attached to this token transaction, and it should be sent unaltered in call
     *     to `onIRC31Received` in `_to`. `_data` can be empty.
     *     Throws unless the caller is the current token holder or the approved address for the token IDs.
     *     Throws if length of `_ids` is not the same as length of `_values`.
     *     Throws if `_from` does not have enough amount to transfer for any of the token IDs.
     *     Throws if `_to` is the zero address.
     *
     * @param _from source address
     * @param _to target address
     * @param _ids IDs of the tokens (order and length must match `_values` list)
     * @param _values transfer amounts per token (order and length must match `_ids` list)
     * @param _data additional data that should be sent unaltered in call to `_to`
     */
    void transferFromBatch(Address _from, Address _to, BigInteger[] _ids, BigInteger[] _values, @Optional  byte[] _data);

    /**
     * Enables or disables approval for a third party ("operator") to manage all of the caller's tokens,
     *     and must emit `ApprovalForAll` event on success.
     *
     * @param _operator address to add to the set of authorized operators
     * @param _approved true if the operator is approved, false to revoke approval
     */
    void setApprovalForAll(Address _operator, boolean _approved);

    /**
     * Returns the approval status of an operator for a given owner.
     *
     * @param _owner the owner of the tokens
     * @param _operator the address of authorized operator
     * @return true if the operator is approved, false otherwise
     */
    boolean isApprovedForAll(Address _owner, Address _operator);

}
