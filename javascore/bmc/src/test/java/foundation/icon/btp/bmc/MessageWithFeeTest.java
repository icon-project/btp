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

package foundation.icon.btp.bmc;

import foundation.icon.btp.lib.BTPAddress;
import foundation.icon.btp.lib.BTPException;
import foundation.icon.btp.test.AssertBTPException;
import foundation.icon.btp.test.BTPIntegrationTest;
import foundation.icon.btp.test.MockBMVIntegrationTest;
import foundation.icon.btp.test.MockBSHIntegrationTest;
import foundation.icon.jsonrpc.Address;
import foundation.icon.jsonrpc.model.TransactionResult;
import foundation.icon.score.util.ArrayUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class MessageWithFeeTest implements BMCIntegrationTest {
    static BTPAddress link = BTPIntegrationTest.Faker.btpLink();
    static BTPAddress reachable = BTPIntegrationTest.Faker.btpLink();
    static String svc = MockBSHIntegrationTest.SERVICE;
    static Address relay = Address.of(bmc._wallet());

    static Fee linkFee = FeeManagementTest.fakeFee(link.net());
    static Fee reachableFee = FeeManagementTest.fakeFee(reachable.net(), 1, linkFee);
    //for intermediate path test
    static BTPAddress secondLink = BTPIntegrationTest.Faker.btpLink();
    static Fee secondLinkFee = FeeManagementTest.fakeFee(secondLink.net());
    static Fee secondLinkToLinkInIntermediate = FeeManagementTest.fakeFee(
            secondLink.net(),
            reverse(secondLinkFee.getValues()), linkFee.getValues());

    static BigInteger[] reverse(BigInteger[] values) {
        List<BigInteger> list = new ArrayList<>(Arrays.asList(values));
        Collections.reverse(list);
        return list.toArray(BigInteger[]::new);
    }

    static FeeInfo consume(FeeInfo feeInfo) {
        if (feeInfo == null) {
            return null;
        }
        return new FeeInfo(feeInfo.getNetwork(),
                Arrays.copyOfRange(
                        feeInfo.getValues(), 1, feeInfo.getValues().length));
    }

    static FeeInfo tailSwap(FeeInfo feeInfo, int numOfTail) {
        if (feeInfo == null) {
            return null;
        }
        BigInteger[] values = feeInfo.getValues();
        int numOfHead = values.length - numOfTail;
        BigInteger[] nextFeeList = new BigInteger[values.length];
        System.arraycopy(values, numOfHead, nextFeeList, 0, numOfTail);
        System.arraycopy(values, 0, nextFeeList, numOfTail, numOfHead);
        feeInfo.setValues(nextFeeList);
        return new FeeInfo(feeInfo.getNetwork(), nextFeeList);
    }

    @BeforeAll
    static void beforeAll() {
        System.out.println("MessageWithFeeTest:beforeAll start");
        bmc.setDumpJson(true);
        BMVManagementTest.addVerifier(link.net(), MockBMVIntegrationTest.mockBMV._address());
        LinkManagementTest.addLink(link.toString());
        BMRManagementTest.addRelay(link.toString(), relay);
        MessageTest.ensureReachable(link, new BTPAddress[]{reachable});

        BMVManagementTest.addVerifier(secondLink.net(), MockBMVIntegrationTest.mockBMV._address());
        LinkManagementTest.addLink(secondLink.toString());
        BMRManagementTest.addRelay(secondLink.toString(), relay);

        BSHManagementTest.clearService(svc);
        BSHManagementTest.addService(svc, MockBSHIntegrationTest.mockBSH._address());

        FeeManagementTest.setFeeTable(linkFee, reachableFee, secondLinkFee);
        System.out.println("MessageWithFeeTest:beforeAll end");
    }

    @AfterAll
    static void afterAll() {
        System.out.println("MessageWithFeeTest:afterAll start");
        BSHManagementTest.clearService(svc);

        BMRManagementTest.clearRelay(link.toString(), relay);
        LinkManagementTest.clearLink(link.toString());
        BMVManagementTest.clearVerifier(link.net());

        BMRManagementTest.clearRelay(secondLink.toString(), relay);
        LinkManagementTest.clearLink(secondLink.toString());
        BMVManagementTest.clearVerifier(secondLink.net());
        System.out.println("MessageWithFeeTest:afterAll end");
    }

    @ParameterizedTest
    @MethodSource("sendMessageShouldSuccessArguments")
    void sendMessageShouldSuccess(
            String display,
            BTPAddress dst, BTPAddress next, BigInteger sn, BigInteger[] fee) {
        System.out.println(display);
        byte[] payload = BTPIntegrationTest.Faker.btpLink().toBytes();
        FeeInfo feeInfo = new FeeInfo(btpAddress.net(), fee);

        BigInteger txSeq = BMCIntegrationTest.getStatus(bmc, next.toString())
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
            assertEquals(bmc.getNetworkSn(), btpMessage.getNsn());
            MessageTest.assertEqualsFeeInfo(feeInfo, btpMessage.getFeeInfo());
        });
        System.out.println("pay:" + ArrayUtil.sum(feeInfo.getValues()));
        MockBSHIntegrationTest.mockBSH.sendMessage(
                checker,
                ArrayUtil.sum(feeInfo.getValues()),
                bmc._address(),
                dst.net(), svc, sn, payload);
    }

    static Stream<Arguments> sendMessageShouldSuccessArguments() {
        return Stream.of(
                Arguments.of(
                        "unidirectionalSendToLink",
                        link, link,
                        BigInteger.ZERO,
                        FeeManagementTest.forward(linkFee.getValues())),
                Arguments.of(
                        "bidirectionalSendToLink",
                        link, link,
                        BigInteger.ONE,
                        linkFee.getValues()),
                Arguments.of(
                        "unidirectionalSendToReachable",
                        reachable, link,
                        BigInteger.ZERO,
                        FeeManagementTest.forward(reachableFee.getValues())),
                Arguments.of(
                        "bidirectionalSendToReachable",
                        reachable, link,
                        BigInteger.ONE,
                        reachableFee.getValues())
        );
    }

    @Test
    void sendMessageShouldAccumulateRemainFee() {
        BigInteger prevReward = bmc.getReward(btpAddress.net(), bmc._address());
        BigInteger remainAmount = BigInteger.ONE;
        MockBSHIntegrationTest.mockBSH.sendMessage(
                bmc.getFee(link.net(), false).add(remainAmount),
                bmc._address(),
                link.net(), svc, BigInteger.ZERO, Faker.btpLink().toBytes());
        BigInteger reward = bmc.getReward(btpAddress.net(), bmc._address());
        assertEquals(prevReward.add(remainAmount), reward);
    }

    @SuppressWarnings("ThrowableNotThrown")
    @ParameterizedTest
    @MethodSource("sendMessageShouldRevertArguments")
    void sendMessageShouldRevert(
            String display,
            BTPException exception,
            String dstNet, String svc, BigInteger sn, BigInteger fee) {
        System.out.println(display);
        AssertBTPException.assertBTPException(exception, () ->
                MockBSHIntegrationTest.mockBSH.sendMessage(
                        fee,
                        bmc._address(),
                        dstNet, svc, sn, BTPIntegrationTest.Faker.btpLink().toBytes()));
    }

    static Stream<Arguments> sendMessageShouldRevertArguments() {
        return Stream.of(
                Arguments.of(
                        "sendMessageShouldRevertNotExistsBSH",
                        BMCException.notExistsBSH(),
                        link.net(),
                        BTPIntegrationTest.Faker.btpService(),
                        BigInteger.ZERO,
                        ArrayUtil.sum(FeeManagementTest.forward(linkFee.getValues()))),
                Arguments.of(
                        "sendMessageShouldRevertUnreachable",
                        BMCException.unreachable(),
                        BTPIntegrationTest.Faker.btpNetwork(),
                        svc,
                        BigInteger.ZERO,
                        ArrayUtil.sum(FeeManagementTest.forward(linkFee.getValues()))),
                Arguments.of(
                        "unidirectionalSendMessageShouldRevertNotEnoughFee",
                        BMCException.unknown("not enough fee"),
                        link.net(),
                        svc,
                        BigInteger.ZERO,
                        BigInteger.ZERO),
                Arguments.of(
                        "bidirectionalSendMessageShouldRevertNotEnoughFee",
                        BMCException.unknown("not enough fee"),
                        link.net(),
                        svc,
                        BigInteger.ONE,
                        BigInteger.ZERO),
                Arguments.of(
                        "replySendMessageShouldRevert",
                        BMCException.unknown("not exists response"),
                        link.net(),
                        svc,
                        BigInteger.valueOf(Long.MAX_VALUE).negate(),
                        BigInteger.ZERO)
        );
    }

    @Test
    void sendMessageShouldRevertUnauthorized() {
        AssertBMCException.assertUnauthorized(() -> bmc.sendMessage(
                ArrayUtil.sum(FeeManagementTest.forward(linkFee.getValues())),
                link.net(), svc, BigInteger.ZERO, BTPIntegrationTest.Faker.btpLink().toBytes()));
    }


    @ParameterizedTest
    @MethodSource("handleRelayMessageShouldSuccessArguments")
    void handleRelayMessageShouldSuccess(
            String display,
            BTPAddress src, BTPAddress dst, BTPAddress prev, BTPAddress next,
            String svc, BigInteger sn, BTPException expectBTPError, BigInteger[] fee) {
        System.out.println(display);
        if (sn.compareTo(BigInteger.ZERO) < 0) {
            throw new IllegalArgumentException("sn should be positive or zero");
        }

        BTPMessage msg = new BTPMessage();
        msg.setSrc(src.net());
        msg.setDst(dst.net());
        msg.setSvc(svc);
        msg.setSn(sn);
        msg.setPayload(Faker.btpLink().toBytes());
        msg.setNsn(BigInteger.ONE);
        msg.setFeeInfo(new FeeInfo(src.net(), fee));

        System.out.println("handleRelayMessageShouldIncreaseRxSeqAndAccumulateReward");
        Consumer<TransactionResult> checker = MessageTest.rxSeqChecker(prev)
                .andThen(rewardChecker(msg));
        if (expectBTPError != null) {
            System.out.println("handleRelayMessageShouldReplyBTPError");
            checker = checker.andThen(responseMessageChecker(prev, msg, expectBTPError));
        } else {
            if (!dst.equals(btpAddress)) {
                if (next != null) {
                    System.out.println("handleRelayMessageShouldSendToNext");
                    checker = checker.andThen(routeChecker(next, msg));
                } else {
                    System.out.println("handleRelayMessageShouldDrop");
//                    checker = checker.andThen(dropChecker(prev, msg));
                }
            } else {
                if (svc.equals(MessageWithFeeTest.svc)) {
                    System.out.println("handleRelayMessageShouldCallHandleBTPMessage");
                    checker = checker.andThen(MessageTest.handleBTPMessageChecker(msg));
                    if (sn.compareTo(BigInteger.ZERO) > 0) {
                        //handleRelayMessageShouldStoreFeeInfo
                        System.out.println("handleRelayMessageShouldStoreFeeInfo");
                        checker = checker.andThen(responseChecker(prev, msg));
                    }
                } else {
                    System.out.println("handleRelayMessageShouldDrop");
//                    checker = checker.andThen(dropChecker(prev, msg));
                }
            }
        }
        bmc.handleRelayMessage(
                checker,
                prev.toString(),
                MessageTest.mockRelayMessage(msg).toBase64String());
    }

    static Consumer<TransactionResult> rewardChecker(
            final BTPMessage msg) {
        BigInteger prevReward = bmc.getReward(msg.getFeeInfo().getNetwork(), relay);
        return (txr) -> {
            assertEquals(prevReward.add(msg.getFeeInfo().getValues()[0]),
                    bmc.getReward(msg.getFeeInfo().getNetwork(), relay));
        };
    }

    static Consumer<TransactionResult> routeChecker(
            final BTPAddress next, final BTPMessage msg) {
        return MessageTest.routeChecker(next, msg, MessageWithFeeTest::consume);
    }

    static Consumer<TransactionResult> responseMessageChecker(
            final BTPAddress prev, final BTPMessage msg, final BTPException exception) {
        return MessageTest.responseMessageChecker(prev, msg, exception,
                (v) -> tailSwap(consume(v), 1));
    }

    static Consumer<TransactionResult> responseChecker(
            final BTPAddress prev, final BTPMessage msg) {
        return MessageTest.responseChecker(prev, msg, (v) ->
                new FeeInfo(v.getNetwork(), FeeManagementTest.backward(v.getValues())));
    }

    static Consumer<TransactionResult> dropChecker(
            final BTPAddress prev, final BTPMessage msg, final BTPException e) {
        BTPMessage consumedMsg = new BTPMessage();
        consumedMsg.setSrc(msg.getSrc());
        consumedMsg.setDst(msg.getDst());
        consumedMsg.setSvc(msg.getSvc());
        consumedMsg.setSn(msg.getSn());
        consumedMsg.setNsn(msg.getNsn());
        consumedMsg.setPayload(msg.getPayload());
        consumedMsg.setFeeInfo(consume(msg.getFeeInfo()));
        return MessageTest.dropChecker(prev, consumedMsg, e);
    }

    static Stream<Arguments> handleRelayMessageShouldSuccessArguments() {
        return Stream.of(
                Arguments.of(
                        "unidirectionalHandleRelayMessage",
                        link, btpAddress, link, null,
                        svc, BigInteger.ZERO,
                        null,
                        FeeManagementTest.backward(linkFee.getValues())),
                Arguments.of(
                        "bidirectionalHandleRelayMessage",
                        link, btpAddress, link, null,
                        svc, BigInteger.ONE,
                        null,
                        linkFee.getValues()),
                Arguments.of(
                        "unidirectionalHandleRelayMessageShouldNotReplyBTPError",
                        link, btpAddress, link, null,
                        BTPIntegrationTest.Faker.btpService(), BigInteger.ZERO,
                        null,
                        FeeManagementTest.backward(linkFee.getValues())),
                Arguments.of(
                        "bidirectionalHandleRelayMessageShouldReplyBTPError",
                        link, btpAddress, link, null,
                        BTPIntegrationTest.Faker.btpService(), BigInteger.ONE,
                        BMCException.notExistsBSH(),
                        linkFee.getValues()),
                Arguments.of(
                        "handleRelayMessageInIntermediate",
                        secondLink, link, secondLink, link,
                        svc, BigInteger.ZERO,
                        null,
                        FeeManagementTest.forward(secondLinkToLinkInIntermediate.getValues())),
                Arguments.of(
                        "handleRelayMessageShouldNotReplyBTPErrorInIntermediate",
                        secondLink, BTPIntegrationTest.Faker.btpLink(), secondLink, null,
                        svc, BigInteger.ZERO,
                        null,
                        FeeManagementTest.forward(secondLinkToLinkInIntermediate.getValues())),
                Arguments.of(
                        "handleRelayMessageShouldReplyBTPErrorInIntermediate",
                        secondLink, BTPIntegrationTest.Faker.btpLink(), secondLink, null,
                        svc, BigInteger.ONE,
                        BMCException.unreachable(),
                        secondLinkToLinkInIntermediate.getValues())
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
                btpAddress.net(), FeeManagementTest.backward(linkFee.getValues())));
        Consumer<TransactionResult> checker = MockBSHIntegrationTest.handleBTPErrorEvent(
                (el) -> {
                    assertEquals(msg.getSrc(), el.getSrc());
                    assertEquals(msg.getSvc(), el.getSvc());
                    assertEquals(msg.getSn().negate(), el.getSn());
                    assertEquals(responseMsg.getCode(), el.getCode());
                    assertEquals(responseMsg.getMsg(), el.getMsg());
                }
        );
        bmc.handleRelayMessage(
                checker,
                link.toString(),
                MessageTest.mockRelayMessage(msg).toBase64String());
    }

    @Disabled("duplicated test, refer MessageTest")
    @Test
    void handleRelayMessageShouldRevertNotExistsLink() {
    }

    @Disabled("duplicated test, refer MessageTest")
    @Test
    void handleRelayMessageShouldRevertUnauthorized() {
    }

    @Disabled("duplicated test, refer MessageTest")
    @Test
    void handleFragment() {
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
                link.net(),
                sn.compareTo(BigInteger.ZERO) > 0 ?
                        reverse(linkFee.getValues()) :
                        FeeManagementTest.backward(linkFee.getValues())));

        System.out.println("dropMessageShouldIncreaseRxSeqAndDrop");
        BigInteger rxSeq = BMCIntegrationTest.getStatus(bmc, link.toString())
                .getRx_seq();
        Consumer<TransactionResult> checker = MessageTest.rxSeqChecker(link);
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

    @Disabled("duplicated test, refer MessageTest")
    @Test
    void dropMessageShouldRevert() {
    }

}
