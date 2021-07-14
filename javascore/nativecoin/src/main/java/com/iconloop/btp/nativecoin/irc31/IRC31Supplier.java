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

package com.iconloop.btp.nativecoin.irc31;

import com.iconloop.score.token.irc31.IRC31;
import score.Address;
import score.annotation.EventLog;
import score.annotation.External;

import java.math.BigInteger;

public interface IRC31Supplier extends IRC31 {

    /**
     * Returns total amount of tokens in with a given id
     *
     * @param _id ID of the token
     * @return total amount of tokens in with a given id
     */
    @External(readonly = true)
    BigInteger totalSupply(BigInteger _id);

    /**
     * Mint token
     *
     * @param _owner the address of the token holder
     * @param _id ID of the token
     * @param _amount amount of the token
     */
    @External
    void mint(Address _owner, BigInteger _id, BigInteger _amount);

    /**
     * Mint tokens
     *
     * @param _owner the address of the token holder
     * @param _ids IDs of the tokens
     * @param _amounts transfer amounts per token (order and length must match `_ids` list)
     */
    @External
    void mintBatch(Address _owner, BigInteger[] _ids, BigInteger[] _amounts);

    /**
     * Burn tokens for a given amount
     *
     * @param _owner the address of the token holder
     * @param _id ID of the token
     * @param _amount amount to burn
     */
    @External
    void burn(Address _owner, BigInteger _id, BigInteger _amount);

    /**
     * Burn tokens for a given amount
     *
     * @param _owner the address of the token holder
     * @param _ids IDs of the tokens
     * @param _amounts transfer amounts per token (order and length must match `_ids` list)
     */
    @External
    void burnBatch(Address _owner, BigInteger[] _ids, BigInteger[] _amounts);

    /**
     * Updates the given token URI
     *
     * @param _id ID of the token
     * @param _uri the URI string
     */
    @External
    void setTokenURI(BigInteger _id, String _uri);

    //Annotate for ScoreInterface generation
    @EventLog
    void TransferSingle(Address _operator, Address _from, Address _to, BigInteger _id, BigInteger _value);

    @EventLog
    void TransferBatch(Address _operator, Address _from, Address _to, byte[] _ids, byte[] _values);

    @EventLog
    void ApprovalForAll(Address _owner, Address _operator, boolean _approved);

    @EventLog
    void URI(BigInteger _id, String _value);
}
