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

package foundation.icon.btp.bmv.bridge;

import foundation.icon.btp.lib.BTPAddress;
import foundation.icon.btp.test.BTPIntegrationTest;
import foundation.icon.btp.test.EVMIntegrationTest;
import foundation.icon.btp.test.MockBMCIntegrationTest;
import org.junit.jupiter.api.Test;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class BMVIntegrationTest implements BTPIntegrationTest {
    static BTPAddress bmc = MockBMCIntegrationTest.btpAddress();
    static BTPAddress prev = BTPIntegrationTest.Faker.btpLink();
    static BMV bmv = deployBMV();

    static BMV deployBMV() {
        EVMIntegrationTest.replaceContractBinary(BMV.class, "bmv.", System.getProperties());
        return EVMIntegrationTest.deploy(BMV.class,
                MockBMCIntegrationTest.mockBMC.getContractAddress(),
                prev.net(),
                BigInteger.ZERO);
    }

    @Test
    void handleRelayMessage() throws Exception {
        BigInteger seq = BigInteger.ZERO;
        String msg = "testMessage";

        EventDataBTPMessage ed = new EventDataBTPMessage(
                bmc.toString(), seq.add(BigInteger.ONE), msg.getBytes());
        BigInteger height = BigInteger.ONE;
        ReceiptProof rp = new ReceiptProof(0, List.of(ed), height);
        RelayMessage rm = new RelayMessage(new ArrayList<>(List.of(rp)));

        Consumer<TransactionReceipt> checker = MockBMCIntegrationTest.handleRelayMessageEvent(
                (el) -> assertArrayEquals(new byte[][]{msg.getBytes()}, el._ret.toArray(byte[][]::new)));
        checker.accept(MockBMCIntegrationTest.mockBMC.handleRelayMessage(
                bmv.getContractAddress(),
                prev.toString(),
                seq,
                RelayMessage.toBytes(rm)).send());

        BMV.VerifierStatus status = bmv.getStatus().send();
        assertEquals(height, status.height);
    }
}
