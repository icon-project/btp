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
import foundation.icon.btp.mock.ChainScore;
import foundation.icon.btp.mock.ChainScoreClient;
import foundation.icon.btp.test.AssertBTPException;
import foundation.icon.btp.test.BTPIntegrationTest;
import foundation.icon.btp.test.MockBMVIntegrationTest;
import foundation.icon.btp.test.MockBSHIntegrationTest;
import foundation.icon.jsonrpc.Address;
import foundation.icon.jsonrpc.model.TransactionResult;
import foundation.icon.score.test.ScoreIntegrationTest;
import foundation.icon.score.util.ArrayUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RewardTest implements BMCIntegrationTest {
    static BTPAddress link = BTPIntegrationTest.Faker.btpLink();
    static String svc = MockBSHIntegrationTest.SERVICE;
    static Address relay = Address.of(bmc._wallet());
    static Fee linkFee = FeeManagementTest.fakeFee(link.net());

    @BeforeAll
    static void beforeAll() {
        System.out.println("RewardTest:beforeAll start");
        bmc.setDumpJson(true);
        BMVManagementTest.addVerifier(link.net(), MockBMVIntegrationTest.mockBMV._address());
        LinkManagementTest.addLink(link.toString());
        BMRManagementTest.addRelay(link.toString(), relay);

        BSHManagementTest.clearService(svc);
        BSHManagementTest.addService(svc, MockBSHIntegrationTest.mockBSH._address());

        FeeManagementTest.setFeeTable(linkFee);

        //To transfer, BMC should have enough balance more than contractCallStep * StepPrice
        ChainScoreClient chainScore = new ChainScoreClient(
                client.endpoint(),
                client._nid(),
                client._wallet(),
                new Address(ChainScore.ADDRESS.toString()));
        BigInteger stepPrice = chainScore.getStepPrice();
        BigInteger minBalance = chainScore.getStepCost("contractCall").multiply(stepPrice);
        if (client._balance(bmc._address()).compareTo(minBalance) < 0) {
            client._transfer(bmc._address(), minBalance, null);
            System.out.println("transferred "+bmc._address() + ":" + client._balance(bmc._address()));
        }
        System.out.println("RewardTest:beforeAll end");
    }

    @AfterAll
    static void afterAll() {
        System.out.println("RewardTest:afterAll start");
        BSHManagementTest.clearService(svc);

        BMRManagementTest.clearRelay(link.toString(), relay);
        LinkManagementTest.clearLink(link.toString());
        BMVManagementTest.clearVerifier(link.net());
        System.out.println("RewardTest:afterAll end");
    }

    static BTPMessage btpMessageForReward(String net, BigInteger[] values) {
        BTPMessage msg = new BTPMessage();
        msg.setSrc(link.net());
        msg.setDst(btpAddress.net());
        msg.setSvc(svc);
        msg.setSn(BigInteger.ZERO);
        msg.setPayload(Faker.btpLink().toBytes());
        msg.setNsn(BigInteger.ONE);
        msg.setFeeInfo(new FeeInfo(net, values));
        return msg;
    }

    static Address rewardAddress(boolean isRemain) {
        return isRemain ? bmc._address() : relay;
    }

    static BigInteger ensureReward(String net, boolean isRemain) {
        System.out.println(isRemain ?
                "handleRelayMessageShouldAccumulateRemainFee" :
                "handleRelayMessageShouldAccumulateRewardIfClaimRewardMessage");
        Address address = rewardAddress(isRemain);
        BigInteger preReward = bmc.getReward(net, address);
        if (preReward.compareTo(BigInteger.ZERO) < 1) {
            BigInteger amount = BigInteger.ONE;
            BTPMessage msg = btpMessageForReward(net,
                    isRemain ? new BigInteger[]{BigInteger.ZERO, amount} : new BigInteger[]{amount});
            bmc.handleRelayMessage(link.toString(),
                    MessageTest.mockRelayMessage(msg).toBase64String());
            BigInteger reward = bmc.getReward(net, address);
            assertEquals(preReward.add(amount), reward);
            return reward;
        }
        return preReward;
    }

    static Consumer<TransactionResult> rewardConsumedChecker(String net, boolean isRemain) {
        return (txr) -> {
            assertEquals(BigInteger.ZERO, bmc.getReward(net, rewardAddress(isRemain)));
        };
    }

    static Consumer<TransactionResult> claimRewardMessageChecker(BigInteger amount, String receiver) {
        return (txr) -> {
            List<BMCMessage> bmcMessages = BMCIntegrationTest.bmcMessages(
                    txr, (next) -> next.equals(link.toString()));
            List<ClaimRewardMessage> rewardMessages = BMCIntegrationTest.internalMessages(
                    bmcMessages, BTPMessageCenter.Internal.ClaimReward, ClaimRewardMessage::fromBytes);
            assertEquals(rewardMessages.size(), 1);
            ClaimRewardMessage rewardMessage = rewardMessages.get(0);
            assertEquals(amount, rewardMessage.getAmount());
            assertEquals(receiver, rewardMessage.getReceiver());
        };
    }

    static Consumer<TransactionResult> claimRewardEventChecker(
            String net, String receiver, BigInteger amount, AtomicReference<BigInteger> snContainer) {
        return BMCIntegrationTest.claimRewardEvent(
                (el) -> {
                    assertEquals(net, el.getNetwork());
                    assertEquals(receiver, el.getReceiver());
                    assertEquals(amount, el.getAmount());
                    snContainer.set(el.getSn());
                });
    }

    @Test
    void claimRewardShouldSendClaimRewardMessageAndHandleRelayMessageShouldRefundRewardIfError() {
        System.out.println("claimRewardShouldSendClaimRewardMessage");
        boolean isRemain = false;
        String feeNetwork = link.net();
        BigInteger reward = ensureReward(feeNetwork, isRemain);
        Address address = rewardAddress(isRemain);
        String receiver = address.toString();
        AtomicReference<BigInteger> sn = new AtomicReference<>();
        Consumer<TransactionResult> checker = claimRewardMessageChecker(reward, receiver)
                .andThen(rewardConsumedChecker(feeNetwork, isRemain))
                .andThen(claimRewardEventChecker(feeNetwork, receiver, reward, sn));
        bmc.claimReward(checker,
                bmc.getFee(feeNetwork, true),
                feeNetwork, receiver);

        System.out.println("handleRelayMessageShouldRefundRewardIfError");
        ErrorMessage errorMsg = MessageTest.toErrorMessage(BMCException.unknown("error"));
        BTPMessage msg = new BTPMessage();
        msg.setSrc(link.net());
        msg.setDst(btpAddress.net());
        msg.setSvc(BTPMessageCenter.INTERNAL_SERVICE);
        msg.setSn(sn.get().negate());
        msg.setPayload(errorMsg.toBytes());
        msg.setNsn(BigInteger.ONE.negate());
        msg.setFeeInfo(new FeeInfo(
                btpAddress.net(), FeeManagementTest.backward(linkFee.getValues())));
        bmc.handleRelayMessage(link.toString(), MessageTest.mockRelayMessage(msg).toBase64String());
        assertEquals(reward, bmc.getReward(feeNetwork, address));
    }

    static BTPMessage btpMessageForClaimReward(BigInteger amount, String receiver) {
        ClaimRewardMessage claimRewardMessage = new ClaimRewardMessage(amount, receiver);
        BMCMessage bmcMessage = new BMCMessage(BTPMessageCenter.Internal.ClaimReward.name(), claimRewardMessage.toBytes());
        BTPMessage msg = new BTPMessage();
        msg.setSrc(link.net());
        msg.setDst(btpAddress.net());
        msg.setSvc(BTPMessageCenter.INTERNAL_SERVICE);
        msg.setSn(BigInteger.ONE);
        msg.setPayload(bmcMessage.toBytes());
        msg.setNsn(BigInteger.ONE);
        msg.setFeeInfo(new FeeInfo(
                link.net(), MessageWithFeeTest.reverse(linkFee.getValues())));
        return msg;
    }

    static Consumer<TransactionResult> responseMessageChecker(BigInteger sn, long code) {
        return (txr) -> {
            List<BMCMessage> bmcMessages = BMCIntegrationTest.bmcMessages(
                    txr, (next) -> next.equals(link.toString()));
            List<ResponseMessage> responseMessages = BMCIntegrationTest.internalMessages(
                    bmcMessages, BTPMessageCenter.Internal.Response, ResponseMessage::fromBytes);
            assertEquals(responseMessages.size(), 1);
            ResponseMessage resp = responseMessages.get(0);
            assertEquals(sn, resp.getRequestSn());
            assertEquals(code, resp.getCode());
        };
    }

    @Test
    void handleRelayMessageShouldAccumulateRewardToReceiverAndSendResponse() {
        BigInteger amount = BigInteger.ONE;
        BTPMessage msg = btpMessageForClaimReward(amount, relay.toString());
        BigInteger preReward = bmc.getReward(btpAddress.net(), relay);
        Consumer<TransactionResult> checker = (txr) -> {
            assertEquals(preReward.add(amount), bmc.getReward(btpAddress.net(), relay));
        };
        checker = checker.andThen(responseMessageChecker(msg.getSn(), ResponseMessage.CODE_SUCCESS));
        bmc.handleRelayMessage(
                checker,
                link.toString(), MessageTest.mockRelayMessage(msg).toBase64String());
    }

    @Test
    void handleRelayMessageShouldSendErrorIfInvalidReceiver() {
        BigInteger amount = BigInteger.ONE;
        BTPMessage msg = btpMessageForClaimReward(amount, "invalid");
        Consumer<TransactionResult> checker = MessageWithFeeTest.replyBTPErrorChecker(
                link, msg, BMCException.unknown("invalid address format"));
        bmc.handleRelayMessage(
                checker,
                link.toString(), MessageTest.mockRelayMessage(msg).toBase64String());
    }

    @Test
    void claimRewardShouldTransfer() {
        boolean isRemain = false;
        Address receiver = Address.of(tester);
        BigInteger reward = ensureReward(btpAddress.net(), isRemain);
        Consumer<TransactionResult> checker = rewardConsumedChecker(btpAddress.net(), isRemain)
                .andThen(ScoreIntegrationTest.balanceChecker(receiver, reward));
        bmc.claimReward(checker, btpAddress.net(), receiver.toString());
    }

    @SuppressWarnings("ThrowableNotThrown")
    @ParameterizedTest
    @MethodSource("claimRewardShouldRevertArguments")
    void claimRewardShouldRevert(
            String display,
            BTPException exception,
            String network, String receiver, BigInteger fee) {
        System.out.println(display);
        if (network.equals(link.net()) || network.equals(btpAddress.net())){
            ensureReward(network, false);
        }
        AssertBTPException.assertBTPException(exception, () ->
                bmc.claimReward(fee, network, receiver));
    }

    static Stream<Arguments> claimRewardShouldRevertArguments() {
        return Stream.of(
                Arguments.of(
                        "claimRewardShouldRevertNotExistsReward",
                        BMCException.unknown("not exists claimable reward"),
                        Faker.btpNetwork(),
                        relay.toString(),
                        ArrayUtil.sum(MessageWithFeeTest.reverse(linkFee.getValues()))),
                Arguments.of(
                        "claimRewardShouldRevertInvalidReceiver",
                        BMCException.unknown("invalid address format"),
                        btpAddress.net(),
                        "invalid",
                        BigInteger.ZERO),
                Arguments.of(
                        "claimRewardShouldRevertNotEnoughFee",
                        BMCException.unknown("not enough fee"),
                        link.net(),
                        relay.toString(),
                        BigInteger.ZERO)
                );
    }

    @Test
    void claimRewardShouldSendRemainFeeIfCallerIsFeeHandler() {
        System.out.println("setFeeHandlerShouldSuccess");
        Address feeHandler = Address.of(tester);
        bmc.setFeeHandler(feeHandler);
        assertEquals(feeHandler, bmc.getFeeHandler());

        System.out.println("claimRewardShouldSendRemainFee");
        boolean isRemain = true;
        String feeNetwork = link.net();
        BigInteger reward = ensureReward(feeNetwork, isRemain);
        Address address = rewardAddress(isRemain);
        String receiver = relay.toString();
        AtomicReference<BigInteger> sn = new AtomicReference<>();
        Consumer<TransactionResult> checker = claimRewardMessageChecker(reward, receiver)
                .andThen(rewardConsumedChecker(feeNetwork, isRemain))
                .andThen(claimRewardEventChecker(feeNetwork, receiver, reward, sn));
        bmcWithTester.claimReward(checker,
                bmc.getFee(feeNetwork, true),
                feeNetwork, receiver);

        System.out.println("handleRelayMessageShouldRefundRewardIfError");
        ErrorMessage errorMsg = MessageTest.toErrorMessage(BMCException.unknown("error"));
        BTPMessage msg = new BTPMessage();
        msg.setSrc(link.net());
        msg.setDst(btpAddress.net());
        msg.setSvc(BTPMessageCenter.INTERNAL_SERVICE);
        msg.setSn(sn.get().negate());
        msg.setPayload(errorMsg.toBytes());
        msg.setNsn(BigInteger.ONE.negate());
        msg.setFeeInfo(new FeeInfo(
                btpAddress.net(), FeeManagementTest.backward(linkFee.getValues())));
        bmc.handleRelayMessage(link.toString(), MessageTest.mockRelayMessage(msg).toBase64String());
        assertEquals(reward, bmc.getReward(feeNetwork, address));
    }

}
