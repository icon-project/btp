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
import foundation.icon.btp.mock.MockBSH;
import foundation.icon.btp.mock.MockBSHScoreClient;
import foundation.icon.btp.test.BTPIntegrationTest;
import foundation.icon.btp.test.MockBMVIntegrationTest;
import foundation.icon.btp.test.MockBSHIntegrationTest;
import foundation.icon.btp.test.MockGovIntegrationTest;
import foundation.icon.icx.IconService;
import foundation.icon.icx.data.Base64;
import foundation.icon.icx.transport.http.HttpProvider;
import foundation.icon.jsonrpc.Address;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class BTPLinkManagementTest implements BMCIntegrationTest {
    static BTPAddress linkBtpAddress = BTPIntegrationTest.Faker.btpLink();
    static String link = linkBtpAddress.toString();
    static BTPAddress secondLinkBtpAddress = BTPIntegrationTest.Faker.btpLink();
    static String secondLink = secondLinkBtpAddress.toString();
    static BTPAddress btpAddress = BTPAddress.valueOf(bmc.getBtpAddress());
    static IconService iconService = new IconService(new HttpProvider(client.endpoint()));

    static void addBTPLink(String link, long networkId) {
        List<String> links = Arrays.asList(bmc.getLinks());
        ((ICONSpecificScoreClient) iconSpecific).addBTPLink((txr) -> {
            LinkManagementTest.initMessageChecker(links)
                    .accept(bmcMessages(
                            txr.getBlockHeight().add(BigInteger.ONE), BigInteger.valueOf(networkId)));
        }, link, networkId);
        assertTrue(LinkManagementTest.isExistsLink(link));
        assertEquals(networkId, iconSpecific.getBTPLinkNetworkId(link));
    }

    static List<BTPMessage> btpMessages(BigInteger height, BigInteger networkId, Predicate<BTPMessage> filter) {
        Base64[] messages = null;
        try {
            messages = iconService.btpGetMessages(height, networkId).execute();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Stream<BTPMessage> stream = Arrays.stream(messages)
                .map((m) -> BTPMessage.fromBytes(m.decode()));
        if (filter != null) {
            stream = stream.filter(filter);
        }
        return stream.collect(Collectors.toList());
    }

    static List<BTPMessage> btpMessages(BigInteger height, BigInteger networkId, String svc) {
        Predicate<BTPMessage> filter = (m) -> m.getSvc().equals(svc);
        return btpMessages(height, networkId, filter);
    }

    static <T> List<T> svcMessages(BigInteger height, BigInteger networkId, String svc,
                                   Function<BTPMessage, T> mapperFunc) {
        return btpMessages(height, networkId, svc).stream()
                .map(mapperFunc)
                .collect(Collectors.toList());
    }

    static List<BMCMessage> bmcMessages(BigInteger height, BigInteger networkId) {
        return svcMessages(height, networkId, BTPMessageCenter.INTERNAL_SERVICE,
                (m) -> BMCMessage.fromBytes(m.getPayload()));
    }

    @BeforeAll
    static void beforeAll() {
        System.out.println("beforeAll start");
        Address mockBMVAddress = MockBMVIntegrationTest.mockBMVClient._address();
        BMVManagementTest.addVerifier(linkBtpAddress.net(), mockBMVAddress);
        BMVManagementTest.addVerifier(secondLinkBtpAddress.net(), mockBMVAddress);
        System.out.println("beforeAll end");
    }

    @AfterAll
    static void afterAll() {
        System.out.println("afterAll start");
        BMVManagementTest.clearVerifier(linkBtpAddress.net());
        BMVManagementTest.clearVerifier(secondLinkBtpAddress.net());
        System.out.println("afterAll end");
    }

    @Override
    public void clearIfExists(TestInfo testInfo) {
        LinkManagementTest.clearLink(link);
    }

    @Test
    void addBTPLinkShouldSuccessAndRemoveLinkShouldRemoveNetworkId() {
        long networkId = MockGovIntegrationTest.openBTPNetwork("icon", link, bmcClient._address());
        addBTPLink(link, networkId);

        LinkManagementTest.removeLink(link);
        assertEquals(0, iconSpecific.getBTPLinkNetworkId(link));
    }

    @Test
    void addBTPLinkShouldRevertInvalidNetworkId() {
        AssertBMCException.assertUnknown(() -> iconSpecific.addBTPLink(link, 0));

        long networkId = MockGovIntegrationTest.openBTPNetwork("icon", link, bmcClient._address());
        addBTPLink(link, networkId);

        AssertBMCException.assertUnknown(() -> addBTPLink(secondLink, networkId));
    }

    @Test
    void setBTPLinkNetworkIdShouldSuccess() {
        LinkManagementTest.addLink(link);
        String svc = MockBSH.SERVICE;
        BigInteger sn = BigInteger.ONE;
        byte[] payload = Faker.btpLink().toBytes();

        BigInteger seq = BMCIntegrationTest.getStatus(bmc, link).getTx_seq().add(BigInteger.ONE);
        BSHManagementTest.addService(svc, MockBSHIntegrationTest.mockBSHClient._address());
        ((MockBSHScoreClient) MockBSHIntegrationTest.mockBSH).intercallSendMessage(
                BMCIntegrationTest.eventLogChecker(MessageEventLog::eventLogs, (el) -> {
                    assertEquals(link, el.getNext());
                    assertEquals(seq, el.getSeq());
                    BTPMessage btpMessage = el.getMsg();
                    assertEquals(btpAddress, btpMessage.getSrc());
                    assertEquals(linkBtpAddress, btpMessage.getDst());
                    assertEquals(svc, btpMessage.getSvc());
                    assertEquals(sn, btpMessage.getSn());
                    assertArrayEquals(payload, btpMessage.getPayload());
                }),
                bmcClient._address(),
                linkBtpAddress.net(), svc, sn, payload);

        BigInteger sn2 = sn.add(BigInteger.ONE);
        byte[] payload2 = Faker.btpLink().toBytes();
        long networkId = MockGovIntegrationTest.openBTPNetwork("icon", link, bmcClient._address());
        iconSpecific.setBTPLinkNetworkId(link, networkId);
        ((MockBSHScoreClient) MockBSHIntegrationTest.mockBSH).intercallSendMessage(
                (txr) -> {
                    List<BTPMessage> msgs = btpMessages(txr.getBlockHeight().add(BigInteger.ONE),
                            BigInteger.valueOf(networkId),
                            svc);
                    assertEquals(1, msgs.size());
                    BTPMessage btpMessage = msgs.get(0);
                    assertEquals(btpAddress, btpMessage.getSrc());
                    assertEquals(linkBtpAddress, btpMessage.getDst());
                    assertEquals(svc, btpMessage.getSvc());
                    assertEquals(sn2, btpMessage.getSn());
                    assertArrayEquals(payload2, btpMessage.getPayload());
                },
                bmcClient._address(),
                linkBtpAddress.net(), svc, sn2, payload2);
    }

}
