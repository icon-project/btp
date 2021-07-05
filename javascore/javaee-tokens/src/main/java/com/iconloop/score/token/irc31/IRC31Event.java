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
import score.annotation.EventLog;

import java.math.BigInteger;

public interface IRC31Event {
    /**
     * (EventLog) Must trigger on any successful token transfers, including zero value transfers as well as minting or burning.
     *     When minting/creating tokens, the `_from` must be set to zero address.
     *     When burning/destroying tokens, the `_to` must be set to zero address.
     *
     * @param _operator (indexed) the address of an account/contract that is approved to make the transfer
     * @param _from (indexed) the address of the token holder whose balance is decreased
     * @param _to (indexed) the address of the recipient whose balance is increased
     * @param _id ID of the token
     * @param _value the amount of transfer
     */
    @EventLog(indexed = 3)
    void TransferSingle(Address _operator, Address _from, Address _to, BigInteger _id, BigInteger _value);

    /**
     * (EventLog) Must trigger on any successful token transfers, including zero value transfers as well as minting or burning.
     *     When minting/creating tokens, the `_from` must be set to zero address.
     *     When burning/destroying tokens, the `_to` must be set to zero address.
     *
     * @param _operator (indexed) the address of an account/contract that is approved to make the transfer
     * @param _from (indexed) the address of the token holder whose balance is decreased
     * @param _to (indexed) the address of the recipient whose balance is increased
     * @param _ids serialized bytes of list for token IDs (order and length must match `_values`)
     * @param _values serialized bytes of list for transfer amounts per token (order and length must match `_ids`)
     *
     * @apiNote RLP (Recursive Length Prefix) would be used for the serialized bytes to represent list type.
     */
    @EventLog(indexed = 3)
    void TransferBatch(Address _operator, Address _from, Address _to, byte[] _ids, byte[] _values);

    /**
     * (EventLog) Must trigger on any successful approval (either enabled or disabled) for a third party/operator address
     *     to manage all tokens for the `_owner` address.
     * @param _owner (indexed) the address of the token holder
     * @param _operator (indexed) the address of authorized operator
     * @param _approved true if the operator is approved, false to revoke approval
     */
    @EventLog(indexed = 2)
    void ApprovalForAll(Address _owner, Address _operator, boolean _approved);

    /**
     * (EventLog) Must trigger on any successful URI updates for a token ID.
     *     URIs are defined in RFC 3986.
     *     The URI must point to a JSON file that conforms to the "ERC-1155 Metadata URI JSON Schema".
     *
     * @param _id (indexed) ID of the token
     * @param _value the updated URI string
     */
    @EventLog(indexed = 1)
    void URI(BigInteger _id, String _value);
}
