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

import foundation.icon.btp.test.BTPIntegrationTest;
import foundation.icon.btp.test.EVMIntegrationTest;
import foundation.icon.btp.test.LibRLPIntegrationTest;
import foundation.icon.btp.util.StringUtil;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class LibRLPStructTest implements LibRLPIntegrationTest {
    static LibRLPStruct libRLPStruct = EVMIntegrationTest.deploy(LibRLPStruct.class);

    static void assertEqualsMessageEvent(EventDataBTPMessage o1, EventDataBTPMessage o2) {
        assertEquals(o1.getNext_bmc(), o2.getNext_bmc());
        assertEquals(o1.getSeq(), o2.getSeq());
        assertArrayEquals(o1.getMsg(), o2.getMsg());
    }

    static EventDataBTPMessage newMessageEvent(LibRLPStruct.MessageEvent s) {
        return new EventDataBTPMessage(
                s.nextBmc,
                s.seq,
                s.message);
    }

    @Test
    void testMessageEvent() throws Exception {
        EventDataBTPMessage expected = new EventDataBTPMessage(
                BTPIntegrationTest.Faker.btpLink().toString(),
                BigInteger.ONE,
                EVMIntegrationTest.Faker.bytes(1));

        byte[] expectedBytes = expected.toBytes();
        System.out.println("expected:" + StringUtil.bytesToHex(expectedBytes));
        assertEqualsMessageEvent(expected, EventDataBTPMessage.fromBytes(expectedBytes));

        //for encode test
//        byte[] encoded = libRLPStruct.encodeMessageEvent(
//                new LibRLPStruct.MessageEvent(
//                        expected.getNext_bmc(),
//                        expected.getSeq(),
//                        expected.getMsg())).send();
//        System.out.println("encoded:" + StringUtil.bytesToHex(encoded));
//        assertArrayEquals(expectedBytes, encoded);

        LibRLPStruct.MessageEvent decoded = libRLPStruct.decodeMessageEvent(expectedBytes).send();
        System.out.println("decoded:" + newMessageEvent(decoded));
        assertEqualsMessageEvent(expected, newMessageEvent(decoded));
    }

}
