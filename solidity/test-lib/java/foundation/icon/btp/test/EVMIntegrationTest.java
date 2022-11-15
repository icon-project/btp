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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.TestMethodOrder;
import org.slf4j.LoggerFactory;
import org.web3j.abi.datatypes.Address;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.RemoteCall;
import org.web3j.protocol.core.RemoteFunctionCall;
import org.web3j.protocol.core.methods.response.BaseEventResponse;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.exceptions.TransactionException;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.Contract;
import org.web3j.tx.FastRawTransactionManager;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.Transfer;
import org.web3j.tx.gas.ContractGasProvider;
import org.web3j.tx.gas.DefaultGasProvider;
import org.web3j.tx.gas.StaticGasProvider;
import org.web3j.tx.response.PollingTransactionReceiptProcessor;
import org.web3j.utils.Convert;
import org.web3j.utils.Numeric;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("integration")
@TestMethodOrder(value = MethodOrderer.OrderAnnotation.class)
public interface EVMIntegrationTest {
    Level DEFAULT_LOG_LEVEL = Level.INFO;

    default void clearIfExists(TestInfo testInfo) {
    }

    @BeforeEach
    default void beforeEach(TestInfo testInfo) {
        System.out.println("=".repeat(100));
        System.out.println("beforeEach clearIfExists start" + testInfo.getTestMethod().orElseThrow());
        clearIfExists(testInfo);
        System.out.println("beforeEach clearIfExists end" + testInfo.getTestMethod().orElseThrow());
        System.out.println("-".repeat(100));
    }

    @AfterEach
    default void afterEach(TestInfo testInfo) {
        System.out.println("-".repeat(100));
        System.out.println("afterEach clearIfExists start " + testInfo.getTestMethod().orElseThrow());
        clearIfExists(testInfo);
        System.out.println("afterEach clearIfExists end " + testInfo.getTestMethod().orElseThrow());
        System.out.println("=".repeat(100));
    }

    String DEFAULT_URL = "http://localhost:8545";

    static Web3j newWeb3j(String url) {
        Logger logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(HttpService.class);
        boolean enabled = logger.isDebugEnabled();
        Level level = logger.getLevel();
        if (!enabled) {
            logger.setLevel(Level.DEBUG);
        }
        Web3j w3j = Web3j.build(new HttpService(url));
        if (enabled) {
            System.out.println("HttpService logger level:" + level + " to INFO");
            level = DEFAULT_LOG_LEVEL;
        }
        logger.setLevel(level);
        return w3j;
    }


    Web3j w3j = newWeb3j(System.getProperty("url", DEFAULT_URL));
//    ContractGasProvider cgp = new DefaultGasProvider();
    ContractGasProvider cgp = new StaticGasProvider(DefaultGasProvider.GAS_PRICE,
        BigInteger.valueOf(18_000_000));
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

    static void setDumpJson(boolean enable) {
        Logger logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(HttpService.class);
        if (logger.isDebugEnabled() != enable) {
            logger.setLevel(enable ? Level.DEBUG : DEFAULT_LOG_LEVEL);
        }
    }

    static BigInteger getChainId(Web3j w3j) {
        try {
            return w3j.ethChainId().send().getChainId();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static void replaceContractBinary(Class<?> clazz, String prefix, Properties properties) {
        String jsonFilePath = properties.getProperty(prefix+"jsonFilePath");
        if (jsonFilePath != null && !jsonFilePath.isEmpty()) {
            replaceContractBinary(clazz, jsonFilePath);
        }
    }

    static void replaceContractBinary(Class<?> clazz, String jsonFilePath) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            String bin = (String) mapper.readValue(new File(jsonFilePath), Map.class).get("bytecode");
            System.out.println("replace binary "+clazz.getSimpleName() +
                    " org:"+ ((String)clazz.getDeclaredField("BINARY").get(null)).length()+
                    ",replace:"+bin.length());

            Field field = clazz.getDeclaredField("BINARY");
            field.setAccessible(true);

            Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);

            field.set(null, bin);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    static <T extends Contract> T deploy(Class<T> clazz, Object... params) {
        T contract = null;

        int contractLength;
        try {
            if (params != null && params.length > 0) {
                List<Class<?>> parameterTypes = new ArrayList<>();
                for (Object param : params) {
                    parameterTypes.add(param.getClass());
                }
                Method deployParamsMethod = null;
                try {
                    deployParamsMethod = clazz.getDeclaredMethod("encodeConstructorParams",parameterTypes.toArray(new Class[0]));
                } catch (NoSuchMethodException ignored) {
                }
                if (deployParamsMethod != null) {
                    String binary = (String) clazz.getDeclaredField("BINARY").get(null);
                    contract = Contract.deployRemoteCall(clazz, w3j, tm, cgp, binary,
                            (String) deployParamsMethod.invoke(null, params)).send();
                } else {
                    parameterTypes.addAll(0, List.of(
                            Web3j.class, TransactionManager.class, ContractGasProvider.class));

                    Method method = clazz.getDeclaredMethod("deploy", parameterTypes.toArray(new Class[0]));
                    List<Object> parameters = new ArrayList<>(Arrays.asList(w3j, tm, cgp));
                    parameters.addAll(Arrays.asList(params));
                    RemoteCall<T> remoteCall = (RemoteCall<T>) method.invoke(null, parameters.toArray());
                    contract = remoteCall.send();
                }
            } else {
                String binary = (String) clazz.getDeclaredField("BINARY").get(null);
                contract = Contract.deployRemoteCall(clazz, w3j, tm, cgp, binary, "").send();
            }
            contractLength = w3j.ethGetCode(contract.getContractAddress(), DefaultBlockParameterName.LATEST).send().getCode().length();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        System.out.printf("deploy %s address:%s length:%d %n",
                clazz.getSimpleName(), contract.getContractAddress(),
                contractLength);
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

    static <T extends Contract> T load(Class<T> clazz, Contract contract) {
        return load(clazz, contract.getContractAddress());
    }

    static <T extends Contract> T load(Class<T> clazz, String address) {
        return load(clazz, address, tm);
    }

    @SuppressWarnings("unchecked")
    static <T extends Contract> T load(T contract, Credentials credentials) {
        return (T) load(contract, newTransactionManager(credentials));
    }

    @SuppressWarnings("unchecked")
    static <T extends Contract> T load(T contract, TransactionManager tm) {
        return (T) load(contract.getClass(), contract.getContractAddress(), tm);
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

    static BigInteger getBalance(Credentials credentials) {
        return getBalance(credentials.getAddress());
    }

    static BigInteger getBalance(Contract contract) {
        return getBalance(contract.getContractAddress());
    }

    static BigInteger getBalance(String address) {
        try {
            return w3j.ethGetBalance(address, DefaultBlockParameterName.LATEST).send().getBalance();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static Consumer<TransactionReceipt> balanceChecker(String address, BigInteger value) {
        return balanceChecker(address, value, false);
    }

    static Consumer<TransactionReceipt> balanceChecker(String address, BigInteger value, boolean subtractTxFee) {
        BigInteger preBalance = getBalance(address);
        return (txr) -> {
            BigInteger expectBalance = preBalance.add(value);
            if (subtractTxFee) {
                BigInteger gasPrice;
                try {
                    gasPrice = w3j.ethGetTransactionByHash(txr.getTransactionHash()).send()
                            .getTransaction().orElseThrow()
                            .getGasPrice();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                expectBalance = expectBalance.subtract(
                        gasPrice.multiply(txr.getGasUsed()));
            }
            assertEquals(expectBalance, getBalance(address));
        };
    }

    static BigInteger transfer(Credentials to, BigInteger value) {
        return transfer(to.getAddress(), value);
    }

    static BigInteger transfer(Contract to, BigInteger value) {
        return transfer(to.getContractAddress(), value);
    }

    static BigInteger transfer(String to, BigInteger value) {
        //transfer to contract, if receive function is empty.
        BigInteger TRANSFER_GAS_LIMIT = Transfer.GAS_LIMIT.add(BigInteger.valueOf(55));
        Transfer transfer = new Transfer(w3j, tm);
        try {
            TransactionReceipt txr = transfer.sendFunds(
                    to,
                    new BigDecimal(value),
                    Convert.Unit.ETHER,
                    w3j.ethGasPrice().send().getGasPrice(),
                    TRANSFER_GAS_LIMIT).send();
            if (!txr.isStatusOK()) {
                throw new TransactionException(
                        String.format(
                                "Transaction %s has failed with status: %s. "
                                        + "Gas used: %s. "
                                        + "Revert reason: '%s'.",
                                txr.getTransactionHash(),
                                txr.getStatus(),
                                txr.getGasUsedRaw() != null
                                        ? txr.getGasUsed().toString()
                                        : "unknown",
                                txr.getRevertReason()),
                        txr);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        BigInteger balance = getBalance(to);
        System.out.println("Transferred to:" + to + " value:" + new BigDecimal(value) + " balance:" + balance);
        return balance;
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
    TransactionManager testerTm = newTransactionManager(tester);

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

        static int positive(int bound) {
            return random.nextInt(bound) + 1;
        }
    }

}
