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
import foundation.icon.btp.mock.MockRelayMessage;
import foundation.icon.btp.test.BTPIntegrationTest;
import foundation.icon.btp.test.MockBMVIntegrationTest;
import foundation.icon.btp.test.MockBSHIntegrationTest;
import foundation.icon.jsonrpc.Address;
import foundation.icon.jsonrpc.model.TransactionResult;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

public class MessageTest implements BMCIntegrationTest {
    static BTPAddress linkBtpAddress = BTPIntegrationTest.Faker.btpLink();
    static String link = linkBtpAddress.toString();
    static String net = linkBtpAddress.net();
    static Address relay = Address.of(bmc._wallet());
    static BTPAddress btpAddress = BTPAddress.valueOf(bmc.getBtpAddress());
    static String svc = MockBSHIntegrationTest.SERVICE;

    @BeforeAll
    static void beforeAll() {
        System.out.println("MessageTest:beforeAll start");
        BMVManagementTest.addVerifier(net, MockBMVIntegrationTest.mockBMV._address());
        LinkManagementTest.addLink(link);
        BMRManagementTest.addRelay(link, relay);

        BSHManagementTest.clearService(svc);
        BSHManagementTest.addService(svc, MockBSHIntegrationTest.mockBSH._address());
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

    static BTPMessage btpMessage(BTPMessageCenter.Internal internal, byte[] payload) {
        BMCMessage bmcMsg = new BMCMessage();
        bmcMsg.setType(internal.name());
        bmcMsg.setPayload(payload);
        return btpMessage(BTPMessageCenter.INTERNAL_SERVICE, BigInteger.ZERO, bmcMsg.toBytes());
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
        ErrorMessage errMsg = ErrorMessage.fromBytes(o2.getPayload());
        assertEquals(e.getCode(), errMsg.getCode());
        assertEquals(e.getMessage(), errMsg.getMsg());
    }

    @Test
    void handleRelayMessageShouldCallHandleBTPMessage() {
        //BMC.handleRelayMessage -> BSHMock.HandleBTPMessage(str,str,int,bytes)
        BigInteger sn = BigInteger.ONE;
        byte[] payload = Faker.btpLink().toBytes();
        List<BTPMessage> btpMessages = new ArrayList<>();
        btpMessages.add(btpMessage(svc, sn, payload));
        MockRelayMessage relayMessage = new MockRelayMessage();
        relayMessage.setBtpMessages(toBytesArray(btpMessages));
        bmc.handleRelayMessage(
                MockBSHIntegrationTest.handleBTPMessageEvent(
                        (el) -> {
                            assertEquals(net, el.getFrom());
                            assertEquals(svc, el.getSvc());
                            assertEquals(sn, el.getSn());
                            assertArrayEquals(payload, el.getMsg());
                        }
                ),
                link,
                relayMessage.toBase64String());
    }

    @Test
    void handleRelayMessageShouldCallHandleBTPError() {
        //BMC.handleRelayMessage -> BSHMock.HandleBTPError(str,str,int,int,str)
        ErrorMessage errorMessage = new ErrorMessage();
        errorMessage.setCode(1);
        errorMessage.setMsg("error");
        BigInteger sn = BigInteger.ONE;
        List<BTPMessage> btpMessages = new ArrayList<>();
        btpMessages.add(btpMessage(svc, sn.negate(), errorMessage.toBytes()));
        MockRelayMessage relayMessage = new MockRelayMessage();
        relayMessage.setBtpMessages(toBytesArray(btpMessages));
        bmc.handleRelayMessage(
                MockBSHIntegrationTest.handleBTPErrorEvent(
                        (el) -> {
                            assertEquals(link, el.getSrc());
                            assertEquals(svc, el.getSvc());
                            assertEquals(sn, el.getSn());
                            assertEquals(errorMessage.getCode(), el.getCode());
                            assertEquals(errorMessage.getMsg(), el.getMsg());
                        }
                ),
                link,
                relayMessage.toBase64String());
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

    @Test
    void handleFragment() {
        //BMC.handleFragment -> BSHMock.HandleBTPMessage(str,str,int,bytes)
        BigInteger sn = BigInteger.ONE;
        byte[] payload = Faker.btpLink().toBytes();
        List<BTPMessage> btpMessages = new ArrayList<>();
        btpMessages.add(btpMessage(svc, sn, payload));
        MockRelayMessage relayMessage = new MockRelayMessage();
        relayMessage.setBtpMessages(toBytesArray(btpMessages));
        byte[] msg = relayMessage.toBytes();
        int count = 3;
        int last = count - 1;
        String[] fragments = fragments(msg, count);
        for (int i = 0; i < count; i++) {
            if (i == 0) {
                iconSpecific.handleFragment(link, fragments[i], -1 * last);
            } else if (i == last) {
                iconSpecific.handleFragment(
                        MockBSHIntegrationTest.handleBTPMessageEvent(
                                (el) -> {
                                    assertEquals(net, el.getFrom());
                                    assertEquals(svc, el.getSvc());
                                    assertEquals(sn, el.getSn());
                                    assertArrayEquals(payload, el.getMsg());
                                }
                        ),
                        link, fragments[i], 0);
            } else {
                iconSpecific.handleFragment(link, fragments[i], last - i);
            }
        }
    }

    @Test
    void sendMessageShouldSuccess() {
        //BSHMock.sendMessage -> BMC.Message(str,int,bytes)
        BigInteger sn = BigInteger.ONE;
        byte[] payload = Faker.btpLink().toBytes();

        BigInteger seq = BMCIntegrationTest.getStatus(bmc, link).getTx_seq().add(BigInteger.ONE);
        MockBSHIntegrationTest.mockBSH.sendMessage(
                BMCIntegrationTest.messageEvent((el) -> {
                    assertEquals(link, el.getNext());
                    assertEquals(seq, el.getSeq());
                    BTPMessage btpMessage = el.getMsg();
                    assertEquals(btpAddress, btpMessage.getSrc());
                    assertEquals(linkBtpAddress, btpMessage.getDst());
                    assertEquals(svc, btpMessage.getSvc());
                    assertEquals(sn, btpMessage.getSn());
                    assertArrayEquals(payload, btpMessage.getPayload());
                }),
                bmc._address(),
                net, svc, sn, payload);
    }

    @Test
    void sendMessageShouldRevertUnreachable() {
        BigInteger sn = BigInteger.ONE;
        byte[] payload = Faker.btpLink().toBytes();

        AssertBMCException.assertUnreachable(() -> MockBSHIntegrationTest.mockBSH.sendMessage(
                bmc._address(),
                Faker.btpNetwork(), svc, sn, payload));
    }

    @Test
    void sendMessageShouldRevertNotExistsBSH() {
        BigInteger sn = BigInteger.ONE;
        byte[] payload = Faker.btpLink().toBytes();

        String notRegisteredSvc = BTPIntegrationTest.Faker.btpService();

        AssertBMCException.assertNotExistsBSH(() -> MockBSHIntegrationTest.mockBSH.sendMessage(
                bmc._address(),
                Faker.btpNetwork(), notRegisteredSvc, sn, payload));
    }

    @Test
    void sendMessageShouldRevertUnauthorized() {
        BigInteger sn = BigInteger.ONE;
        byte[] payload = Faker.btpLink().toBytes();

        AssertBMCException.assertUnauthorized(() -> bmc.sendMessage(
                Faker.btpNetwork(), svc, sn, payload));
    }

    @Test
    void dropMessageShouldSendErrorMessageAndMakeEventLog() {
        BigInteger sn = BigInteger.ONE;

        BTPMessage assumeMsg = new BTPMessage();
        assumeMsg.setSrc(linkBtpAddress);
        assumeMsg.setSvc(svc);
        assumeMsg.setDst(BTPAddress.parse(""));
        assumeMsg.setSn(sn);
        assumeMsg.setPayload(new byte[0]);

        BMCStatus status = BMCIntegrationTest.getStatus(bmc, link);
        BigInteger rxSeq = status.getRx_seq().add(BigInteger.ONE);
        BigInteger txSeq = status.getTx_seq().add(BigInteger.ONE);
        iconSpecific.dropMessage(
                BMCIntegrationTest.messageDroppedEvent((el) -> {
                    assertEquals(link, el.getLink());
                    assertEquals(rxSeq, el.getSeq());
                    assertEqualsBTPMessage(assumeMsg, el.getMsg());
                }).andThen(BMCIntegrationTest.messageEvent((el) -> {
                    assertEquals(link, el.getNext());
                    assertEquals(txSeq, el.getSeq());
                    BTPMessage btpMsg = new BTPMessage();
                    btpMsg.setSrc(assumeMsg.getSrc());
                    btpMsg.setDst(btpAddress);
                    btpMsg.setSvc(assumeMsg.getSvc());
                    btpMsg.setSn(assumeMsg.getSn());
                    assertEqualsErrorMessage(btpMsg, el.getMsg(), BMCException.drop());
                })),
                link, rxSeq, svc, sn);
    }
}
