package foundation.icon.lib.btp;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.TestMethodOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import foundation.icon.btp.lib.btpAddress.BTPAddress;

@TestMethodOrder(OrderAnnotation.class)
class BTPAddressTest {
    @Test
    @Order(1)
    public void initializeBTPAdress() {
        String protocol = "btp";
        String net = "0x1234.icon";
        String address = "hx3e9be7c57c769adb06dd0b4943aab9222c30d825";

        BTPAddress btpAddr = new BTPAddress(protocol, net, address);

        assertEquals(btpAddr.getNid(), "0x1234");
        assertEquals(btpAddr.getChain(), "icon");
        assertEquals(btpAddr.getProtocol(), protocol);
        assertEquals(btpAddr.getAddress(), address);
        assertEquals(btpAddr.getNet(), "0x1234.icon");
        assertEquals(btpAddr.toString(), "btp://0x1234.icon/hx3e9be7c57c769adb06dd0b4943aab9222c30d825");
    }

    @Test
    @Order(2)
    public void initializeFromString() {
        String protocol = "btp";
        String net = "0x1234.icon";
        String address = "hx3e9be7c57c769adb06dd0b4943aab9222c30d825";

        BTPAddress btpAddr = BTPAddress.fromString("btp://0x1234.icon/hx3e9be7c57c769adb06dd0b4943aab9222c30d825");

        assertEquals(btpAddr.getNid(), "0x1234");
        assertEquals(btpAddr.getChain(), "icon");
        assertEquals(btpAddr.getProtocol(), protocol);
        assertEquals(btpAddr.getAddress(), address);
        assertTrue(btpAddr.isValid());
        assertEquals(btpAddr.getNet(), "0x1234.icon");
        assertEquals(btpAddr.toString(), "btp://0x1234.icon/hx3e9be7c57c769adb06dd0b4943aab9222c30d825");
    }

    @Test
    @Order(2)
    public void testInvalidBTPAddress() {
        String protocol = "btp";
        String net = "0x1234.icon";
        String address = "hx3e9be7c57c769adb06dd0b4943aab9222c30d825";

        BTPAddress invalidBtpAddr1 = BTPAddress.fromString("btp://0x1234.icon");
        BTPAddress invalidBtpAddr2 = new BTPAddress(protocol, net, null);
        BTPAddress invalidBtpAddr3 = new BTPAddress(null, net, null);
        BTPAddress invalidBtpAddr4 = new BTPAddress(null, null, null);
        BTPAddress invalidBtpAddr5 = BTPAddress.fromString("btp");

        assertTrue(!invalidBtpAddr1.isValid());
        assertTrue(!invalidBtpAddr2.isValid());
        assertTrue(!invalidBtpAddr3.isValid());
        assertTrue(!invalidBtpAddr4.isValid());
        assertTrue(!invalidBtpAddr5.isValid());
    }
}