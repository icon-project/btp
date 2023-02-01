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

package foundation.icon.btp.test;

import foundation.icon.btp.mock.ChainScore;
import foundation.icon.btp.mock.ChainScoreClient;
import foundation.icon.btp.mock.MockGov;
import foundation.icon.btp.mock.MockGovScoreClient;
import foundation.icon.jsonrpc.Address;
import foundation.icon.score.client.DefaultScoreClient;
import foundation.icon.score.client.Wallet;
import foundation.icon.score.util.StringUtil;
import org.junit.jupiter.api.Tag;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("integration")
public interface MockGovIntegrationTest {

    MockGovScoreClient mockGovClient = new MockGovScoreClient(
            DefaultScoreClient.of("gov-mock.", System.getProperties()));
    MockGov mockGov = mockGovClient;
    Wallet validatorWallet = DefaultScoreClient.wallet("validator.", System.getProperties());
    ChainScoreClient chainScoreClient = new ChainScoreClient(mockGovClient.endpoint(), mockGovClient._nid(), validatorWallet,
            new Address(ChainScore.ADDRESS));
    ChainScore chainScore = chainScoreClient;

    static long openBTPNetwork(String networkTypeName, String name, score.Address owner) {
        ensureRevision();
        ensureBTPPublicKey();
        AtomicLong networkId = new AtomicLong();

        mockGovClient.openBTPNetwork(chainScoreClient.BTPNetworkOpened((l) -> {
                    assertEquals(1, l.size());
                    networkId.set(l.get(0).getNetworkID());
                }, null),
                networkTypeName, name, owner);
        return networkId.get();
    }

    static void closeBTPNetwork(long networkId) {
        mockGovClient.closeBTPNetwork(
                chainScoreClient.BTPNetworkClosed((l) -> {
                    assertEquals(1, l.size());
                    assertEquals(networkId, l.get(0).getNetworkID());
                }, null),
                networkId);
    }

    static void ensureRevision() {
        final int revision = 9;
        if (revision != chainScore.getRevision()) {
            mockGov.setRevision(revision);
        }
    }

    static void ensureBTPPublicKey() {
        String DSA = "ecdsa/secp256k1";
        Address address = validatorWallet.getAddress();
        byte[] pubKey = chainScore.getBTPPublicKey(address, DSA);
        System.out.println("getPublicKey:" + StringUtil.bytesToHex(pubKey));
        if (pubKey == null) {
            pubKey = validatorWallet.getPublicKey();
            System.out.println("setBTPPublicKey:" + StringUtil.bytesToHex(pubKey));
            chainScore.setBTPPublicKey(DSA, pubKey);
        }
    }


}
