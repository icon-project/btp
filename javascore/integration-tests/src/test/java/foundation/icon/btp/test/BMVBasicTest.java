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
import foundation.icon.btp.bmv.lib.*;
import foundation.icon.btp.bmv.lib.mpt.*;
import foundation.icon.btp.bmv.lib.mta.MTAException;
import foundation.icon.btp.bmv.lib.mta.MerkleTreeAccumulator;
import foundation.icon.btp.bmv.types.BlockHeader;
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
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;

import static foundation.icon.test.common.Env.LOG;

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

    }

    private static final String bmc = "cxea19a7d6e9a926767d1d05eea467299fe461c0eb"; //address of the MOCKBMC from local node without prefix for now
    private static final String prevBMCAdd = "0xAaFc8EeaEE8d9C8bD3262CCE3D73E56DeE3FB776";
    private static final String currBMCNet = "0xf8aac3.icon";
    private static final String prevBMCnet = "0x97.bsc";
    //private static final String currBMCAdd = bmc;

    public static Score deployMessageVerifier(TransactionHandler txHandler, Wallet owner)
            throws ResultTimeoutException, TransactionFailureException, IOException {
        LOG.infoEntering("deploy", "BTPMessageVerifier");

        RpcObject args = new RpcObject.Builder()
                .put("bmc", new RpcValue(bmc))
                .put("network", new RpcValue(prevBMCnet)) //for sample
                .put("offset", new RpcValue(Integer.toString(0)))
                .put("rootSize", new RpcValue(Integer.toString(3)))
                .put("cacheSize", new RpcValue(Integer.toString(10)))
                .put("isAllowNewerWitness", new RpcValue(true))
                .build();

        Score score = txHandler.deploy(owner, new Class[]{
                        BTPMessageVerifier.class,
                        //Hex.class,
                        BTPAddress.class,
                        BTPMessage.class,
                        BlockHeader.class,
                        BlockProof.class,
                        BlockUpdate.class,
                        BlockWitness.class,
                        EventProof.class,
                        EventDataBTPMessage.class,
                        MTAException.class,
                        MPTException.class,
                        RelayMessage.class,
                        ReceiptProof.class,
                        Receipt.class,
                        ReceiptEventLog.class,
                        MerkleTreeAccumulator.class,
                        MerklePatriciaTree.class,
                        Votes.class,
                        foundation.icon.btp.bmv.types.ValidatorList.class,
                        TypeDecoder.class,
                        BMVStatus.class,
                        HexConverter.class,
                        scorex.util.Arrays.class,
                        scorex.util.ArrayList.class,
                        scorex.util.HashMap.class,
                        TrieNode.class,
                        Value.class,
                        scorex.util.Base64.class,
                        ArrayList.class,
                        scorex.util.AbstractCollection.class,
                        Nibbles.class,
                        ArraysUtil.class,
                        BytesUtil.class,
                        Pair.class,
                        Trie.class,
                        ExtraDataTypeDecoder.class},
                args);

        LOG.info("Deployed Message Verifier Address address " + score.getAddress());

        LOG.infoExiting();
        return score;
    }

    @Test
    public void shouldDeploy() throws Exception {
        Score mtaScore = deployMessageVerifier(txHandler, ownerWallet);
    }
}
