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

package foundation.icon.btp.nativecoin.irc31;

import foundation.icon.btp.lib.OwnerManager;
import foundation.icon.btp.lib.OwnerManagerImpl;
import com.iconloop.score.token.irc31.IRC31Basic;
import foundation.icon.btp.nativecoin.NCSException;
import score.Address;
import score.Context;
import score.DictDB;
import score.annotation.External;

import java.math.BigInteger;

public class OwnerBasedIRC31Supplier extends IRC31Basic implements IRC31Supplier, OwnerManager {

    private final DictDB<BigInteger, BigInteger> supplies = Context.newDictDB("supplies", BigInteger.class);
    private final OwnerManager ownerManager = new OwnerManagerImpl("owners");

    @External(readonly = true)
    public BigInteger totalSupply(BigInteger _id) {
        BigInteger supply = supplies.get(_id);
        //"Invalid token id"
        Context.require(supply != null);
        return supply;
    }

    @External
    public void mint(Address _owner, BigInteger _id, BigInteger _amount) {
        requireOwnerAccess();
        //"Amount should be positive"
        Context.require(_amount.compareTo(BigInteger.ZERO) > 0);

        supplies.set(_id, supplies.getOrDefault(_id, BigInteger.ZERO).add(_amount));

        //FIXME IRC31Basic._mint should emit
        //  TransferSingle(Context.caller(), ZERO_ADDRESS, owner, id, amount);
        super._mint(_owner, _id, _amount);
    }

    @External
    public void mintBatch(Address _owner, BigInteger[] _ids, BigInteger[] _amounts) {
        requireOwnerAccess();
        //"id/amount pairs mismatch"
        Context.require(_ids.length == _amounts.length);

        for (int i = 0; i < _ids.length; i++) {
            BigInteger id = _ids[i];
            BigInteger amount = _amounts[i];
            //"Amount should be positive"
            Context.require(amount.compareTo(BigInteger.ZERO) > 0);

            supplies.set(id, supplies.getOrDefault(id, BigInteger.ZERO).add(amount));
        }

        //FIXME IRC31Basic._mintBatch should emit
        //  TransferBatch(Context.caller(), ZERO_ADDRESS, owner, ids, amounts);
        super._mintBatch(_owner, _ids, _amounts);
    }

    @External
    public void burn(Address _owner, BigInteger _id, BigInteger _amount) {
        requireOwnerAccess();
        //"Amount should be positive"
        Context.require(_amount.compareTo(BigInteger.ZERO) > 0);
        BigInteger supply = supplies.get(_id);
        //"Invalid token id"
        Context.require(supply != null);
        supplies.set(_id, supply.subtract(_amount));

        super._burn(_owner, _id, _amount);
    }

    @External
    public void burnBatch(Address _owner, BigInteger[] _ids, BigInteger[] _amounts) {
        requireOwnerAccess();
        //"id/amount pairs mismatch"
        Context.require(_ids.length == _amounts.length);

        for (int i = 0; i < _ids.length; i++) {
            BigInteger id = _ids[i];
            BigInteger amount = _amounts[i];
            //"Amount should be positive"
            Context.require(amount.compareTo(BigInteger.ZERO) > 0);

            BigInteger supply = supplies.get(id);
            //"Invalid token id"
            Context.require(supply != null);
            supplies.set(id, supply.subtract(amount));
        }

        super._burnBatch(_owner, _ids, _amounts);
    }

    @External
    public void setTokenURI(BigInteger _id, String _uri) {
        requireOwnerAccess();
        //"Uri should be set"
        Context.require(!_uri.isEmpty());

        super._setTokenURI(_id, _uri);
    }

    /* Delegate OwnerManager */
    private void requireOwnerAccess() {
        Context.require(ownerManager.isOwner(Context.getCaller()));
    }

    @External
    public void addOwner(Address _addr) {
        try {
            ownerManager.addOwner(_addr);
        } catch (IllegalStateException | IllegalArgumentException e) {
            Context.revert(0, e.getMessage());
        }
    }

    @External
    public void removeOwner(Address _addr) {
        try {
            ownerManager.removeOwner(_addr);
        } catch (IllegalStateException | IllegalArgumentException e) {
            Context.revert(0, e.getMessage());
        }
    }

    @External(readonly = true)
    public Address[] getOwners() {
        return ownerManager.getOwners();
    }

    @External(readonly = true)
    public boolean isOwner(Address _addr) {
        return ownerManager.isOwner(_addr);
    }
}
