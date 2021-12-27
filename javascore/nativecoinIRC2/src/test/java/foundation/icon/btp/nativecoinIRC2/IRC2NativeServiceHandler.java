package foundation.icon.btp.nativecoinIRC2;


import com.iconloop.testsvc.Account;
import com.iconloop.testsvc.Score;
import com.iconloop.testsvc.ServiceManager;
import com.iconloop.testsvc.TestBase;
import foundation.icon.btp.nativecoinIRC2.irc2.IRC2Basic;
import org.junit.jupiter.api.*;
import score.ByteArrayObjectWriter;
import score.Context;
import scorex.util.ArrayList;

import java.math.BigInteger;
import java.util.List;

import static java.math.BigInteger.TEN;
import static org.junit.jupiter.api.Assertions.assertEquals;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class IRC2NativeServiceHandler extends TestBase {
    final static String RLPn = "RLPn";
    final static String _svc = "NativeCoinIRC2BSH";
    final static String _net = "icon";
    final static String tokenName = "MOVR";
    final static String symbol = "MOVR";
    final static int decimals = 18;
    final static BigInteger transferAmount = new BigInteger("10000000000000000000");
    private static final BigInteger initialSupply = BigInteger.valueOf(2000);
    private static final BigInteger totalSupply = initialSupply.multiply(TEN.pow(decimals));
    private static final int REQUEST_TOKEN_TRANSFER = 0;
    private static final int RESPONSE_HANDLE_SERVICE = 2;
    private static final ServiceManager sm = getServiceManager();
    private static Account[] owners;
    private static Score bsh;
    private static Score token;
    private static Score bmc;

    @BeforeAll
    public static void setup() throws Exception {
        // setup accounts and deploy
        owners = new Account[5];
        for (int i = 0; i < owners.length; i++) {
            owners[i] = sm.createAccount(100);
        }

        bmc = sm.deploy(owners[0], BMCMock.class, "0x03.icon");
        token = sm.deploy(owners[0], IRC2Basic.class, tokenName, symbol, decimals, initialSupply);
        bsh = sm.deploy(owners[0], NativeCoinService.class, bmc.getAddress(), token.getAddress(), "ICX", "MOVR");

        BigInteger balance = (BigInteger) token.call("balanceOf", owners[0].getAddress());
        assertEquals(totalSupply, balance);
        //fund user1 with tokens
        token.invoke(owners[0], "transfer", owners[1].getAddress(), transferAmount.multiply(BigInteger.valueOf(100)), new byte[0]);
    }


    /**
     * Secnario#: Transfer IRC2 tokens from Token contract to BSH via fallback - success
     */
    @Order(1)
    @Test
    public void scenario1() {
        String _to, _from;
        _from = _to = "btp://0x97.bsc/0xa36a32c114ee13090e35cb086459a690f5c1f8e8";

        bmc.invoke(owners[0], "addService", _svc, bsh.getAddress());
        //Add some enough token balance for BSH
        Balance balanceBefore = ((Balance) bsh.call("balanceOf", owners[0].getAddress(), tokenName));
        BigInteger bshTokenBalanceBefore = (BigInteger) token.call("balanceOf", bsh.getAddress());
        //TransferAmount multiplied by 10 to reclaim unused coins later
        token.invoke(owners[0], "transfer", bsh.getAddress(), transferAmount.multiply(BigInteger.valueOf(10)), new byte[0]);
        //Fund BSH with more tokens by another user (owners[1]) for future funds for reclaim & credit
        token.invoke(owners[1], "transfer", bsh.getAddress(), transferAmount.multiply(BigInteger.valueOf(100)), new byte[0]);
        BigInteger bshTokenBalanceAfter = (BigInteger) token.call("balanceOf", bsh.getAddress());

        //Initiate a transfer and check for locked balance
        bsh.invoke(owners[0], "transfer", tokenName, transferAmount, _to);
        Balance balanceAfterTransfer = ((Balance) bsh.call("balanceOf", owners[0].getAddress(), tokenName));
        assertEquals(balanceBefore.getLocked().add(transferAmount), balanceAfterTransfer.getLocked());

        //BSH receives HandleBTPMessgae for token transfer request
        BigInteger userBalanceBefore = (BigInteger) token.call("balanceOf", owners[1].getAddress());
        bmc.invoke(owners[0], "handleBTPMessage", _from, _svc, BigInteger.ZERO, handleBTPRequestBtpMsg(_from, owners[1].getAddress().toString()));
        BigInteger userBalanceAfter = (BigInteger) token.call("balanceOf", owners[1].getAddress());
        assertEquals(userBalanceBefore.add(transferAmount), userBalanceAfter);

        //Send Success response after successfull transfer
        Balance balanceBeforeSuccess = (Balance) bsh.call("balanceOf", owners[0].getAddress(), tokenName);
        bmc.invoke(owners[0], "handleBTPMessage", _from, _svc, BigInteger.ONE, handleBTPResponseBtpMsg(0, "Transfer Success"));
        Balance balanceAfterSuccess = (Balance) bsh.call("balanceOf", owners[0].getAddress(), tokenName);
        assertEquals(balanceAfterSuccess.getLocked().add(transferAmount), balanceBeforeSuccess.getLocked());

        //Reclaim unused available balance back to user
        BigInteger bshTokenBalanceNow = (BigInteger) token.call("balanceOf", bsh.getAddress());
        bsh.invoke(owners[0], "reclaim", tokenName, balanceAfterSuccess.getUsable());
        Balance balanceAfterReclaim = (Balance) bsh.call("balanceOf", owners[0].getAddress(), tokenName);
        assertEquals(balanceAfterReclaim.getUsable(), BigInteger.ZERO);
        assertEquals(balanceAfterReclaim.getLocked(), BigInteger.ZERO);
    }

    public byte[] handleBTPRequestBtpMsg(String from, String to) {
        ByteArrayObjectWriter writer = Context.newByteArrayObjectWriter(RLPn);
        writer.beginList(2);
        writer.write(REQUEST_TOKEN_TRANSFER);//ActionType
        List<Asset> assets = new ArrayList<Asset>();
        assets.add(new Asset(tokenName, transferAmount));
        ByteArrayObjectWriter writerTa = Context.newByteArrayObjectWriter(RLPn);
        TransferAsset _ta = new TransferAsset(from, to, assets);
        TransferAsset.writeObject(writerTa, _ta);
        writer.write(writerTa.toByteArray());
        writer.end();
        return writer.toByteArray();
    }

    public byte[] handleBTPResponseBtpMsg(int code, String msg) {
        ByteArrayObjectWriter writer = Context.newByteArrayObjectWriter(RLPn);
        writer.beginList(2);
        writer.write(RESPONSE_HANDLE_SERVICE);//ActionType
        ByteArrayObjectWriter writerRespMsg = Context.newByteArrayObjectWriter(RLPn);
        writerRespMsg.beginList(2);
        writerRespMsg.write(code);//Code
        writerRespMsg.write(msg);//Msg
        writerRespMsg.end();
        writer.write(writerRespMsg.toByteArray());
        writer.end();
        return writer.toByteArray();
    }
}
