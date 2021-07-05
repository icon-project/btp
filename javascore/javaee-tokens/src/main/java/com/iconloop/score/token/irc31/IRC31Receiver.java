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
import score.annotation.External;

import java.math.BigInteger;

/**
 * Smart contracts that want to receive tokens from IRC31-compatible token contracts must implement all of the following receiver methods to accept transfers.
 */
public interface IRC31Receiver {
    /**
     * A method for handling a single token type transfer, which is called from the multi token contract.
     *     It works by analogy with the fallback method of the normal transactions and returns nothing.
     *     Throws if it rejects the transfer.
     *
     * @param _operator The address which initiated the transfer
     * @param _from the address which previously owned the token
     * @param _id the ID of the token being transferred
     * @param _value the amount of tokens being transferred
     * @param _data additional data with no specified format
     */
    @External
    void onIRC31Received(Address _operator, Address _from, BigInteger _id, BigInteger _value, byte[] _data);

    /**
     * A method for handling multiple token type transfers, which is called from the multi token contract.
     *     It works by analogy with the fallback method of the normal transactions and returns nothing.
     *     Throws if it rejects the transfer.
     *
     * @param _operator The address which initiated the transfer
     * @param _from the address which previously owned the token
     * @param _ids the list of IDs of each token being transferred (order and length must match `_values` list)
     * @param _values the list of amounts of each token being transferred (order and length must match `_ids` list)
     * @param _data additional data with no specified format
     */
    @External
    void onIRC31BatchReceived(Address _operator, Address _from, BigInteger[] _ids, BigInteger[] _values, byte[] _data);
}
