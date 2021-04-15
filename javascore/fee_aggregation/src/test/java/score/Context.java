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

package score;

import score.impl.AnyDBImpl;
import tools.Account;
import tools.ServiceManager;
import tools.TestBase;

import java.math.BigInteger;

public class Context extends TestBase {
    private static final ServiceManager sm = getServiceManager();
    private static final StackWalker stackWalker =
            StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

    private Context() {
    }

    public static byte[] getTransactionHash() {
        return null;
    }

    public static int getTransactionIndex() {
        return 0;
    }

    public static long getTransactionTimestamp() {
        return 0L;
    }

    public static BigInteger getTransactionNonce() {
        return BigInteger.ZERO;
    }

    public static Address getAddress() {
        return sm.getAddress();
    }

    public static Address getCaller() {
        return sm.getCaller();
    }

    public static Address getOrigin() {
        return sm.getOrigin();
    }

    public static Address getOwner() {
        return sm.getOwner();
    }

    public static BigInteger getValue() {
        return sm.getCurrentFrame().getValue();
    }

    public static long getBlockTimestamp() {
        return sm.getBlock().getTimestamp();
    }

    public static long getBlockHeight() {
        return sm.getBlock().getHeight();
    }

    public static BigInteger getBalance(Address address) throws IllegalArgumentException {
        return Account.getAccount(address).getBalance();
    }

    public static Object call(BigInteger value, Address targetAddress, String method, Object... params) {
        var caller = stackWalker.getCallerClass();
        return sm.call(caller, value, targetAddress, method, params);
    }

    public static Object call(Address targetAddress, String method, Object... params) {
        var caller = stackWalker.getCallerClass();
        return sm.call(caller, BigInteger.ZERO, targetAddress, method, params);
    }

    public static void transfer(Address targetAddress, BigInteger value) {
        var caller = stackWalker.getCallerClass();
        sm.call(caller, value, targetAddress, "fallback");
    }

    public static void revert(int code, String message) {
        throw new AssertionError(String.format("Reverted(%d): %s", code, message));
    }

    public static void revert(int code) {
        throw new AssertionError(String.format("Reverted(%d)", code));
    }

    public static void revert(String message) {
        revert(0, message);
    }

    public static void revert() {
        revert(0);
    }

    public static void require(boolean condition) {
        if (!condition) {
            throw new AssertionError();
        }
    }

    public static void println(String message) {
        System.out.println(message);
    }

    public static byte[] sha3_256(byte[] data) throws IllegalArgumentException {
        return null;
    }

    public static byte[] sha256(byte[] data) throws IllegalArgumentException {
        return null;
    }

    public static byte[] recoverKey(byte[] msgHash, byte[] signature, boolean compressed) {
        return null;
    }

    public static Address getAddressFromKey(byte[] publicKey) {
        return null;
    }

    public static void logEvent(Object[] indexed, Object[] data) {
    }

    @SuppressWarnings("unchecked")
    public static<K, V> BranchDB<K, V> newBranchDB(String id, Class<?> leafValueClass) {
        return new AnyDBImpl(id, leafValueClass);
    }

    @SuppressWarnings("unchecked")
    public static<K, V> DictDB<K, V> newDictDB(String id, Class<V> valueClass) {
        return new AnyDBImpl(id, valueClass);
    }

    @SuppressWarnings("unchecked")
    public static<E> ArrayDB<E> newArrayDB(String id, Class<E> valueClass) {
        return new AnyDBImpl(id, valueClass);
    }

    @SuppressWarnings("unchecked")
    public static<E> VarDB<E> newVarDB(String id, Class<E> valueClass) {
        return new AnyDBImpl(id, valueClass);
    }
}
