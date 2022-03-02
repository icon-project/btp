/*
 * Copyright 2020 ICONLOOP Inc.
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

package com.iconloop.testsvc;

import score.Address;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

public class Account {
    private static final Map<Address, Account> accounts = new HashMap<>();

    private final Address address;
    private final Map<String, BigInteger> balances = new HashMap<>();

    public static Account newExternalAccount(int seed) {
        var acct = new Account(0, seed);
        accounts.put(acct.getAddress(), acct);
        return acct;
    }

    public static Account newScoreAccount(int seed) {
        var acct = new Account(1, seed);
        accounts.put(acct.getAddress(), acct);
        return acct;
    }

    public static Account getAccount(Address address) {
        return accounts.get(address);
    }

    private Account(int type, int seed) {
        var ba = new byte[Address.LENGTH];
        ba[0] = (byte) type;
        var index = ba.length - 1;
        ba[index--] = (byte) seed;
        ba[index--] = (byte) (seed >> 8);
        ba[index--] = (byte) (seed >> 16);
        ba[index] = (byte) (seed >> 24);
        this.address = new Address(ba);
    }

    public Address getAddress() {
        return address;
    }

    public void addBalance(String symbol, BigInteger value) {
        balances.put(symbol, getBalance(symbol).add(value));
    }

    public void subtractBalance(String symbol, BigInteger value) {
        balances.put(symbol, getBalance(symbol).subtract(value));
    }

    public BigInteger getBalance(String symbol) {
        return balances.getOrDefault(symbol, BigInteger.ZERO);
    }

    public BigInteger getBalance() {
        return getBalance("ICX");
    }

    @Override
    public String toString() {
        return "Account{" +
                "address=" + address +
                ", balances=" + balances +
                '}';
    }
}
