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

package foundation.icon.btp.irc2Tradeable;

import foundation.icon.btp.lib.OwnerManager;
import foundation.icon.btp.lib.OwnerManagerImpl;
import score.*;
import score.annotation.EventLog;
import score.annotation.External;
import score.annotation.Payable;
import score.annotation.Optional;

import java.math.BigInteger;
import java.util.List;

public class IRC2Tradeable extends IRC2Basic implements OwnerManager {
    private final BranchDB<Address, DictDB<Address, BigInteger>> allowances = Context.newBranchDB("allowances", BigInteger.class);
    private final Address ZERO_ADDRESS = Address.fromString("hx0000000000000000000000000000000000000000");

    private final OwnerManager ownerManager = new OwnerManagerImpl("owners");

    public IRC2Tradeable(String _name, String _symbol, int _decimals) {
        super(_name, _symbol, _decimals);
    }

    @External
    public void burn(BigInteger _amount) {
        requireOwnerAccess();
        _burn(Context.getCaller(), _amount);
    }

    @External(readonly=true)
    public BigInteger allowance(Address owner, Address spender) {
        return this.allowances.at(owner).getOrDefault(spender, BigInteger.ZERO);
    }

    /**
     * Creates _amount number of tokens, and assigns to caller.
     * Increases the balance of that account and the total supply.
     */
    @External
    public void mint(Address _to, BigInteger _amount) {
        // simple access control - only the minter can mint new token
        requireOwnerAccess();
        _mint(_to, _amount);
    }

    @External
    public boolean approve(Address spender, BigInteger amount) {
        this._approve(Context.getCaller(), spender, amount);
        return true;
    }

    @External
    public boolean transferFrom(
        Address _from,
        Address _to,
        BigInteger _value,
        @Optional byte[] _data
    ) {
        BigInteger currentAllowance = this.allowances.at(_from).getOrDefault(Context.getCaller(), BigInteger.ZERO);
        Context.require(currentAllowance.compareTo(_value) >= 0, "IRC20: transfer amount exceeds allowance");
        _approve(_from, Context.getCaller(), currentAllowance.subtract(_value));
        
        this._transfer(_from, _to, _value, _data);
        return true;
    }

    @External
    public boolean increaseAllowance(Address spender, BigInteger addedValue) {
        _approve(Context.getCaller(), spender, this.allowances.at(Context.getCaller()).getOrDefault(spender, BigInteger.ZERO).add(addedValue));
        return true;
    }

    @External
    public boolean decreaseAllowance(Address spender, BigInteger subtractedValue) {
        BigInteger currentAllowance = this.allowances.at(Context.getCaller()).getOrDefault(spender, BigInteger.ZERO);
        Context.require(currentAllowance.compareTo(subtractedValue) >= 0, "IRC20: decreased allowance below zero");
        _approve(Context.getCaller(), spender, this.allowances.at(Context.getCaller()).getOrDefault(spender, BigInteger.ZERO).subtract(subtractedValue));
        return true;
    }

    private void _approve(
        Address owner,
        Address spender,
        BigInteger amount
    ) {
        Context.require(owner != ZERO_ADDRESS, "IRC20: approve from the zero address");
        Context.require(spender != ZERO_ADDRESS, "IRC20: approve to the zero address");

        this.allowances.at(owner).set(spender, amount);

        // emit Approval event
        Approval(owner, spender, amount);
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


    @EventLog(indexed = 2)
    protected void Approval(Address owner, Address spender, BigInteger value) {};
}