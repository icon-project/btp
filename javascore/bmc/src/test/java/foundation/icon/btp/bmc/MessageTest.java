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
import foundation.icon.btp.lib.BTPException;
import foundation.icon.btp.mock.MockBSH;
import foundation.icon.btp.mock.MockBSHScoreClient;
import foundation.icon.btp.mock.MockRelayMessage;
import foundation.icon.btp.test.*;
import foundation.icon.jsonrpc.Address;
import foundation.icon.jsonrpc.model.TransactionResult;
import foundation.icon.score.test.ScoreIntegrationTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

public class MessageTest implements BMCIntegrationTest {
    static BTPAddress linkBtpAddress = BTPIntegrationTest.Faker.btpLink();
    static String link = linkBtpAddress.toString();
    static String net = linkBtpAddress.net();
    static Address relay = Address.of(bmcClient._wallet());
    static BTPAddress btpAddress = BTPAddress.valueOf(bmc.getBtpAddress());
    static String svc = MockBSH.SERVICE;

    @BeforeAll
    static void beforeAll() {
        System.out.println("beforeAll start");
        BMVManagementTest.addVerifier(net, MockBMVIntegrationTest.mockBMVClient._address());
        LinkManagementTest.addLink(link);
        BMRManagementTest.addRelay(link, relay);

        BSHManagementTest.clearService(svc);
        BSHManagementTest.addService(svc, MockBSHIntegrationTest.mockBSHClient._address());
        System.out.println("beforeAll end");
    }

    @AfterAll
    static void afterAll() {
        System.out.println("afterAll start");
        BSHManagementTest.clearService(svc);

        BMRManagementTest.clearRelay(link, relay);
        LinkManagementTest.clearLink(link);
        BMVManagementTest.clearVerifier(net);
        System.out.println("afterAll end");
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

    static Consumer<List<BMCMessage>> sackMessageChecker(long height, BigInteger seq) {
        return (bmcMessages) -> {
            List<SackMessage> sackMessages = BMCIntegrationTest.internalMessages(
                    bmcMessages, BTPMessageCenter.Internal.Sack, SackMessage::fromBytes);
            assertEquals(1, sackMessages.size());
            SackMessage sackMessage = sackMessages.get(0);
            assertEquals(height, sackMessage.getHeight());
            assertEquals(seq, sackMessage.getSeq());
        };
    }

    static Consumer<List<BMCMessage>> feeGatheringMessageChecker(BTPAddress fa, List<String> services, int size) {
        return (bmcMessages) -> {
            List<FeeGatheringMessage> feeGatheringMessages = BMCIntegrationTest.internalMessages(
                    bmcMessages, BTPMessageCenter.Internal.FeeGathering, FeeGatheringMessage::fromBytes);
            assertEquals(size, feeGatheringMessages.size());
            assertTrue(feeGatheringMessages.stream()
                    .allMatch((feeGatheringMsg) -> feeGatheringMsg.getFa().equals(fa)
                            && services.size() == feeGatheringMsg.getSvcs().length
                            && services.containsAll(Arrays.asList(feeGatheringMsg.getSvcs()))));
        };
    }

    @Test
    void sackMessageShouldUpdateSackHeightAndSackSeq() {
        SackMessage sackMessage = new SackMessage();
        sackMessage.setHeight(1);
        sackMessage.setSeq(BigInteger.ONE);
        List<BTPMessage> btpMessages = new ArrayList<>();
        btpMessages.add(btpMessage(BTPMessageCenter.Internal.Sack, sackMessage.toBytes()));

        MockRelayMessage relayMessage = new MockRelayMessage();
        relayMessage.setBtpMessages(toBytesArray(btpMessages));
        bmc.handleRelayMessage(link, relayMessage.toBase64String());
        BMCStatus status = BMCIntegrationTest.getStatus(bmc, link);
        System.out.println(status);
        assertEquals(sackMessage.getHeight(), status.getSack_height());
        assertEquals(sackMessage.getSeq(), status.getSack_seq());
    }

    @Test
    void handleRelayMessageShouldSendSackMessageToPrev() {
        //if sackTerm > 0 && sackNext <= blockHeight
        int sackTerm = 2;
        iconSpecific.setLinkSackTerm(link, sackTerm);
        BigInteger seq = BMCIntegrationTest.getStatus(bmc, link).getRx_seq();
        long height = 1;
        MockRelayMessage relayMessage = new MockRelayMessage();
        relayMessage.setHeight(height);
        //check SackMessage
        Consumer<TransactionResult> sackMessageCheck = (txr) -> {
            sackMessageChecker(height, seq)
                    .accept(BMCIntegrationTest.bmcMessages(txr, (next) -> next.equals(link)));
        };
        ((BMCScoreClient) bmc).handleRelayMessage(sackMessageCheck, link, relayMessage.toBase64String());
        sackTerm = 0;
        iconSpecific.setLinkSackTerm(link, sackTerm);
    }

    @Test
    void handleRelayMessageShouldNotSendSackMessage() {
        int sackTerm = 10;
        iconSpecific.setLinkSackTerm(link, sackTerm);
        long height = 1;
        MockRelayMessage relayMessage = new MockRelayMessage();
        relayMessage.setHeight(height);

        Consumer<TransactionResult> notExistsSackMessageCheck = (txr) -> {
            assertFalse(BMCIntegrationTest.bmcMessages(txr, (next) -> next.equals(link)).stream()
                    .anyMatch((bmcMsg) -> bmcMsg.getType().equals(BTPMessageCenter.Internal.Sack.name())));
        };
        ((BMCScoreClient) bmc).handleRelayMessage(notExistsSackMessageCheck, link, relayMessage.toBase64String());
        sackTerm = 0;
        iconSpecific.setLinkSackTerm(link, sackTerm);
    }

    @Test
    void feeGatheringMessageShouldCallHandleFeeGathering() {
        //handleRelayMessage -> BSHMock.handleFeeGathering -> EventLog
        FeeGatheringMessage feeGatheringMessage = new FeeGatheringMessage();
        BTPAddress fa = new BTPAddress(
                BTPAddress.PROTOCOL_BTP,
                net,
                ScoreIntegrationTest.Faker.address(Address.Type.CONTRACT).toString());
        feeGatheringMessage.setFa(fa);
        feeGatheringMessage.setSvcs(new String[]{svc});
        List<BTPMessage> btpMessages = new ArrayList<>();
        btpMessages.add(btpMessage(BTPMessageCenter.Internal.FeeGathering, feeGatheringMessage.toBytes()));

        MockRelayMessage relayMessage = new MockRelayMessage();
        relayMessage.setBtpMessages(toBytesArray(btpMessages));
        ((BMCScoreClient) bmc).handleRelayMessage(
                MockBSHIntegrationTest.eventLogChecker(HandleFeeGatheringEventLog::eventLogs, (el) -> {
                    assertEquals(fa.toString(), el.getFa());
                    assertEquals(svc, el.getSvc());
                }),
                link, relayMessage.toBase64String());
    }

    @Test
    void handleRelayMessageShouldSendFeeGatheringMessage() {
        //if gatherFeeTerm > 0 && gatherFeeNext <= blockHeight
        int gatherFeeTerm = 2;
        iconSpecific.setFeeGatheringTerm(gatherFeeTerm);

        Address feeAggregator = ScoreIntegrationTest.Faker.address(Address.Type.CONTRACT);
        iconSpecific.setFeeAggregator(feeAggregator);
        BTPAddress fa = new BTPAddress(BTPAddress.PROTOCOL_BTP, btpAddress.net(), feeAggregator.toString());

        @SuppressWarnings({"rawtypes", "unchecked"})
        List<String> services = new ArrayList<>(bmc.getServices().keySet());

        List<String> links = Arrays.asList(bmc.getLinks());
        Consumer<TransactionResult> feeGatheringMessageCheck = (txr) -> {
            feeGatheringMessageChecker(fa, services, links.size())
                    .accept(BMCIntegrationTest.bmcMessages(txr, links::contains));
        };
        ((BMCScoreClient) bmc).handleRelayMessage(feeGatheringMessageCheck, link, new MockRelayMessage().toBase64String());
        gatherFeeTerm = 0;
        iconSpecific.setFeeGatheringTerm(gatherFeeTerm);
    }

    @Test
    void handleRelayMessageShouldNotSendFeeGatheringMessage() {
        //check FeeGathering notExists
        int gatherFeeTerm = 10;
        iconSpecific.setFeeGatheringTerm(gatherFeeTerm);

        List<String> links = Arrays.asList(bmc.getLinks());
        Consumer<TransactionResult> notExistsSackMessageCheck = (txr) -> {
            assertFalse(BMCIntegrationTest.bmcMessages(txr, links::contains).stream()
                    .anyMatch((bmcMsg) -> bmcMsg.getType().equals(BTPMessageCenter.Internal.FeeGathering.name())));
        };
        ((BMCScoreClient) bmc).handleRelayMessage(notExistsSackMessageCheck, link, new MockRelayMessage().toBase64String());
        gatherFeeTerm = 0;
        iconSpecific.setFeeGatheringTerm(gatherFeeTerm);
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
        ((BMCScoreClient) bmc).handleRelayMessage(
                MockBSHIntegrationTest.eventLogChecker(HandleBTPMessageEventLog::eventLogs, (el) -> {
                    assertEquals(net, el.getFrom());
                    assertEquals(svc, el.getSvc());
                    assertEquals(sn, el.getSn());
                    assertArrayEquals(payload, el.getMsg());
                }),
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
        ((BMCScoreClient) bmc).handleRelayMessage(
                MockBSHIntegrationTest.eventLogChecker(HandleBTPErrorEventLog::eventLogs, (el) -> {
                    assertEquals(link, el.getSrc());
                    assertEquals(svc, el.getSvc());
                    assertEquals(sn, el.getSn());
                    assertEquals(errorMessage.getCode(), el.getCode());
                    assertEquals(errorMessage.getMsg(), el.getMsg());
                }),
                link,
                relayMessage.toBase64String());
    }

    @Test
    void handleRelayMessageShouldDropAndSendErrorMessageAndMakeEventLog() {
        //BMC.dropMessage(BMC.getStatus().getRx_seq().add(BigInteger.ONE))
        //BMC.handleRelayMessage -> BMC.Message(str,int,bytes)
        BMCStatus status = BMCIntegrationTest.getStatus(bmc, link);
        BigInteger rxSeq = status.getRx_seq().add(BigInteger.ONE);
        DropMessageTest.scheduleDropMessage(link, rxSeq);

        BigInteger txSeq = status.getTx_seq().add(BigInteger.ONE);

        BigInteger sn = BigInteger.ONE;
        byte[] payload = BTPIntegrationTest.Faker.btpLink().toBytes();
        List<BTPMessage> btpMessages = new ArrayList<>();
        btpMessages.add(btpMessage(svc, sn, payload));
        MockRelayMessage relayMessage = new MockRelayMessage();
        relayMessage.setBtpMessages(toBytesArray(btpMessages));
        ((BMCScoreClient) bmc).handleRelayMessage(
                BMCIntegrationTest.eventLogChecker(MessageDroppedEventLog::eventLogs, (el) -> {
                    assertEquals(link, el.getLink());
                    assertEquals(rxSeq, el.getSeq());
                    assertEqualsBTPMessage(btpMessages.get(0), el.getMsg());
                }).andThen(BMCIntegrationTest.eventLogChecker(MessageEventLog::eventLogs, (el) -> {
                    assertEquals(link, el.getNext());
                    assertEquals(txSeq, el.getSeq());
                    assertEqualsErrorMessage(btpMessages.get(0), el.getMsg(), BMCException.drop());
                })).andThen(MockBSHIntegrationTest.notExistsEventLogChecker(
                        HandleBTPMessageEventLog::eventLogs)),
                link,
                relayMessage.toBase64String());

        assertFalse(DropMessageTest.isExistsScheduledDropMessage(link, rxSeq));
    }

    @Test
    void handleRelayMessageShouldDropAndMakeEventLog() {
        //BMC.dropMessage
        //BMC.handleRelayMessage -> BMC.DropMessage(str,int,bytes)
        BigInteger seq = BMCIntegrationTest.getStatus(bmc, link).getRx_seq().add(BigInteger.ONE);
        DropMessageTest.scheduleDropMessage(link, seq);

        ErrorMessage errorMessage = new ErrorMessage();
        errorMessage.setCode(0);
        errorMessage.setMsg("error");
        BigInteger sn = BigInteger.ONE.negate();
        byte[] payload = errorMessage.toBytes();
        List<BTPMessage> btpMessages = new ArrayList<>();
        btpMessages.add(btpMessage(svc, sn, payload));
        MockRelayMessage relayMessage = new MockRelayMessage();
        relayMessage.setBtpMessages(toBytesArray(btpMessages));
        ((BMCScoreClient) bmc).handleRelayMessage(
                BMCIntegrationTest.eventLogChecker(MessageDroppedEventLog::eventLogs, (el) -> {
                    assertEquals(link, el.getLink());
                    assertEquals(seq, el.getSeq());
                    assertEqualsBTPMessage(btpMessages.get(0), el.getMsg());
                }).andThen(MockBSHIntegrationTest.notExistsEventLogChecker(
                        HandleBTPMessageEventLog::eventLogs)),
                link,
                relayMessage.toBase64String());

        assertFalse(DropMessageTest.isExistsScheduledDropMessage(link, seq));
    }

    static String[] fragments(String s, int count) {
        int len = s.length();
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
            if (end < len) {
                arr[i] = s.substring(begin, end);
            } else {
                arr[i] = s.substring(begin);
            }
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
        String msg = relayMessage.toBase64String();
        int count = 3;
        int last = count - 1;
        String[] fragments = fragments(msg, count);
        for (int i = 0; i < count; i++) {
            if (i == 0) {
                iconSpecific.handleFragment(link, fragments[i], -1 * last);
            } else if (i == last) {
                ((ICONSpecificScoreClient) iconSpecific).handleFragment(
                        MockBSHIntegrationTest.eventLogChecker(HandleBTPMessageEventLog::eventLogs, (el) -> {
                            assertEquals(net, el.getFrom());
                            assertEquals(svc, el.getSvc());
                            assertEquals(sn, el.getSn());
                            assertArrayEquals(payload, el.getMsg());
                        }),
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
        ((MockBSHScoreClient) MockBSHIntegrationTest.mockBSH).intercallSendMessage(
                BMCIntegrationTest.eventLogChecker(MessageEventLog::eventLogs, (el) -> {
                    assertEquals(link, el.getNext());
                    assertEquals(seq, el.getSeq());
                    BTPMessage btpMessage = el.getMsg();
                    assertEquals(btpAddress, btpMessage.getSrc());
                    assertEquals(linkBtpAddress, btpMessage.getDst());
                    assertEquals(svc, btpMessage.getSvc());
                    assertEquals(sn, btpMessage.getSn());
                    assertArrayEquals(payload, btpMessage.getPayload());
                }),
                ((BMCScoreClient) bmc)._address(),
                net, svc, sn, payload);
    }

    @Test
    void sendMessageShouldRevertUnreachable() {
        BigInteger sn = BigInteger.ONE;
        byte[] payload = Faker.btpLink().toBytes();

        AssertBMCException.assertUnreachable(() -> MockBSHIntegrationTest.mockBSH.intercallSendMessage(
                ((BMCScoreClient) bmc)._address(),
                Faker.btpNetwork(), svc, sn, payload));
    }

    @Test
    void sendMessageShouldRevertNotExistsBSH() {
        BigInteger sn = BigInteger.ONE;
        byte[] payload = Faker.btpLink().toBytes();

        String notRegisteredSvc = BTPIntegrationTest.Faker.btpService();

        AssertBMCException.assertNotExistsBSH(() -> MockBSHIntegrationTest.mockBSH.intercallSendMessage(
                ((BMCScoreClient) bmc)._address(),
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
        assumeMsg.setSn(sn);

        BMCStatus status = BMCIntegrationTest.getStatus(bmc, link);
        BigInteger rxSeq = status.getRx_seq().add(BigInteger.ONE);
        BigInteger txSeq = status.getTx_seq().add(BigInteger.ONE);
        ((ICONSpecificScoreClient)iconSpecific).dropMessage(
                BMCIntegrationTest.eventLogChecker(MessageDroppedEventLog::eventLogs, (el) -> {
                    assertEquals(link, el.getLink());
                    assertEquals(rxSeq, el.getSeq());
                    assertEqualsBTPMessage(assumeMsg, el.getMsg());
                }).andThen(BMCIntegrationTest.eventLogChecker(MessageEventLog::eventLogs, (el) -> {
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
