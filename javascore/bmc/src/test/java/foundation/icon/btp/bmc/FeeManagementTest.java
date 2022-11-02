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
import foundation.icon.btp.test.BTPIntegrationTest;
import foundation.icon.btp.test.MockBMVIntegrationTest;
import foundation.icon.jsonrpc.Address;
import foundation.icon.score.test.ScoreIntegrationTest;
import foundation.icon.score.util.ArrayUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class FeeManagementTest implements BMCIntegrationTest {
    static BTPAddress link = BTPIntegrationTest.Faker.btpLink();
    static BTPAddress reachable = BTPIntegrationTest.Faker.btpLink();
    static Address relay = Address.of(bmc._wallet());
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
                    ScoreIntegrationTest.Faker.positive(MAX_FEE_VALUE)));
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
    static void beforeAll() {
        System.out.println("FeeManagementTest:beforeAll start");
        BMVManagementTest.addVerifier(link.net(), MockBMVIntegrationTest.mockBMV._address());
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

    static void setFeeTable(FeeInfo... fees) {
        setFeeTable(Arrays.asList(fees));
    }

    static void setFeeTable(List<FeeInfo> fees) {
        String[] _dst = fees.stream()
                .map(FeeInfo::getNetwork)
                .toArray(String[]::new);
        BigInteger[][] _value = fees.stream()
                .map(FeeInfo::getValues)
                .toArray(BigInteger[][]::new);
        bmc.setFeeTable(_dst, _value);

        BigInteger[][] ret = bmc.getFeeTable(_dst);
        assertArrayEquals(_value, ret);
    }

    @Test
    void setFeeTableShouldSuccess() {
        setFeeTable(linkFee, reachableFee);
    }

    @Test
    void setFeeTableShouldRevert() {
        System.out.println("setFeeTableShouldRevertInvalidArrayLength");
        AssertBMCException.assertUnknown(() ->
                bmc.setFeeTable(new String[]{Faker.btpNetwork()}, new BigInteger[][]{}));

        System.out.println("setFeeTableShouldRevertOddValues");
        FeeInfo oddValues = new FeeInfo(Faker.btpNetwork(), new BigInteger[]{BigInteger.ZERO});
        AssertBMCException.assertUnknown(() -> setFeeTable(oddValues));

        System.out.println("setFeeTableShouldRevertNegativeValue");
        FeeInfo negativeValues = new FeeInfo(Faker.btpNetwork(),
                new BigInteger[]{BigInteger.ZERO, BigInteger.ONE.negate()});
        AssertBMCException.assertUnknown(() -> setFeeTable(negativeValues));

        System.out.println("setFeeTableShouldRevertNotExistsLink");
        AssertBMCException.assertNotExistsLink(() ->
                setFeeTable(fakeFee(Faker.btpNetwork())));

        System.out.println("setFeeTableShouldRevertUnreachable");
        AssertBMCException.assertUnreachable(() ->
                setFeeTable(fakeFee(Faker.btpNetwork(), 2, null)));
    }

    @Test
    void getFeeShouldReturnsSumOfFee() {
        setFeeTable(linkFee, reachableFee);
        assertEquals(ArrayUtil.sum(forward(linkFee.getValues())), bmc.getFee(link.net(), false));
        assertEquals(ArrayUtil.sum(linkFee.getValues()), bmc.getFee(link.net(), true));
        assertEquals(ArrayUtil.sum(forward(reachableFee.getValues())), bmc.getFee(reachable.net(), false));
        assertEquals(ArrayUtil.sum(reachableFee.getValues()), bmc.getFee(reachable.net(), true));
    }
}
