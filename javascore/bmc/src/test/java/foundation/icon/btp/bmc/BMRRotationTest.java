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

package foundation.icon.btp.bmc;

import foundation.icon.btp.lib.BMCScoreClient;
import foundation.icon.btp.lib.BTPAddress;
import foundation.icon.btp.mock.MockBSH;
import foundation.icon.btp.mock.MockRelayMessage;
import foundation.icon.btp.test.BTPIntegrationTest;
import foundation.icon.btp.test.MockBMVIntegrationTest;
import foundation.icon.btp.test.MockBSHIntegrationTest;
import foundation.icon.icx.Wallet;
import foundation.icon.jsonrpc.Address;
import foundation.icon.score.test.ScoreIntegrationTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BMRRotationTest implements BMCIntegrationTest {
    static BTPAddress linkBtpAddress = BMCIntegrationTest.Faker.btpLink();
    static String link = linkBtpAddress.toString();
    static String net = linkBtpAddress.net();
    static BTPAddress secondLinkBtpAddress = BTPIntegrationTest.Faker.btpLink();
    static String secondLink = secondLinkBtpAddress.toString();
    static BTPAddress btpAddress = BTPAddress.valueOf(bmc.getBtpAddress());
    static String svc = MockBSH.SERVICE;
    static int numOfRelays = 3;
    static Address[] relayAddresses = new Address[numOfRelays];
    static BMCScoreClient[] relays = new BMCScoreClient[numOfRelays];
    static int blockInterval = BTPMessageCenter.BLOCK_INTERVAL_MSEC;
    static int maxAggregation = 10;
    static int delayLimit = 3;
    static int executeMargin = 2;//pool(1)+execute(1)
    static int finalizeMargin = executeMargin + 1; //result(1)

    static BTPMessage btpMessage(BigInteger sn, byte[] payload) {
        BTPMessage btpMsg = new BTPMessage();
        btpMsg.setSrc(linkBtpAddress);
        btpMsg.setDst(btpAddress);
        btpMsg.setSvc(svc);
        btpMsg.setSn(sn);
        btpMsg.setPayload(payload);
        return btpMsg;
    }

    void setRelayRotation(String link) {
        iconSpecific.setRelayRotation(link, blockInterval, maxAggregation, delayLimit);
        RelayRotation rotation = iconSpecific.getRelayRotation(link);
        assertEquals(blockInterval, rotation.getBlockIntervalDst());
        assertEquals(maxAggregation, rotation.getMaxAggregation());
        assertEquals(delayLimit, rotation.getDelayLimit());
    }

    @BeforeAll
    static void beforeAll() {
        System.out.println("beforeAll start");
        BSHManagementTest.addService(svc, MockBSHIntegrationTest.mockBSHClient._address());
        BMVManagementTest.addVerifier(net, MockBMVIntegrationTest.mockBMVClient._address());
        LinkManagementTest.addLink(link);
        MockBMVIntegrationTest.mockBMV.setHeight(client._lastBlockHeight().longValue());
        iconSpecific.setRelayRotation(link, blockInterval, maxAggregation, delayLimit);
        for (int i = 0; i < numOfRelays; i++) {
            Wallet wallet = ScoreIntegrationTest.generateWallet();
            relays[i] = new BMCScoreClient(
                    bmcClient.endpoint(),
                    bmcClient._nid(),
                    wallet,
                    bmcClient._address());
            Address relayAddress = Address.of(wallet);
            relayAddresses[i] = relayAddress;
            BMRManagementTest.addRelay(link, relayAddress);
        }
        System.out.println("beforeAll end");
    }

    @AfterAll
    static void afterAll() {
        System.out.println("afterAll start");
        //when removeLink, clear all of relays of link, so not required clear relay
//        for (int i = 0; i < numOfRelays; i++) {
//            BMRManagementTest.clearRelay(link,relayAddresses[i]);
//        }
        LinkManagementTest.clearLink(link);
        BMVManagementTest.clearVerifier(net);
        BSHManagementTest.clearService(svc);
        System.out.println("afterAll end");
    }

    @Test
    void relayRotationNotContainsBTPMessage() throws Exception {
        MockRelayMessage relayMessage = new MockRelayMessage();
        ExecutorService executorService = Executors.newFixedThreadPool(numOfRelays);

        for (int i = 0; i < numOfRelays; i++) {
            RelayRotation rotation = iconSpecific.getRelayRotation(link);
            System.out.println(rotation);

            long sendTxHeight = BMCIntegrationTest.getStatus(bmc, link).getCur_height();
            rotation.rotate(sendTxHeight + executeMargin, sendTxHeight, false, numOfRelays);
            int relayIdx = rotation.getRelayIdx();
            relayMessage.setHeight(sendTxHeight);
            String msg = relayMessage.toBase64String();

            List<Callable<Void>> tasks = new ArrayList<>();
            for (int j = 0; j < numOfRelays; j++) {
                BMCScoreClient relay = relays[j];
                Runnable runable;
                if (relayIdx == j) {
                    runable = () -> relay.handleRelayMessage(link, msg);
                } else {
                    runable = () ->
                            AssertBMCException.assertUnauthorized(
                                    () -> relay.handleRelayMessage(link, msg));
                }
                tasks.add(Executors.callable(runable, null));
            }
            List<Future<Void>> futures = executorService.invokeAll(tasks);
            for(Future<Void> future : futures) {
                future.get();
            }
            //wait to rotateHeight
            ScoreIntegrationTest.waitByHeight(iconSpecific.getRelayRotation(link).getRotateHeight());
        }
    }

    @Test
    void relayRotationContainsBTPMessage() throws Exception {
        BigInteger sn = BigInteger.ONE;
        byte[] payload = Faker.btpLink().toBytes();
        List<BTPMessage> btpMessages = new ArrayList<>();
        btpMessages.add(btpMessage(sn, payload));
        MockRelayMessage relayMessage = new MockRelayMessage();
        relayMessage.setBtpMessages(MessageTest.toBytesArray(btpMessages));

        ExecutorService executorService = Executors.newFixedThreadPool(numOfRelays);
        for (int i = 0; i < numOfRelays; i++) {
            RelayRotation rotation = iconSpecific.getRelayRotation(link);
            System.out.println(rotation);

            long msgHeight = BMCIntegrationTest.getStatus(bmc, link).getCur_height();
            long sendTxHeight = msgHeight + delayLimit;
            rotation.rotate(sendTxHeight + executeMargin, msgHeight, true, numOfRelays);
            int relayIdx = rotation.getRelayIdx();
            relayMessage.setHeight(msgHeight);
            relayMessage.setLastHeight(relayMessage.getHeight());
            String msg = relayMessage.toBase64String();

            //wait to msgHeight + delayLimit
            ScoreIntegrationTest.waitByHeight(sendTxHeight);

            List<Callable<Void>> tasks = new ArrayList<>();
            for (int j = 0; j < numOfRelays; j++) {
                BMCScoreClient relay = relays[j];
                Runnable runable;
                if (relayIdx == j) {
                    runable = () -> relay.handleRelayMessage(link, msg);
                } else {
                    runable = () ->
                            AssertBMCException.assertUnauthorized(
                                    () -> relay.handleRelayMessage(link, msg));
                }
                tasks.add(Executors.callable(runable, null));
            }
            List<Future<Void>> futures = executorService.invokeAll(tasks);
            for(Future<Void> future : futures) {
                future.get();
            }

            //reset Link.rxHeight, Link.rxHeightSrc
            relayMessage.setHeight(client._lastBlockHeight().longValue());
            relayMessage.setLastHeight(relayMessage.getHeight());
            relays[relayIdx].handleRelayMessage(link, relayMessage.toBase64String());
        }
    }

    @Test
    void setRelayRotationShouldSuccess() {
        setRelayRotation(link);
    }

    @Test
    void setRelayRotationShouldRevertNotExistsLink() {
        AssertBMCException.assertNotExistsLink(
                () -> setRelayRotation(secondLink));
    }

    @Test
    void setRelayRotationShouldRevertIllegalArgument() {
        int invalidValue = -2;
        AssertBMCException.assertUnknown(
                () -> iconSpecific.setRelayRotation(link, invalidValue, maxAggregation, delayLimit));
        AssertBMCException.assertUnknown(
                () -> iconSpecific.setRelayRotation(link, blockInterval, invalidValue, delayLimit));
        AssertBMCException.assertUnknown(
                () -> iconSpecific.setRelayRotation(link, blockInterval, maxAggregation, invalidValue));
    }

}
