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

package com.iconloop.score.util;

import score.Address;
import score.ArrayDB;
import score.Context;
import scorex.util.ArrayList;

import java.util.List;

public class OwnerManagerImpl implements OwnerManager {
    private final ArrayDB<Address> owners;

    public OwnerManagerImpl(String id) {
        owners = Context.newArrayDB(id, Address.class);
    }

    protected int indexOf(Address address) {
        for (int i = 0; i < owners.size(); i++) {
            if (owners.get(i).equals(address)) {
                return i;
            }
        }
        return -1;
    }

    protected void add(Address address) throws IllegalArgumentException {
        int idx = indexOf(address);
        if (idx >= 0) {
            throw new IllegalArgumentException("already exists owner");
        }
        owners.add(address);
    }

    protected void remove(Address address) throws IllegalArgumentException {
        int idx = indexOf(address);
        if (idx < 0) {
            throw new IllegalArgumentException("not exists owner");
        }
        Address last = owners.pop();
        if (idx != owners.size()) {
            owners.set(idx, last);
        }
    }

    protected void requireOwnerAccess() {
        if (!isOwner(Context.getCaller())) {
            throw new IllegalStateException("caller is not owner");
        }
    }

    protected void requireNotScoreOwner(Address address) {
        if (Context.getOwner().equals(address)) {
            throw new IllegalArgumentException("given address is score owner");
        }
    }

    @Override
    public void addOwner(Address _addr) throws IllegalStateException, IllegalArgumentException{
        requireOwnerAccess();
        requireNotScoreOwner(_addr);
        add(_addr);
    }

    @Override
    public void removeOwner(Address _addr) throws IllegalStateException, IllegalArgumentException {
        requireOwnerAccess();
        requireNotScoreOwner(_addr);
        remove(_addr);
    }

    @Override
    public Address[] getOwners() {
        Address[] owners = new Address[this.owners.size() + 1];
        owners[0] = Context.getOwner();
        int size = this.owners.size();
        for (int i = 0; i < size; i++) {
            owners[i+1] = this.owners.get(i);
        }
        return owners;
    }

    @Override
    public boolean isOwner(Address _addr) {
        if (Context.getOwner().equals(_addr) || indexOf(_addr) >= 0) {
            return true;
        }
        return false;
    }
}
