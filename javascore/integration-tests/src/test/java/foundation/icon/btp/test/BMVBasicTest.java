/*
 * Copyright 2021 ICON Foundation
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

import foundation.icon.btp.bmv.BTPMessageVerifier;
import foundation.icon.btp.bmv.lib.mpt.MerklePatriciaTree;
import foundation.icon.btp.bmv.lib.mta.MerkleTreeAccumulator;
import foundation.icon.btp.bmv.types.BlockHeader;
import foundation.icon.btp.bmv.types.ValidatorList;
import foundation.icon.btp.bmv.types.Votes;
import foundation.icon.btp.bmv.types.*;
import foundation.icon.icx.IconService;
import foundation.icon.icx.KeyWallet;
import foundation.icon.icx.Wallet;
import foundation.icon.icx.data.Address;
import foundation.icon.icx.transport.http.HttpProvider;
import foundation.icon.icx.transport.jsonrpc.RpcObject;
import foundation.icon.icx.transport.jsonrpc.RpcValue;
import foundation.icon.test.common.*;
import foundation.icon.test.score.Score;
import org.bouncycastle.util.encoders.Hex;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Base64;

import static foundation.icon.test.common.Env.LOG;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag(Constants.TAG_JAVA_SCORE)
class BMVBasicTest extends TestBase {
    private static final Address ZERO_ADDRESS = new Address("hx0000000000000000000000000000000000000000");
    private static IconService iconService;
    private static TransactionHandler txHandler;
    private static SecureRandom secureRandom;
    private static KeyWallet[] wallets;
    private static KeyWallet ownerWallet, caller;

    @BeforeAll
    static void init() throws Exception {
        Env.Node node = Env.nodes[0];
        Env.Channel channel = node.channels[0];
        Env.Chain chain = channel.chain;
        HttpProvider provider = new HttpProvider(channel.getAPIUrl(Env.testApiVer));
        iconService = new IconService(provider);

        System.out.println("iconService => " + channel.getAPIUrl(Env.testApiVer));
        txHandler = new TransactionHandler(iconService, chain);
        secureRandom = new SecureRandom();

        // init wallets
        wallets = new KeyWallet[2];
        BigInteger amount = ICX.multiply(BigInteger.valueOf(100));
        for (int i = 0; i < wallets.length; i++) {
            wallets[i] = KeyWallet.create();
            txHandler.transfer(wallets[i].getAddress(), amount);
        }
        for (KeyWallet wallet : wallets) {
            ensureIcxBalance(txHandler, wallet.getAddress(), BigInteger.ZERO, amount);
        }
        ownerWallet = wallets[0];
        caller = wallets[1];
    }

    @AfterAll
    static void shutdown() throws Exception {
        for (KeyWallet wallet : wallets) {
            txHandler.refundAll(wallet);
        }
    }

    public static Score deployMessageVerifier(TransactionHandler txHandler, Wallet owner)
            throws ResultTimeoutException, TransactionFailureException, IOException {
        LOG.infoEntering("deploy", "BTPMessageVerifier");

        byte[] mta = Base64.getUrlDecoder().decode("-EcF-ESgLRW8UPg-kGpGwRkzumLmCqfvZJ_97J7DYioRPCizgcH4AKAUevyJu0uatfNpHBb_AJs5WDRfAtrOsbGsMXLamB2Alw==");

        RpcObject args = new RpcObject.Builder()
                .put("network", new RpcValue("0x1.iconee"))
                .put("bmcScoreAddress", new RpcValue(""))
                .put("validators", new RpcValue(new byte[]{}))
                .put("encMTA", new RpcValue(new byte[]{}))
                .put("offset", new RpcValue(BigInteger.ZERO))
                .build();

        Score score = txHandler.deploy(owner,
                new Class[]{BTPMessageVerifier.class,
                        scorex.util.ArrayList.class,
                        BTPAddress.class,
                        BlockHeader.class,
                        BlockUpdate.class,
                        BlockWitness.class,
                        BlockProof.class,
                        EventProof.class,
                        ReceiptProof.class,
                        ValidatorList.class,
                        Votes.class,
                        RelayMessage.class,
                        MerkleTreeAccumulator.class,
                        MerklePatriciaTree.class,
                        Receipt.class},
                args);

        LOG.info("Deployed Message Verifier Address address " + score.getAddress());

        LOG.infoExiting();
        return score;
    }

    public static Score deployBlockWitness(TransactionHandler txHandler, Wallet owner)
            throws ResultTimeoutException, TransactionFailureException, IOException {
        LOG.infoEntering("deploy", "BlockWitness");

        Score score = txHandler.deploy(owner, BlockWitness.class,
                new RpcObject.Builder().put("enc", null).build());

        LOG.info("Deployed address " + score.getAddress());

        LOG.infoExiting();
        return score;
    }

    @Test
    public void shouldDeploy() throws Exception {
        Score mtaScore = deployMessageVerifier(txHandler, ownerWallet);
    }

    //@Test
    public void shouldRelayVerifiedMessage() throws Exception {
        Score bmv = deployMessageVerifier(txHandler, ownerWallet);

        var msg = Hex.decode("f901a0f9019ab90197f90194b0ef800100808080a05019d16daa7707229a181cec30b05e70c64669aa2f0ec031383eac452952f78f80808084c3808080b9015ef9015b00e201a0f82ae206d6bd526717410b45b1d40861255ad94967a93e1ab22d7ec862e946cbf90134f84b870598c2d9aaf5deb841bb85a58605af9292376cf59cf60d7a0f58a98b158580be96c3dbef518ca4f50a2b0845df43086a1c9f86fc2d9bd9c0a270f35ead3225f027c93d01080e931b5c01f84b870598c2d9aaf5deb841a00d6c2ed00e7ffcc0faa74360d948f7f82529e1ded9431906b60a75b8af83f816e91da8a0c14cb0f319c2a7d19ae38377a239b5bdb1f9ca4215cf698f09c62a01f84b870598c2d9aaf5deb8410ca467e4a27bbdb26e3aee9baa622875b889be6563941ace0ce784761e09050a4578ebdae2a27b9a1b6a62ad4bf343eb280633b34c87d837989c7d83c98731e200f84b870598c2d9aaf5deb841eda7ed393fcfecc843dab4d5515def474a6eb780f8ac3d3b2cbb82b94460995c439b6ae38ce087bdf59709b3e2752b9be65d1dcb9c97b2f921c82c2bbd364d3501f800f800c0");

        RpcObject args = new RpcObject.Builder()
                .put("bmc", new RpcValue("btp://0x1.bsc/c0"))
                .put("prev", new RpcValue("btp://0x2.icon/c0"))
                .put("seq", new RpcValue(BigInteger.valueOf(1)))
                .put("msg", new RpcValue(msg))
                .build();

        var tr = bmv.invokeAndWaitResult(ownerWallet, "handleRelayMessage", args);
        System.out.println(tr.getEventLogs());
        assertEquals(BigInteger.valueOf(1), bmv.call("getLastHeight", null).asInteger());
    }
}
