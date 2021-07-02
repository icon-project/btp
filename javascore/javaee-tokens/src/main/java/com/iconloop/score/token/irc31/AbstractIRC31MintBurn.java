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
import score.Context;
import score.DictDB;
import score.annotation.External;

import java.math.BigInteger;

public abstract class AbstractIRC31MintBurn extends AbstractIRC31 {
    //id ==> creator
    private final DictDB<BigInteger, Address> creators = Context.newDictDB("creators", Address.class);

    /**
     * Creates a new token type and assigns _supply to creator
     *
     * @param _id ID of the token
     * @param _amount amount of the token
     * @param _uri URI of the token
     */
    @External
    public void mint(BigInteger _id, BigInteger _amount, String _uri) {
        Address creator = creators.get(_id);
        Address caller = Context.getCaller();
        //"Token is already minted"
        Context.require(creator == null);
        //"Supply should be positive"
        Context.require(_amount.compareTo(BigInteger.ZERO) > 0);
        //"Uri should be set"
        Context.require(!_uri.isEmpty());

        creators.set(_id, caller);
        super.mint(caller, _id, _amount);
        super.setTokenURI(_id, _uri);
    }

    /**
     * Destroys tokens for a given amount
     *
     * @param _id ID of the token
     * @param _amount amount to burn
     */
    @External
    public void burn(BigInteger _id, BigInteger _amount) {
        Address creator = creators.get(_id);
        //"Invalid token id"
        Context.require(creator != null);
        //"Amount should be positive"
        Context.require(_amount.compareTo(BigInteger.ZERO) > 0);

        super.burn(Context.getCaller(), _id, _amount);
    }

    /**
     * Updates the given token URI
     *
     * @param _id ID of the token
     * @param _uri the URI string
     */
    @External
    public void setTokenURI(BigInteger _id, String _uri) {
        //"Not token creator"
        Context.require(Context.getCaller().equals(creators.get(_id)));
        //"Uri should be set"
        Context.require(!_uri.isEmpty());

        super.setTokenURI(_id, _uri);
    }
}
