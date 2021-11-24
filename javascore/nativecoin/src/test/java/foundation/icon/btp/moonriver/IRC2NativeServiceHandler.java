package foundation.icon.btp.moonriver;


import com.iconloop.testsvc.Account;
import com.iconloop.testsvc.Score;
import com.iconloop.testsvc.ServiceManager;
import com.iconloop.testsvc.TestBase;
import foundation.icon.btp.moonriver.irc2.IRC2Basic;
import org.junit.jupiter.api.*;
import score.ByteArrayObjectWriter;
import score.Context;
import scorex.util.ArrayList;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.math.BigInteger.TEN;
import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class IRC2NativeServiceHandler extends TestBase {
        final static String RLPn = "RLPn";
        final static String _svc = "TokenBSH";
        final static String _net = "icon";
        final static String tokenName = "MOVR";
        final static String symbol = "MOVR";
        final static int decimals = 18;
        final static BigInteger fees = BigInteger.valueOf(100);
        final static BigInteger transferAmount = new BigInteger("10000000000000000000");
        private static final BigInteger initialSupply = BigInteger.valueOf(2000);
        private static final BigInteger totalSupply = initialSupply.multiply(TEN.pow(decimals));
        private static final int REQUEST_TOKEN_TRANSFER = 0;
        private static final int REQUEST_TOKEN_REGISTER = 1;
        private static final int RESPONSE_HANDLE_SERVICE = 2;
        private static final int RESPONSE_UNKNOWN_ = 3;
        private static final ServiceManager sm = getServiceManager();
        private static Account[] owners;
        private static Score bsh;
        private static Score token;
        private static Score bmc;
        private static Score irc2Basic;
        private NativeCoinService bshSyp;

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
            bmc = sm.deploy(owners[0], BMCMock.class,"0x03.icon");

            token = sm.deploy(owners[0], IRC2Basic.class, tokenName, symbol, decimals, initialSupply);
            bsh = sm.deploy(owners[0], NativeCoinService.class, bmc.getAddress(),token.getAddress(),"ICX","MOVR");
            BigInteger balance = (BigInteger) token.call("balanceOf", owners[0].getAddress());
            assertEquals(totalSupply, balance);
        }


        /**
         * Secnario#: Transfer IRC2 tokens from Token contract to BSH via fallback - success
         */
        @Order(1)
        @Test
        public void scenario1() {
            bmc.invoke(owners[0], "addService", _svc, bsh.getAddress());
            token.invoke(owners[0], "transfer", bsh.getAddress(), transferAmount, new byte[0]);
            String _to = "btp://0x1.bsc/0xa36a32c114ee13090e35cb086459a690f5c1f8e8";
            Balance balanceBefore = (Balance) bsh.call("balanceOf", owners[0].getAddress(), tokenName);
            bsh.invoke(owners[0], "transfer", tokenName, transferAmount, _to);
            Balance balanceAfter = (Balance) bsh.call("balanceOf", owners[0].getAddress(), tokenName);
            assertEquals(balanceBefore.getLocked().add(transferAmount), balanceAfter.getLocked());
        }

}
