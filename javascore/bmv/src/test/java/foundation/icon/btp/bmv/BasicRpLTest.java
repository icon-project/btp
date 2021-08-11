package foundation.icon.btp.bmv;

import a.ByteArray;
import foundation.icon.btp.bmv.lib.HexConverter;
import foundation.icon.ee.io.RLPDataReader;
import i.IInstrumentation;
import org.bouncycastle.util.encoders.Hex;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pi.ObjectReaderImpl;
import testutils.TestInstrumentation;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

public class BasicRpLTest {

    final static String RLPCODEC = "RLPn";

    @BeforeEach
    public void setup() throws Exception {
        IInstrumentation.attachedThreadInstrumentation.set(new TestInstrumentation());
    }

    @AfterEach
    public void tearDown() throws Exception {
        IInstrumentation.attachedThreadInstrumentation.remove();
    }

    @Test
    public void testRplDecoder1() {
        byte[] data = Hex.decode("f87100f86eaa687831353039363863663733313134376661343033386635396265356630663236336632346263613832b8396274703a2f2f307839372e6273632f307862363837353030636537373939303630363339366333336137326232454537623545443936354246c7c6834554480500");
        ObjectReaderImpl r = new ObjectReaderImpl(new RLPDataReader(data));
        r.avm_beginList();
        int serviceType= r.avm_readInt();
        System.out.println(serviceType);
        System.out.println(r.avm_hasNext());
        r.avm_beginList();
        r.avm_hasNext();
       // r.avm_readByteArray();
        System.out.println(r.avm_readString());
        System.out.println(r.avm_readString());
        /*ByteArray msg= r.avm_readByteArray();
        byte[] _msg=msg.getUnderlying();
        System.out.println(Hex.toHexString(_msg));*/

    }

  /*  @Test
    public void testRplDecoder() {
        String data = "-EcF-ESgLRW8UPg-kGpGwRkzumLmCqfvZJ_97J7DYioRPCizgcH4AKAUevyJu0uatfNpHBb_AJs5WDRfAtrOsbGsMXLamB2Alw==";
        StringBuilder res = new StringBuilder();
        byte[] bytes = Base64.getUrlDecoder().decode(data);
        ObjectReaderImpl r = new ObjectReaderImpl(new RLPDataReader(bytes));
        r.avm_beginList();
        assertTrue(r.avm_hasNext());
        assertEquals(5, r.avm_readInt());
        assertTrue(r.avm_hasNext());
        r.avm_beginList();
        assertEquals("2d15bc50f83e906a46c11933ba62e60aa7ef649ffdec9ec3622a113c28b381c1",
                Hex.toHexString(r.avm_readByteArray().getUnderlying()));

        r.avm_skip(); // r.avm_readNullable fails compilation as it expects

        assertEquals("147afc89bb4b9ab5f3691c16ff009b3958345f02daceb1b1ac3172da981d80917",
                Hex.toHexString(r.avm_readByteArray().getUnderlying()));

        r.avm_end();
        assertFalse(r.avm_hasNext());
    }*/
}

