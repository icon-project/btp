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

package foundation.icon.score.test;

import foundation.icon.jsonrpc.Address;
import foundation.icon.jsonrpc.IconJsonModule;
import foundation.icon.jsonrpc.model.TransactionResult;
import foundation.icon.score.client.DefaultScoreClient;
import foundation.icon.score.client.RevertedException;
import foundation.icon.icx.KeyWallet;
import foundation.icon.icx.Wallet;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.function.Executable;
import score.UserRevertedException;

import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestMethodOrder(value = MethodOrderer.OrderAnnotation.class)
public interface ScoreIntegrationTest {

    void internalBeforeEach(TestInfo testInfo);

    void internalAfterEach(TestInfo testInfo);

    void clearIfExists(TestInfo testInfo);

    @BeforeEach
    default void beforeEach(TestInfo testInfo) {
        System.out.println("=".repeat(100));
        System.out.println("beforeEach start " + testInfo.getTestMethod().orElseThrow());
        System.out.println("clearIfExists start");
        clearIfExists(testInfo);
        System.out.println("clearIfExists end");
        internalBeforeEach(testInfo);
        System.out.println("beforeEach end " + testInfo.getTestMethod().orElseThrow());
        System.out.println("-".repeat(100));
    }

    @AfterEach
    default void afterEach(TestInfo testInfo) {
        System.out.println("-".repeat(100));
        System.out.println("afterEach start " + testInfo.getTestMethod().orElseThrow());
        internalAfterEach(testInfo);
        System.out.println("clearIfExists start");
        clearIfExists(testInfo);
        System.out.println("clearIfExists end");
        System.out.println("afterEach end " + testInfo.getTestMethod().orElseThrow());
        System.out.println("=".repeat(100));
    }

    static <T> int indexOf(T[] array, T value) {
        return indexOf(array, value::equals);
    }

    static <T> int indexOf(T[] array, Predicate<T> predicate) {
        for (int i = 0; i < array.length; i++) {
            if (predicate.test(array[i])) {
                return i;
            }
        }
        return -1;
    }

    static boolean contains(Map<String, Object> map, String key, Object value) {
        return contains(map, key, value::equals);
    }

    static <T> boolean contains(Map<String, T> map, String key, Predicate<T> predicate) {
        return map.containsKey(key) && predicate.test(map.get(key));
    }

    static <T> List<T> eventLogs(TransactionResult txr,
                                 String signature,
                                 Address scoreAddress,
                                 Function<TransactionResult.EventLog, T> mapperFunc,
                                 Predicate<T> filter) {
        Predicate<TransactionResult.EventLog> predicate =
                (el) -> el.getIndexed().get(0).equals(signature);
        if (scoreAddress != null) {
            predicate = predicate.and((el) -> el.getScoreAddress().equals(scoreAddress));
        }
        Stream<T> stream = txr.getEventLogs().stream()
                .filter(predicate)
                .map(mapperFunc);
        if(filter != null) {
            stream = stream.filter(filter);
        }
        return stream.collect(Collectors.toList());
    }

    DefaultScoreClient client = new DefaultScoreClient(
            DefaultScoreClient.url(System.getProperties()),
            DefaultScoreClient.nid(System.getProperties()),
            null,
            null
    );

    static void waitHeight(long numOfBlock) {
        BigInteger height = client._lastBlockHeight();
        BigInteger waitHeight = height.add(BigInteger.valueOf(numOfBlock));
        while (waitHeight.compareTo(height) >= 0) {
            System.out.println("height: "+height+", waitHeight: "+waitHeight);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            height = client._lastBlockHeight();
        }
    }

    static void balanceCheck(Address address, BigInteger value, Executable executable) {
        BigInteger balance = client._balance(address);
        try {
            executable.execute();
        } catch (UserRevertedException | RevertedException e) {
            throw e;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
        assertEquals(balance.add(value), client._balance(address));
    }

    Wallet tester = getOrGenerateWallet("tester.", System.getProperties());

    static Wallet getOrGenerateWallet(String prefix, Properties properties) {
        Wallet wallet = DefaultScoreClient.wallet(prefix, properties);
        return wallet == null ? generateWallet() : wallet;
    }

    static KeyWallet generateWallet() {
        try {
            return KeyWallet.create();
        } catch (InvalidAlgorithmParameterException | NoSuchAlgorithmException | NoSuchProviderException e) {
            throw new RuntimeException(e);
        }
    }

    interface Faker {
        com.github.javafaker.Faker faker = new com.github.javafaker.Faker();
        Random random = new Random();

        static Address address(Address.Type type) {
            byte[] body = IconJsonModule.hexToBytes(
                    faker.crypto().sha256().substring(0, (Address.LENGTH - 1) * 2));
            return new Address(type, body);
        }

        static byte[] bytes(int length) {
            byte[] bytes = new byte[length];
            random.nextBytes(bytes);
            return bytes;
        }
    }
}
