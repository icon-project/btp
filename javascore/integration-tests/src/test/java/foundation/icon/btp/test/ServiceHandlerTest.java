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


import foundation.icon.btp.bsh.BMCMock;
import foundation.icon.btp.bsh.ServiceHandler;
import foundation.icon.btp.bsh.types.*;
import foundation.icon.btp.irc2.IRC2;
import foundation.icon.btp.irc2.IRC2Basic;
import foundation.icon.ee.io.DataWriter;
import foundation.icon.icx.IconService;
import foundation.icon.icx.KeyWallet;
import foundation.icon.icx.Wallet;
import foundation.icon.icx.data.Bytes;
import foundation.icon.icx.data.TransactionResult;
import foundation.icon.icx.transport.http.HttpProvider;
import foundation.icon.icx.transport.jsonrpc.RpcArray;
import foundation.icon.icx.transport.jsonrpc.RpcItem;
import foundation.icon.icx.transport.jsonrpc.RpcObject;
import foundation.icon.icx.transport.jsonrpc.RpcValue;
import foundation.icon.test.common.*;
import foundation.icon.test.score.Score;
import org.junit.jupiter.api.*;
import scorex.util.ArrayList;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.List;

import static foundation.icon.test.common.Env.LOG;
import static java.math.BigInteger.TEN;
import static org.junit.jupiter.api.Assertions.assertTrue;


import static org.junit.jupiter.api.Assertions.assertThrows;

@Tag(Constants.TAG_JAVA_SCORE)
class ServiceHandlerTest extends TestBase {

    final static String RLPn = "RLPn";
    final static String _svc = "TokenBSH";
    final static String _net = "icon";
    final static String tokenName = "ETH";
    final static String symbol = "ETH";
    final static int decimals = 18;
    final static BigInteger fees = BigInteger.valueOf(1);
    final static BigInteger transferAmount = BigInteger.valueOf(100);
    private static final BigInteger initialSupply = BigInteger.valueOf(2000);
    private static final BigInteger totalSupply = initialSupply.multiply(TEN.pow(decimals));
    private static final int REQUEST_TOKEN_TRANSFER = 0;
    private static final int REQUEST_TOKEN_REGISTER = 1;
    private static final int RESPONSE_HANDLE_SERVICE = 2;
    private static final int RESPONSE_UNKNOWN_ = 3;
    private static Score bsh;
    private static Score token;
    private static Score bmc;
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
        wallets = new KeyWallet[3];
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
        bsh = deployServiceHandler(txHandler, ownerWallet);
        token = deployIRC2Token(txHandler, ownerWallet);
    }

    @AfterAll
    static void shutdown() throws Exception {
        for (KeyWallet wallet : wallets) {
            txHandler.refundAll(wallet);
        }
    }

    public static Score deployBMCMock(TransactionHandler txHandler, Wallet owner)
            throws ResultTimeoutException, TransactionFailureException, IOException {
        LOG.infoEntering("deploy", "deployBMCMock");
        RpcObject args = new RpcObject.Builder()
                .build();
        Score score = txHandler.deploy(owner, BMCMock.class,
                args);
        LOG.info("Deployed BMC address " + score.getAddress());
        LOG.infoExiting();
        return score;
    }

    public static Score deployServiceHandler(TransactionHandler txHandler, Wallet owner)
            throws ResultTimeoutException, TransactionFailureException, IOException {
        LOG.infoEntering("deploy", "BTPServiceHandler");
        bmc = deployBMCMock(txHandler, owner);
        RpcObject args = new RpcObject.Builder()
                .put("_bmc", new RpcValue(bmc.getAddress()))
                .build();
        Score score = txHandler.deploy(owner,
                new Class[]{
                        ServiceHandler.class,
                        Asset.class,
                        Balance.class,
                        TransferAsset.class,
                        Token.class,
                        BTPAddress.class,
                        scorex.util.ArrayList.class
                },
                args);
        LOG.info("Deployed BSH address " + score.getAddress());
        LOG.infoExiting();
        return score;
    }

    public static Score deployIRC2Token(TransactionHandler txHandler, Wallet owner)
            throws ResultTimeoutException, TransactionFailureException, IOException {
        LOG.infoEntering("deploy", "deployIRC2Token");

        RpcObject args = new RpcObject.Builder()
                .put("_name", new RpcValue(tokenName))
                .put("_symbol", new RpcValue(symbol))
                .put("_decimals", new RpcValue(String.valueOf(decimals)))
                .put("_initialSupply", new RpcValue(initialSupply))
                .build();

        Score score = txHandler.deploy(owner, new Class[]{IRC2Basic.class, IRC2.class},
                args);

        LOG.info("Deployed Token address " + score.getAddress());
        LOG.infoExiting();
        return score;
    }

    /**
     * Scenario 1: Receiving address is an invalid address - fail
     */
    @Order(1)
    @Test
    public void scenario1() throws IOException, ResultTimeoutException {
        String _from = "0x12345678";
        String _to = "0x1234567890123456789";
        RpcObject args = new RpcObject.Builder()
                .put("_svc", new RpcValue(_svc))
                .put("_addr", new RpcValue(bsh.getAddress()))
                .build();
        bmc.invokeAndWaitResult(wallets[0], "addService", args);
        args = new RpcObject.Builder()
                .put("from", new RpcValue(_from))
                .put("svc", new RpcValue(_svc))
                .put("sn", new RpcValue(BigInteger.ZERO))
                .put("msg", new RpcValue(handleBTPRequestBtpMsg(_from, _to)))
                .build();
        TransactionResult txResult = bmc.invokeAndWaitResult(wallets[0], "handleBTPMessage", args);
        assertTrue(txResult.getFailure().getMessage().equals("UnknownFailure"));
    }

    /**
     * Scenario 2:  User creates a transfer, but a token_name has not yet registered - fail
     */
    @Order(2)
    @Test
    public void scenario2() throws IOException, ResultTimeoutException {
        String _to = wallets[0].getAddress().toString();
        RpcObject args = new RpcObject.Builder()
                .put("tokenName", new RpcValue(tokenName))
                .put("value", new RpcValue(transferAmount))
                .put("to", new RpcValue(_to))
                .build();
        TransactionResult txResult = bsh.invokeAndWaitResult(wallets[0], "transfer", args);
        assertTrue(txResult.getFailure().getMessage().contains("Reverted(" + ErrorCodes.BSH_TOKEN_NOT_REGISTERED + ")"));

    }

    /**
     * Scenario 3:  Register Token without permission - fail
     */
    @Order(3)
    @Test
    public void scenario3() throws IOException, ResultTimeoutException {
        RpcObject args = new RpcObject.Builder()
                .put("name", new RpcValue(tokenName))
                .put("symbol", new RpcValue(symbol))
                .put("decimals", new RpcValue(BigInteger.valueOf(decimals)))
                .put("feeNumerator", new RpcValue(fees))
                .put("address", new RpcValue(token.getAddress()))
                .build();
        TransactionResult txResult = bsh.invokeAndWaitResult(wallets[1], "register", args);
        assertTrue(txResult.getFailure().getMessage().contains("Reverted(" + ErrorCodes.BSH_NO_PERMISSION + ")"));

    }

    /**
     * Scenario 4:  Register Token with permission - success
     */
    @Order(4)
    @Test
    public void scenario4() throws IOException, ResultTimeoutException {
        RpcObject args = new RpcObject.Builder()
                .put("name", new RpcValue(tokenName))
                .put("symbol", new RpcValue(symbol))
                .put("decimals", new RpcValue(BigInteger.valueOf(decimals)))
                .put("feeNumerator", new RpcValue(fees))
                .put("address", new RpcValue(token.getAddress()))
                .build();
        bsh.invokeAndWaitResult(wallets[0], "register", args);
        RpcItem res = bsh.call("tokenNames", null);
        RpcArray array = res.asArray();
        assertTrue(array.get(0).asString().equals(tokenName));
    }

    /**
     * Scenario 5:  Register Token - Token already exists - Failure
     */
    @Order(5)
    @Test
    public void scenario5() throws IOException, ResultTimeoutException {
        RpcObject args = new RpcObject.Builder()
                .put("name", new RpcValue(tokenName))
                .put("symbol", new RpcValue(symbol))
                .put("decimals", new RpcValue(BigInteger.valueOf(decimals)))
                .put("feeNumerator", new RpcValue(fees))
                .put("address", new RpcValue(token.getAddress()))
                .build();
        TransactionResult txResult = bsh.invokeAndWaitResult(wallets[0], "register", args);
        assertTrue(txResult.getFailure().getMessage().contains("Reverted(" + ErrorCodes.BSH_TOKEN_EXISTS + ")"));

    }

    /**
     * Scenario 6:  User does not have enough balance - fail
     */
    @Order(6)
    @Test
    public void scenario6() throws IOException, ResultTimeoutException {
        String _to = "btp://bsc/0xa36a32c114ee13090e35cb086459a690f5c1f8e8";
        RpcObject args = new RpcObject.Builder()
                .put("tokenName", new RpcValue(tokenName))
                .put("value", new RpcValue(transferAmount))
                .put("to", new RpcValue(_to))
                .build();
        TransactionResult txResult = bsh.invokeAndWaitResult(wallets[0], "transfer", args);
        assertTrue(txResult.getFailure().getMessage().contains("Reverted(" + ErrorCodes.BSH_OVERDRAWN + ")"));

    }

    /**
     * Scenario #:  Invalid amount specified(transfer amount = 0) - fail
     */
    @Order(7)
    @Test
    public void scenario7() throws IOException, ResultTimeoutException {
        String _to = "btp://bsc/0xa36a32c114ee13090e35cb086459a690f5c1f8e8";
        RpcObject args = new RpcObject.Builder()
                .put("tokenName", new RpcValue(tokenName))
                .put("value", new RpcValue(BigInteger.ZERO))
                .put("to", new RpcValue(_to))
                .build();
        TransactionResult txResult = bsh.invokeAndWaitResult(wallets[0], "transfer", args);
        assertTrue(txResult.getFailure().getMessage().contains("Reverted(" + ErrorCodes.BSH_INVALID_AMOUNT + ")"));
    }

    /**
     * Secnario#: Transfer IRC2 tokens from Token contract to BSH via fallback - success
     */
    @Order(8)
    @Test
    public void scenario8() throws IOException, ResultTimeoutException {
        String _to = "btp://bsc/0xa36a32c114ee13090e35cb086459a690f5c1f8e8";
        RpcObject args = new RpcObject.Builder()
                .put("_to", new RpcValue(bsh.getAddress()))
                .put("_value", new RpcValue(transferAmount))
                .put("_data", new RpcValue(new byte[0]))
                .build();
        TransactionResult txResult = token.invokeAndWaitResult(wallets[0], "transfer", args);
        args = new RpcObject.Builder()
                .put("user", new RpcValue(wallets[0].getAddress()))
                .put("tokenName", new RpcValue(tokenName))
                .build();
        RpcObject res = bsh.call("getBalance", args).asObject();
        RpcValue value = res.getItem("usable").asValue();
        assertTrue(convertHex(value).equals(transferAmount));
    }

    /**
     * Scenario #:  User transfers to an invalid BTP address - fail
     */
    @Order(9)
    @Test
    public void scenario9() throws IOException, ResultTimeoutException {
        String _to = "btp://bsc:0xa36a32c114ee13090e35cb086459a690f5c1f8e8";
        RpcObject args = new RpcObject.Builder()
                .put("tokenName", new RpcValue(tokenName))
                .put("value", new RpcValue(transferAmount))
                .put("to", new RpcValue(_to))
                .build();
        TransactionResult txResult = bsh.invokeAndWaitResult(wallets[0], "transfer", args);
        assertTrue(txResult.getFailure().getMessage().contains("UnknownFailure"));
    }

    /**
     * Scenario #:   All requirements are qualified and BSH initiates Transfer start - Success
     */
    @Order(10)
    @Test
    public void scenario10() throws IOException, ResultTimeoutException {
        String _to = "btp://0x1.bsc/0xa36a32c114ee13090e35cb086459a690f5c1f8e8";
        RpcObject getBalanceArgs = new RpcObject.Builder()
                .put("user", new RpcValue(wallets[0].getAddress()))
                .put("tokenName", new RpcValue(tokenName))
                .build();
        RpcObject res = bsh.call("getBalance", getBalanceArgs).asObject();
        RpcValue lockedBefore = res.getItem("locked").asValue();
        RpcObject args = new RpcObject.Builder()
                .put("tokenName", new RpcValue(tokenName))
                .put("value", new RpcValue(transferAmount))
                .put("to", new RpcValue(_to))
                .build();
        TransactionResult txResult = bsh.invokeAndWaitResult(wallets[0], "transfer", args);
        assertTrue(txResult.getFailure() == null);
        res = bsh.call("getBalance", getBalanceArgs).asObject();
        RpcValue lockedAfter = res.getItem("locked").asValue();
        assertTrue(convertHex(lockedAfter).equals(convertHex(lockedBefore).add(transferAmount)));
    }

    /**
     * Scenario #:  All requirements are qualified and BSH receives a failed message with invalid serial number - Failed
     */
    @Order(11)
    @Test
    public void scenario11() throws IOException, ResultTimeoutException {
        String _from = "0x1234567890123456789";
        RpcObject args = new RpcObject.Builder()
                .put("from", new RpcValue(_from))
                .put("svc", new RpcValue(_svc))
                .put("sn", new RpcValue(BigInteger.ZERO))
                .put("msg", new RpcValue(handleBTPResponseBtpMsg(1, "Transfer Failed")))
                .build();
        TransactionResult txResult = bmc.invokeAndWaitResult(wallets[0], "handleBTPMessage", args);
        assertTrue(txResult.getFailure().getMessage().contains("UnknownFailure"));
    }

    /**
     * Scenario #:  All requirements are qualified and BSH receives a failed message - Success
     */
    @Order(12)
    @Test
    public void scenario12() throws IOException, ResultTimeoutException {
        String _from = "0x1234567890123456789";
        RpcObject getBalanceArgs = new RpcObject.Builder()
                .put("user", new RpcValue(wallets[0].getAddress()))
                .put("tokenName", new RpcValue(tokenName))
                .build();
        RpcObject res = bsh.call("getBalance", getBalanceArgs).asObject();
        RpcValue usableBefore = res.getItem("usable").asValue();
        RpcObject args = new RpcObject.Builder()
                .put("from", new RpcValue(_from))
                .put("svc", new RpcValue(_svc))
                .put("sn", new RpcValue(BigInteger.ONE))
                .put("msg", new RpcValue(handleBTPResponseBtpMsg(1, "Transfer Failed")))
                .build();
        TransactionResult txResult = bmc.invokeAndWaitResult(wallets[0], "handleBTPMessage", args);
        assertTrue(txResult.getFailure() == null);
        res = bsh.call("getBalance", getBalanceArgs).asObject();
        RpcValue usableAfter = res.getItem("usable").asValue();
        assertTrue(convertHex(usableAfter).equals(convertHex(usableBefore).add(transferAmount)));
    }


    /**
     * Scenario #:  AAll requirements are qualified and BSH receives a successful message - Success
     */
    @Order(13)
    @Test
    public void scenario13() throws IOException, ResultTimeoutException {
        String _from = "0x1234567890123456789";
        String _to = "btp://0x1.bsc/0xa36a32c114ee13090e35cb086459a690f5c1f8e8";
        RpcObject getBalanceArgs = new RpcObject.Builder()
                .put("user", new RpcValue(wallets[0].getAddress()))
                .put("tokenName", new RpcValue(tokenName))
                .build();
        RpcObject res = bsh.call("getBalance", getBalanceArgs).asObject();
        RpcValue lockedBefore = res.getItem("locked").asValue();
        RpcObject args = new RpcObject.Builder()
                .put("tokenName", new RpcValue(tokenName))
                .put("value", new RpcValue(transferAmount))
                .put("to", new RpcValue(_to))
                .build();
        bsh.invokeAndWaitResult(wallets[0], "transfer", args);
        res = bsh.call("getBalance", getBalanceArgs).asObject();
        RpcValue lockedAfter = res.getItem("locked").asValue();
        assertTrue(convertHex(lockedBefore).add(transferAmount).equals(convertHex(lockedAfter)));
        lockedBefore = res.getItem("locked").asValue();
        args = new RpcObject.Builder()
                .put("from", new RpcValue(_from))
                .put("svc", new RpcValue(_svc))
                .put("sn", new RpcValue(BigInteger.TWO))
                .put("msg", new RpcValue(handleBTPResponseBtpMsg(0, "Transfer Success")))
                .build();
        TransactionResult txResult = bmc.invokeAndWaitResult(wallets[0], "handleBTPMessage", args);
        assertTrue(txResult.getFailure() == null);
        res = bsh.call("getBalance", getBalanceArgs).asObject();
        lockedAfter = res.getItem("locked").asValue();
        assertTrue(convertHex(lockedBefore).subtract(transferAmount).equals(convertHex(lockedAfter)));
    }


    /**
     * Scenario #:  All requirements are qualified handleBTPMessage mints balance for the user- Success
     */
    @Test
    @Order(14)
    public void scenario14() throws IOException, ResultTimeoutException {
        String _from = "0x1234567890123456789";
      /*  RpcObject getBalanceArgs = new RpcObject.Builder()
                .put("user", new RpcValue(wallets[0].getAddress()))
                .put("tokenName", new RpcValue(tokenName))
                .build();
        RpcObject res = bsh.call("getBalance", getBalanceArgs).asObject();*/
        RpcObject getBalanceArgs = new RpcObject.Builder()
                .put("_owner", new RpcValue(wallets[0].getAddress()))
                .build();
        BigInteger balanceBefore = token.call("balanceOf", getBalanceArgs).asInteger();
        RpcObject args = new RpcObject.Builder()
                .put("from", new RpcValue(_from))
                .put("svc", new RpcValue(_svc))
                .put("sn", new RpcValue(BigInteger.ZERO))
                .put("msg", new RpcValue(handleBTPRequestBtpMsg(_from, wallets[0].getAddress().toString())))
                .build();
        TransactionResult txResult = bmc.invokeAndWaitResult(wallets[0], "handleBTPMessage", args);
        assertTrue(txResult.getFailure() == null);
        /*res = bsh.call("getBalance", getBalanceArgs).asObject();
        RpcValue usableAfter = res.getItem("usable").asValue();*/
        BigInteger balanceAfter = token.call("balanceOf", getBalanceArgs).asInteger();
        assertTrue(balanceAfter.equals(balanceBefore.add(transferAmount)));
    }

    /**
     * scenario #: Add remove owner - min owner - should fail
     */
    @Test
    @Order(15)
    public void scenario15() throws IOException, ResultTimeoutException {
        RpcObject args = new RpcObject.Builder()
                .put("address", new RpcValue(wallets[0].getAddress()))
                .build();
        TransactionResult txResult = bsh.invokeAndWaitResult(wallets[0], "removeOwner", args);
        assertTrue(txResult.getFailure().getMessage().contains("Reverted(0)"));
    }

    /**
     * scenario #: Add add owner - without permission - should fail
     */
    @Test
    @Order(16)
    public void scenario16() throws IOException, ResultTimeoutException {
        RpcObject args = new RpcObject.Builder()
                .put("address", new RpcValue(wallets[1].getAddress()))
                .build();
        TransactionResult txResult = bsh.invokeAndWaitResult(wallets[1], "addOwner", args);
        assertTrue(txResult.getFailure().getMessage().contains("Reverted(" + ErrorCodes.BSH_NO_PERMISSION + ")"));
    }

    /**
     * scenario #: Add add owner - with permission - success
     */
    @Test
    @Order(17)
    public void scenario17() throws IOException, ResultTimeoutException {
        RpcObject args = new RpcObject.Builder()
                .put("address", new RpcValue(wallets[1].getAddress()))
                .build();
        TransactionResult txResult = bsh.invokeAndWaitResult(wallets[0], "addOwner", args);
        assertTrue(txResult.getFailure() == null);
    }


    /**
     * Scenario #:   Register Token with permission new owner - Success
     */
    @Order(18)
    @Test
    public void scenario18() throws IOException, ResultTimeoutException {
        RpcObject args = new RpcObject.Builder()
                .put("name", new RpcValue("BNB"))
                .put("symbol", new RpcValue("BNB"))
                .put("decimals", new RpcValue(BigInteger.valueOf(decimals)))
                .put("feeNumerator", new RpcValue(fees))
                .put("address", new RpcValue(token.getAddress()))
                .build();
        TransactionResult txResult = bsh.invokeAndWaitResult(wallets[1], "register", args);
        assertTrue(txResult.getFailure() == null);

    }

    /**
     * Scenario #:  Handle Accumulated Fees - Failure
     */
    @Order(19)
    @Test
    public void scenario19() throws IOException, ResultTimeoutException {
        String _fa = "btp://0x1.icon/cx87ed9048b594b95199f326fc76e76a9d33dd665b";
        BigInteger _fees = transferAmount.multiply(fees).divide(BigInteger.valueOf(100));
        RpcArray res = bsh.call("getAccumulatedFees", null).asArray();
        List<RpcItem> resArray = res.asList();
        BigInteger feesFA = convertHex(resArray.get(0).asObject().getItem("ETH").asValue());
        assertTrue(_fees.equals(feesFA));

        RpcObject args = new RpcObject.Builder()
                .put("_fa", new RpcValue(_fa))
                .put("_svc", new RpcValue(_svc))
                .build();
        TransactionResult txResult = bmc.invokeAndWaitResult(wallets[1], "handleFeeGathering", args);
        assertTrue(txResult.getFailure() == null);

        String _from = "0x1234567890123456789";
        args = new RpcObject.Builder()
                .put("from", new RpcValue(_from))
                .put("svc", new RpcValue(_svc))
                .put("sn", new RpcValue(BigInteger.valueOf(3)))
                .put("msg", new RpcValue(handleBTPResponseBtpMsg(1, "Transfer Failed")))
                .build();
        txResult = bmc.invokeAndWaitResult(wallets[0], "handleBTPMessage", args);
        assertTrue(txResult.getFailure() == null);

        res = bsh.call("getAccumulatedFees", null).asArray();
        resArray = res.asList();
        feesFA = convertHex(resArray.get(0).asObject().getItem("ETH").asValue());
        assertTrue(_fees.equals(feesFA));

    }


    /**
     * Scenario #:  Handle Accumulated Fees - Failure
     */
    @Order(20)
    @Test
    public void scenario20() throws IOException, ResultTimeoutException {
        String _fa = "btp://0x1.icon/cx87ed9048b594b95199f326fc76e76a9d33dd665b";
        BigInteger _fees = transferAmount.multiply(fees).divide(BigInteger.valueOf(100));
        RpcArray res = bsh.call("getAccumulatedFees", null).asArray();
        List<RpcItem> resArray = res.asList();
        BigInteger feesFA = convertHex(resArray.get(0).asObject().getItem("ETH").asValue());
        assertTrue(_fees.equals(feesFA));

        RpcObject args = new RpcObject.Builder()
                .put("_fa", new RpcValue(_fa))
                .put("_svc", new RpcValue(_svc))
                .build();
        TransactionResult txResult = bmc.invokeAndWaitResult(wallets[1], "handleFeeGathering", args);
        assertTrue(txResult.getFailure() == null);

        String _from = "0x1234567890123456789";
        args = new RpcObject.Builder()
                .put("from", new RpcValue(_from))
                .put("svc", new RpcValue(_svc))
                .put("sn", new RpcValue(BigInteger.valueOf(4)))
                .put("msg", new RpcValue(handleBTPResponseBtpMsg(0, "Transfer Success")))
                .build();
        txResult = bmc.invokeAndWaitResult(wallets[0], "handleBTPMessage", args);
        assertTrue(txResult.getFailure() == null);

        res = bsh.call("getAccumulatedFees", null).asArray();
        resArray = res.asList();
        assertTrue(resArray.size() == 0);

    }

    private BigInteger convertHex(RpcValue value) {
        // The value of 'value' and nonce in v2 specs is a decimal string.
        // But there are hex strings without 0x in v2 blocks.
        //
        // This method converts the value as hex no matter it has  0x prefix or not.

        String stringValue = value.asString();
        String sign = "";
        if (stringValue.charAt(0) == '-') {
            sign = stringValue.substring(0, 1);
            stringValue = stringValue.substring(1);
        }
        return new BigInteger(sign + Bytes.cleanHexPrefix(stringValue), 16);
    }


    public byte[] handleBTPRequestBtpMsg(String from, String to) {
        DataWriter writer = foundation.icon.test.common.Codec.rlp.newWriter();
        writer.writeListHeader(2);
        writer.write(REQUEST_TOKEN_TRANSFER);//ActionType
        //Action Data writer -start
        List<Asset> assets = new ArrayList<Asset>();
        assets.add(new Asset(tokenName, transferAmount, BigInteger.ZERO));

        writer.writeListHeader(3);
        writer.write(from);
        writer.write(to);
        writer.writeListHeader(assets.size());
        for (int i = 0; i < assets.size(); i++) {
            writer.writeListHeader(3);
            Asset _asset = assets.get(0);
            writer.write(_asset.getName());
            writer.write(_asset.getValue());
            writer.write(_asset.getFee());
            writer.writeFooter();
        }
        writer.writeFooter();
        writer.writeFooter();
        writer.writeFooter();
        return writer.toByteArray();
    }

    public byte[] handleBTPResponseBtpMsg(int code, String msg) {
        DataWriter writer = foundation.icon.test.common.Codec.rlp.newWriter();
        writer.writeListHeader(3);
        writer.write(RESPONSE_HANDLE_SERVICE);//ActionType
        writer.write(code);//Code
        writer.write(msg);//Msg
        writer.writeFooter();
        return writer.toByteArray();
    }

}
