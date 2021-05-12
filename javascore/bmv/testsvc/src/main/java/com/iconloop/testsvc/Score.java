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

import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;

public class Score extends TestBase {
    private static final ServiceManager sm = getServiceManager();

    private final Account score;
    private final Account owner;
    private Object instance;

    public Score(Account score, Account owner) {
        this.score = score;
        this.owner = owner;
    }

    public Account getAccount() {
        return this.score;
    }

    public Address getAddress() {
        return this.score.getAddress();
    }

    public Account getOwner() {
        return this.owner;
    }

    public void setInstance(Object newInstance) {
        this.instance = newInstance;
    }

    public Object getInstance() {
        return this.instance;
    }

    public Object call(String method, Object... params) {
        return call(null, true, BigInteger.ZERO, method, params);
    }

    public void invoke(Account from, String method, Object... params) {
        sm.getBlock().increase();
        call(from, false, BigInteger.ZERO, method, params);
    }

    Object call(Account from, boolean readonly, BigInteger value, String method, Object... params) {
        sm.pushFrame(from, this.score, readonly, method, value);
        Class<?>[] paramClasses = new Class<?>[params.length];
        for (int i = 0; i < params.length; i++) {
            Class<?> type = params[i].getClass();
            paramClasses[i] = (type == Boolean.class) ? Boolean.TYPE : type;
        }
        try {
            Class<?> clazz = instance.getClass();
            var m = clazz.getMethod(method, paramClasses);
            return m.invoke(instance, params);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        } catch (InvocationTargetException e) {
            e.printStackTrace();
            throw new AssertionError(e.getTargetException().getMessage());
        } finally {
            sm.popFrame();
        }
    }
}
