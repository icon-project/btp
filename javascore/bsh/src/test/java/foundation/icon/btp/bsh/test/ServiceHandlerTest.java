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

package foundation.icon.btp.bsh.test;

import com.iconloop.testsvc.Account;
import com.iconloop.testsvc.Score;
import com.iconloop.testsvc.ServiceManager;
import com.iconloop.testsvc.TestBase;
import foundation.icon.btp.bsh.BMCMock;
import foundation.icon.btp.bsh.ServiceHandler;
import foundation.icon.btp.bsh.types.Asset;
import foundation.icon.btp.bsh.types.Balance;
import foundation.icon.btp.bsh.types.TransferAsset;
import foundation.icon.btp.irc2.IRC2Basic;
import org.junit.jupiter.api.*;
import score.ByteArrayObjectWriter;
import score.Context;
import scorex.util.ArrayList;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static java.math.BigInteger.TEN;
import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
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
    private static Account[] owners;

    private static final ServiceManager sm = getServiceManager();
    private static Score bsh;
    private static Score token;
    private static Score bmc;
    private static Score irc2Basic;
    private ServiceHandler bshSyp;

    @BeforeAll
    public static void setup() throws Exception {
        // setup accounts and deploy
        owners = new Account[3];
        for (int i = 0; i < owners.length; i++) {
            owners[i] = sm.createAccount(100);
        }
        String initialOwners = Arrays.stream(owners)
                .map(a -> a.getAddress().toString())
                .collect(Collectors.joining(","));
        bmc = sm.deploy(owners[0], BMCMock.class);
        bsh = sm.deploy(owners[0], ServiceHandler.class, bmc.getAddress());
        token = sm.deploy(owners[0], IRC2Basic.class, tokenName, symbol, decimals, initialSupply);

        BigInteger balance = (BigInteger) token.call("balanceOf", owners[0].getAddress());
        assertEquals(totalSupply, balance);
        // setup spy
       /* bshSyp = (ServiceHandler) spy(bshScore.getInstance());
        bshScore.setInstance(bshSyp);*/
    }

    /**
     * Scenario 1: Receiving address is an invalid address - fail
     */
    @Order(1)
    @Test
    public void scenario1() {
        String _from = "0x12345678";
        String _to = "0x1234567890123456789";
        bmc.invoke(owners[0], "addService", _svc, bsh.getAddress());
        AssertionError thrown = assertThrows(AssertionError.class, () ->
                bmc.invoke(owners[0], "handleBTPMessage", _from, _svc, BigInteger.ZERO, handleBTPRequestBtpMsg(_from, _to))
        );
        assertTrue(thrown.getMessage().contains("Invalid Address format exception"));
    }

    /**
     * Scenario 2:  User creates a transfer, but a token_name has not yet registered - fail
     */
    @Order(2)
    @Test
    public void scenario2() {
        String _to = owners[0].getAddress().toString();
        AssertionError thrown = assertThrows(AssertionError.class, () ->
                bsh.invoke(owners[0], "transfer", tokenName, transferAmount, _to)
        );
        assertTrue(thrown.getMessage().contains("Token not registered"));

    }

    /**
     * Scenario 3:  Register Token without permission - fail
     */
    @Order(3)
    @Test
    public void scenario3() {
        String _to = "btp://bsc/0xa36a32c114ee13090e35cb086459a690f5c1f8e8";
        AssertionError thrown = assertThrows(AssertionError.class, () ->
                bsh.invoke(owners[1], "register", tokenName, symbol, BigInteger.valueOf(decimals), fees, token.getAddress()));
        assertTrue(thrown.getMessage().contains("No Permission"));
    }

    /**
     * Scenario 4:  Register Token with permission - Success
     */
    @Order(4)
    @Test
    public void scenario4() {
        String _to = "btp://bsc/0xa36a32c114ee13090e35cb086459a690f5c1f8e8";
        bsh.invoke(owners[0], "register", tokenName, symbol, BigInteger.valueOf(decimals), fees, token.getAddress());
    }


    /**
     * Scenario 5:  Register Token - Token already exists - Failure
     */
    @Order(5)
    @Test
    public void scenario5() {
        String _to = "btp://bsc/0xa36a32c114ee13090e35cb086459a690f5c1f8e8";
        AssertionError thrown = assertThrows(AssertionError.class, () ->
                bsh.invoke(owners[0], "register", tokenName, symbol, BigInteger.valueOf(decimals), fees, token.getAddress()));
        assertTrue(thrown.getMessage().contains("Token with same name exists already"));
    }


    /**
     * Scenario 6:  User does not have enough balance - fail
     */
    @Order(6)
    @Test
    public void scenario6() {
        String _to = "btp://bsc/0xa36a32c114ee13090e35cb086459a690f5c1f8e8";
        AssertionError thrown = assertThrows(AssertionError.class, () ->
                bsh.invoke(owners[0], "transfer", tokenName, transferAmount, _to)
        );
        assertTrue(thrown.getMessage().contains("Overdrawn"));

    }


    /**
     * Scenario #:  Invalid amount specified(transfer amount = 0) - fail
     */
    @Order(7)
    @Test
    public void scenario7() {
        String _to = "btp://bsc/0xa36a32c114ee13090e35cb086459a690f5c1f8e8";
        AssertionError thrown = assertThrows(AssertionError.class, () ->
                bsh.invoke(owners[0], "transfer", tokenName, BigInteger.ZERO, _to)
        );
        assertTrue(thrown.getMessage().contains("Invalid amount specified"));

    }

    /**
     * Secnario#: Transfer IRC2 tokens from Token contract to BSH via fallback - success
     */
    @Order(8)
    @Test
    public void scenario8() {
        token.invoke(owners[0], "transfer", bsh.getAddress(), transferAmount, new byte[0]);
    }


    /**
     * Scenario #:  User transfers to an invalid BTP address - fail
     */
    @Order(9)
    @Test
    public void scenario9() {
        String _to = "btp://0x1.bsc:0xa36a32c114ee13090e35cb086459a690f5c1f8e8";
        //bsh.invoke(owners[0],"transfer", tokenName, transferAmount,_to);
        assertThrows(AssertionError.class, () ->
                bsh.invoke(owners[0], "transfer", tokenName, transferAmount, _to)
        );
    }


    /**
     * Scenario #:   All requirements are qualified and BSH initiates Transfer start - Success
     */
    @Order(10)
    @Test
    public void scenario10() {
        String _to = "btp://0x1.bsc/0xa36a32c114ee13090e35cb086459a690f5c1f8e8";
        Balance balanceBefore = (Balance) bsh.call("getBalance", owners[0].getAddress(), tokenName);
        bsh.invoke(owners[0], "transfer", tokenName, transferAmount, _to);
        Balance balanceAfter = (Balance) bsh.call("getBalance", owners[0].getAddress(), tokenName);
        assertEquals(balanceBefore.getLocked().add(transferAmount), balanceAfter.getLocked());
    }


    /**
     * Scenario #:  All requirements are qualified and BSH receives a failed message - Success
     */
    @Order(11)
    @Test
    public void scenario11() {
        String _from = "0x1234567890123456789";
        Balance balanceBefore = (Balance) bsh.call("getBalance", owners[0].getAddress(), tokenName);
        bmc.invoke(owners[0], "handleBTPMessage", _from, _svc, BigInteger.ONE, handleBTPResponseBtpMsg(1, "Transfer Failed"));
        Balance balanceAfter = (Balance) bsh.call("getBalance", owners[0].getAddress(), tokenName);
        assertEquals(balanceAfter.getUsable(), balanceBefore.getUsable().add(transferAmount));
    }


    /**
     * Scenario #:  AAll requirements are qualified and BSH receives a successful message - Success
     */
    @Order(12)
    @Test
    public void scenario12() {
        String _from = "0x1234567890123456789";
        String _to = "btp://0x1.bsc/0xa36a32c114ee13090e35cb086459a690f5c1f8e8";
        Balance balanceBefore = (Balance) bsh.call("getBalance", owners[0].getAddress(), tokenName);
        bsh.invoke(owners[0], "transfer", tokenName, transferAmount, _to);
        Balance balanceAfter = (Balance) bsh.call("getBalance", owners[0].getAddress(), tokenName);
        assertEquals(balanceBefore.getLocked().add(transferAmount), balanceAfter.getLocked());
        balanceBefore = (Balance) bsh.call("getBalance", owners[0].getAddress(), tokenName);
        bmc.invoke(owners[0], "handleBTPMessage", _from, _svc, BigInteger.TWO, handleBTPResponseBtpMsg(0, "Transfer Failed"));
        balanceAfter = (Balance) bsh.call("getBalance", owners[0].getAddress(), tokenName);
        assertEquals(balanceAfter.getLocked().add(transferAmount), balanceBefore.getLocked());
    }


    /**
     * Scenario #:  All requirements are qualified handleBTPMessage mints balance for the user- Success
     */
    @Test
    @Order(13)
    public void scenario13() {
        String _from = "0x1234567890123456789";
        Balance balanceBefore = (Balance) bsh.call("getBalance", owners[0].getAddress(), tokenName);
        bmc.invoke(owners[0], "handleBTPMessage", _from, _svc, BigInteger.ZERO, handleBTPRequestBtpMsg(_from, owners[0].getAddress().toString()));
        Balance balanceAfter = (Balance) bsh.call("getBalance", owners[0].getAddress(), tokenName);
        assertEquals(balanceBefore.getUsable().add(transferAmount), balanceAfter.getUsable());
    }

    /**
     * scenario #: Add remove owner - min owner - should fail
     */
    @Test
    @Order(14)
    public void scenario14() {
        assertThrows(AssertionError.class, () ->
                bsh.invoke(owners[0], "removeOwner", owners[0].getAddress())
        );
    }

    /**
     * scenario #: Add add owner - without permission - should fail
     */
    @Test
    @Order(15)
    public void scenario15() {
        AssertionError thrown = assertThrows(AssertionError.class, () ->
                bsh.invoke(owners[1], "addOwner", owners[1].getAddress())
        );
        assertTrue(thrown.getMessage().contains("No Permission"));
    }

    /**
     * scenario #: Add add owner - with permission - success
     */
    @Test
    @Order(16)
    public void scenario16() {
        bsh.invoke(owners[0], "addOwner", owners[1].getAddress());
    }

    /**
     * Scenario #:  Register Token with permission new owner - fail
     */
    @Order(17)
    @Test
    public void scenario17() {
        String _to = "btp://bsc/0xa36a32c114ee13090e35cb086459a690f5c1f8e8";
        bsh.invoke(owners[1], "register", "BSC", "BSC", BigInteger.valueOf(decimals), fees, token.getAddress());
    }

    /**
     * Scenario #:  Handle Accumulated Fees - Failure
     */
    @Order(18)
    @Test
    public void scenario18() {
        String _fa = "btp://0x1.icon/cx87ed9048b594b95199f326fc76e76a9d33dd665b";
        List<Asset> _assets = (List<Asset>) bsh.call("getAccumulatedFees");
        BigInteger _fees = transferAmount.multiply(fees).divide(BigInteger.valueOf(100));
        assertEquals(_assets.get(0).getValue(), _fees);
        bmc.invoke(owners[1], "handleFeeGathering", _fa, _svc);
        String _from = "0x1234567890123456789";
        bmc.invoke(owners[0], "handleBTPMessage", _from, _svc, BigInteger.valueOf(3), handleBTPResponseBtpMsg(1, "Trasnfer Failed"));
        _assets = (List<Asset>) bsh.call("getAccumulatedFees");
        // should still have the fees in accumulator after failure handleresponse
        assertEquals(_assets.get(0).getValue(), _fees);
    }

    /**
     * Scenario #:  Handle Accumulated Fees - Success
     */
    @Order(19)
    @Test
    public void scenario19() {
        String _fa = "btp://0x1.icon/cx87ed9048b594b95199f326fc76e76a9d33dd665b";
        List<Asset> _assets = (List<Asset>) bsh.call("getAccumulatedFees");
        BigInteger _fees = transferAmount.multiply(fees).divide(BigInteger.valueOf(100));
        assertEquals(_assets.get(0).getValue(), _fees);
        bmc.invoke(owners[1], "handleFeeGathering", _fa, _svc);
        String _from = "0x1234567890123456789";
        bmc.invoke(owners[0], "handleBTPMessage", _from, _svc, BigInteger.valueOf(4), handleBTPResponseBtpMsg(0, "Trasnfer Success"));
        _assets = (List<Asset>) bsh.call("getAccumulatedFees");
        // Should not have any fees left in FeeAccumulator db
        assertEquals(_assets.size(), 0);
    }

    /*@Order(12)
    @Test
    public void scenario12(){
        String _from="0x1234567890123456789";
        String _to="btp://0x1.bsc/0xa36a32c114ee13090e35cb086459a690f5c1f8e8";
        byte[] req=handleBTPRequestBtpMsg(_from,_to);
        ObjectReader pmsgReader = Context.newByteArrayObjectReader(RLPn, req);
        pmsgReader.beginList();

        TransferAsset _asset= TransferAsset.readObject(pmsgReader);
        System.out.println(_asset.getFrom());
    }*/


    public byte[] handleBTPRequestBtpMsg(String from, String to) {
        ByteArrayObjectWriter writer = Context.newByteArrayObjectWriter(RLPn);
        writer.beginList(2);
        writer.write(REQUEST_TOKEN_TRANSFER);//ActionType
        List<Asset> assets = new ArrayList<Asset>();
        assets.add(new Asset(tokenName, transferAmount, BigInteger.ZERO));
        TransferAsset _ta = new TransferAsset(from, to, assets);
        TransferAsset.writeObject(writer, _ta);
        writer.end();
        return writer.toByteArray();
    }

    public byte[] handleBTPResponseBtpMsg(int code, String msg) {
        ByteArrayObjectWriter writer = Context.newByteArrayObjectWriter(RLPn);
        writer.beginList(3);
        writer.write(RESPONSE_HANDLE_SERVICE);//ActionType
        writer.write(code);//Code
        writer.write(msg);//Msg
        writer.end();
        return writer.toByteArray();
    }

}
