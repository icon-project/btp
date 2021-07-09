package foundation.icon.btp.bmv;

import foundation.icon.btp.bmv.lib.HexConverter;
import foundation.icon.btp.bmv.lib.mta.MTAException;
import foundation.icon.btp.bmv.lib.mta.MerkleTreeAccumulator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.*;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.spy;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MTATest {
    final static byte[] EMPTY_NODE = new byte[]{};
    private static MerkleTreeAccumulator mta;
    private static long height = 8;
    private static List<byte[]> roots;
    private static long offset = 8;
    private static int rootSize = 3;
    private static int cacheSize = 10;
    private static List<byte[]> caches;
    private static byte[] lastBlockHash = HexConverter.hexStringToByteArray("fb147d7417db613d12685b9affdf6dd06bcfbba78a477a7fc9a79a21075a776a");
    private static boolean isAllowNewerWitness = true;
    String[] data = {"dog", "cat", "elephant", "bird", "monkey", "lion", "tiger"};

    @BeforeAll
    public static void setup() {
        mta = spy(new MerkleTreeAccumulator(
                height,
                offset,
                rootSize,
                cacheSize,
                isAllowNewerWitness,
                lastBlockHash,
                roots,
                caches
        ));
    }

    @Test
    @Order(1)
    public void setOffset() {
        offset = 10;
        mta.setOffset(offset);
        assertEquals(mta.getOffset(), offset);
        assertEquals(mta.getHeight(), offset);
        height = 10;
    }


    @Test
    @Order(2)
    public void basicTest() {
        int count = 1;
        byte[] hash;
        for (String _data : data) {
            hash = this.getHash(_data.getBytes());
            mta.add(hash);
            if (count % 2 == 1) {
                assertArrayEquals(mta.getRoot(0), hash);
            } else {
                assertArrayEquals(mta.getRoot(0), EMPTY_NODE);
            }
            count++;
        }
    }


    @Test
    @Order(3)
    public void verifyWithHightEqualToAt() throws MTAException {

        byte[] hash1 = this.getHash(data[0].getBytes());
        byte[] hash2 = this.getHash(data[1].getBytes());
        byte[] hash3 = this.getHash(data[2].getBytes());
        byte[] hash4 = this.getHash(data[3].getBytes());
        byte[] addHash34 = new byte[64];
        concat(addHash34, hash3, hash4);
        byte[] hash34 = this.getHash(addHash34);

        List<byte[]> listWitness = new ArrayList<byte[]>(2);
        listWitness.add(hash1);
        listWitness.add(hash34);
        // prove cat in tree
        mta.verify(listWitness, hash2, 12, 17);
    }

    @Test
    @Order(4)
    public void addNewDataWhenRootFull() {
        byte[] hash8 = this.getHash("extra".getBytes());
        byte[] hashItem7 = mta.getRoot(0);
        byte[] hash56 = mta.getRoot(1);
        byte[] addHash78 = new byte[64];
        concat(addHash78, hashItem7, hash8);
        byte[] hash78 = this.getHash(addHash78);
        byte[] addHash5678 = new byte[64];
        concat(addHash5678, hash56, hash78);
        byte[] hash5678 = this.getHash(addHash5678);
        mta.add(hash8);

        assertEquals(mta.getHeight(), height + 8);
        assertEquals(mta.getOffset(), offset + 4);
        assertArrayEquals(mta.getRoot(0), EMPTY_NODE);
        assertArrayEquals(mta.getRoot(1), EMPTY_NODE);
        assertArrayEquals(mta.getRoot(2), hash5678);
    }

    @Test
    @Order(6)
    public void verifyAfterNewAdd() throws MTAException {
        byte[] hash6 = this.getHash(data[5].getBytes());
        byte[] hash5 = this.getHash(data[4].getBytes());

        byte[] hash8 = this.getHash("extra".getBytes());
        byte[] hash7 = this.getHash(data[6].getBytes());
        byte[] addHash78 = new byte[64];
        concat(addHash78, hash7, hash8);
        byte[] hash78 = this.getHash(addHash78);

        List<byte[]> witness = new ArrayList<byte[]>(2);
        witness.add(hash5);
        witness.add(hash78);
        // prove data6=tiger in tree
        mta.verify(witness, hash6, 16, 18);
    }


    //TODO: THis should not fail?
    @Test
    @Order(7)
    public void verifyWithHeightLessThanAt() throws MTAException {
        byte[] hash6 = this.getHash(data[5].getBytes());
        byte[] hash5 = this.getHash(data[4].getBytes());

        byte[] hash8 = this.getHash("extra".getBytes());
        byte[] hash7 = this.getHash(data[6].getBytes());
        byte[] addHash78 = new byte[64];
        concat(addHash78, hash7, hash8);
        byte[] hash78 = this.getHash(addHash78);

        List<byte[]> witness = new ArrayList<byte[]>(2);
        witness.add(hash5);
        witness.add(hash78);
        // prove data6=tiger in tree
        mta.verify(witness, hash6, 16, 21);
    }

    @Test
    @Order(8)
    public void verifyFailedWrongBlockHeight() {
        byte[] hash6 = this.getHash(data[5].getBytes());
        byte[] hash5 = this.getHash(data[4].getBytes());

        byte[] hash8 = this.getHash("extra".getBytes());
        byte[] hash7 = this.getHash(data[6].getBytes());
        byte[] addHash78 = new byte[64];
        concat(addHash78, hash7, hash8);
        byte[] hash78 = this.getHash(addHash78);

        List<byte[]> witness = new ArrayList<byte[]>(2);
        witness.add(hash5);
        witness.add(hash78);
        // prove data6=tiger in tree
        MTAException thrown = assertThrows(MTAException.class, () -> mta.verify(witness, hash6, 17, 18));
        assertTrue(thrown.getMessage().contains("Verification failed: Invalid Witness"));
    }

    @Test
    @Order(9)
    public void verifyInvalidWitness() {
        byte[] hash6 = this.getHash(data[5].getBytes());
        byte[] hash5 = this.getHash(data[4].getBytes());

        byte[] hash8 = this.getHash("extra".getBytes());
        byte[] hash7 = this.getHash(data[6].getBytes());
        byte[] addHash78 = new byte[64];
        concat(addHash78, hash7, hash8);
        byte[] hash78 = this.getHash(addHash78);

        List<byte[]> witness = new ArrayList<byte[]>(2);
        witness.add(0, hash5);
        witness.add(1, hash78);

        // modified witness
        witness.get(1)[30] = 10;
        witness.get(1)[31] = 123;

        List<byte[]> witnessLst = new ArrayList<byte[]>(2);
        witnessLst.add(witness.get(0));
        witnessLst.add(witness.get(1));
        // prove data6=tiger in tree
        //invalid witness
        MTAException thrown = assertThrows(MTAException.class, () -> mta.verify(witness, hash6, 16, 18));
        assertTrue(thrown.getMessage().contains("Verification failed: Invalid Witness"));
    }

    //Throw given witness for newer node
    @Test
    @Order(9)
    public void verifyInvalidBlockHeightAt() throws MTAException {
        byte[] hash8 = this.getHash("extra".getBytes());
        List<byte[]> witness = new ArrayList<byte[]>(2);
        MTAException thrown = assertThrows(MTAException.class, () -> mta.verify(witness, hash8, 19, 19));
        assertTrue(thrown.getMessage().contains("Verification failed: Given witness for newer node"));
    }

    @Test
    @Order(10)
    public void verifyWithCache() throws MTAException {
        byte[] hash5 = this.getHash(data[4].getBytes());
        byte[] hash6 = this.getHash(data[5].getBytes());
        List<byte[]> witness = new ArrayList<byte[]>(2);
        witness.add(hash5);
        mta.verify(witness, hash6, 16, 17);
    }

    @Test
    @Order(9)
    public void verifyCannotAllowCacheWitness() throws MTAException {
        byte[] hash8 = this.getHash("extra".getBytes());
        List<byte[]> witness = new ArrayList<byte[]>(2);
        MTAException thrown = assertThrows(MTAException.class, () -> mta.verify(witness, hash8, 5, 17));
        assertTrue(thrown.getMessage().contains("Verification failed: Cannot allow old cached witness"));
    }

    public byte[] getHash(byte[] data) {
        return mta.getHash(data);
    }

    public byte[] concat(byte[] concatenation, byte[] item1, byte[] item2) {
        System.arraycopy(item1, 0, concatenation, 0, item1.length);
        System.arraycopy(item2, 0, concatenation, item1.length, item2.length);
        return concatenation;
    }
}
