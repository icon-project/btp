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
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RewardTest implements BMCIntegrationTest {
    static BTPAddress link = BTPIntegrationTest.Faker.btpLink();
    static String svc = MockBSHIntegrationTest.SERVICE;
    static Address relay = bmc._wallet().getAddress();
    static FeeInfo linkFee = FeeManagementTest.fakeFee(link.net());

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
                new Address(ChainScore.ADDRESS));
        BigInteger stepPrice = chainScore.getStepPrice();
        BigInteger minBalance = chainScore.getStepCost("contractCall").multiply(stepPrice);
        if (client._balance(bmc._address()).compareTo(minBalance) < 0) {
            client._transfer(bmc._address(), minBalance, null);
            System.out.println("transferred " + bmc._address() + ":" + client._balance(bmc._address()));
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
        System.out.println("ensureReward " + (isRemain ?
                "handleRelayMessageShouldAccumulateRemainFee" :
                "handleRelayMessageShouldAccumulateRewardIfClaimRewardMessage"));
        Address address = rewardAddress(isRemain);
        BigInteger reward = bmc.getReward(net, address);
        if (reward.compareTo(BigInteger.ZERO) <= 0) {
            BigInteger amount = BigInteger.ONE;
            BTPMessage msg = btpMessageForReward(net,
                    isRemain ? new BigInteger[]{BigInteger.ZERO, amount} : new BigInteger[]{amount});
            bmc.handleRelayMessage(link.toString(),
                    MessageTest.mockRelayMessage(msg).toBase64String());
            reward = bmc.getReward(net, address);
            assertEquals(amount, reward);
        }
        if (net.equals(btpAddress.net()) &&
                client._balance(bmc._address()).compareTo(reward) < 0) {
            client._transfer(bmc._address(), reward, null);
            System.out.println("transferred " + bmc._address() + ":" + client._balance(bmc._address()));
        }
        return reward;
    }

    static Consumer<TransactionResult> rewardChecker(String net, Address address, BigInteger amount) {
        BigInteger preReward = bmc.getReward(net, address);
        return (txr) -> {
            assertEquals(preReward.add(amount), bmc.getReward(net, address));
        };
    }

    static Consumer<TransactionResult> claimRewardMessageChecker(BigInteger amount, String receiver) {
        return (txr) -> {
            List<BMCMessage> bmcMessages = BMCIntegrationTest.bmcMessages(txr, null);
            List<ClaimMessage> rewardMessages = BMCIntegrationTest.internalMessages(
                    bmcMessages, BTPMessageCenter.Internal.Claim, ClaimMessage::fromBytes);
            assertEquals(rewardMessages.size(), 1);
            ClaimMessage rewardMessage = rewardMessages.get(0);
            assertEquals(amount, rewardMessage.getAmount());
            assertEquals(receiver, rewardMessage.getReceiver());
        };
    }

    static Consumer<TransactionResult> claimRewardEventChecker(
            Address sender, String net, String receiver, BigInteger amount, BigInteger nsn) {
        return BMCIntegrationTest.claimRewardEvent(
                (el) -> {
                    assertEquals(sender, el.get_sender());
                    assertEquals(net, el.get_network());
                    assertEquals(receiver, el.get_receiver());
                    assertEquals(amount, el.get_amount());
                    assertEquals(nsn, el.get_nsn());
                });
    }

    static Consumer<TransactionResult> claimRewardResultEventChecker(
            BigInteger nsn, String net, BigInteger result) {
        return BMCIntegrationTest.claimRewardResultEvent(
                (el) -> {
                    assertEquals(nsn, el.get_nsn());
                    assertEquals(net, el.get_network());
                    assertEquals(result, el.get_result());
                });
    }

    static BTPMessage btpMessageForResponse(String feeNetwork, ResponseMessage responseMessage) {
        BTPMessage msg = new BTPMessage();
        msg.setSrc(feeNetwork);
        msg.setDst(btpAddress.net());
        msg.setSvc(BTPMessageCenter.INTERNAL_SERVICE);
        BigInteger nsn = bmc.getNetworkSn();
        if (responseMessage.getCode() == ResponseMessage.CODE_SUCCESS) {
            msg.setSn(BigInteger.ZERO);
            BMCMessage bmcMsg = new BMCMessage(BTPMessageCenter.Internal.Response.name(),
                    responseMessage.toBytes());
            msg.setPayload(bmcMsg.toBytes());
        } else {
            msg.setSn(nsn.negate());
            msg.setPayload(responseMessage.toBytes());
        }
        msg.setNsn(nsn.negate());
        msg.setFeeInfo(new FeeInfo(
                btpAddress.net(),
                FeeManagementTest.backward(
                        bmc.getFeeTable(new String[]{feeNetwork})[0])));
        return msg;
    }

    @ParameterizedTest
    @MethodSource("claimRewardShouldSuccessArguments")
    void claimRewardShouldSuccess(
            String display,
            boolean isRemain,
            String feeNetwork,
            BTPException error) {
        System.out.println(display);
        if (isRemain) {
            Address feeHandler = tester.getAddress();
            bmc.setFeeHandler(feeHandler);
            assertEquals(feeHandler, bmc.getFeeHandler());
        }

        Address sender = rewardAddress(isRemain);
        Address receiver;
        BigInteger reward = ensureReward(feeNetwork, isRemain);
        BigInteger pay;
        Consumer<TransactionResult> checker = rewardChecker(feeNetwork, sender, reward.negate());
        if (feeNetwork.equals(btpAddress.net())) {
            System.out.println("claimRewardShouldTransfer");
            receiver = tester.getAddress();
            pay = BigInteger.ZERO;
            checker = checker.andThen(claimRewardEventChecker(
                            sender, feeNetwork, receiver.toString(), reward, BigInteger.ZERO))
                    .andThen(ScoreIntegrationTest.balanceChecker(receiver, reward, isRemain))
                    .andThen(ScoreIntegrationTest.icxTransferEvent(
                            bmc._address(),
                            (el) -> {
                                assertEquals(bmc._address(), el.getFrom());
                                assertEquals(receiver, el.getTo());
                                assertEquals(reward, el.getAmount());
                            }));
        } else {
            System.out.println("claimRewardShouldSendClaimRewardMessage");
            receiver = relay;
            pay = bmc.getFee(feeNetwork, true);
            BigInteger nsn = bmc.getNetworkSn();
            checker = checker.andThen(claimRewardEventChecker(
                            sender, feeNetwork, receiver.toString(), reward, nsn.add(BigInteger.ONE)))
                    .andThen(claimRewardMessageChecker(reward, receiver.toString()));
        }
        (isRemain ? bmcWithTester : bmc).claimReward(
                checker,
                pay,
                feeNetwork, receiver.toString());

        if (!feeNetwork.equals(btpAddress.net())) {
            ResponseMessage responseMessage = BTPMessageCenter.toResponseMessage(error);
            BTPMessage msg = btpMessageForResponse(feeNetwork, responseMessage);
            BigInteger afterReward;
            if (error != null) {
                System.out.println("handleRelayMessageShouldRollbackIfError");
                afterReward = reward;
            } else {
                System.out.println("handleRelayMessageShouldClearClaimRequest");
                afterReward = bmc.getReward(feeNetwork, sender);
            }
            checker = claimRewardResultEventChecker(
                    msg.getNsn().negate(),
                    feeNetwork,
                    BigInteger.valueOf(responseMessage.getCode()));
            bmc.handleRelayMessage(
                    checker,
                    link.toString(), MessageTest.mockRelayMessage(msg).toBase64String());
            assertEquals(afterReward, bmc.getReward(feeNetwork, sender));
        }
    }

    static Stream<Arguments> claimRewardShouldSuccessArguments() {
        return Stream.of(
                Arguments.of(
                        "claimRewardShouldTransfer",
                        false,
                        btpAddress.net(),
                        null),
                Arguments.of(
                        "claimRewardShouldTransferWithFeeHandler",
                        true,
                        btpAddress.net(),
                        null),
                Arguments.of(
                        "claimRewardShouldSuccess",
                        false,
                        link.net(),
                        null),
                Arguments.of(
                        "claimRewardShouldRollback",
                        false,
                        link.net(),
                        BMCException.unknown("error")),
                Arguments.of(
                        "claimRewardShouldRollbackWithFeeHandler",
                        true,
                        link.net(),
                        null)
        );
    }

    static BTPMessage btpMessageForClaimReward(BigInteger amount, String receiver) {
        ClaimMessage claimMessage = new ClaimMessage(amount, receiver);
        BMCMessage bmcMessage = new BMCMessage(BTPMessageCenter.Internal.Claim.name(), claimMessage.toBytes());
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

    @Test
    void handleRelayMessageShouldAccumulateRewardToReceiverAndSendResponse() {
        BigInteger amount = BigInteger.ONE;
        BTPMessage msg = btpMessageForClaimReward(amount, relay.toString());
        Consumer<TransactionResult> checker = rewardChecker(btpAddress.net(), relay, amount)
                .andThen(MessageWithFeeTest.responseMessageChecker(link, msg, null));
        bmc.handleRelayMessage(
                checker,
                link.toString(), MessageTest.mockRelayMessage(msg).toBase64String());
    }

    @Test
    void handleRelayMessageShouldSendErrorIfInvalidReceiver() {
        BigInteger amount = BigInteger.ONE;
        BTPMessage msg = btpMessageForClaimReward(amount, "invalid");
        Consumer<TransactionResult> checker = MessageWithFeeTest.responseMessageChecker(
                link, msg, BMCException.unknown("invalid address format"));
        bmc.handleRelayMessage(
                checker,
                link.toString(), MessageTest.mockRelayMessage(msg).toBase64String());
    }

    @SuppressWarnings("ThrowableNotThrown")
    @ParameterizedTest
    @MethodSource("claimRewardShouldRevertArguments")
    void claimRewardShouldRevert(
            String display,
            BTPException exception,
            String network, String receiver, BigInteger fee) {
        System.out.println(display);
        if (network.equals(link.net()) || network.equals(btpAddress.net())) {
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

}
