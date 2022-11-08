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
import foundation.icon.btp.test.EVMIntegrationTest;
import foundation.icon.btp.test.MockBMVIntegrationTest;
import foundation.icon.btp.test.MockBSHIntegrationTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import java.math.BigInteger;
import java.util.ArrayList;
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
    static String relay = EVMIntegrationTest.credentials.getAddress();
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

    static void ensureReachable(BTPAddress link, BTPAddress[] reachable) throws Exception {
        InitMessage initMessage = new InitMessage();
        initMessage.setLinks(reachable);
        BMCMessage bmcMessage = new BMCMessage();
        bmcMessage.setType(BMCIntegrationTest.Internal.Init.name());
        bmcMessage.setPayload(initMessage.toBytes());
        BTPMessage msg = new BTPMessage();
        msg.setSrc(link.net());
        msg.setDst(btpAddress.net());
        msg.setSvc(BMCIntegrationTest.INTERNAL_SERVICE);
        msg.setSn(BigInteger.ZERO);
        msg.setPayload(bmcMessage.toBytes());
        msg.setNsn(BigInteger.ONE);
        msg.setFeeInfo(new FeeInfo(msg.getSrc(), new BigInteger[]{}));
        bmcPeriphery.handleRelayMessage(
                link.toString(),
                mockRelayMessage(msg).toBytes()).send();
    }

    @BeforeAll
    static void beforeAll() throws Exception {
        System.out.println("MessageTest:beforeAll start");
        BMVManagementTest.addVerifier(link.net(), MockBMVIntegrationTest.mockBMV.getContractAddress());
        LinkManagementTest.addLink(link.toString());
        BMRManagementTest.addRelay(link.toString(), relay);
        ensureReachable(link, new BTPAddress[]{reachable});

        BMVManagementTest.addVerifier(secondLink.net(), MockBMVIntegrationTest.mockBMV.getContractAddress());
        LinkManagementTest.addLink(secondLink.toString());
        BMRManagementTest.addRelay(secondLink.toString(), relay);

        BSHManagementTest.clearService(svc);
        BSHManagementTest.addService(svc, MockBSHIntegrationTest.mockBSH.getContractAddress());

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

    static Consumer<TransactionReceipt> btpEventChecker(
            BTPMessage msg, BTPAddress next, BMCIntegrationTest.Event event) {
        if (msg.getNsn().compareTo(BigInteger.ZERO) > 0) {
            return btpEventChecker(msg.getSrc(), msg.getNsn(), next, event);
        } else {
            return btpEventChecker(msg.getDst(), msg.getNsn().negate(), next, event);
        }
    }

    static Consumer<TransactionReceipt> btpEventChecker(
            String src, BigInteger nsn, BTPAddress next, BMCIntegrationTest.Event event) {
        return BMCIntegrationTest.btpEvent(
                (l) -> assertTrue(l.stream().anyMatch((el) ->
                        el._src.equals(src) &&
                                el._nsn.equals(nsn) &&
                                el._next.equals(next == null ? "" : next.toString()) &&
                                el._event.equals(event.name())
                )));
    }

    @ParameterizedTest
    @MethodSource("sendMessageShouldSuccessArguments")
    void sendMessageShouldSuccess(
            String display,
            BTPAddress dst, BTPAddress next, BigInteger sn) throws Exception {
        System.out.println(display);
        byte[] payload = Faker.btpLink().toBytes();
        FeeInfo feeInfo = new FeeInfo(btpAddress.net(), emptyFeeValues);

        BigInteger nsn = bmcPeriphery.getNetworkSn().send();
        BigInteger txSeq = BMCIntegrationTest.getStatus(next.toString())
                .getTx_seq();
        Consumer<TransactionReceipt> checker = BMCIntegrationTest.messageEvent((el) -> {
            assertEquals(next.toString(), el._next);
            assertEquals(txSeq.add(BigInteger.ONE), el._seq);
            BTPMessage btpMessage = BTPMessage.fromBytes(el._msg);
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
                BMCIntegrationTest.Event.SEND));
        checker.accept(
                MockBSHIntegrationTest.mockBSH.sendMessage(
                        bmcPeriphery.getContractAddress(),
                        dst.net(), svc, sn, payload,
                        BigInteger.ZERO).send()
        );
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
                        bmcPeriphery.getContractAddress(),
                        dstNet, svc, sn, Faker.btpLink().toBytes(),
                        BigInteger.ZERO).send());
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
        AssertBMCException.assertUnauthorized(() -> bmcPeriphery.sendMessage(
                link.net(), svc, BigInteger.ZERO, Faker.btpLink().toBytes(),
                BigInteger.ZERO).send());
    }

    @ParameterizedTest
    @MethodSource("handleRelayMessageShouldSuccessArguments")
    void handleRelayMessageShouldSuccess(
            String display,
            BTPAddress src, BTPAddress dst, BTPAddress prev, BTPAddress next,
            String svc, BigInteger sn, BTPException expectBTPError) throws Exception {
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
        Consumer<TransactionReceipt> checker = rxSeqChecker(prev);
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
        checker.accept(bmcPeriphery.handleRelayMessage(
                prev.toString(),
                mockRelayMessage(msg).toBytes()).send());
    }

    static Consumer<TransactionReceipt> rxSeqChecker(
            final BTPAddress prev) {
        BigInteger rxSeq = BMCIntegrationTest.getStatus(prev.toString())
                .getRx_seq();
        return (txr) -> {
            assertEquals(rxSeq.add(BigInteger.ONE),
                    BMCIntegrationTest.getStatus(prev.toString()).getRx_seq());
        };
    }

    static Consumer<TransactionReceipt> handleBTPMessageChecker(
            final BTPMessage msg) {
        return MockBSHIntegrationTest.handleBTPMessageEvent((el) -> {
            assertEquals(msg.getSrc(), el._from);
            assertEquals(msg.getSvc(), el._svc);
            assertEquals(msg.getSn(), el._sn);
            assertArrayEquals(msg.getPayload(), el._msg);
        }).andThen(btpEventChecker(msg, null, BMCIntegrationTest.Event.RECEIVE));
    }

    static Consumer<TransactionReceipt> sendMessageChecker(
            final BTPAddress next, final BTPMessage msg) {
        BigInteger txSeq = BMCIntegrationTest.getStatus(next.toString())
                .getTx_seq();
        return BMCIntegrationTest.messageEvent((el) -> {
            assertEquals(next.toString(), el._next);
            assertEquals(txSeq.add(BigInteger.ONE), el._seq);
            assertEqualsBTPMessage(msg, BTPMessage.fromBytes(el._msg));
        });
    }

    static Consumer<TransactionReceipt> routeChecker(
            final BTPAddress next, final BTPMessage msg) {
        return routeChecker(next, msg, (v) -> v).andThen(
                btpEventChecker(msg, next, BMCIntegrationTest.Event.ROUTE));
    }

    static Consumer<TransactionReceipt> routeChecker(
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

    static Consumer<TransactionReceipt> responseMessageChecker(
            final BTPAddress prev, final BTPMessage msg, final BTPException exception) {
        return responseMessageChecker(prev, msg, exception, (v) -> v);
    }

    static ResponseMessage toResponseMessage(BTPException exception) {
        if (exception == null) {
            return new ResponseMessage(ResponseMessage.CODE_SUCCESS, "");
        }
        long code = ResponseMessage.CODE_UNKNOWN;
        if (BMCException.Code.Unreachable.code == exception.getCodeOfType()) {
            code = ResponseMessage.CODE_NO_ROUTE;
        } else if (BMCException.Code.NotExistsBSH.code == exception.getCodeOfType()) {
            code = ResponseMessage.CODE_NO_BSH;
        } else if (BTPException.Type.BSH.equals(exception.getType())) {
            code = ResponseMessage.CODE_BSH_REVERT;
        }
        return new ResponseMessage(code, String.format("%d:%s",
                exception.getCode(),
                exception.getMessage()));
    }

    static Consumer<TransactionReceipt> responseMessageChecker(
            final BTPAddress prev, final BTPMessage msg, final BTPException exception,
            Function<FeeInfo, FeeInfo> feeInfoSupplier) {
        ResponseMessage errMsg = toResponseMessage(exception);
        BTPMessage response = new BTPMessage();
        response.setSrc(btpAddress.net());
        response.setDst(msg.getSrc());
        response.setSvc(msg.getSvc());
        response.setSn(exception == null ? BigInteger.ZERO : msg.getSn().negate());
        response.setPayload(exception == null ?
                new BMCMessage(
                        BMCIntegrationTest.Internal.Response.name(),
                        errMsg.toBytes()).toBytes() :
                errMsg.toBytes());
        response.setNsn(msg.getNsn().negate());
        response.setFeeInfo(feeInfoSupplier.apply(msg.getFeeInfo()));
        return sendMessageChecker(prev, response).andThen(
                btpEventChecker(msg.getSrc(), msg.getNsn(), prev,
                        exception == null ? BMCIntegrationTest.Event.REPLY : BMCIntegrationTest.Event.ERROR));
    }

    static Consumer<TransactionReceipt> responseChecker(
            final BTPAddress prev, final BTPMessage msg) {
        return responseChecker(prev, msg, (v) ->
                new FeeInfo(v.getNetwork(), emptyFeeValues));
    }

    static Consumer<TransactionReceipt> responseChecker(
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
            Consumer<TransactionReceipt> checker = sendMessageChecker(prev, resp)
                    .andThen(btpEventChecker(msg.getSrc(), msg.getNsn(), prev, BMCIntegrationTest.Event.REPLY));
            //TODO remove try catch
            try {
                checker.accept(
                        MockBSHIntegrationTest.mockBSH.sendMessage(
                                bmcPeriphery.getContractAddress(),
                                resp.getDst(), resp.getSvc(), msg.getSn().negate(), resp.getPayload(),
                                BigInteger.ZERO).send());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }

    static Consumer<TransactionReceipt> dropChecker(
            final BTPAddress prev, final BTPMessage msg, final BTPException e) {
        BigInteger rxSeq = BMCIntegrationTest.getStatus(prev.toString())
                .getRx_seq();
        return BMCIntegrationTest.messageDroppedEvent((el) -> {
            assertEquals(prev.toString(), el._prev);
            assertEquals(rxSeq.add(BigInteger.ONE), el._seq);
            assertEqualsBTPMessage(msg, BTPMessage.fromBytes(el._msg));
//            assertEquals(BigInteger.valueOf(e.getCode()), el._ecode);
//            assertEquals(e.getMessage(), el._emsg);
            ResponseMessage resp = toResponseMessage(e);
            assertEquals(BigInteger.valueOf(resp.getCode()), el._ecode);
            assertEquals(resp.getMsg(), el._emsg);
        }).andThen(btpEventChecker(msg, null, BMCIntegrationTest.Event.DROP));
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
                        null, BMCIntegrationTest.Event.ROUTE),
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
    void handleRelayMessageShouldCallHandleBTPError() throws Exception {
        ResponseMessage responseMsg = toResponseMessage(BMCException.unknown("error"));
        BTPMessage msg = new BTPMessage();
        msg.setSrc(link.net());
        msg.setDst(btpAddress.net());
        msg.setSvc(svc);
        msg.setSn(BigInteger.ONE.negate());
        msg.setPayload(responseMsg.toBytes());
        msg.setNsn(BigInteger.ONE.negate());
        msg.setFeeInfo(new FeeInfo(
                btpAddress.net(), emptyFeeValues));
        Consumer<TransactionReceipt> checker = MockBSHIntegrationTest.handleBTPErrorEvent(
                (el) -> {
                    assertEquals(msg.getSrc(), el._src);
                    assertEquals(msg.getSvc(), el._svc);
                    assertEquals(msg.getSn().negate(), el._sn);
                    assertEquals(responseMsg.getCode(), el._code.longValue());
                    assertEquals(responseMsg.getMsg(), el._msg);
                }
        ).andThen(btpEventChecker(msg, null, BMCIntegrationTest.Event.RECEIVE));
        checker.accept(
                bmcPeriphery.handleRelayMessage(
                        link.toString(),
                        mockRelayMessage(msg).toBytes()).send()
        );
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
                bmcPeriphery.handleRelayMessage(new BTPAddress(link.net(), Faker.btpLink().account()).toString(),
                        mockRelayMessage(btpMessageForSuccess(link)).toBytes()).send());
    }

    @Test
    void handleRelayMessageShouldRevertUnauthorized() {
        AssertBMCException.assertUnauthorized(() ->
                bmcPeripheryWithTester.handleRelayMessage(link.toString(),
                        mockRelayMessage(btpMessageForSuccess(link)).toBytes()).send());
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

    @Disabled("handleFragment not implemented")
    @Test
    void handleFragment() throws Exception {
        //BMC.handleFragment -> BMC.handleRelayMessage -> BSHMock.HandleBTPMessage
        BTPMessage msg = btpMessageForSuccess(link);
        MockRelayMessage relayMessage = mockRelayMessage(msg);
        byte[] bytes = relayMessage.toBytes();
        int count = 3;
        int last = count - 1;
        String[] fragments = fragments(bytes, count);
        for (int i = 0; i < count; i++) {
            if (i == 0) {
//                bmcPeriphery.handleFragment(link.toString(), fragments[i], -1 * last).send();
            } else if (i == last) {
                Consumer<TransactionReceipt> checker = handleBTPMessageChecker(msg);
//                checker.accept(bmcPeriphery.handleFragment(
//                            checker,
//                            link.toString(), fragments[i], 0).send());
            } else {
//                bmcPeriphery.handleFragment(link.toString(), fragments[i], last - i).send();
            }
        }
    }

    @ParameterizedTest
    @MethodSource("dropMessageShouldSuccessArguments")
    void dropMessageShouldSuccess(
            String display,
            BigInteger sn, BigInteger nsn) throws Exception {
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
        BigInteger rxSeq = BMCIntegrationTest.getStatus(link.toString())
                .getRx_seq();
        Consumer<TransactionReceipt> checker = rxSeqChecker(link);
        if (sn.compareTo(BigInteger.ZERO) > 0) {
            System.out.println("dropMessageShouldReplyBTPError");
            checker = checker.andThen(responseMessageChecker(link, msg, BMCException.drop()));
        } else {
            checker = checker.andThen(dropChecker(link, msg, BMCException.drop()));
        }
        checker.accept(bmcManagement.dropMessage(
                msg.getSrc(), rxSeq.add(BigInteger.ONE), msg.getSvc(), sn, nsn,
                msg.getFeeInfo().getNetwork(), Arrays.asList(msg.getFeeInfo().getValues())).send());
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
        BigInteger rxSeq = BMCIntegrationTest.getStatus(link.toString())
                .getRx_seq();
        AssertBTPException.assertBTPException(exception, () ->
                bmcManagement.dropMessage(
                        src, rxSeq.add(diffSeq), svc, sn, nsn,
                        "", new ArrayList<>()).send());
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
