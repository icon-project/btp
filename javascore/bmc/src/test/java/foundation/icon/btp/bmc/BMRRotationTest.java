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
import foundation.icon.btp.lib.BMCStatus;
import foundation.icon.btp.lib.BTPAddress;
import foundation.icon.btp.mock.MockBSH;
import foundation.icon.btp.mock.MockRelayMessage;
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

public class BMRRotationTest implements BMCIntegrationTest {
    static BTPAddress linkBtpAddress = BMCIntegrationTest.Faker.btpLink();
    static String link = linkBtpAddress.toString();
    static String net = linkBtpAddress.net();
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

    @BeforeAll
    static void beforeAll() {
        System.out.println("beforeAll start");
        BSHManagementTest.addService(svc, MockBSHIntegrationTest.mockBSHClient._address());
        BMVManagementTest.addVerifier(net, MockBMVIntegrationTest.mockBMVClient._address());
        LinkManagementTest.addLink(link);
        MockBMVIntegrationTest.mockBMV.setHeight(client._lastBlockHeight().longValue());
        iconSpecific.setLinkRotateTerm(link, blockInterval, maxAggregation);
        iconSpecific.setLinkDelayLimit(link, delayLimit);
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

    static int rotateRelayIdx(int idx, int num) {
        idx += num;
        return idx >= numOfRelays ? 0 : idx;
    }

    static int rotate(BMCStatus status, long at) {
        long rotateHeight = status.getRotate_height();
        int rotateTerm = status.getRotate_term();
        int rotateCnt = (int)StrictMath.ceil((double)(at - rotateHeight)/(double)rotateTerm);
        int relayIdx = status.getRelay_idx();
        if (rotateCnt > 0) {
            rotateHeight += ((long) rotateCnt * rotateTerm);
            relayIdx = rotateRelayIdx(status.getRelay_idx(), rotateCnt);
        }
        System.out.println("rotateCnt:"+rotateCnt+
                ", relayIdx:"+relayIdx+
                ", rotateHeight:"+rotateHeight+
                ", at:"+at);
        return relayIdx;
    }

    @Test
    void relayRotationNotContainsBTPMessage() throws Exception {
        MockRelayMessage relayMessage = new MockRelayMessage();
        ExecutorService executorService = Executors.newFixedThreadPool(numOfRelays);

        for (int i = 0; i < numOfRelays; i++) {
            BMCStatus status = BMCIntegrationTest.getStatus(bmc, link);
            System.out.println(status);

            long sendTxHeight = status.getCur_height();
            int relayIdx = rotate(status, sendTxHeight + executeMargin);
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
            ScoreIntegrationTest.waitByHeight(BMCIntegrationTest.getStatus(bmc, link).getRotate_height());
        }
    }

    static int guessRotate(BMCStatus status, long msgHeight, long at) {
        double scale = (double) status.getBlock_interval_src() / (double) status.getBlock_interval_dst();
        long guessHeight = status.getRx_height() +
                (long) StrictMath.ceil(
                        (double) (msgHeight - status.getRx_height_src()) / scale) - 1;
        if (guessHeight > at) {
            System.out.println("guessHeight > at,"+guessHeight);
            guessHeight = at;
        }
        long rotateHeight = status.getRotate_height();
        int rotateTerm = status.getRotate_term();
        int rotateCnt = (int)StrictMath.ceil((double)(guessHeight - rotateHeight)/(double)rotateTerm);
        if (rotateCnt < 0) {
            rotateCnt = 0;
        } else {
            rotateHeight += ((long) rotateCnt * rotateTerm);
        }
        int skipCnt = (int)StrictMath.ceil((double)(at - guessHeight)/(double)delayLimit) - 1;
        if (skipCnt > 0) {
            rotateHeight = at + rotateTerm;
        } else {
            skipCnt = 0;
        }
        int relayIdx = status.getRelay_idx();
        if (rotateCnt > 0 || skipCnt > 0) {
            relayIdx = rotateRelayIdx(status.getRelay_idx(), rotateCnt + skipCnt);
        }
        System.out.println("guessHeight:"+guessHeight+
                ", rotateCnt:"+rotateCnt+
                ", skipCnt:"+skipCnt+
                ", relayIdx:"+relayIdx+
                ", rotateHeight:"+rotateHeight+
                ", msgHeight:"+msgHeight+
                ", at:"+at);
        return relayIdx;
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
            BMCStatus status = BMCIntegrationTest.getStatus(bmc, link);
            System.out.println(status);
            long msgHeight = status.getCur_height();
            long sendTxHeight = msgHeight + delayLimit;
            int relayIdx = guessRotate(status, msgHeight, sendTxHeight+executeMargin);
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

}
