package foundation.icon.btp.bsh.test;

import com.iconloop.testsvc.Account;
import com.iconloop.testsvc.Score;
import com.iconloop.testsvc.ServiceManager;
import com.iconloop.testsvc.TestBase;
import foundation.icon.btp.bsh.HashMock;
import foundation.icon.btp.irc2.IRC2Basic;
import foundation.icon.ee.util.Strings;
import org.bouncycastle.util.encoders.Hex;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;

import static java.math.BigInteger.TEN;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class HashTest extends TestBase {

    private static final ServiceManager sm = getServiceManager();
    private static final Account owner = sm.createAccount();
    private static Score hashScore;
    @BeforeAll
    public static void setup() throws Exception {
        hashScore = sm.deploy(owner, HashMock.class);
    }

    @Test
    void transfer() {
        String msg="abc";
        byte[] m= msg.getBytes(StandardCharsets.UTF_8);
        byte[] res=(byte[])hashScore.call("check", m);
        System.out.println(res.toString());
        byte[] expected= Hex.decode("4e03657aea45a94fc7d47ba826c8d667c0d1e6e33a64a036ec44f58fa12d6c45");
        //assertEquals(res, "4018d299463629c8dd0474194fba047494b0a3608f3d27a57cf484fa743a5c53".getBytes(StandardCharsets.UTF_8));
        Assertions.assertArrayEquals(res,expected, "for message"+ Strings.hexFromBytes(m));
    }

    public byte[] hexToBytes(String hexString) {
        byte[] byteArray = new BigInteger(hexString, 16)
                .toByteArray();
        if (byteArray[0] == 0) {
            byte[] output = new byte[byteArray.length - 1];
            System.arraycopy(
                    byteArray, 1, output,
                    0, output.length);
            return output;
        }
        return byteArray;
    }
}
