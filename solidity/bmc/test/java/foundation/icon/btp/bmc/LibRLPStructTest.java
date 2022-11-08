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
import org.junit.jupiter.api.Disabled;
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

    static void assertEqualsResponseMessage(ResponseMessage o1, ResponseMessage o2) {
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

    static void assertEqualsClaimMessage(ClaimMessage o1, ClaimMessage o2) {
        assertEquals(o1.getAmount(), o2.getAmount());
        assertEquals(o1.getReceiver(), o2.getReceiver());
    }

    static BTPMessage newBTPMessage(LibRLPStruct.BTPMessage s) {
        BTPMessage r = new BTPMessage();
        r.setSrc(s.src);
        r.setDst(s.dst);
        r.setSvc(s.svc);
        r.setSn(s.sn);
        r.setPayload(s.message);
        r.setNsn(s.nsn);
        r.setFeeInfo(new FeeInfo(
                s.feeInfo.network,
                s.feeInfo.values.toArray(BigInteger[]::new)));
        return r;
    }

    static ResponseMessage newBTPErrorMessage(LibRLPStruct.ResponseMessage s) {
        ResponseMessage r = new ResponseMessage();
        r.setCode(s.code.longValue());
        r.setMsg(s.message);
        return r;
    }

    static BMCMessage newBMCMessage(LibRLPStruct.BMCMessage s) {
        BMCMessage r = new BMCMessage();
        r.setType(s.msgType);
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

    static ClaimMessage newClaimMessage(LibRLPStruct.ClaimMessage s) {
        return new ClaimMessage(s.amount, s.receiver);
    }

    @Disabled("web3j not support array in Struct")
    @Test
    void testBTPMessage() throws Exception {
        BTPMessage expected = new BTPMessage();
        expected.setSrc(BTPIntegrationTest.Faker.btpNetwork());
        expected.setDst(BTPIntegrationTest.Faker.btpNetwork());
        expected.setSn(BigInteger.ONE);
        expected.setSvc(BTPIntegrationTest.Faker.btpService());
        expected.setPayload(EVMIntegrationTest.Faker.bytes(1));
        expected.setNsn(BigInteger.ONE);
        expected.setFeeInfo(new FeeInfo(expected.getSrc(),
                new BigInteger[]{BigInteger.ZERO}));
//        expected.setFeeInfo(new FeeInfo(expected.getSrc(),
//                new BigInteger[]{}));

        byte[] expectedBytes = expected.toBytes();
        System.out.println("expected:" + StringUtil.bytesToHex(expectedBytes));
        assertEqualsBTPMessage(expected, BTPMessage.fromBytes(expectedBytes));

        byte[] encoded = libRLPStruct.encodeBTPMessage(
                new LibRLPStruct.BTPMessage(
                        expected.getSrc(),
                        expected.getDst(),
                        expected.getSvc(),
                        expected.getSn(),
                        expected.getPayload(),
                        expected.getNsn(),
                        new LibRLPStruct.FeeInfo(
                                expected.getFeeInfo().getNetwork(),
                                Arrays.asList(expected.getFeeInfo().getValues()))
                )
        ).send();
        System.out.println("encoded:" + StringUtil.bytesToHex(encoded));
        assertArrayEquals(expectedBytes, encoded);

        //TODO fail to decode LibRLPStruct.BTPMessage.FeeInfo
        //java.lang.UnsupportedOperationException: Array types must be wrapped in a TypeReference
        LibRLPStruct.BTPMessage decoded = libRLPStruct.decodeBTPMessage(encoded).send();
        System.out.println("decoded:" + newBTPMessage(decoded));
        assertEqualsBTPMessage(expected, newBTPMessage(decoded));
    }

    @Test
    void testResponseMessage() throws Exception {
        ResponseMessage expected = new ResponseMessage();
        expected.setCode(BigInteger.ONE.intValue());
        expected.setMsg("");

        byte[] expectedBytes = expected.toBytes();
        System.out.println("expected:" + StringUtil.bytesToHex(expectedBytes));
        assertEqualsResponseMessage(expected, ResponseMessage.fromBytes(expectedBytes));

        byte[] encoded = libRLPStruct.encodeResponseMessage(
                new LibRLPStruct.ResponseMessage(
                        BigInteger.valueOf(expected.getCode()),
                        expected.getMsg())).send();
        System.out.println("encoded:" + StringUtil.bytesToHex(encoded));
        assertArrayEquals(expectedBytes, encoded);

        LibRLPStruct.ResponseMessage decoded = libRLPStruct.decodeResponseMessage(encoded).send();
        System.out.println("decoded:" + newBTPErrorMessage(decoded));
        assertEqualsResponseMessage(expected, newBTPErrorMessage(decoded));
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

        byte[] encoded = libRLPStruct.encodeBMCMessage(
                new LibRLPStruct.BMCMessage(
                        expected.getType(),
                        expected.getPayload())).send();
        System.out.println("encoded:" + StringUtil.bytesToHex(encoded));
        assertArrayEquals(expectedBytes, encoded);

        LibRLPStruct.BMCMessage decoded = libRLPStruct.decodeBMCMessage(encoded).send();
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
    void testClaimMessage() throws Exception {
        ClaimMessage expected = new ClaimMessage(
                BigInteger.ONE,
                EVMIntegrationTest.Faker.address().toString()
        );

        byte[] expectedBytes = expected.toBytes();
        System.out.println("expected:" + StringUtil.bytesToHex(expectedBytes));
        assertEqualsClaimMessage(expected, ClaimMessage.fromBytes(expectedBytes));

        byte[] encoded = libRLPStruct.encodeClaimMessage(
                new LibRLPStruct.ClaimMessage(
                    expected.getAmount(),
                    expected.getReceiver()
                )).send();
        System.out.println("encoded:" + StringUtil.bytesToHex(encoded));
        assertArrayEquals(expectedBytes, encoded);

        LibRLPStruct.ClaimMessage decoded = libRLPStruct.decodeClaimMessage(encoded).send();
        System.out.println("decoded:" + newClaimMessage(decoded));
        assertEqualsClaimMessage(expected, newClaimMessage(decoded));
    }

}
