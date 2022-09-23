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
import foundation.icon.btp.test.EVMIntegrationTest;
import foundation.icon.btp.test.LibRLPIntegrationTest;
import foundation.icon.btp.util.StringUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class LibRLPStructTest implements LibRLPIntegrationTest {
    static LibRLPStruct libRLPStruct = EVMIntegrationTest.deploy(LibRLPStruct.class);

    static void assertEqualsBTPMessage(BTPMessage o1, BTPMessage o2) {
        assertEquals(o1.getSrc(), o2.getSrc());
        assertEquals(o1.getDst(), o2.getDst());
        assertEquals(o1.getSvc(), o2.getSvc());
        assertEquals(o1.getSn(), o2.getSn());
        assertArrayEquals(o1.getPayload(), o2.getPayload());
    }

    static void assertEqualsBTPErrorMessage(BTPErrorMessage o1, BTPErrorMessage o2) {
        assertEquals(o1.getCode(), o2.getCode());
        assertEquals(o1.getMsg(), o2.getMsg());
    }

    static void assertEqualsBMCMessage(BMCMessage o1, BMCMessage o2) {
        assertEquals(o1.getType(), o2.getType());
        assertArrayEquals(o1.getPayload(), o2.getPayload());
    }

    static void assertEqualsInitMessage(InitMessage o1, InitMessage o2) {
        if (o1.getLinks() == null || o1.getLinks().length == 0) {
            assertTrue(o2.getLinks() == null || o2.getLinks().length == 0);
        } else {
            assertArrayEquals(o1.getLinks(), o2.getLinks());
        }
    }

    static void assertEqualsLinkMessage(LinkMessage o1, LinkMessage o2) {
        assertEquals(o1.getLink(), o2.getLink());
    }

    static void assertEqualsUnlinkMessage(UnlinkMessage o1, UnlinkMessage o2) {
        assertEquals(o1.getLink(), o2.getLink());
    }

    static BTPMessage newBTPMessage(LibRLPStruct.BTPMessage s) {
        BTPMessage r = new BTPMessage();
        r.setSrc(BTPAddress.parse(s.src));
        r.setDst(BTPAddress.parse(s.dst));
        r.setSvc(s.svc);
        r.setSn(s.sn);
        r.setPayload(s.message);
        return r;
    }

    static BTPErrorMessage newBTPErrorMessage(LibRLPStruct.ErrorMessage s) {
        BTPErrorMessage r = new BTPErrorMessage();
        r.setCode(s.code.longValue());
        r.setMsg(s.message);
        return r;
    }

    static BMCMessage newBMCMessage(LibRLPStruct.BMCService s) {
        BMCMessage r = new BMCMessage();
        r.setType(s.serviceType);
        r.setPayload(s.payload);
        return r;
    }

    static InitMessage newInitMessage(List<String> s) {
        InitMessage r = new InitMessage();
        r.setLinks(s.stream()
                .map(BTPAddress::parse).toArray(BTPAddress[]::new));
        return r;
    }

    static LinkMessage newLinkMessage(String s) {
        LinkMessage r = new LinkMessage();
        r.setLink(BTPAddress.parse(s));
        return r;
    }

    static UnlinkMessage newUnlinkMessage(String s) {
        UnlinkMessage r = new UnlinkMessage();
        r.setLink(BTPAddress.parse(s));
        return r;
    }

    @Test
    void testBTPMessage() throws Exception {
        BTPMessage expected = new BTPMessage();
        expected.setSrc(BTPIntegrationTest.Faker.btpLink());
        expected.setDst(BTPIntegrationTest.Faker.btpLink());
        expected.setSn(BigInteger.ONE);
        expected.setSvc(BTPIntegrationTest.Faker.btpService());
        expected.setPayload(EVMIntegrationTest.Faker.bytes(1));

        byte[] expectedBytes = expected.toBytes();
        System.out.println("expected:" + StringUtil.bytesToHex(expectedBytes));
        assertEqualsBTPMessage(expected, BTPMessage.fromBytes(expectedBytes));

        byte[] encoded = libRLPStruct.encodeBTPMessage(
                new LibRLPStruct.BTPMessage(
                        expected.getSrc().toString(),
                        expected.getDst().toString(),
                        expected.getSvc(),
                        expected.getSn(),
                        expected.getPayload())).send();
        System.out.println("encoded:" + StringUtil.bytesToHex(encoded));
        assertArrayEquals(expectedBytes, encoded);

        LibRLPStruct.BTPMessage decoded = libRLPStruct.decodeBTPMessage(encoded).send();
        System.out.println("decoded:" + newBTPMessage(decoded));
        assertEqualsBTPMessage(expected, newBTPMessage(decoded));
    }

    @Test
    void testBTPErrorMessage() throws Exception {
        BTPErrorMessage expected = new BTPErrorMessage();
        expected.setCode(BigInteger.ONE.intValue());
        expected.setMsg("");

        byte[] expectedBytes = expected.toBytes();
        System.out.println("expected:" + StringUtil.bytesToHex(expectedBytes));
        assertEqualsBTPErrorMessage(expected, BTPErrorMessage.fromBytes(expectedBytes));

        byte[] encoded = libRLPStruct.encodeErrorMessage(
                new LibRLPStruct.ErrorMessage(
                        BigInteger.valueOf(expected.getCode()),
                        expected.getMsg())).send();
        System.out.println("encoded:" + StringUtil.bytesToHex(encoded));
        assertArrayEquals(expectedBytes, encoded);

        LibRLPStruct.ErrorMessage decoded = libRLPStruct.decodeErrorMessage(encoded).send();
        System.out.println("decoded:" + newBTPErrorMessage(decoded));
        assertEqualsBTPErrorMessage(expected, newBTPErrorMessage(decoded));
    }

    @ParameterizedTest
    @MethodSource("testBMCMessageParameters")
    void testBMCMessage(BMCIntegrationTest.Internal type, byte[] payload) throws Exception {
        BMCMessage expected = new BMCMessage();
        expected.setType(type.name());
        expected.setPayload(payload);

        byte[] expectedBytes = expected.toBytes();
        System.out.println("expected:" + StringUtil.bytesToHex(expectedBytes));
        assertEqualsBMCMessage(expected, BMCMessage.fromBytes(expectedBytes));

        byte[] encoded = libRLPStruct.encodeBMCService(
                new LibRLPStruct.BMCService(
                        expected.getType(),
                        expected.getPayload())).send();
        System.out.println("encoded:" + StringUtil.bytesToHex(encoded));
        assertArrayEquals(expectedBytes, encoded);

        LibRLPStruct.BMCService decoded = libRLPStruct.decodeBMCService(encoded).send();
        System.out.println("decoded:" + newBMCMessage(decoded));
        assertEqualsBMCMessage(expected, newBMCMessage(decoded));
    }

    private static Stream<Arguments> testBMCMessageParameters() {
        return Stream.of(
                Arguments.of(
                        BMCIntegrationTest.Internal.Init,
                        newInitMessage(new ArrayList<>()).toBytes()),
                Arguments.of(
                        BMCIntegrationTest.Internal.Link,
                        newLinkMessage(BTPIntegrationTest.Faker.btpLink().toString()).toBytes()));
    }

    @SuppressWarnings("unchecked")
    @Test
    void testInitMessage() throws Exception {
        InitMessage expected = new InitMessage();
        expected.setLinks(new BTPAddress[0]);

        byte[] expectedBytes = expected.toBytes();
        System.out.println("expected:" + StringUtil.bytesToHex(expectedBytes));
        assertEqualsInitMessage(expected, InitMessage.fromBytes(expectedBytes));

        byte[] encoded = libRLPStruct.encodeInitMessage(
                expected.getLinks() == null ? new ArrayList<>() :
                        Arrays.stream(expected.getLinks())
                                .map(BTPAddress::toString)
                                .collect(Collectors.toList())).send();
        System.out.println("encoded:" + StringUtil.bytesToHex(encoded));
        assertArrayEquals(expectedBytes, encoded);

        List<String> decoded = libRLPStruct.decodeInitMessage(encoded).send();
        System.out.println("decoded:" + newInitMessage(decoded));
        assertEqualsInitMessage(expected, newInitMessage(decoded));
    }

    @Test
    void testLinkMessage() throws Exception {
        LinkMessage expected = new LinkMessage();
        expected.setLink(BTPIntegrationTest.Faker.btpLink());

        byte[] expectedBytes = expected.toBytes();
        System.out.println("expected:" + StringUtil.bytesToHex(expectedBytes));
        assertEqualsLinkMessage(expected, LinkMessage.fromBytes(expectedBytes));

        byte[] encoded = libRLPStruct.encodePropagateMessage(
                expected.getLink().toString()).send();
        System.out.println("encoded:" + StringUtil.bytesToHex(encoded));
        assertArrayEquals(expectedBytes, encoded);

        String decoded = libRLPStruct.decodePropagateMessage(encoded).send();
        System.out.println("decoded:" + newLinkMessage(decoded));
        assertEqualsLinkMessage(expected, newLinkMessage(decoded));
    }

    @Test
    void decodeBTPMessage() throws Exception {
        String hex = "f8dfb8396274703a2f2f3078332e69636f6e2f637862633462346162306461313638653563633533316437653863383362383432363237613233666536b8396274703a2f2f307836312e6273632f307830353931316534634345313931323931394641393943613833376136416538643834343630363334857863616c6c01b860f85e01b85bf859aa637834616637333562646461343566313437653432356535643661623330386538386533303234363235aa307864313330356636346230306162623035653965626535636135393561653133373731623564386665010031";
        byte[] bytes = StringUtil.hexToBytes(hex);
        BTPMessage expected = BTPMessage.fromBytes(bytes);
        System.out.println("expected:" + expected);
        assertArrayEquals(bytes, expected.toBytes());

        LibRLPIntegrationTest.decode(expected.getSrc());
        LibRLPIntegrationTest.decode(expected.getDst());
        LibRLPIntegrationTest.decode(expected.getSvc());
        LibRLPIntegrationTest.decode(expected.getSn());
        LibRLPIntegrationTest.decode(expected.getPayload());

        LibRLPStruct.BTPMessage decoded = libRLPStruct.decodeBTPMessage(bytes).send();
        System.out.println("decoded:" + newBTPMessage(decoded));
    }


}
