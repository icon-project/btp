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
import foundation.icon.btp.test.EVMIntegrationTest;
import foundation.icon.btp.test.MockBMVIntegrationTest;
import foundation.icon.btp.test.MockBSHIntegrationTest;
import foundation.icon.btp.util.ArrayUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import java.math.BigInteger;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RewardTest implements BMCIntegrationTest {
    static BTPAddress link = Faker.btpLink();
    static String svc = MockBSHIntegrationTest.SERVICE;
    static String relay = EVMIntegrationTest.credentials.getAddress();
    static FeeInfo linkFee = FeeManagementTest.fakeFee(link.net());

    @BeforeAll
    static void beforeAll() throws Exception {
        System.out.println("RewardTest:beforeAll start");
        BMVManagementTest.addVerifier(link.net(),
                MockBMVIntegrationTest.mockBMV.getContractAddress());
        LinkManagementTest.addLink(link.toString());
        BMRManagementTest.addRelay(link.toString(), relay);

        BSHManagementTest.clearService(svc);
        BSHManagementTest.addService(svc,
                MockBSHIntegrationTest.mockBSH.getContractAddress());

        FeeManagementTest.setFeeTable(linkFee);
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

    static String rewardAddress(boolean isRemain) {
        return isRemain ?
                bmcPeriphery.getContractAddress() : relay;
    }

    static BigInteger ensureReward(String net, boolean isRemain) throws Exception {
        System.out.println("ensureReward " + (isRemain ?
                "handleRelayMessageShouldAccumulateRemainFee" :
                "handleRelayMessageShouldAccumulateRewardIfClaimMessage"));
        String address = rewardAddress(isRemain);
        BigInteger reward = bmcPeriphery.getReward(net, address).send();
        if (reward.compareTo(BigInteger.ZERO) <= 0) {
            BigInteger amount = BigInteger.ONE;
            BTPMessage msg = btpMessageForReward(net,
                    isRemain ? new BigInteger[]{BigInteger.ZERO, amount} : new BigInteger[]{amount});
            bmcPeriphery.handleRelayMessage(link.toString(),
                    MessageTest.mockRelayMessage(msg).toBytes()).send();
            reward = bmcPeriphery.getReward(net, address).send();
            assertEquals(amount, reward);
        }
        if (net.equals(btpAddress.net()) &&
                EVMIntegrationTest.getBalance(bmcPeriphery).compareTo(reward) < 0) {
            EVMIntegrationTest.transfer(bmcPeriphery, reward);
        }
        return reward;
    }

    static Consumer<TransactionReceipt> rewardChecker(String net, String address, BigInteger amount) throws Exception {
        BigInteger preReward = bmcPeriphery.getReward(net, address).send();
        return (txr) -> {
            try {
                assertEquals(preReward.add(amount), bmcPeriphery.getReward(net, address).send());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }

    static Consumer<TransactionReceipt> claimMessageChecker(BigInteger amount, String receiver) {
        return (txr) -> {
            List<BMCMessage> bmcMessages = BMCIntegrationTest.bmcMessages(txr, null);
            List<ClaimMessage> rewardMessages = BMCIntegrationTest.internalMessages(
                    bmcMessages, BMCIntegrationTest.Internal.Claim, ClaimMessage::fromBytes);
            assertEquals(rewardMessages.size(), 1);
            ClaimMessage rewardMessage = rewardMessages.get(0);
            assertEquals(amount, rewardMessage.getAmount());
            assertEquals(receiver, rewardMessage.getReceiver());
        };
    }

    static Consumer<TransactionReceipt> claimRewardEventChecker(
            String sender, String net, String receiver, BigInteger amount, BigInteger nsn) {
        return BMCIntegrationTest.claimRewardEvent(
                (el) -> {
                    assertEquals(sender, el._sender);
                    assertEquals(net, el._network);
                    assertEquals(receiver, el._receiver);
                    assertEquals(amount, el._amount);
                    assertEquals(nsn, el._nsn);
                });
    }

    static Consumer<TransactionReceipt> claimRewardResultEventChecker(
            BigInteger nsn, String net, BigInteger result) {
        return BMCIntegrationTest.claimRewardResultEvent(
                (el) -> {
                    assertEquals(nsn, el._nsn);
                    assertEquals(net, el._network);
                    assertEquals(result, el._result);
                });
    }

    static BTPMessage btpMessageForResponse(String feeNetwork, ResponseMessage responseMessage) throws Exception {
        BTPMessage msg = new BTPMessage();
        msg.setSrc(feeNetwork);
        msg.setDst(btpAddress.net());
        msg.setSvc(BMCIntegrationTest.INTERNAL_SERVICE);
        BigInteger nsn = bmcPeriphery.getNetworkSn().send();
        if (responseMessage.getCode() == ResponseMessage.CODE_SUCCESS) {
            msg.setSn(BigInteger.ZERO);
            BMCMessage bmcMsg = new BMCMessage(BMCIntegrationTest.Internal.Response.name(),
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
                        FeeManagementTest.getFeeTable(List.of(feeNetwork))
                                .get(0).toArray(BigInteger[]::new))));
        return msg;
    }

    @ParameterizedTest
    @MethodSource("claimRewardShouldSuccessArguments")
    void claimRewardShouldSuccess(
            String display,
            boolean isRemain,
            String feeNetwork,
            BTPException error) throws Exception {
        System.out.println(display);
        if (isRemain) {
            String feeHandler = tester.getAddress();
            bmcManagement.setFeeHandler(feeHandler).send();
            assertEquals(feeHandler, bmcManagement.getFeeHandler().send());
        }

        String sender = rewardAddress(isRemain);
        String receiver;
        BigInteger reward = ensureReward(feeNetwork, isRemain);
        BigInteger pay;
        Consumer<TransactionReceipt> checker = rewardChecker(feeNetwork, sender, reward.negate());
        if (feeNetwork.equals(btpAddress.net())) {
            System.out.println("claimRewardShouldTransfer to "+tester.getAddress());
            receiver = tester.getAddress();
            pay = BigInteger.ZERO;
            checker = checker.andThen(claimRewardEventChecker(
                            sender, feeNetwork, receiver, reward, BigInteger.ZERO))
                    .andThen(EVMIntegrationTest.balanceChecker(receiver, reward, isRemain));
            //If evm-based-chain emit transfer-eventlog as ICXTransfer of ICON,
            //check reward transfer from bmcPeriphery to receiver
        } else {
            System.out.println("claimRewardShouldSendClaimMessage");
            receiver = relay;
            pay = bmcPeriphery.getFee(feeNetwork, true).send();
            BigInteger nsn = bmcPeriphery.getNetworkSn().send();
            checker = checker.andThen(claimRewardEventChecker(
                            sender, feeNetwork, receiver.toString(), reward, nsn.add(BigInteger.ONE)))
                    .andThen(claimMessageChecker(reward, receiver.toString()));
        }
        checker.accept(
                (isRemain ? bmcPeripheryWithTester : bmcPeriphery).claimReward(
                     feeNetwork, receiver,
                     pay).send()
        );

        if (!feeNetwork.equals(btpAddress.net())) {
            ResponseMessage responseMessage = MessageTest.toResponseMessage(error);
            BTPMessage msg = btpMessageForResponse(feeNetwork, responseMessage);
            BigInteger afterReward;
            if (error != null) {
                System.out.println("handleRelayMessageShouldRollbackIfError");
                afterReward = reward;
            } else {
                System.out.println("handleRelayMessageShouldClearClaimRequest");
                afterReward = bmcPeriphery.getReward(feeNetwork, sender).send();
            }
            checker = claimRewardResultEventChecker(
                    msg.getNsn().negate(),
                    feeNetwork,
                    BigInteger.valueOf(responseMessage.getCode()));
            checker.accept(
                    bmcPeriphery.handleRelayMessage(
                            link.toString(), MessageTest.mockRelayMessage(msg).toBytes()).send()
            );
            assertEquals(afterReward, bmcPeriphery.getReward(feeNetwork, sender).send());
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
        BMCMessage bmcMessage = new BMCMessage(BMCIntegrationTest.Internal.Claim.name(), claimMessage.toBytes());
        BTPMessage msg = new BTPMessage();
        msg.setSrc(link.net());
        msg.setDst(btpAddress.net());
        msg.setSvc(BMCIntegrationTest.INTERNAL_SERVICE);
        msg.setSn(BigInteger.ONE);
        msg.setPayload(bmcMessage.toBytes());
        msg.setNsn(BigInteger.ONE);
        msg.setFeeInfo(new FeeInfo(
                link.net(), MessageWithFeeTest.reverse(linkFee.getValues())));
        return msg;
    }

    @Test
    void handleRelayMessageShouldAccumulateRewardToReceiverAndSendResponse() throws Exception {
        BigInteger amount = BigInteger.ONE;
        BTPMessage msg = btpMessageForClaimReward(amount, relay.toString());
        Consumer<TransactionReceipt> checker = rewardChecker(btpAddress.net(), relay, amount)
                .andThen(MessageWithFeeTest.responseMessageChecker(link, msg, null));
        checker.accept(
                bmcPeriphery.handleRelayMessage(
                        link.toString(), MessageTest.mockRelayMessage(msg).toBytes()).send()
        );
    }

    @Test
    void handleRelayMessageShouldSendErrorIfInvalidReceiver() throws Exception {
        BigInteger amount = BigInteger.ONE;
        BTPMessage msg = btpMessageForClaimReward(amount, "invalid");
        Consumer<TransactionReceipt> checker = MessageWithFeeTest.responseMessageChecker(
                link, msg, BMCException.unknown("InvalidArgument"));
        checker.accept(
                bmcPeriphery.handleRelayMessage(
                        link.toString(), MessageTest.mockRelayMessage(msg).toBytes()).send()
        );
    }

    @SuppressWarnings("ThrowableNotThrown")
    @ParameterizedTest
    @MethodSource("claimRewardShouldRevertArguments")
    void claimRewardShouldRevert(
            String display,
            BTPException exception,
            String network, String receiver, BigInteger fee) throws Exception {
        System.out.println(display);
        if (network.equals(link.net()) || network.equals(btpAddress.net())) {
            ensureReward(network, false);
        }
        AssertBTPException.assertBTPException(exception, () ->
                bmcPeriphery.claimReward(network, receiver, fee).send());
    }

    static Stream<Arguments> claimRewardShouldRevertArguments() {
        return Stream.of(
                Arguments.of(
                        "claimRewardShouldRevertNotExistsReward",
                        BMCException.unknown("NotExistsReward"),
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
                        BMCException.unknown("NotEnoughFee"),
                        link.net(),
                        relay.toString(),
                        BigInteger.ZERO)
        );
    }

}
