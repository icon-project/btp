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

import foundation.icon.btp.bmv.types.BlockWitness;
import foundation.icon.btp.bmv.lib.mta.MerkleTreeAccumulator;
import foundation.icon.icx.IconService;
import foundation.icon.icx.KeyWallet;
import foundation.icon.icx.Wallet;
import foundation.icon.icx.data.Address;
import foundation.icon.icx.transport.http.HttpProvider;
import foundation.icon.icx.transport.jsonrpc.RpcItem;
import foundation.icon.icx.transport.jsonrpc.RpcObject;
import foundation.icon.icx.transport.jsonrpc.RpcValue;
import foundation.icon.test.common.*;
import foundation.icon.test.score.Score;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import scorex.util.ArrayList;

import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Base64;

import static foundation.icon.test.common.Env.LOG;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@Tag(Constants.TAG_JAVA_SCORE)
class MTATest1 extends TestBase {
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

        System.out.println("iconService => " + channel.getAPIUrl(Env.testApiVer) );
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

    public static Score deployMTA(TransactionHandler txHandler, Wallet owner, String enc)
            throws ResultTimeoutException, TransactionFailureException, IOException {
        LOG.infoEntering("deploy", "MerkelTreeAccumulator");

        byte[] data = Base64.getUrlDecoder().decode(enc);

        Score score = txHandler.deploy(owner, MerkleTreeAccumulator.class,
               new RpcObject.Builder().put("mta", new RpcValue(data)).build());

        LOG.info("Deployed address " + score.getAddress());

        LOG.infoExiting();
        return score;
    }

    public static Score deployBlockWitness(TransactionHandler txHandler, Wallet owner, String enc)
            throws ResultTimeoutException, TransactionFailureException, IOException {
        LOG.infoEntering("deploy", "BlockWitness");

        Score score = txHandler.deploy(owner,
                new Class[]{BlockWitness.class,
                        ArrayList.class},
                new RpcObject.Builder()
                        .put("enc", new RpcValue(Base64.getUrlDecoder().decode(enc)))
                        .build());

        LOG.info("Deployed address " + score.getAddress());

        LOG.infoExiting();
        return score;
    }

    @Test
    public void testCreateMerkelTreeAccumulator() throws Exception {
        Score mtaScore = deployMTA(txHandler, ownerWallet,
                "-EcF-ESgLRW8UPg-kGpGwRkzumLmCqfvZJ_97J7DYioRPCizgcH4AKAUevyJu0uatfNpHBb_AJs5WDRfAtrOsbGsMXLamB2Alw==");

        RpcItem res = mtaScore.call("getHeight", null);
        assertEquals(res.asInteger(), BigInteger.valueOf(5));

        RpcObject arg = new RpcObject.Builder().put("index", new RpcValue(BigInteger.valueOf(0))).build();
        res = mtaScore.call("getRoot", arg);
        assertEquals(res.asString(), "0x2d15bc50f83e906a46c11933ba62e60aa7ef649ffdec9ec3622a113c28b381c1");

        arg = new RpcObject.Builder().put("index", new RpcValue(BigInteger.valueOf(1))).build();
        res = mtaScore.call("getRoot", arg);
        assertNull(res);

        arg = new RpcObject.Builder().put("index", new RpcValue(BigInteger.valueOf(2))).build();
        res = mtaScore.call("getRoot", arg);
        assertEquals(res.asString(), "0x147afc89bb4b9ab5f3691c16ff009b3958345f02daceb1b1ac3172da981d8097");

        //assertEquals(5, mta.getHeight());
        //assertEquals(0, mta.getOffset());
    }

    @Test
    public void shouldAddToAnEmptyRoot() throws Exception {
        Score mta = deployMTA(txHandler, ownerWallet,
                "-EcF-ESgLRW8UPg-kGpGwRkzumLmCqfvZJ_97J7DYioRPCizgcH4AKAUevyJu0uatfNpHBb_AJs5WDRfAtrOsbGsMXLamB2Alw==");

        SampleData data = new SampleData("4gagAB7_6SlDr-8ReDBwNuRqYGuZv5c_A1k6006BIm765hQ=");

        assertEquals(BigInteger.valueOf(3), mta.call("getRootsSize", null).asInteger());

        mta.invokeAndWaitResult(ownerWallet, "add",
                new RpcObject.Builder().put("hash", new RpcValue(data.hash)).build());

        // Root size unchanged
        assertEquals(BigInteger.valueOf(3), mta.call("getRootsSize", null).asInteger());

        assertEquals(BigInteger.valueOf(6), mta.call("getHeight", null).asInteger());
    }


    @Test
    void shouldVerify() throws TransactionFailureException, IOException, ResultTimeoutException {
        Score mtaScore = deployMTA(txHandler, ownerWallet,
                "-EcF-ESgLRW8UPg-kGpGwRkzumLmCqfvZJ_97J7DYioRPCizgcH4AKAUevyJu0uatfNpHBb_AJs5WDRfAtrOsbGsMXLamB2Alw==");

        Score score = deployBlockWitness(txHandler, ownerWallet,
                "-EUF-EKg1hZgfT5LqWp08yPP_F8go8eOfKuOy9uwOxP6j_yb9kSgQPQJm3foiaGDDqEJM3EWYtW1RWXcM0PmPhEdvYzfwZ4=");

        RpcItem height = score.call("getHeight", null);

        assertEquals(BigInteger.valueOf(5), height.asInteger());

        RpcItem res = score.call("getWitness", null);

        SampleData data = new SampleData("4gGgBc2Y_ezHRTgYKhI_PZHgMYM9o-mwolWNZlLki_MYobI=");

        RpcObject arg = new RpcObject.Builder()
                .put("witness", res)
                .put("hash", new RpcValue(data.hash))
                .put("blockHeight", new RpcValue(BigInteger.valueOf(data.height)))
                .put("cur", height)
                .build();

        mtaScore.invokeAndWaitResult(ownerWallet, "verify", arg);
    }


    @Test
    void testAddWitnessCanVerify() throws Exception {
        //mta.setCacheSize(2);
        Score mta = deployMTA(txHandler, ownerWallet,
                "-EcF-ESgLRW8UPg-kGpGwRkzumLmCqfvZJ_97J7DYioRPCizgcH4AKAUevyJu0uatfNpHBb_AJs5WDRfAtrOsbGsMXLamB2Alw==");

        Score bw = deployBlockWitness(txHandler, ownerWallet,
                "4wbhoC0VvFD4PpBqRsEZM7pi5gqn72Sf_eyew2IqETwos4HB");

        //"4wbhoC0VvFD4PpBqRsEZM7pi5gqn72Sf_eyew2IqETwos4HB",
          //      "wgfA",
            //    "-GYI-GOgG7APaC4bJqueNu5_n4dJR7wAtZJsaUUaItyZ_oz9IcagC0625ghZePK1aO09juXkQVLKj1_6yEUZbJ22fPVoEX2gFHr8ibtLmrXzaRwW_wCbOVg0XwLazrGxrDFy2pgdgJc=");

        SampleData data1 = new SampleData("4gagAB7_6SlDr-8ReDBwNuRqYGuZv5c_A1k6006BIm765hQ=");
        assertEquals(BigInteger.valueOf(6), bw.call("getHeight", null).asInteger());

        //assertEquals(6, mta.getHeight());
        //assertEquals(0, mta.getOffset());
        //blockWitness.verify(mta, data1.hash, data1.height);

        // Witness 7 last at Accumulator 8
        //SampleData data2 = new SampleData("4gegG7APaC4bJqueNu5_n4dJR7wAtZJsaUUaItyZ_oz9IcY=");
        //assertEquals(7, data2.height);
        //mta.add(data2.hash);
    }

}
