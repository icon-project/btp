/*
 * Copyright 2022 ICON Foundation
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

package foundation.icon.btp.test;

import org.junit.jupiter.api.*;
import org.web3j.abi.datatypes.Address;
import org.web3j.crypto.*;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.RemoteCall;
import org.web3j.protocol.core.RemoteFunctionCall;
import org.web3j.protocol.core.methods.response.BaseEventResponse;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.Contract;
import org.web3j.tx.FastRawTransactionManager;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.ContractGasProvider;
import org.web3j.tx.gas.DefaultGasProvider;
import org.web3j.tx.response.PollingTransactionReceiptProcessor;
import org.web3j.utils.Numeric;

import java.io.IOException;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("integration")
@TestMethodOrder(value = MethodOrderer.OrderAnnotation.class)
public interface EVMIntegrationTest {

    default void internalBeforeEach(TestInfo testInfo) {
    }

    default void internalAfterEach(TestInfo testInfo) {
    }

    default void clearIfExists(TestInfo testInfo) {
    }

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

    int DEFAULT_RPC_PORT = 8545;
    Web3j w3j = Web3j.build(new HttpService("http://localhost:" + DEFAULT_RPC_PORT));
    ContractGasProvider cgp = new DefaultGasProvider();
    Credentials credentials = Credentials.create("0x8f2a55949038a9610f50fb23b5883af3b4ecb3c3bb792cbcefbd1542c692be63");
    BigInteger chainId = getChainId(w3j);
    TransactionManager tm = newTransactionManager(credentials);

    static TransactionManager newTransactionManager(Credentials credentials) {
        return new FastRawTransactionManager(
                w3j,
                credentials,
                chainId.longValue(),
                new PollingTransactionReceiptProcessor(
                        w3j,
                        1000,
                        30));
    }

    static BigInteger getChainId(Web3j w3j) {
        try {
            return w3j.ethChainId().send().getChainId();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    static <T extends Contract> T deploy(Class<T> clazz, Object... params) {
        T contract = null;
        try {
            if (params != null && params.length > 0) {
                List<Class<?>> parameterTypes = new ArrayList<>();
                parameterTypes.add(Web3j.class);
                parameterTypes.add(TransactionManager.class);
                parameterTypes.add(ContractGasProvider.class);
                for (Object param : params) {
                    parameterTypes.add(param.getClass());
                }
                Method method = clazz.getDeclaredMethod("deploy", parameterTypes.toArray(new Class[0]));
                List<Object> parameters = new ArrayList<>(Arrays.asList(w3j, tm, cgp));
                parameters.addAll(Arrays.asList(params));
                RemoteCall<T> remoteCall = (RemoteCall<T>) method.invoke(null, parameters.toArray());
                contract = remoteCall.send();
            } else {
                String binary = (String) clazz.getDeclaredField("BINARY").get(null);
                contract = Contract.deployRemoteCall(clazz, w3j, tm, cgp, binary, "").send();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        System.out.printf("deploy %s address:%s%n",
                clazz.getSimpleName(), contract.getContractAddress());
        return contract;
    }

    @SuppressWarnings("unchecked")
    static <T extends Contract> TransactionReceipt initialize(T contract, Object... params) {
        try {
            Class<T> clazz = (Class<T>) contract.getClass();
            Method[] ms = clazz.getDeclaredMethods();
            Method method = null;
            for (Method m : ms) {
                if (m.getName().equals("initialize")) {
                    method = m;
                }
            }
            if (method == null) {
                throw new NoSuchMethodException();
            }
            TransactionReceipt txr = ((RemoteFunctionCall<TransactionReceipt>) method.invoke(contract, params)).send();
            System.out.printf("initialize %s params:%s%n",
                    clazz.getSimpleName(), Arrays.toString(params));
            return txr;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static <T extends Contract> T deployWithInitialize(Class<T> clazz, Object... params) {
        T contract = deploy(clazz);
        initialize(contract, params);
        return contract;
    }


    static <T extends Contract> T load(Class<T> clazz, String address) {
        return load(clazz, address, tm);
    }

    @SuppressWarnings("unchecked")
    static <T extends Contract> T load(T contract, Credentials credentials) {
        return (T) load(contract.getClass(), contract.getContractAddress(), newTransactionManager(credentials));
    }

    @SuppressWarnings("unchecked")
    static <T extends Contract> T load(Class<T> clazz, String address, TransactionManager tm) {
        try {
            Method method = clazz.getMethod("load",
                    String.class,
                    Web3j.class,
                    TransactionManager.class,
                    ContractGasProvider.class);
            return (T) method.invoke(null, address, w3j, tm, cgp);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @FunctionalInterface
    interface EventLogsSupplier<T extends BaseEventResponse> {
        List<T> apply(TransactionReceipt txr);
    }

    static <T extends BaseEventResponse> List<T> eventLogs(
            TransactionReceipt txr,
            String address,
            EventLogsSupplier<T> supplier,
            Predicate<T> filter) {
        Predicate<T> predicate = address == null ? null :
                (el) -> el.log.getAddress().equals(address);
        if (filter != null) {
            predicate = predicate == null ? filter : predicate.and(filter);
        }
        List<T> l = supplier.apply(txr);
        if (predicate != null) {
            return l.stream()
                    .filter(predicate)
                    .collect(Collectors.toList());
        }
        return l;
    }

    static <T extends BaseEventResponse> Consumer<TransactionReceipt> eventLogChecker(
            String address, EventLogsSupplier<T> supplier, Consumer<T> consumer) {
        return (txr) -> {
            List<T> eventLogs = supplier.apply(txr).stream()
                    .filter((el) -> el.log.getAddress().equals(address))
                    .collect(Collectors.toList());
            assertEquals(1, eventLogs.size());
            if (consumer != null) {
                consumer.accept(eventLogs.get(0));
            }
        };
    }

    static <T extends BaseEventResponse> Consumer<TransactionReceipt> eventLogsChecker(
            String address, EventLogsSupplier<T> supplier, Consumer<List<T>> consumer) {
        return (txr) -> {
            List<T> eventLogs = supplier.apply(txr).stream()
                    .filter((el) -> el.log.getAddress().equals(address))
                    .collect(Collectors.toList());
            if (consumer != null) {
                consumer.accept(eventLogs);
            }
        };
    }

    static <T extends BaseEventResponse> Consumer<TransactionReceipt> eventLogShouldNotExistsChecker(
            String address, EventLogsSupplier<T> supplier) {
        return (txr) -> {
            List<T> eventLogs = supplier.apply(txr).stream()
                    .filter((el) -> el.log.getAddress().equals(address))
                    .collect(Collectors.toList());
            assertEquals(0, eventLogs.size());
        };
    }

    Credentials tester = Credentials.create("0xa6d23a0b704b649a92dd56bdff0f9874eeccc9746f10d78b683159af1617e08f");

    static Credentials generateCredentials() {
        try {
            ECKeyPair ecKeyPair = Keys.createEcKeyPair();
            System.out.println("generate keyPair:" + Numeric.toHexStringWithPrefix(ecKeyPair.getPrivateKey()));
            return Credentials.create(ecKeyPair);
        } catch (InvalidAlgorithmParameterException | NoSuchAlgorithmException | NoSuchProviderException e) {
            throw new RuntimeException(e);
        }
    }

    interface Faker {
        com.github.javafaker.Faker faker = new com.github.javafaker.Faker();
        Random random = new Random();

        static Address address() {
            String body = faker.crypto().sha256().substring(0, (Address.DEFAULT_LENGTH / 8) * 2);
            return new Address(body);
        }

        static byte[] bytes(int length) {
            byte[] bytes = new byte[length];
            random.nextBytes(bytes);
            return bytes;
        }
    }

}
