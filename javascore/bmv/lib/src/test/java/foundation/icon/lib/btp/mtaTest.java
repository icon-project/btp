package foundation.icon.btp.lib.mta;

import java.util.Arrays;
import java.util.List;

import foundation.icon.btp.lib.exception.mta.MTAException;
import foundation.icon.btp.lib.mta.MerkleTreeAccumulator;
import foundation.icon.btp.lib.utils.HexConverter;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.TestMethodOrder;
import score.Context;
import scorex.util.ArrayList;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doReturn;

@TestMethodOrder(OrderAnnotation.class)
class MTATest {
    private static MerkleTreeAccumulator mta;
    private static long height = 8;
    private static List<byte[]> roots;
    private static long offset = 8;
    private static int rootSize = 3;
    private static int cacheSize = 10;
    private static List<byte[]> caches;
    private static byte[] lastBlockHash = HexConverter.hexStringToByteArray("fb147d7417db613d12685b9affdf6dd06bcfbba78a477a7fc9a79a21075a776a");
    private static boolean isAllowNewerWitness = true;

    public byte[] getHash(byte[] data) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA3-256");
            return digest.digest(data);
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }

    public byte[] getConcatenation(byte[] concatenation, byte[] item1, byte[] item2) {
        System.arraycopy(item1, 0, concatenation, 0, item1.length);
        System.arraycopy(item2, 0, concatenation, item1.length, item2.length);  
        return concatenation;
    }

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
    public void initialized() {
        assertEquals(mta.height(), height);
        assertEquals(mta.offset(), offset);
        assertEquals(mta.rootSize(), rootSize);
        assertEquals(mta.cacheSize(), cacheSize);
        assertEquals(mta.isAllowNewerWitness(), isAllowNewerWitness);
    }

    // @Test
    // @Order(2)
    // public void setRootSize() {
    //     rootSize = 3;
    //     mta.setRootSize(rootSize);
    //     assertEquals(mta.rootSize(), rootSize);
    // }

    @Test
    @Order(3)
    public void setOffset() throws MTAException {
        offset = 10;
        mta.setOffset(offset);
        assertEquals(mta.offset(), offset);
        assertEquals(mta.height(), offset);
        height = 10;
    }

    @Test
    @Order(4)
    public void addToMTA() {
        /* add first item "dog"
         * Root    [   hash(dog) ]
         * Data    [     dog     ]
        */
        String item1 = "dog";
        byte[] hashItem1 = this.getHash(item1.getBytes());

        mta.add(hashItem1);

        assertEquals(mta.height(), height + 1);
        assertArrayEquals(mta.getRoot(0), hashItem1);

        /* add second item "dog"
         * Root (higher item first)   [       hash(dog, cat)     null ]
         * Data                       [     dog          cat     ]
        */
        String item2 = "cat";
        byte[] hashItem2 = this.getHash(item2.getBytes());

        byte[] concatenationHash12 = new byte[64];
        getConcatenation(concatenationHash12, hashItem1, hashItem2);
        byte[] hash12 = this.getHash(concatenationHash12);
        doReturn(hash12).when(mta).getHash(concatenationHash12);

        mta.add(hashItem2);

        assertEquals(mta.height(), height + 2);
        assertArrayEquals(mta.getRoot(1), hash12);
        assertArrayEquals(mta.getRoot(0), null);

        /* add third item "snake"
         * Root       [    hash(dog, cat)       hash(snake) ]
         * Data    [     dog          cat          snake     ]
        */
        String item3 = "snake";
        byte[] hashItem3 = this.getHash(item3.getBytes());

        mta.add(hashItem3);

        assertEquals(mta.height(), height + 3);
        assertEquals(mta.offset(), offset);
        assertArrayEquals(mta.getRoot(1), hash12);
        assertArrayEquals(mta.getRoot(0), hashItem3);

        /* add 4th item "pig"
         * Root       [    hash(hash(dog, cat), hash(snake, pig))     null     null ]
         * Data    [     dog          cat          snake     pig  ]
        */
        String item4 = "pig";
        byte[] hashItem4 = this.getHash(item3.getBytes());

        byte[] concatenationHash34 = new byte[64];
        getConcatenation(concatenationHash34, hashItem3, hashItem4);
        byte[] hash34 = this.getHash(concatenationHash34);
        doReturn(hash34).when(mta).getHash(concatenationHash34);

        byte[] concatenationHash1234 = new byte[64];
        getConcatenation(concatenationHash1234, hash12, hash34);
        byte[] hash1234 = this.getHash(concatenationHash1234);
        doReturn(hash1234).when(mta).getHash(concatenationHash1234);

        mta.add(hashItem4);

        assertEquals(mta.height(), height + 4);
        assertEquals(mta.offset(), offset);
        assertArrayEquals(mta.getRoot(0), null);
        assertArrayEquals(mta.getRoot(1), null);
        assertArrayEquals(mta.getRoot(2), hash1234);

        /* add 5th item "chicken"
         * Root       [    hash(hash(dog, cat), hash(snake, pig))     null     H(chicken) ]
         * Data    [     dog          cat          snake     pig                chicken ]
        */
        String item5 = "chicken";
        byte[] hashItem5 = this.getHash(item5.getBytes());

        mta.add(hashItem5);

        assertEquals(mta.height(), height + 5);
        assertEquals(mta.offset(), offset);
        assertArrayEquals(mta.getRoot(0), hashItem5);
        assertArrayEquals(mta.getRoot(1), null);
        assertArrayEquals(mta.getRoot(2), hash1234);

        /* add 6th item "cow"
         * Root       [    hash(hash(dog, cat)  hash(snake, pig))    H(chicken, cow)     null ]
         * Data    [     dog          cat          snake     pig      chicken  cow  ]
        */
        String item6 = "cow";
        byte[] hashItem6 = this.getHash(item6.getBytes());

        byte[] concatenationHash56 = new byte[64];
        getConcatenation(concatenationHash56, hashItem5, hashItem6);
        byte[] hash56 = this.getHash(concatenationHash56);
        doReturn(hash56).when(mta).getHash(concatenationHash56);

        mta.add(hashItem6);
        assertEquals(mta.height(), height + 6);
        assertEquals(mta.offset(), offset);
        assertArrayEquals(mta.getRoot(0), null);
        assertArrayEquals(mta.getRoot(1), hash56);
        assertArrayEquals(mta.getRoot(2), hash1234);

        /* add 7th item "fish"
         * Root       [    hash(hash(dog, cat), hash(snake, pig))     H(chicken, cow)     H(fish) ]
         * Data    [     dog          cat          snake     pig       chicken  cow        fish ]
        */
        String item7 = "fish";
        byte[] hashItem7 = this.getHash(item7.getBytes());

        mta.add(hashItem7);

        assertEquals(mta.height(), height + 7);
        assertEquals(mta.offset(), offset);
        assertArrayEquals(mta.getRoot(0), hashItem7);
        assertArrayEquals(mta.getRoot(1), hash56);
        assertArrayEquals(mta.getRoot(2), hash1234);
    }

    @Test
    @Order(5)
    public void addNewItemAndRootArrayIsFull() {
        /* add 8th item "duck"
         * Root       [    hash(hash(chicken, cow), hash(fish, duch))     null     null ]
         * Data       [        chicken    cow    fish  duck                               ]
        */
        String item8 = "duck";
        byte[] hashItem8 = this.getHash(item8.getBytes());

        byte[] hashItem7 = mta.getRoot(0);
        byte[] hash56 = mta.getRoot(1);
        byte[] concatenationHash78 = new byte[64];
        getConcatenation(concatenationHash78, hashItem7, hashItem8);
        byte[] hash78 = this.getHash(concatenationHash78);
        doReturn(hash78).when(mta).getHash(concatenationHash78);

        byte[] concatenationHash5678 = new byte[64];
        getConcatenation(concatenationHash5678, hash56, hash78);
        byte[] hash5678 = this.getHash(concatenationHash5678);
        doReturn(hash5678).when(mta).getHash(concatenationHash5678);

        mta.add(hashItem8);

        assertEquals(mta.height(), height + 8);
        assertEquals(mta.offset(), offset + 4);
        assertArrayEquals(mta.getRoot(0), null);
        assertArrayEquals(mta.getRoot(1), null);
        assertArrayEquals(mta.getRoot(2), hash5678);
    }

    @Test
    @Order(6)
    public void verifyWithHightEqualToAt() throws MTAException {
        /* current root
         * Root       [    hash(hash(chicken, cow), hash(fish, duch))     null     null ]
         * Data       [        chicken    cow    fish  duck                               ]
        */
        String item6 = "cow";
        byte[] hashItem6 = this.getHash(item6.getBytes());

        String item5 = "chicken";
        byte[] hashItem5 = this.getHash(item5.getBytes());

        String item8 = "duck";
        byte[] hashItem8 = this.getHash(item8.getBytes());
        String item7 = "fish";
        byte[] hashItem7 = this.getHash(item7.getBytes());
        byte[] concatenationHash78 = new byte[64];
        getConcatenation(concatenationHash78, hashItem7, hashItem8);
        byte[] hash78 = this.getHash(concatenationHash78);

        List<byte[]> listWitness = new ArrayList<byte[]>(2);
        listWitness.add(hashItem5);
        listWitness.add(hash78);
        // prove item 6 (cow) in tree
        mta.verify(listWitness, hashItem6, 16, 18);
    }

    @Test
    @Order(7)
    public void verifyWithHeightLessThanAt() throws MTAException {
        /* contract mta, height 18, offset 14
         * Root       [    hash(hash(chicken, cow), hash(fish, duch))     null     null ]
         * Data       [        chicken    cow    fish  duck                               ]
        */

        /* client mta, height 21, offset 14
         * Root       [    hash(hash(chicken, cow), hash(fish, duck))       H(tiger, lion)        H(bird) ]
         * Data       [        chicken    cow    fish  duck              tiger   lion             bird    ]
        */
        String item6 = "cow";
        byte[] hashItem6 = this.getHash(item6.getBytes());

        String item5 = "chicken";
        byte[] hashItem5 = this.getHash(item5.getBytes());

        String item8 = "duck";
        byte[] hashItem8 = this.getHash(item8.getBytes());
        String item7 = "fish";
        byte[] hashItem7 = this.getHash(item7.getBytes());
        byte[] concatenationHash78 = new byte[64];
        getConcatenation(concatenationHash78, hashItem7, hashItem8);
        byte[] hash78 = this.getHash(concatenationHash78);

        List<byte[]> listWitness = new ArrayList<byte[]>(2);
        listWitness.add(hashItem5);
        listWitness.add(hash78);
        // prove item 6 (cow) in tree
        mta.verify(listWitness, hashItem6, 16, 21);
    }

    @Test
    @Order(8)
    public void verifyWithHeightLessThanAt2() throws MTAException {
        /* contract mta, height 21, offset 14
         * Root       [    hash(hash(chicken, cow), hash(fish, duck))       H(tiger, lion)       H(bird) ]
         * Data       [        chicken    cow    fish  duck                 tiger   lion          bird    ]
        */

        /* client mta, height 23, offset 18
         * Root       [    hash(hash(tiger, lion), hash(bird, mouse))     null     H(monkey) ]
         * Data       [        tiger    lion    bird  mouse                         monkey   ]
        */
        String item9 = "tiger";
        byte[] hashItem9 = this.getHash(item9.getBytes());
        String item10 = "lion";
        byte[] hashItem10 = this.getHash(item10.getBytes());
        byte[] concatenationHash9_10 = new byte[64];
        getConcatenation(concatenationHash9_10, hashItem9, hashItem10);
        byte[] hash9_10 = this.getHash(concatenationHash9_10);

        String item11 = "bird";
        byte[] hashItem11 = this.getHash(item11.getBytes());

        String item12 = "mouse";
        byte[] hashItem12 = this.getHash(item12.getBytes());

        doReturn(hash9_10).when(mta).getHash(concatenationHash9_10);

        // add tiger
        mta.add(hashItem9);
        // add lion
        mta.add(hashItem10);
        // add bird
        mta.add(hashItem11);

        List<byte[]> listWitness = new ArrayList<byte[]>(2);
        listWitness.add(hashItem12);
        listWitness.add(hash9_10);
        // prove item 21 (bird) in tree
        mta.verify(listWitness, hashItem11, 21, 23);
    }

    @Test
    @Order(9)
    public void verifyWithHightEqualToAtFail() {
        /* contract mta, height 21, offset 14
         * Root       [    hash(hash(chicken, cow), hash(fish, duck))       H(tiger, lion)       H(bird)  ]
         * Data       [        chicken    cow    fish  duck                 tiger   lion          bird    ]
        */
        String item6 = "cow";
        byte[] hashItem6 = this.getHash(item6.getBytes());

        String item5 = "chicken";
        byte[] hashItem5 = this.getHash(item5.getBytes());

        String item8 = "duck";
        byte[] hashItem8 = this.getHash(item8.getBytes());
        String item7 = "fish";
        byte[] hashItem7 = this.getHash(item7.getBytes());
        byte[] concatenationHash78 = new byte[64];
        getConcatenation(concatenationHash78, hashItem7, hashItem8);
        byte[] hash78 = this.getHash(concatenationHash78);

        byte[][] witness = new byte[2][32];
        witness[0] = hashItem5;
        witness[1] = hash78;

        // modified correct witness
        witness[1][30] = 10;
        witness[1][31] = 123;

        List<byte[]> listWitness = new ArrayList<byte[]>(2);
        listWitness.add(witness[0]);
        listWitness.add(witness[1]);

        // prove item 6 (cow) in tree
        MTAException thrown = assertThrows(
            MTAException.class,
           () -> mta.verify(listWitness, hashItem6, 16, 21)
        );

        assertTrue(thrown.getMessage().contains("invalid witness"));
    }

    @Test
    @Order(10)
    public void verifyWithHeightLessThanAtFail() {
        /* contract mta, height 21, offset 14
         * Root       [    hash(hash(chicken, cow), hash(fish, duck))       H(tiger, lion)       H(bird) ]
         * Data       [        chicken    cow    fish  duck                 tiger   lion          bird    ]
        */

        /* client mta, height 23, offset 18
         * Root       [    hash(hash(tiger, lion), hash(bird, mouse))     null     H(monkey) ]
         * Data       [        tiger    lion    bird  mouse                         monkey   ]
        */
        String item9 = "tiger";
        byte[] hashItem9 = this.getHash(item9.getBytes());
        String item10 = "lion";
        byte[] hashItem10 = this.getHash(item10.getBytes());

        String item11 = "bird";
        byte[] hashItem11 = this.getHash(item11.getBytes());
        String item12 = "mouse";
        byte[] hashItem12 = this.getHash(item12.getBytes());
        byte[] concatenationHash11_12 = new byte[64];
        getConcatenation(concatenationHash11_12, hashItem9, hashItem10);
        byte[] hash11_12 = this.getHash(concatenationHash11_12);

        byte[][] witness = new byte[2][32];
        witness[0] = hashItem9;
        witness[1] = hash11_12;

        // modify witness
        witness[0][30] = 15;
        witness[0][31] = 111;

        List<byte[]> listWitness = new ArrayList<byte[]>(2);
        listWitness.add(witness[0]);
        listWitness.add(witness[1]);

        // prove item 10 (lion) in tree
        MTAException thrown = assertThrows(
            MTAException.class,
           () -> mta.verify(listWitness, hashItem10, 20, 23)
        );

        assertTrue(thrown.getMessage().contains("invalid witness"));
    }

    @Test
    @Order(11)
    public void throwErrorIfGivenWitnessForNewerNode() {
        /* contract mta, height 21, offset 14
         * Root       [    hash(hash(chicken, cow), hash(fish, duck))       H(tiger, lion)       H(bird) ]
         * Data       [        chicken    cow    fish  duck                 tiger   lion          bird    ]
        */

        /* client mta, height 23, offset 18
         * Root       [    hash(hash(tiger, lion), hash(bird, mouse))     null     H(monkey) ]
         * Data       [        tiger    lion    bird  mouse                         monkey   ]
        */
        String item23 = "monkey";
        byte[] hashItem23 = this.getHash(item23.getBytes());

        // prove item 13 (monkey) in tree
        MTAException thrown = assertThrows(
            MTAException.class,
           () -> mta.verify(new ArrayList<byte[]>(10), hashItem23, 23, 23)
        );

        assertTrue(thrown.getMessage().contains("given witness for newer node"));
    }

    @Test
    @Order(12)
    public void verifyByCache() throws MTAException {
        /* contract mta, height 21, offset 14
         * Root       [    hash(hash(chicken, cow), hash(fish, duck))       H(tiger, lion)       H(bird) ]
         * Data       [        chicken    cow    fish  duck                 tiger   lion          bird    ]
        */

        /* client mta, height 17, offset 10
         * Root       [    hash(hash(dog, cat), hash(snake, pig))     H(chicken, cow)     H(fish) ]
         * Data    [     dog          cat          snake     pig       chicken  cow        fish ]
        */
        String item6 = "cow";
        byte[] hashItem6 = this.getHash(item6.getBytes());

        String item5 = "chicken";
        byte[] hashItem5 = this.getHash(item5.getBytes());

        byte[][] witness = new byte[1][32];
        witness[0] = hashItem5;

        List<byte[]> listWitness = new ArrayList<byte[]>(2);
        listWitness.add(witness[0]);

        // prove item 6 (cow) in tree
        mta.verify(listWitness, hashItem6, 16, 17);
    }

    @Test
    @Order(13)
    public void verifyByCacheFail() {
        /* contract mta, height 21, offset 14
         * Root       [    hash(hash(chicken, cow), hash(fish, duck))       H(tiger, lion)       H(bird) ]
         * Data       [        chicken    cow    fish  duck                 tiger   lion          bird    ]
        */

        /* client mta, height 17, offset 10
         * Root       [    hash(hash(dog, cat), hash(snake, pig))     H(chicken, cow)     H(fish) ]
         * Data    [     dog          cat          snake     pig       chicken  cow        fish ]
        */
        String fakeItem = "aaa";
        byte[] hashFakeItem = this.getHash(fakeItem.getBytes());

        byte[][] witness = new byte[1][32];
        witness[0] = hashFakeItem;

        List<byte[]> listWitness = new ArrayList<byte[]>(2);
        listWitness.add(witness[0]);
        
        MTAException thrown = assertThrows(
            MTAException.class,
           () -> mta.verify(listWitness, hashFakeItem, 12, 17)
        );

        assertTrue(thrown.getMessage().contains("invalid old witness"));
    }

    @Test
    @Order(13)
    public void verifyFailBecuaseOldWitness() {
        String fakeItem = "aaa";
        byte[] hashFakeItem = this.getHash(fakeItem.getBytes());

        byte[][] witness = new byte[1][32];
        witness[0] = hashFakeItem;

        List<byte[]> listWitness = new ArrayList<byte[]>(2);
        listWitness.add(witness[0]);

        MTAException thrown = assertThrows(
            MTAException.class,
           () -> mta.verify(listWitness, hashFakeItem, 8, 17)
        );

        assertTrue(thrown.getMessage().contains("not allowed old witness"));
    }

    @Test
    @Order(14)
    public void getMTAStatus() {
        MTAStatus status = mta.getStatus();

        assertEquals(status.height, 21);
        assertEquals(status.offset, 14);
    }

    @Test
    @Order(15)
    public void initializedMTAWithNullRoot() {
        List<byte[]> roots = new ArrayList<byte[]>(rootSize);
        List<byte[]> caches = new ArrayList<byte[]>(cacheSize);
        MerkleTreeAccumulator initMta = spy(new MerkleTreeAccumulator(
            height,
            offset,
            rootSize,
            cacheSize,
            isAllowNewerWitness,
            lastBlockHash,
            roots,
            caches
        ));

        assertEquals(initMta.height(), height);
    }

    @Test
    @Order(16)
    public void initializedMTAWithNullRootItem() {
        List<byte[]> roots = new ArrayList<byte[]>(rootSize);
        List<byte[]> caches = new ArrayList<byte[]>(cacheSize);
        roots.add(null);
        roots.add(new byte[]{ 0x11, 0x22, 0x34 });
        roots.add(null);
        MerkleTreeAccumulator initMta = spy(new MerkleTreeAccumulator(
            height,
            offset,
            rootSize,
            cacheSize,
            isAllowNewerWitness,
            lastBlockHash,
            roots,
            caches
        ));

        assertEquals(initMta.height(), height);
    }
}
