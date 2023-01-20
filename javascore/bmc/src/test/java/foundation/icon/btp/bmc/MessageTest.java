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

import foundation.icon.btp.lib.BTPAddress;
import foundation.icon.btp.lib.BTPException;
import foundation.icon.btp.mock.MockRelayMessage;
import foundation.icon.btp.test.AssertBTPException;
import foundation.icon.btp.test.MockBMVIntegrationTest;
import foundation.icon.btp.test.MockBSHIntegrationTest;
import foundation.icon.jsonrpc.Address;
import foundation.icon.jsonrpc.model.TransactionResult;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class MessageTest implements BMCIntegrationTest {
    static BTPAddress link = Faker.btpLink();
    static BTPAddress reachable = Faker.btpLink();
    static String svc = MockBSHIntegrationTest.SERVICE;
    static Address relay = Address.of(bmc._wallet());
    static BigInteger[] emptyFeeValues = new BigInteger[]{};
    //for intermediate path test
    static BTPAddress secondLink = Faker.btpLink();

    static byte[][] toBytesArray(List<BTPMessage> btpMessages) {
        int len = btpMessages.size();
        byte[][] bytesArray = new byte[len][];
        for (int i = 0; i < len; i++) {
            bytesArray[i] = btpMessages.get(i).toBytes();
        }
        return bytesArray;
    }

    static void ensureReachable(BTPAddress link, BTPAddress[] reachable) {
        InitMessage initMessage = new InitMessage();
        initMessage.setLinks(reachable);
        BMCMessage bmcMessage = new BMCMessage();
        bmcMessage.setType(BTPMessageCenter.Internal.Init.name());
        bmcMessage.setPayload(initMessage.toBytes());
        BTPMessage msg = new BTPMessage();
        msg.setSrc(link.net());
        msg.setDst(btpAddress.net());
        msg.setSvc(BTPMessageCenter.INTERNAL_SERVICE);
        msg.setSn(BigInteger.ZERO);
        msg.setPayload(bmcMessage.toBytes());
        msg.setNsn(BigInteger.ONE);
        msg.setFeeInfo(null);
        bmc.handleRelayMessage(
                link.toString(),
                mockRelayMessage(msg).toBase64String());
    }

    @BeforeAll
    static void beforeAll() {
        System.out.println("MessageTest:beforeAll start");
        bmc.setDumpJson(true);
        BMVManagementTest.addVerifier(link.net(), MockBMVIntegrationTest.mockBMV._address());
        LinkManagementTest.addLink(link.toString());
        BMRManagementTest.addRelay(link.toString(), relay);
        ensureReachable(link, new BTPAddress[]{reachable});

        BMVManagementTest.addVerifier(secondLink.net(), MockBMVIntegrationTest.mockBMV._address());
        LinkManagementTest.addLink(secondLink.toString());
        BMRManagementTest.addRelay(secondLink.toString(), relay);

        BSHManagementTest.clearService(svc);
        BSHManagementTest.addService(svc, MockBSHIntegrationTest.mockBSH._address());

        System.out.println("MessageTest:beforeAll end");
    }

    @AfterAll
    static void afterAll() {
        System.out.println("MessageTest:afterAll start");
        BSHManagementTest.clearService(svc);

        BMRManagementTest.clearRelay(link.toString(), relay);
        LinkManagementTest.clearLink(link.toString());
        BMVManagementTest.clearVerifier(link.net());

        BMRManagementTest.clearRelay(secondLink.toString(), relay);
        LinkManagementTest.clearLink(secondLink.toString());
        BMVManagementTest.clearVerifier(secondLink.net());
        System.out.println("MessageTest:afterAll end");
    }

    static void assertEqualsFeeInfo(FeeInfo o1, FeeInfo o2) {
        if (o1 == null) {
            assertNull(o2);
        } else {
            assertEquals(o1.getNetwork(), o2.getNetwork());
            assertArrayEquals(o1.getValues(), o2.getValues());
        }
    }

    static void assertEqualsBTPMessage(BTPMessage o1, BTPMessage o2) {
        assertEquals(o1.getSrc(), o2.getSrc());
        assertEquals(o1.getDst(), o2.getDst());
        assertEquals(o1.getSvc(), o2.getSvc());
        assertEquals(o1.getSn(), o2.getSn());
        assertArrayEquals(o1.getPayload(), o2.getPayload());
        assertEquals(o1.getNsn(), o2.getNsn());
        assertEqualsFeeInfo(o1.getFeeInfo(), o2.getFeeInfo());
    }

    static Consumer<TransactionResult> btpEventChecker(
            BTPMessage msg, BTPAddress next, BTPMessageCenter.Event event) {
        if (msg.getNsn().compareTo(BigInteger.ZERO) > 0) {
            return btpEventChecker(msg.getSrc(), msg.getNsn(), next, event);
        } else {
            return btpEventChecker(msg.getDst(), msg.getNsn().negate(), next, event);
        }
    }

    static Consumer<TransactionResult> btpEventChecker(
            String src, BigInteger nsn, BTPAddress next, BTPMessageCenter.Event event) {
        return BMCIntegrationTest.btpEvent(
                (l) -> assertTrue(l.stream().anyMatch((el) ->
                        el.getSrc().equals(src) &&
                                el.getNsn().equals(nsn) &&
                                el.getNext().equals(next == null ? "" : next.toString()) &&
                                el.getEvent().equals(event.name())
                )));
    }

    @ParameterizedTest
    @MethodSource("sendMessageShouldSuccessArguments")
    void sendMessageShouldSuccess(
            String display,
            BTPAddress dst, BTPAddress next, BigInteger sn) {
        System.out.println(display);
        byte[] payload = Faker.btpLink().toBytes();
        FeeInfo feeInfo = new FeeInfo(btpAddress.net(), emptyFeeValues);

        BigInteger nsn = bmc.getNetworkSn();
        BigInteger txSeq = bmc.getStatus(next.toString())
                .getTx_seq();
        Consumer<TransactionResult> checker = BMCIntegrationTest.messageEvent((el) -> {
            assertEquals(next.toString(), el.getNext());
            assertEquals(txSeq.add(BigInteger.ONE), el.getSeq());
            BTPMessage btpMessage = el.getMsg();
            assertEquals(btpAddress.net(), btpMessage.getSrc());
            assertEquals(dst.net(), btpMessage.getDst());
            assertEquals(svc, btpMessage.getSvc());
            assertEquals(sn, btpMessage.getSn());
            assertArrayEquals(payload, btpMessage.getPayload());
            assertEquals(nsn.add(BigInteger.ONE), btpMessage.getNsn());
            assertEqualsFeeInfo(feeInfo, btpMessage.getFeeInfo());
        });
        checker = checker.andThen(btpEventChecker(
                btpAddress.net(),
                nsn.add(BigInteger.ONE),
                next,
                BTPMessageCenter.Event.SEND));
        MockBSHIntegrationTest.mockBSH.sendMessage(
                checker,
                bmc._address(),
                dst.net(), svc, sn, payload);
    }

    static Stream<Arguments> sendMessageShouldSuccessArguments() {
        return Stream.of(
                Arguments.of(
                        "unidirectionalSendToLink",
                        link, link,
                        BigInteger.ZERO),
                Arguments.of(
                        "bidirectionalSendToLink",
                        link, link,
                        BigInteger.ONE),
                Arguments.of(
                        "unidirectionalSendToReachable",
                        reachable, link,
                        BigInteger.ZERO),
                Arguments.of(
                        "bidirectionalSendToReachable",
                        reachable, link,
                        BigInteger.ONE)
        );
    }

    @SuppressWarnings("ThrowableNotThrown")
    @ParameterizedTest
    @MethodSource("sendMessageShouldRevertArguments")
    void sendMessageShouldRevert(
            String display,
            BTPException exception,
            String dstNet, String svc, BigInteger sn) {
        System.out.println(display);
        AssertBTPException.assertBTPException(exception, () ->
                MockBSHIntegrationTest.mockBSH.sendMessage(
                        bmc._address(),
                        dstNet, svc, sn, Faker.btpLink().toBytes()));
    }

    static Stream<Arguments> sendMessageShouldRevertArguments() {
        return Stream.of(
                Arguments.of(
                        "sendMessageShouldRevertNotExistsBSH",
                        BMCException.notExistsBSH(),
                        link.net(),
                        Faker.btpService(),
                        BigInteger.ZERO),
                Arguments.of(
                        "sendMessageShouldRevertUnreachable",
                        BMCException.unreachable(),
                        Faker.btpNetwork(),
                        svc,
                        BigInteger.ZERO),
                Arguments.of(
                        "replySendMessageShouldRevert",
                        BMCException.unknown("not exists response"),
                        link.net(),
                        svc,
                        BigInteger.valueOf(Long.MAX_VALUE).negate())
        );
    }

    @Test
    void sendMessageShouldRevertUnauthorized() {
        AssertBMCException.assertUnauthorized(() -> bmc.sendMessage(
                (txr) -> {
                },
                link.net(), svc, BigInteger.ZERO, Faker.btpLink().toBytes()));
    }

    @ParameterizedTest
    @MethodSource("handleRelayMessageShouldSuccessArguments")
    void handleRelayMessageShouldSuccess(
            String display,
            BTPAddress src, BTPAddress dst, BTPAddress prev, BTPAddress next,
            String svc, BigInteger sn, BTPException expectBTPError) {
        System.out.println(display);
        int snCompare = sn.compareTo(BigInteger.ZERO);
        if (snCompare < 0) {
            throw new IllegalArgumentException("sn should be positive or zero");
        }

        BTPMessage msg = new BTPMessage();
        msg.setSrc(src.net());
        msg.setDst(dst.net());
        msg.setSvc(svc);
        msg.setSn(sn);
        msg.setPayload(Faker.btpLink().toBytes());
        msg.setNsn(BigInteger.ONE);
        msg.setFeeInfo(new FeeInfo(src.net(), emptyFeeValues));

        System.out.println("handleRelayMessageShouldIncreaseRxSeq");
        Consumer<TransactionResult> checker = rxSeqChecker(prev);
        if (expectBTPError != null) {
            if (snCompare > 0) {
                System.out.println("handleRelayMessageShouldReplyBTPError");
                checker = checker.andThen(responseMessageChecker(prev, msg, expectBTPError));
            } else {//snCompare == 0
                System.out.println("handleRelayMessageShouldDrop");
                checker = checker.andThen(dropChecker(prev, msg, expectBTPError));
            }
        } else {
            if (!dst.equals(btpAddress)) {
                System.out.println("handleRelayMessageShouldSendToNext");
                checker = checker.andThen(routeChecker(next, msg));
            } else {
                System.out.println("handleRelayMessageShouldCallHandleBTPMessage");
                checker = checker.andThen(handleBTPMessageChecker(msg));
                if (snCompare > 0) {
                    System.out.println("handleRelayMessageShouldStoreResponse");
                    checker = checker.andThen(responseChecker(prev, msg));
                }
            }
        }
        bmc.handleRelayMessage(
                checker,
                prev.toString(),
                mockRelayMessage(msg).toBase64String());
    }

    static Consumer<TransactionResult> rxSeqChecker(
            final BTPAddress prev) {
        BigInteger rxSeq = bmc.getStatus(prev.toString())
                .getRx_seq();
        return (txr) -> {
            assertEquals(rxSeq.add(BigInteger.ONE),
                    bmc.getStatus(prev.toString()).getRx_seq());
        };
    }

    static Consumer<TransactionResult> handleBTPMessageChecker(
            final BTPMessage msg) {
        return MockBSHIntegrationTest.handleBTPMessageEvent((el) -> {
            assertEquals(msg.getSrc(), el.getFrom());
            assertEquals(msg.getSvc(), el.getSvc());
            assertEquals(msg.getSn(), el.getSn());
            assertArrayEquals(msg.getPayload(), el.getMsg());
        }).andThen(btpEventChecker(msg, null, BTPMessageCenter.Event.RECEIVE));
    }

    static Consumer<TransactionResult> sendMessageChecker(
            final BTPAddress next, final BTPMessage msg) {
        BigInteger txSeq = bmc.getStatus(next.toString())
                .getTx_seq();
        return BMCIntegrationTest.messageEvent((el) -> {
            assertEquals(next.toString(), el.getNext());
            assertEquals(txSeq.add(BigInteger.ONE), el.getSeq());
            assertEqualsBTPMessage(msg, el.getMsg());
        });
    }

    static Consumer<TransactionResult> routeChecker(
            final BTPAddress next, final BTPMessage msg) {
        return routeChecker(next, msg, (v) -> v).andThen(
                btpEventChecker(msg, next, BTPMessageCenter.Event.ROUTE));
    }

    static Consumer<TransactionResult> routeChecker(
            final BTPAddress next, final BTPMessage msg,
            Function<FeeInfo, FeeInfo> feeInfoSupplier) {
        BTPMessage routeMsg = new BTPMessage();
        routeMsg.setSrc(msg.getSrc());
        routeMsg.setDst(msg.getDst());
        routeMsg.setSvc(msg.getSvc());
        routeMsg.setSn(msg.getSn());
        routeMsg.setPayload(msg.getPayload());
        routeMsg.setNsn(msg.getNsn());
        routeMsg.setFeeInfo(feeInfoSupplier.apply(msg.getFeeInfo()));
        return sendMessageChecker(next, routeMsg);
    }

    static Consumer<TransactionResult> responseMessageChecker(
            final BTPAddress prev, final BTPMessage msg, final BTPException exception) {
        return responseMessageChecker(prev, msg, exception, (v) -> v);
    }

    static Consumer<TransactionResult> responseMessageChecker(
            final BTPAddress prev, final BTPMessage msg, final BTPException exception,
            Function<FeeInfo, FeeInfo> feeInfoSupplier) {
        ResponseMessage errMsg = BTPMessageCenter.toResponseMessage(exception);
        BTPMessage response = new BTPMessage();
        response.setSrc(btpAddress.net());
        response.setDst(msg.getSrc());
        response.setSvc(msg.getSvc());
        response.setSn(exception == null ? BigInteger.ZERO : msg.getSn().negate());
        response.setPayload(exception == null ?
                new BMCMessage(
                        BTPMessageCenter.Internal.Response.name(),
                        errMsg.toBytes()).toBytes() :
                errMsg.toBytes());
        response.setNsn(msg.getNsn().negate());
        response.setFeeInfo(feeInfoSupplier.apply(msg.getFeeInfo()));
        return sendMessageChecker(prev, response).andThen(
                btpEventChecker(msg.getSrc(), msg.getNsn(), prev,
                        exception == null ? BTPMessageCenter.Event.REPLY : BTPMessageCenter.Event.ERROR));
    }

    static Consumer<TransactionResult> responseChecker(
            final BTPAddress prev, final BTPMessage msg) {
        return responseChecker(prev, msg, (v) ->
                new FeeInfo(v.getNetwork(), emptyFeeValues));
    }

    static Consumer<TransactionResult> responseChecker(
            final BTPAddress prev, final BTPMessage msg,
            Function<FeeInfo, FeeInfo> feeInfoSupplier) {
        return (txr) -> {
            BTPMessage resp = new BTPMessage();
            resp.setSrc(msg.getDst());
            resp.setDst(msg.getSrc());
            resp.setSvc(msg.getSvc());
            resp.setSn(BigInteger.ZERO);
            resp.setPayload(msg.getPayload());
            resp.setNsn(msg.getNsn().negate());
            resp.setFeeInfo(feeInfoSupplier.apply(msg.getFeeInfo()));
            Consumer<TransactionResult> checker = sendMessageChecker(prev, resp)
                    .andThen(btpEventChecker(msg.getSrc(), msg.getNsn(), prev, BTPMessageCenter.Event.REPLY));
            MockBSHIntegrationTest.mockBSH.sendMessage(
                    checker,
                    bmc._address(),
                    resp.getDst(), resp.getSvc(), msg.getSn().negate(), resp.getPayload());
        };
    }

    static Consumer<TransactionResult> dropChecker(
            final BTPAddress prev, final BTPMessage msg, final BTPException e) {
        BigInteger rxSeq = bmc.getStatus(prev.toString())
                .getRx_seq();
        return BMCIntegrationTest.messageDroppedEvent((el) -> {
            assertEquals(prev.toString(), el.getPrev());
            assertEquals(rxSeq.add(BigInteger.ONE), el.getSeq());
            assertEqualsBTPMessage(msg, el.getMsg());
            assertEquals(BigInteger.valueOf(e.getCode()), el.getEcode());
            assertEquals(e.getMessage(), el.getEmsg());
        }).andThen(btpEventChecker(msg, null, BTPMessageCenter.Event.DROP));
    }

    static Stream<Arguments> handleRelayMessageShouldSuccessArguments() {
        return Stream.of(
                Arguments.of(
                        "unidirectionalHandleRelayMessage",
                        link, btpAddress, link, null,
                        svc, BigInteger.ZERO,
                        null),
                Arguments.of(
                        "bidirectionalHandleRelayMessage",
                        link, btpAddress, link, null,
                        svc, BigInteger.ONE,
                        null),
                Arguments.of(
                        "unidirectionalHandleRelayMessageShouldNotReplyBTPError",
                        link, btpAddress, link, null,
                        Faker.btpService(), BigInteger.ZERO,
                        BMCException.notExistsBSH()),
                Arguments.of(
                        "bidirectionalHandleRelayMessageShouldReplyBTPError",
                        link, btpAddress, link, null,
                        Faker.btpService(), BigInteger.ONE,
                        BMCException.notExistsBSH()),
                Arguments.of(
                        "handleRelayMessageInIntermediate",
                        secondLink, link, secondLink, link,
                        svc, BigInteger.ZERO,
                        null, BTPMessageCenter.Event.ROUTE),
                Arguments.of(
                        "handleRelayMessageShouldNotReplyBTPErrorInIntermediate",
                        secondLink, Faker.btpLink(), secondLink, null,
                        svc, BigInteger.ZERO,
                        BMCException.unreachable()),
                Arguments.of(
                        "handleRelayMessageShouldReplyBTPErrorInIntermediate",
                        secondLink, Faker.btpLink(), secondLink, null,
                        svc, BigInteger.ONE,
                        BMCException.unreachable())
        );
    }

    @Test
    void handleRelayMessageShouldCallHandleBTPError() {
        ResponseMessage responseMsg = BTPMessageCenter.toResponseMessage(BMCException.unknown("error"));
        BTPMessage msg = new BTPMessage();
        msg.setSrc(link.net());
        msg.setDst(btpAddress.net());
        msg.setSvc(svc);
        msg.setSn(BigInteger.ONE.negate());
        msg.setPayload(responseMsg.toBytes());
        msg.setNsn(BigInteger.ONE.negate());
        msg.setFeeInfo(new FeeInfo(
                btpAddress.net(), emptyFeeValues));
        Consumer<TransactionResult> checker = MockBSHIntegrationTest.handleBTPErrorEvent(
                (el) -> {
                    assertEquals(msg.getSrc(), el.getSrc());
                    assertEquals(msg.getSvc(), el.getSvc());
                    assertEquals(msg.getSn().negate(), el.getSn());
                    assertEquals(responseMsg.getCode(), el.getCode());
                    assertEquals(responseMsg.getMsg(), el.getMsg());
                }
        ).andThen(btpEventChecker(msg, null, BTPMessageCenter.Event.RECEIVE));
        bmc.handleRelayMessage(
                checker,
                link.toString(),
                mockRelayMessage(msg).toBase64String());
    }

    static BTPMessage btpMessageForSuccess(BTPAddress src) {
        BTPMessage msg = new BTPMessage();
        msg.setSrc(src.net());
        msg.setDst(btpAddress.net());
        msg.setSvc(svc);
        msg.setSn(BigInteger.ONE);
        msg.setPayload(Faker.btpLink().toBytes());
        msg.setNsn(BigInteger.ONE);
        msg.setFeeInfo(new FeeInfo(
                btpAddress.net(), emptyFeeValues));
        return msg;
    }

    static MockRelayMessage mockRelayMessage(BTPMessage... msgs) {
        MockRelayMessage relayMessage = new MockRelayMessage();
        relayMessage.setBtpMessages(toBytesArray(List.of(msgs)));
        return relayMessage;
    }

    @Test
    void handleRelayMessageShouldRevertNotExistsLink() {
        AssertBMCException.assertNotExistsLink(() ->
                bmc.handleRelayMessage(new BTPAddress(link.net(), Faker.btpLink().account()).toString(),
                        mockRelayMessage(btpMessageForSuccess(link)).toBase64String()));
    }

    @Test
    void handleRelayMessageShouldRevertUnauthorized() {
        AssertBMCException.assertUnauthorized(() ->
                bmcWithTester.handleRelayMessage(link.toString(),
                        mockRelayMessage(btpMessageForSuccess(link)).toBase64String()));
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
        //BMC.handleFragment -> BMC.handleRelayMessage -> BSHMock.HandleBTPMessage
        BTPMessage msg = btpMessageForSuccess(link);
        MockRelayMessage relayMessage = mockRelayMessage(msg);
        byte[] bytes = relayMessage.toBytes();
        int count = 3;
        int last = count - 1;
        String[] fragments = fragments(bytes, count);
        for (int i = 0; i < count; i++) {
            if (i == 0) {
                iconSpecific.handleFragment(link.toString(), fragments[i], -1 * last);
            } else if (i == last) {
                Consumer<TransactionResult> checker = handleBTPMessageChecker(msg);
                iconSpecific.handleFragment(
                        checker,
                        link.toString(), fragments[i], 0);
            } else {
                iconSpecific.handleFragment(link.toString(), fragments[i], last - i);
            }
        }
    }

    @ParameterizedTest
    @MethodSource("dropMessageShouldSuccessArguments")
    void dropMessageShouldSuccess(
            String display,
            BigInteger sn, BigInteger nsn) {
        System.out.println(display);

        BTPMessage msg = new BTPMessage();
        msg.setSrc(link.net());
        msg.setDst("");
        msg.setSvc(svc);
        msg.setSn(sn);
        msg.setPayload(new byte[0]);
        msg.setNsn(nsn);
        msg.setFeeInfo(new FeeInfo(
                link.net(), emptyFeeValues));

        System.out.println("dropMessageShouldIncreaseRxSeqAndDrop");
        BigInteger rxSeq = bmc.getStatus(link.toString())
                .getRx_seq();
        Consumer<TransactionResult> checker = rxSeqChecker(link);
        if (sn.compareTo(BigInteger.ZERO) > 0) {
            System.out.println("dropMessageShouldReplyBTPError");
            checker = checker.andThen(responseMessageChecker(link, msg, BMCException.drop()));
        } else {
            checker = checker.andThen(dropChecker(link, msg, BMCException.drop()));
        }
        iconSpecific.dropMessage(checker,
                msg.getSrc(), rxSeq.add(BigInteger.ONE), msg.getSvc(), sn, nsn,
                msg.getFeeInfo().getNetwork(), msg.getFeeInfo().getValues());
    }

    static Stream<Arguments> dropMessageShouldSuccessArguments() {
        return Stream.of(
                Arguments.of(
                        "unidirectionalDropMessage",
                        BigInteger.ZERO, BigInteger.ONE),
                Arguments.of(
                        "bidirectionalDropMessage",
                        BigInteger.ONE, BigInteger.ONE),
                Arguments.of(
                        "replyBTPErrorDropMessage",
                        BigInteger.ONE.negate(), BigInteger.ONE.negate()),
                Arguments.of(
                        "replyDropMessage",
                        BigInteger.ZERO, BigInteger.ONE.negate())
        );
    }

    @SuppressWarnings("ThrowableNotThrown")
    @ParameterizedTest
    @MethodSource("dropMessageShouldRevertArguments")
    void dropMessageShouldRevert(
            String display,
            BTPException exception,
            String src, BigInteger diffSeq, String svc, BigInteger sn, BigInteger nsn) {
        System.out.println(display);
        BigInteger rxSeq = bmc.getStatus(link.toString())
                .getRx_seq();
        AssertBTPException.assertBTPException(exception, () ->
                iconSpecific.dropMessage(
                        src, rxSeq.add(diffSeq), svc, sn, nsn,
                        "", new BigInteger[]{}));
    }

    static Stream<Arguments> dropMessageShouldRevertArguments() {
        return Stream.of(
                Arguments.of(
                        "dropMessageShouldRevertUnreachable",
                        BMCException.unreachable(),
                        Faker.btpNetwork(),
                        BigInteger.ONE,
                        svc,
                        BigInteger.ZERO, BigInteger.ONE),
                Arguments.of(
                        "dropMessageShouldRevertInvalidSeqIfLessThan",
                        BMCException.unknown("invalid _seq"),
                        link.net(),
                        BigInteger.ZERO,
                        svc,
                        BigInteger.ZERO, BigInteger.ONE),
                Arguments.of(
                        "dropMessageShouldRevertInvalidSeqIfGreaterThan",
                        BMCException.unknown("invalid _seq"),
                        link.net(),
                        BigInteger.TWO,
                        svc,
                        BigInteger.ZERO, BigInteger.ONE),
                Arguments.of(
                        "dropMessageShouldRevertNotExistsBSH",
                        BMCException.notExistsBSH(),
                        link.net(),
                        BigInteger.ONE,
                        Faker.btpService(),
                        BigInteger.ZERO, BigInteger.ONE),
                Arguments.of(
                        "dropMessageShouldRevertInvalidSnIfZeroNsn",
                        BMCException.invalidSn(),
                        link.net(),
                        BigInteger.ONE,
                        svc,
                        BigInteger.ZERO, BigInteger.ZERO),
                Arguments.of(
                        "dropMessageShouldRevertInvalidSnIfNegativeSnAndPositiveNsn",
                        BMCException.invalidSn(),
                        link.net(),
                        BigInteger.ONE,
                        svc,
                        BigInteger.ONE.negate(), BigInteger.ONE),
                Arguments.of(
                        "dropMessageShouldRevertInvalidSnIfPositiveSnAndNegativeNsn",
                        BMCException.invalidSn(),
                        link.net(),
                        BigInteger.ONE,
                        svc,
                        BigInteger.ONE, BigInteger.ONE.negate())
        );
    }

}
