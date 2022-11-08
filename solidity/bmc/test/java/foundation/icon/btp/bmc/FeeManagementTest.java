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
import foundation.icon.btp.test.EVMIntegrationTest;
import foundation.icon.btp.test.MockBMVIntegrationTest;
import foundation.icon.btp.util.ArrayUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.web3j.abi.datatypes.NumericType;
import org.web3j.abi.datatypes.generated.Uint256;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FeeManagementTest implements BMCIntegrationTest {
    static BTPAddress link = Faker.btpLink();
    static BTPAddress reachable = Faker.btpLink();
    static String relay = EVMIntegrationTest.credentials.getAddress();
    static int MAX_FEE_VALUE = 10;
    static FeeInfo linkFee = fakeFee(link.net());
    static FeeInfo reachableFee = fakeFee(reachable.net(), 1, linkFee);

    static FeeInfo fakeFee(String net) {
        return fakeFee(net, 1, null);
    }

    static FeeInfo fakeFee(String net, int addHop, FeeInfo fee) {
        List<BigInteger> values = new ArrayList<>();

        int offset = 0;
        if (fee != null) {
            offset = fee.getValues().length / 2;
            values.addAll(Arrays.asList(fee.getValues()));
        }
        int num = addHop * 2;
        for (int i = 0; i < num; i++) {
            values.add(offset, BigInteger.valueOf(
                    EVMIntegrationTest.Faker.positive(MAX_FEE_VALUE)));
        }
        return new FeeInfo(net, values.toArray(BigInteger[]::new));
    }

    static FeeInfo fakeFee(String net, BigInteger[] inner, BigInteger[] outer) {
        List<BigInteger> values = new ArrayList<>();
        values.addAll(Arrays.asList(forward(inner)));
        values.addAll(Arrays.asList(forward(outer)));
        values.addAll(Arrays.asList(backward(outer)));
        values.addAll(Arrays.asList(backward(inner)));
        return new FeeInfo(net, values.toArray(BigInteger[]::new));
    }

    static BigInteger[] forward(BigInteger[] values) {
        return ArrayUtil.copyOf(values, values.length / 2);
    }

    static BigInteger[] backward(BigInteger[] values) {
        return Arrays.copyOfRange(values, values.length / 2, values.length);
    }

    @BeforeAll
    static void beforeAll() throws Exception {
        System.out.println("FeeManagementTest:beforeAll start");
        BMVManagementTest.addVerifier(link.net(), MockBMVIntegrationTest.mockBMV.getContractAddress());
        LinkManagementTest.addLink(link.toString());
        BMRManagementTest.addRelay(link.toString(), relay);
        MessageTest.ensureReachable(link, new BTPAddress[]{reachable});
        System.out.println("FeeManagementTest:beforeAll end");
    }

    @AfterAll
    static void afterAll() {
        System.out.println("FeeManagementTest:afterAll start");
        LinkManagementTest.clearLink(link.toString());
        BMVManagementTest.clearVerifier(link.net());
        System.out.println("FeeManagementTest:afterAll end");
    }

    @SuppressWarnings("unchecked")
    static List<List<BigInteger>> getFeeTable(List<String> _dst) throws Exception {
        List<List<Uint256>> ret = bmcManagement.getFeeTable(_dst).send();
        return ret.stream()
                .map((l) ->
                        l.stream()
                                .map(NumericType::getValue)
                                .collect(Collectors.toList()))
                .collect(Collectors.toList());
    }

    static void setFeeTable(FeeInfo... fees) throws Exception {
        setFeeTable(Arrays.asList(fees));
    }

    static void setFeeTable(List<FeeInfo> fees) throws Exception {
        List<String> _dst = fees.stream()
                .map(FeeInfo::getNetwork)
                .collect(Collectors.toList());
        List<List<BigInteger>> _value = fees.stream()
                .map((v) -> Arrays.asList(v.getValues()))
                .collect(Collectors.toList());
        bmcManagement.setFeeTable(_dst, _value).send();

        assertEquals(_value, getFeeTable(_dst));
    }

    @Test
    void setFeeTableShouldSuccess() throws Exception {
        setFeeTable(linkFee, reachableFee);
    }

    @Test
    void setFeeTableShouldRemoveIfValuesEmpty() throws Exception {
        setFeeTable(linkFee, reachableFee);

        setFeeTable(new FeeInfo(reachableFee.getNetwork(), new BigInteger[]{}));
        assertEquals(BigInteger.ZERO, bmcPeriphery.getFee(reachableFee.getNetwork(), true).send());
    }

    @Test
    void setFeeTableShouldRevertInvalidArrayLength() {
        List<List<BigInteger>> invalidArrayLength = new ArrayList<>();
        invalidArrayLength.add(Arrays.asList(linkFee.getValues()));
        invalidArrayLength.add(Arrays.asList(linkFee.getValues()));
        AssertBMCException.assertUnknown(() ->
                bmcManagement.setFeeTable(
                        Arrays.asList(linkFee.getNetwork()),
                        invalidArrayLength).send());
    }

    @Test
    void setFeeTableShouldRevertOddValues() {
        FeeInfo oddValues = new FeeInfo(Faker.btpNetwork(), new BigInteger[]{BigInteger.ZERO});
        AssertBMCException.assertUnknown(() -> setFeeTable(oddValues));
    }

    @Disabled("N/A, because uin256 type used in solidity ")
    @Test
    void setFeeTableShouldRevertNegativeValue() {
        FeeInfo negativeValues = new FeeInfo(Faker.btpNetwork(),
                new BigInteger[]{BigInteger.ZERO, BigInteger.ONE.negate()});
        AssertBMCException.assertUnknown(() -> setFeeTable(negativeValues));
    }

    @Test
    void setFeeTableShouldRevertNotExistsLink() {
        AssertBMCException.assertNotExistsLink(() ->
                setFeeTable(fakeFee(Faker.btpNetwork())));
    }

    @Test
    void setFeeTableShouldRevertUnreachable() {
        AssertBMCException.assertUnreachable(() ->
                setFeeTable(fakeFee(Faker.btpNetwork(), 2, null)));
    }

    @Test
    void getFeeShouldReturnsSumOfFee() throws Exception {
        setFeeTable(linkFee, reachableFee);
        assertEquals(ArrayUtil.sum(forward(linkFee.getValues())),
                bmcPeriphery.getFee(link.net(), false).send());
        assertEquals(ArrayUtil.sum(linkFee.getValues()),
                bmcPeriphery.getFee(link.net(), true).send());
        assertEquals(ArrayUtil.sum(forward(reachableFee.getValues())),
                bmcPeriphery.getFee(reachable.net(), false).send());
        assertEquals(ArrayUtil.sum(reachableFee.getValues()),
                bmcPeriphery.getFee(reachable.net(), true).send());
    }
}
