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

package foundation.icon.btp.xcall;

import foundation.icon.btp.test.EVMIntegrationTest;
import foundation.icon.btp.test.LibRLPIntegrationTest;
import foundation.icon.btp.util.StringUtil;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

public class LibRLPStructTest implements LibRLPIntegrationTest {
    static LibRLPStruct libRLPStruct = EVMIntegrationTest.deploy(LibRLPStruct.class);

    @Test
    void testCSMessage() throws Exception {
        CSMessage expected = new CSMessage(CSMessage.REQUEST, EVMIntegrationTest.Faker.bytes(1));

        byte[] expectedBytes = expected.toBytes();
        System.out.println("expected:"+StringUtil.bytesToHex(expectedBytes));
        AssertCallService.assertEqualsCSMessage(expected, CSMessage.fromBytes(expectedBytes));

        byte[] encoded = libRLPStruct.encodeCSMessage(
                new LibRLPStruct.CSMessage(BigInteger.valueOf(expected.getType()), expected.getData())).send();
        System.out.println("encoded:"+StringUtil.bytesToHex(encoded));
        assertArrayEquals(expectedBytes, encoded);

        LibRLPStruct.CSMessage decoded = libRLPStruct.decodeCSMessage(encoded).send();
        System.out.println("decoded:"+new CSMessage(decoded.msgType.intValue(), decoded.payload));
        AssertCallService.assertEqualsCSMessage(expected, new CSMessage(decoded.msgType.intValue(), decoded.payload));
    }

    @Test
    void testCSMessageRequest() throws Exception {
        CSMessageRequest expected = new CSMessageRequest(
                EVMIntegrationTest.Faker.address().toString(),
                EVMIntegrationTest.Faker.address().toString(),
                BigInteger.ONE,
                false,
                EVMIntegrationTest.Faker.bytes(1));

        byte[] expectedBytes = expected.toBytes();
        System.out.println("expected:"+StringUtil.bytesToHex(expectedBytes));
        AssertCallService.assertEqualsCSMessageRequest(expected, CSMessageRequest.fromBytes(expectedBytes));

        byte[] encoded = libRLPStruct.encodeCSMessageRequest(
                new LibRLPStruct.CSMessageRequest(
                        expected.getFrom(),
                        expected.getTo(),
                        expected.getSn(),
                        expected.needRollback(),
                        expected.getData())).send();
        System.out.println("encoded:"+StringUtil.bytesToHex(encoded));
        assertArrayEquals(expectedBytes, encoded);

        LibRLPStruct.CSMessageRequest decoded = libRLPStruct.decodeCSMessageRequest(encoded).send();
        System.out.println("decoded:"+new CSMessageRequest(
                decoded.from,
                decoded.to,
                decoded.sn,
                decoded.rollback,
                decoded.data));
        AssertCallService.assertEqualsCSMessageRequest(expected, new CSMessageRequest(
                decoded.from,
                decoded.to,
                decoded.sn,
                decoded.rollback,
                decoded.data));
    }

    @Test
    void testCSMessageResponse() throws Exception {
        CSMessageResponse expected = new CSMessageResponse(
                BigInteger.ONE,
                CSMessageResponse.SUCCESS,
                "");

        byte[] expectedBytes = expected.toBytes();
        System.out.println("expected:"+StringUtil.bytesToHex(expectedBytes));
        AssertCallService.assertEqualsCSMessageResponse(expected, CSMessageResponse.fromBytes(expectedBytes));

        byte[] encoded = libRLPStruct.encodeCSMessageResponse(
                new LibRLPStruct.CSMessageResponse(
                        expected.getSn(),
                        BigInteger.valueOf(expected.getCode()),
                        expected.getMsg())).send();
        System.out.println("encoded:"+StringUtil.bytesToHex(encoded));
        assertArrayEquals(expectedBytes, encoded);

        LibRLPStruct.CSMessageResponse decoded = libRLPStruct.decodeCSMessageResponse(encoded).send();
        System.out.println("decoded:"+new CSMessageResponse(
                decoded.sn,
                decoded.code.intValue(),
                decoded.msg));
        AssertCallService.assertEqualsCSMessageResponse(expected, new CSMessageResponse(
                decoded.sn,
                decoded.code.intValue(),
                decoded.msg));
    }

    void decodeCSMessage(byte[] bytes) throws Exception {
        LibRLPStruct.CSMessage decoded = libRLPStruct.decodeCSMessage(
                bytes).send();
        CSMessage msg = new CSMessage(decoded.msgType.intValue(), decoded.payload);
        System.out.println(msg);
        switch (msg.getType()) {
            case CSMessage.REQUEST:
                LibRLPStruct.CSMessageRequest decodedReq = libRLPStruct.decodeCSMessageRequest(msg.getData()).send();
                CSMessageRequest req = new CSMessageRequest(
                        decodedReq.from,
                        decodedReq.to,
                        decodedReq.sn,
                        decodedReq.rollback,
                        decodedReq.data);
                System.out.println(req);
                break;
            case CSMessage.RESPONSE:
                LibRLPStruct.CSMessageResponse decodedResp = libRLPStruct.decodeCSMessageResponse(msg.getData()).send();
                CSMessageResponse resp = new CSMessageResponse(
                        decodedResp.sn,
                        decodedResp.code.intValue(),
                        decodedResp.msg);
                System.out.println(resp);
                break;
            default:
                throw new RuntimeException("unknown CSMessage.type");
        }
    }

}
