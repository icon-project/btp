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


import foundation.icon.btp.bsh.ServiceHandler;
import foundation.icon.ee.io.DataWriter;
import foundation.icon.icx.*;
import foundation.icon.icx.data.Address;
import foundation.icon.icx.data.TransactionResult;
import foundation.icon.icx.transport.http.HttpProvider;
import foundation.icon.icx.transport.jsonrpc.RpcItem;
import foundation.icon.icx.transport.jsonrpc.RpcObject;
import foundation.icon.icx.transport.jsonrpc.RpcValue;
import foundation.icon.test.common.*;
import foundation.icon.test.score.Score;
import org.junit.jupiter.api.*;
import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;
import org.junit.jupiter.api.Test;
import static foundation.icon.test.common.Env.LOG;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag(Constants.TAG_JAVA_SCORE)
class ServiceHandlerTest extends TestBase {
    final static String RLPn = "RLPn";
    private static final Address ZERO_ADDRESS = new Address("hx0000000000000000000000000000000000000000");
    private static final int REQUEST_TOKEN_TRANSFER = 0;
    private static final int REQUEST_TOKEN_REGISTER = 1;
    private static final int RESPONSE_HANDLE_SERVICE = 2;
    private static final int RESPONSE_UNKNOWN_ = 3;
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
        BigInteger amount = ICX.multiply(BigInteger.valueOf(1000));
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

    public static Score deployServiceHandler(TransactionHandler txHandler, Wallet owner)
            throws ResultTimeoutException, TransactionFailureException, IOException {
        LOG.infoEntering("deploy", "BTPServiceHandler");

        RpcObject args = new RpcObject.Builder()
                .put("prefix", new RpcValue("btp"))
                .build();

        Score score = txHandler.deploy(owner,
                new Class[]{ServiceHandler.class,
                        scorex.util.ArrayList.class,
                        scorex.util.HashMap.class},
                null);

        LOG.info("Deployed BSH address " + score.getAddress());

        LOG.infoExiting();
        return score;
    }

    public void shouldDeploy() throws Exception {
        Score mtaScore = deployServiceHandler(txHandler, ownerWallet);
    }

    @Test
    public void shouldTransfer() throws TransactionFailureException, IOException, ResultTimeoutException {
        Score bmv = deployServiceHandler(txHandler, ownerWallet);
        Address address = ownerWallet.getAddress();
        //Step 1: register the token with BSH
        RpcObject args = new RpcObject.Builder()
                .put("name", new RpcValue("BNB"))
                .put("address", new RpcValue(address))
                .build();
        TransactionResult res = bmv.invokeAndWaitResult(ownerWallet, "register", args);

        // Verify the Token Registration
        RpcItem numberOfTokens = bmv.call("numberOfTokens", null);
        assertEquals(BigInteger.ONE, numberOfTokens.asInteger());

        //Step 2: Deposit some registered token into BSH
        RpcObject tokenFallbackArgs = new RpcObject.Builder()
                .put("from", new RpcValue(address))
                .put("value", new RpcValue(BigInteger.TEN))
                .build();
        TransactionResult tokenFallbackRes = bmv.invokeAndWaitResult(ownerWallet, "tokenFallback", tokenFallbackArgs);

        // Verify the Balance of the token
       /* RpcItem balanceOf = bmv.call("getBalanceWithIndex", new RpcObject.Builder()
                .put("user", new RpcValue(address))
                .put("tokenName", new RpcValue("BNB"))
                .put("index", new RpcValue(BigInteger.ZERO))
                .build());*/
        //assertEquals(BigInteger.TEN, balanceOf.asInteger());

        //Step 3: Call transfer method
        RpcObject transferArgs = new RpcObject.Builder()
                .put("tokenName", new RpcValue("BNB"))
                .put("to", new RpcValue("btp://bsc/0xa36a32c114ee13090e35cb086459a690f5c1f8e8"))
                .put("value", new RpcValue(BigInteger.TWO))
                .build();
        TransactionResult TransferRes = bmv.invokeAndWaitResult(ownerWallet, "transfer", transferArgs);

    }

    @Test
    public void shouldHandleBTPMessage() throws Exception {
        Score bmv = deployServiceHandler(txHandler, ownerWallet);
        DataWriter writer = foundation.icon.test.common.Codec.rlp.newWriter();
        writer.writeListHeader(2);
        writer.write(REQUEST_TOKEN_TRANSFER);//ActionType
        //Action Data writer -start
        writer.writeListHeader(4);
        writer.write("hx0000000000000000000000000000000000000001");
        writer.write("hx0000000000000000000000000000000000000001");
        writer.write("ICX");
        writer.write(100);
        writer.writeFooter();
        writer.writeFooter();
        byte[] write = writer.toByteArray();
        RpcObject args = new RpcObject.Builder()
                .put("from", new RpcValue("0x1.icon"))
                .put("svc", new RpcValue("token"))
                .put("sn", new RpcValue(BigInteger.valueOf(2)))
                .put("msg", new RpcValue(writer.toByteArray()))
                .build();

        TransactionResult res = bmv.invokeAndWaitResult(ownerWallet, "handleBTPMessage", args);
    }


}
