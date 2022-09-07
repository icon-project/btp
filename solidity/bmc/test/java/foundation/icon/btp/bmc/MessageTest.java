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

import foundation.icon.btp.lib.BMCStatus;
import foundation.icon.btp.lib.BTPAddress;
import foundation.icon.btp.lib.BTPException;
import foundation.icon.btp.mock.MockBSH;
import foundation.icon.btp.mock.MockRelayMessage;
import foundation.icon.btp.test.EVMIntegrationTest;
import foundation.icon.btp.test.MockBMVIntegrationTest;
import foundation.icon.btp.test.MockBSHIntegrationTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

public class MessageTest implements BMCIntegrationTest {
    static BTPAddress linkBtpAddress = Faker.btpLink();
    static String link = linkBtpAddress.toString();
    static String net = linkBtpAddress.net();
    static String relay = EVMIntegrationTest.credentials.getAddress();
    static BTPAddress btpAddress = BMCIntegrationTest.btpAddress();
    static String svc = MockBSHIntegrationTest.SERVICE;

    @BeforeAll
    static void beforeAll() {
        System.out.println("MessageTest:beforeAll start");
        BMVManagementTest.addVerifier(net, MockBMVIntegrationTest.mockBMV.getContractAddress());
        LinkManagementTest.addLink(link);
        BMRManagementTest.addRelay(link, relay);

        BSHManagementTest.clearService(svc);
        BSHManagementTest.addService(svc, MockBSHIntegrationTest.mockBSH.getContractAddress());
        System.out.println("MessageTest:beforeAll end");
    }

    @AfterAll
    static void afterAll() {
        System.out.println("MessageTest:afterAll start");
        BSHManagementTest.clearService(svc);

        BMRManagementTest.clearRelay(link, relay);
        LinkManagementTest.clearLink(link);
        BMVManagementTest.clearVerifier(net);
        System.out.println("MessageTest:afterAll end");
    }

    static BTPMessage btpMessage(BMCIntegrationTest.Internal internal, byte[] payload) {
        BMCMessage bmcMsg = new BMCMessage();
        bmcMsg.setType(internal.name());
        bmcMsg.setPayload(payload);
        return btpMessage(BMCIntegrationTest.INTERNAL_SERVICE, BigInteger.ZERO, bmcMsg.toBytes());
    }

    static BTPMessage btpMessage(String svc, BigInteger sn, byte[] payload) {
        BTPMessage btpMsg = new BTPMessage();
        btpMsg.setSrc(linkBtpAddress);
        btpMsg.setDst(btpAddress);
        btpMsg.setSvc(svc);
        btpMsg.setSn(sn);
        btpMsg.setPayload(payload);
        return btpMsg;
    }

    static byte[][] toBytesArray(List<BTPMessage> btpMessages) {
        int len = btpMessages.size();
        byte[][] bytesArray = new byte[len][];
        for (int i = 0; i < len; i++) {
            bytesArray[i] = btpMessages.get(i).toBytes();
        }
        return bytesArray;
    }

    static void assertEqualsBTPMessage(BTPMessage o1, BTPMessage o2) {
        assertEquals(o1.getSrc(), o2.getSrc());
        assertEquals(o1.getDst(), o2.getDst());
        assertEquals(o1.getSvc(), o2.getSvc());
        assertEquals(o1.getSn(), o2.getSn());
        assertArrayEquals(o1.getPayload(), o2.getPayload());
    }

    static void assertEqualsErrorMessage(BTPMessage o1, BTPMessage o2, BTPException e) {
        assertEquals(o1.getDst(), o2.getSrc());
        assertEquals(o1.getSrc(), o2.getDst());
        assertEquals(o1.getSvc(), o2.getSvc());
        assertEquals(o1.getSn().negate(), o2.getSn());
        BTPErrorMessage errMsg = BTPErrorMessage.fromBytes(o2.getPayload());
        assertEquals(e.getCode(), errMsg.getCode());
        assertEquals(e.getMessage(), errMsg.getMsg());
    }

    @Test
    void handleRelayMessageShouldCallHandleBTPMessage() throws Exception {
        //BMC.handleRelayMessage -> BSHMock.HandleBTPMessage(str,str,int,bytes)
        BigInteger sn = BigInteger.ONE;
        byte[] payload = Faker.btpLink().toBytes();
        List<BTPMessage> btpMessages = new ArrayList<>();
        btpMessages.add(btpMessage(svc, sn, payload));
        MockRelayMessage relayMessage = new MockRelayMessage();
        relayMessage.setBtpMessages(toBytesArray(btpMessages));

        Consumer<TransactionReceipt> checker = MockBSHIntegrationTest.handleBTPMessageEvent(
                (el) -> {
                    assertEquals(net, el._from);
                    assertEquals(svc, el._svc);
                    assertEquals(sn, el._sn);
                    assertArrayEquals(payload, el._msg);
                }
        );
        checker.accept(bmcPeriphery.handleRelayMessage(
                link,
                relayMessage.toBytes()).send());
    }

    @Test
    void handleRelayMessageShouldCallHandleBTPError() throws Exception {
        //BMC.handleRelayMessage -> BSHMock.HandleBTPError(str,str,int,int,str)
        BTPErrorMessage errorMessage = new BTPErrorMessage();
        errorMessage.setCode(1);
        errorMessage.setMsg("error");
        BigInteger sn = BigInteger.ONE;
        List<BTPMessage> btpMessages = new ArrayList<>();
        btpMessages.add(btpMessage(svc, sn.negate(), errorMessage.toBytes()));
        MockRelayMessage relayMessage = new MockRelayMessage();
        relayMessage.setBtpMessages(toBytesArray(btpMessages));

        Consumer<TransactionReceipt> checker = MockBSHIntegrationTest.handleBTPErrorEvent(
                (el) -> {
                    assertEquals(link, el._src);
                    assertEquals(svc, el._svc);
                    assertEquals(sn, el._sn);
                    assertEquals(errorMessage.getCode(), el._code.longValue());
                    assertEquals(errorMessage.getMsg(), el._msg);
                }
        );
        checker.accept(bmcPeriphery.handleRelayMessage(
                link,
                relayMessage.toBytes()).send());
    }

    @Test
    void handleRelayMessageShouldDropAndSendErrorMessageAndMakeEventLog() throws Exception {
        //BMC.dropMessage(BMC.getStatus().getRx_seq().add(BigInteger.ONE))
        //BMC.handleRelayMessage -> BMC.Message(str,int,bytes)
        BMCStatus status = BMCIntegrationTest.getStatus(link);
        BigInteger rxSeq = status.getRx_seq().add(BigInteger.ONE);
        DropMessageTest.scheduleDropMessage(link, rxSeq);

        BigInteger txSeq = status.getTx_seq().add(BigInteger.ONE);

        BigInteger sn = BigInteger.ONE;
        byte[] payload = Faker.btpLink().toBytes();
        List<BTPMessage> btpMessages = new ArrayList<>();
        btpMessages.add(btpMessage(svc, sn, payload));
        MockRelayMessage relayMessage = new MockRelayMessage();
        relayMessage.setBtpMessages(toBytesArray(btpMessages));

        Consumer<TransactionReceipt> checker = BMCIntegrationTest.messageDroppedEvent(
                (el) -> {
                    assertEquals(link, el._link);
                    assertEquals(rxSeq, el._seq);
                    assertEqualsBTPMessage(btpMessages.get(0), BTPMessage.fromBytes(el._msg));
                });
        checker = checker.andThen(BMCIntegrationTest.messageEvent(
                (el) -> {
                    assertEquals(link, el._next);
                    assertEquals(txSeq, el._seq);
                    assertEqualsErrorMessage(btpMessages.get(0), BTPMessage.fromBytes(el._msg),
                            BMCException.drop());
                }));
        checker = checker.andThen(
                EVMIntegrationTest.notExistsEventLogChecker(
                        MockBSHIntegrationTest.mockBSH.getContractAddress(),
                        MockBSH::getHandleBTPMessageEvents));
        checker.accept(bmcPeriphery.handleRelayMessage(
                link,
                relayMessage.toBytes()).send());
        assertFalse(DropMessageTest.isExistsScheduledDropMessage(link, rxSeq));
    }

    @Test
    void handleRelayMessageShouldDropAndMakeEventLog() throws Exception {
        //BMC.dropMessage
        //BMC.handleRelayMessage -> BMC.DropMessage(str,int,bytes)
        BigInteger seq = BMCIntegrationTest.getStatus(link).getRx_seq().add(BigInteger.ONE);
        DropMessageTest.scheduleDropMessage(link, seq);

        BTPErrorMessage errorMessage = new BTPErrorMessage();
        errorMessage.setCode(0);
        errorMessage.setMsg("error");
        BigInteger sn = BigInteger.ONE.negate();
        byte[] payload = errorMessage.toBytes();
        List<BTPMessage> btpMessages = new ArrayList<>();
        btpMessages.add(btpMessage(svc, sn, payload));
        MockRelayMessage relayMessage = new MockRelayMessage();
        relayMessage.setBtpMessages(toBytesArray(btpMessages));

        Consumer<TransactionReceipt> checker = BMCIntegrationTest.messageDroppedEvent(
                (el) -> {
                    assertEquals(link, el._link);
                    assertEquals(seq, el._seq);
                    assertEqualsBTPMessage(btpMessages.get(0), BTPMessage.fromBytes(el._msg));
                }
        );
        checker = checker.andThen(
                EVMIntegrationTest.notExistsEventLogChecker(
                        MockBSHIntegrationTest.mockBSH.getContractAddress(),
                        MockBSH::getHandleBTPMessageEvents));
        checker.accept(bmcPeriphery.handleRelayMessage(
                link,
                relayMessage.toBytes()).send());
        assertFalse(DropMessageTest.isExistsScheduledDropMessage(link, seq));
    }

    static String[] fragments(byte[] bytes, int count) {
        int len = bytes.length;
        if (len < count || count < 1) {
            throw new IllegalArgumentException();
        }
        int fLen = len / count;
        if (len % count != 0) {
            fLen++;
        }
        int begin = 0;
        String[] arr = new String[count];
        for (int i = 0; i < count; i++) {
            int end = begin + fLen;
            byte[] fragment = null;
            if (end < len) {
                fragment = Arrays.copyOfRange(bytes, begin, end);
            } else {
                fragment = Arrays.copyOfRange(bytes, begin, len);
            }
            arr[i] = Base64.getUrlEncoder().encodeToString(fragment);
            begin = end;
        }
        return arr;
    }

    //TODO BMCPeriphery.handleFragment(str, bytes, int)
//    @Test
//    void handleFragment() throws Exception {
//        //BMC.handleFragment -> BSHMock.HandleBTPMessage(str,str,int,bytes)
//        BigInteger sn = BigInteger.ONE;
//        byte[] payload = Faker.btpLink().toBytes();
//        List<BTPMessage> btpMessages = new ArrayList<>();
//        btpMessages.add(btpMessage(svc, sn, payload));
//        MockRelayMessage relayMessage = new MockRelayMessage();
//        relayMessage.setBtpMessages(toBytesArray(btpMessages));
//        byte[] msg = relayMessage.toBytes();
//        int count = 3;
//        int last = count - 1;
//        String[] fragments = fragments(msg, count);
//        for (int i = 0; i < count; i++) {
//            if (i == 0) {
//                bmcPeriphery.handleFragment(link, fragments[i], -1 * last).send();
//            } else if (i == last) {
//                Consumer<TransactionReceipt> checker = MockBSHIntegrationTest.handleBTPMessageEvent(
//                        (el) -> {
//                            assertEquals(net, el._from);
//                            assertEquals(svc, el._svc);
//                            assertEquals(sn, el._sn);
//                            assertArrayEquals(payload, el._msg);
//                        });
//                checker.accept(bmcPeriphery.handleFragment(
//                        link, fragments[i], 0).send());
//            } else {
//                bmcPeriphery.handleFragment(link, fragments[i], last - i).send();
//            }
//        }
//    }

    @Test
    void sendMessageShouldSuccess() throws Exception {
        //BSHMock.sendMessage -> BMC.Message(str,int,bytes)
        BigInteger sn = BigInteger.ONE;
        byte[] payload = Faker.btpLink().toBytes();

        BigInteger seq = BMCIntegrationTest.getStatus(link).getTx_seq().add(BigInteger.ONE);

        Consumer<TransactionReceipt> checker = BMCIntegrationTest.messageEvent(
                (el) -> {
                    assertEquals(link, el._next);
                    assertEquals(seq, el._seq);
                    BTPMessage btpMessage = BTPMessage.fromBytes(el._msg);
                    assertEquals(btpAddress, btpMessage.getSrc());
                    assertEquals(linkBtpAddress, btpMessage.getDst());
                    assertEquals(svc, btpMessage.getSvc());
                    assertEquals(sn, btpMessage.getSn());
                    assertArrayEquals(payload, btpMessage.getPayload());
                });
        checker.accept(MockBSHIntegrationTest.mockBSH.sendMessage(
                bmcPeriphery.getContractAddress(),
                net, svc, sn, payload).send());
    }

    @Test
    void sendMessageShouldRevertUnreachable() {
        BigInteger sn = BigInteger.ONE;
        byte[] payload = Faker.btpLink().toBytes();

        AssertBMCException.assertUnreachable(() -> MockBSHIntegrationTest.mockBSH.sendMessage(
                bmcPeriphery.getContractAddress(),
                Faker.btpNetwork(), svc, sn, payload).send());
    }

    @Test
    void sendMessageShouldRevertNotExistsBSH() {
        BigInteger sn = BigInteger.ONE;
        byte[] payload = Faker.btpLink().toBytes();

        String notRegisteredSvc = Faker.btpService();

        AssertBMCException.assertNotExistsBSH(() -> MockBSHIntegrationTest.mockBSH.sendMessage(
                bmcPeriphery.getContractAddress(),
                Faker.btpNetwork(), notRegisteredSvc, sn, payload).send());
    }

    @Test
    void sendMessageShouldRevertUnauthorized() {
        BigInteger sn = BigInteger.ONE;
        byte[] payload = Faker.btpLink().toBytes();

        AssertBMCException.assertUnauthorized(() -> bmcPeriphery.sendMessage(
                Faker.btpNetwork(), svc, sn, payload).send());
    }

    @Test
    void dropMessageShouldSendErrorMessageAndMakeEventLog() throws Exception {
        BigInteger sn = BigInteger.ONE;

        BTPMessage assumeMsg = new BTPMessage();
        assumeMsg.setSrc(linkBtpAddress);
        assumeMsg.setSvc(svc);
        assumeMsg.setDst(BTPAddress.parse(""));
        assumeMsg.setSn(sn);
        assumeMsg.setPayload(new byte[0]);

        BMCStatus status = BMCIntegrationTest.getStatus(link);
        BigInteger rxSeq = status.getRx_seq().add(BigInteger.ONE);
        BigInteger txSeq = status.getTx_seq().add(BigInteger.ONE);

        Consumer<TransactionReceipt> checker = BMCIntegrationTest.messageDroppedEvent(
                (el) -> {
                    assertEquals(link, el._link);
                    assertEquals(rxSeq, el._seq);
                    assertEqualsBTPMessage(assumeMsg, BTPMessage.fromBytes(el._msg));
                });
        checker = checker.andThen(BMCIntegrationTest.messageEvent(
                (el) -> {
                    assertEquals(link, el._next);
                    assertEquals(txSeq, el._seq);
                    BTPMessage btpMsg = new BTPMessage();
                    btpMsg.setSrc(assumeMsg.getSrc());
                    btpMsg.setDst(btpAddress);
                    btpMsg.setSvc(assumeMsg.getSvc());
                    btpMsg.setSn(assumeMsg.getSn());
                    assertEqualsErrorMessage(btpMsg, BTPMessage.fromBytes(el._msg),
                            BMCException.drop());
                }));
        checker.accept(bmcManagement.dropMessage(
                link, rxSeq, svc, sn).send());
    }
    @Test
    void getStatus() {
        BMCStatus status = BMCIntegrationTest.getStatus(link);
        System.out.println(status);
    }
}
