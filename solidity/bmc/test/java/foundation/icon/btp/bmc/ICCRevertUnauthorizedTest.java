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

import foundation.icon.btp.test.BTPIntegrationTest;
import foundation.icon.btp.test.EVMIntegrationTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.math.BigInteger;
import java.util.List;

public class ICCRevertUnauthorizedTest implements BMCIntegrationTest {
    static String address = EVMIntegrationTest.Faker.address().toString();
    static String string = "string";
    static String btpAddress = BTPIntegrationTest.Faker.btpLink().toString();
    static BigInteger bigInteger = BigInteger.ZERO;
    static byte[] bytes = EVMIntegrationTest.Faker.bytes(1);

    static void assertUnauthorized(Executable executable) {
        AssertBMCException.assertUnauthorized(executable);
    }

    @Test
    void setPeripheryShouldRevertUnauthorizedIfCallerIsNotOwner() {
        //only for owner
        System.out.println("BMCManagement.setPeripheryShouldRevertUnauthorized");
        assertUnauthorized(() -> bmcManagementWithTester.setBMCPeriphery(address).send());

        System.out.println("BMCService.setPeripheryShouldRevertUnauthorized");
        assertUnauthorized(() -> bmcServiceWithTester.setBMCPeriphery(address).send());
    }

    @Test
    void revertUnauthorizedIfCallerIsNotBMCService() {
        //only for BMCService
        System.out.println("BMCManagement.addReachableToRouteInfoShouldRevertUnauthorized");
        assertUnauthorized(() -> bmcManagementWithTester.addReachable(string, string).send());

        System.out.println("BMCManagement.removeReachableFromRouteInfoShouldRevertUnauthorized");
        assertUnauthorized(() -> bmcManagementWithTester.removeReachable(string, string).send());

        System.out.println("BMCPeriphery.emitClaimRewardResultRevertUnauthorized");
        assertUnauthorized(() -> bmcPeripheryWithTester.emitClaimRewardResult(address, string, bigInteger, bigInteger).send());
    }

    @Test
    void revertUnauthorizedIfCallerIsNotBMCManagement() {
        //only for BMCManagement
        System.out.println("BMCPeriphery.sendInternalRevertUnauthorized");
        assertUnauthorized(() -> bmcPeripheryWithTester.sendInternal(btpAddress, bytes).send());

        System.out.println("BMCPeriphery.clearSeqRevertUnauthorized");
        assertUnauthorized(() -> bmcPeripheryWithTester.clearSeq(btpAddress).send());

        System.out.println("BMCService.handleDropFeeRevertUnauthorized");
        assertUnauthorized(() -> bmcServiceWithTester.handleDropFee(string, List.of(BigInteger.ZERO)).send());
    }

    @Test
    void revertUnauthorizedIfCallerIsNotBMCPeriphery() {
        //only for BMCPeriphery
        System.out.println("BMCService.handleFeeRevertUnauthorized");
        assertUnauthorized(() -> bmcServiceWithTester.handleFee(address, bytes).send());

        System.out.println("BMCService.addRewardRevertUnauthorized");
        assertUnauthorized(() -> bmcServiceWithTester.addReward(string, address, bigInteger).send());

        System.out.println("BMCService.clearRewardRevertUnauthorized");
        assertUnauthorized(() -> bmcServiceWithTester.clearReward(string, address).send());

        System.out.println("BMCService.addRequestRevertUnauthorized");
        assertUnauthorized(() -> bmcServiceWithTester.addRequest(bigInteger, string, address, bigInteger).send());

        System.out.println("BMCService.removeResponseRevertUnauthorized");
        assertUnauthorized(() -> bmcServiceWithTester.removeResponse(string, string, bigInteger).send());

        System.out.println("BMCService.handleBTPMessageRevertUnauthorized");
        assertUnauthorized(() -> bmcServiceWithTester.handleBTPMessage(string, string, bigInteger, bytes).send());

        System.out.println("BMCService.handleBTPErrorRevertUnauthorized");
        assertUnauthorized(() -> bmcServiceWithTester.handleBTPError(string, string, bigInteger, bigInteger, string).send());
    }

    @Disabled("web3j not support array in Struct, BTPMessage.FeeInfo.values")
    @Test
    void revertUnauthorizedDisabled() {
        System.out.println("BMCPeriphery.dropMessageRevertUnauthorizedIfCallerIsNotBMCManagement");
        assertUnauthorized(() -> bmcPeripheryWithTester.dropMessage(btpAddress, bigInteger, new BMCPeriphery.BTPMessage(
                string, string, string, bigInteger, bytes, bigInteger,
                new BMCPeriphery.FeeInfo(string, List.of(BigInteger.ZERO))
        )).send());

        System.out.println("BMCService.handleErrorFeeRevertUnauthorizedIfCallerIsNotBMCPeriphery");
        assertUnauthorized(() -> bmcServiceWithTester.handleErrorFee(btpAddress, bigInteger,
                new BMCService.FeeInfo(string, List.of(BigInteger.ZERO)
                )).send());
    }
}
