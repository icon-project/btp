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

import java.math.BigInteger;

public interface IRC31Supplier extends IRC31, IRC31Metadata {
    /**
     * Returns total amount of tokens in with a given id
     *
     * @param _id ID of the token
     * @return total amount of tokens in with a given id
     */
    BigInteger totalSupply(BigInteger _id);

    /**
     * Mint token
     *
     * @param _owner the address of the token holder
     * @param _id ID of the token
     * @param _amount amount of the token
     */
    void mint(Address _owner, BigInteger _id, BigInteger _amount);

    /**
     * Mint tokens
     *
     * @param _owner the address of the token holder
     * @param _ids IDs of the tokens
     * @param _amounts transfer amounts per token (order and length must match `_ids` list)
     */
    void mintBatch(Address _owner, BigInteger[] _ids, BigInteger[] _amounts);

    /**
     * Burn tokens for a given amount
     *
     * @param _owner the address of the token holder
     * @param _id ID of the token
     * @param _amount amount to burn
     */
    void burn(Address _owner, BigInteger _id, BigInteger _amount);

    /**
     * Burn tokens for a given amount
     *
     * @param _owner the address of the token holder
     * @param _ids IDs of the tokens
     * @param _amounts transfer amounts per token (order and length must match `_ids` list)
     */
    void burnBatch(Address _owner, BigInteger[] _ids, BigInteger[] _amounts);

    /**
     * Updates the given token URI
     *
     * @param _id ID of the token
     * @param _uri the URI string
     */
    void setTokenURI(BigInteger _id, String _uri);

}
