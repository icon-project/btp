package foundation.icon.btp.bmv;

import foundation.icon.btp.bmv.lib.HexConverter;
import foundation.icon.btp.bmv.lib.mpt.*;
import foundation.icon.btp.bmv.types.Receipt;
import i.IInstrumentation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.*;
import score.ByteArrayObjectWriter;
import score.Context;
import testutils.TestInstrumentation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class MPTTest {

    @BeforeEach
    public void setup() throws Exception {
        IInstrumentation.attachedThreadInstrumentation.set(new TestInstrumentation());
    }

    @AfterEach
    public void tearDown() throws Exception {
        IInstrumentation.attachedThreadInstrumentation.remove();
    }

    @Test
    public void branchNodeTest() throws MPTException {
        TrieNode.BranchNode node = new TrieNode.BranchNode();
        assertEquals(17, node.getRaw().size());
        assertArrayEquals(null, node.getValue());
        assertArrayEquals(null, node.getBranch(0));
        node.setBranch(1, new byte[][]{new byte[]{32}, new byte[]{98}});
        assertArrayEquals(new byte[][]{new byte[]{32}, new byte[]{98}}, node.getRaw().get(1));
        assertEquals("D380C22062808080808080808080808080808080", HexConverter.bytesToHex(node.encodeRLP()));

        List<byte[][]> branches = new ArrayList<>();
        for (int i = 0; i < 16; i++)
            branches.add(null);

        branches.set(1, new byte[][]{new byte[]{32}, new byte[]{98}});
        branches.set(3, new byte[][]{new byte[]{32}, new byte[]{100}});
        node = TrieNode.BranchNode.fromList(branches);
        assertEquals("D580C2206280C2206480808080808080808080808080", HexConverter.bytesToHex(node.encodeRLP()));

        //var bytes = HexConverter.hexStringToByteArray("F580CC208A7465737456616C756541CC208A7465737456616C756542CC208A7465737456616C75654380808080808080808080808080");
        //TrieNode.decodeBranch(bytes);
    }

    @Test
    public void trieBasicTest() throws MPTException {
        Trie trie = new Trie();
        trie.put(new byte[]{'a'}, new byte[]{'b'});
        trie.put(new byte[]{'c'}, new byte[]{'d'});
        trie.put(new byte[]{'b'}, new byte[]{'v'});
        assertEquals("9935F35F16F1EADC0481399D5D72AD3583F67656542FFD4FABDFE96D8F014436", HexConverter.bytesToHex(trie.getRoot()));
        //trie.put(new byte[]{'m'}, new byte[]{'n'});
        //System.out.println(HexConverter.bytesToHex(trie.getRoot()));
    }

    @Test
    public void trieGet() throws MPTException {
        Trie trie = new Trie();
        trie.put("testKeyA".getBytes(), "testValueA".getBytes());
        trie.put("testKeyB".getBytes(), "testValueB".getBytes());
        trie.put("testKeyC".getBytes(), "testValueC".getBytes());
        trie.put("testKeyD".getBytes(), "testValueD".getBytes());

        assertEquals("88932ECBD8B2C74160E5863290A62E23B90933BEC241ED74D6242E55E8293500", HexConverter.bytesToHex(trie.getRoot()));

        assertEquals("testValueA", new String(trie.get("testKeyA".getBytes())));
        assertEquals("testValueB", new String(trie.get("testKeyB".getBytes())));
        assertEquals("testValueC", new String(trie.get("testKeyC".getBytes())));
        assertEquals("testValueD", new String(trie.get("testKeyD".getBytes())));
    }

    @Test
    public void testDecode() throws MPTException {
        var bytes = HexConverter.hexStringToByteArray("E980CC208A7465737456616C756541CC208A7465737456616C7565428080808080808080808080808080");
        assertTrue(TrieNode.decode(bytes) instanceof TrieNode.BranchNode);
    }

    //@Test
    public void testTrieMissingKey() throws MPTException {
        Trie trie = new Trie();
        Exception exception = assertThrows(MPTException.class, () -> {
            trie.findPath(new byte[]{'a'});
        });
        assertEquals("missing node in db", exception.getMessage());
    }

    @Test
    public void testVerifyProof() throws MPTException {
        Trie trie = new Trie();
        trie.put("test".getBytes(), "tree".getBytes());
        trie.put(new byte[]{'c'}, new byte[]{'d'});
        trie.put("testKeyA".getBytes(), "testValueA".getBytes());
        trie.put("testKeyC".getBytes(), "testValueC".getBytes());
        trie.put("testKeyG".getBytes(), "testValueG".getBytes());
        //trie.put("testKeyY".getBytes(), "testValueY".getBytes()); incorrect hash root - bug
        assertEquals("9FA02BB50B982D0E34810468680D1366FB1B0C2CF9F61AE8509B7999DC8904FB", HexConverter.bytesToHex(trie.getRoot()));

        var proof = Trie.createProof(trie, "test".getBytes());
        var value = Trie.verifyProof(trie.getRoot(), "test".getBytes(), proof);
        assertEquals("tree", new String(value));
    }

    public void testSimpleEmbeddedExtension() throws MPTException {
        Trie trie = new Trie();
        trie.put("a".getBytes(), "a".getBytes());
        trie.put("b".getBytes(), "b".getBytes());
        trie.put("c".getBytes(), "c".getBytes());
        //trie.put("testKeyY".getBytes(), "testValueY".getBytes()); incorrect hash root - bug
        //assertEquals("9FA02BB50B982D0E34810468680D1366FB1B0C2CF9F61AE8509B7999DC8904FB", HexConverter.bytesToHex(trie.getRoot()));

        var proof = Trie.createProof(trie, "a".getBytes());
        var value = Trie.verifyProof(trie.getRoot(), "a".getBytes(), proof);
        assertEquals("a", new String(value));

        proof = Trie.createProof(trie, "b".getBytes());
        value = Trie.verifyProof(trie.getRoot(), "b".getBytes(), proof);
        assertEquals("b", new String(value));

        proof = Trie.createProof(trie, "c".getBytes());
        value = Trie.verifyProof(trie.getRoot(), "c".getBytes(), proof);
        assertEquals("c", new String(value));
    }

    @Test
    public void testCreateAndVerify() throws MPTException {
        Trie trie = new Trie();
        trie.put("key1aa".getBytes(), "0123456789012345678901234567890123456789xx".getBytes());
        trie.put("key2bb".getBytes(), "aval2".getBytes());
        trie.put("key2cc".getBytes(), "aval3".getBytes());

        assertEquals("4F1DF2FA9FD8B05A0BDA70EA4F5C8180FB69B43A968848B905C9B131B96F520D",
                HexConverter.bytesToHex(trie.getRoot()));

        var proof = Trie.createProof(trie, "key1aa".getBytes());
        var value = Trie.verifyProof(trie.getRoot(), "key1aa".getBytes(), proof);
        assertEquals("0123456789012345678901234567890123456789xx", new String(value));

        proof = Trie.createProof(trie, "key2bb".getBytes());
        value = Trie.verifyProof(trie.getRoot(), "key2bb".getBytes(), proof);
        assertEquals("aval2", new String(value));

        proof = Trie.createProof(trie, "key2bb".getBytes());
        value = Trie.verifyProof(trie.getRoot(), "key2".getBytes(), proof);
        assertNull(value);
    }

    @Test
    public void trieFromProofsTest() throws MPTException {
        List<byte[]> encodedProof = new ArrayList<>();
       byte[] root = HexConverter.hexStringToByteArray("fe2b38c1f594b5c8cd4173c9baf34fdee48a487bc0550783bcaaa5e0403b2d98");
       encodedProof.add(HexConverter.hexStringToByteArray("f901cf822080b901c9f901c6a0b5e5c57f738b1874e7c9a693db757dc3106fe69009e127199842f80a447ab91382cd1bb9010000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000400000000000000000008000000000000000000000000000000000000000000000000000000000000000000000000000000000100000000020010000000000000000000020000000000000000000000000000000000000020000000000040000000200000000000000000000000000000000000000000000000000000000000000002000000000000000000000000000000000000000000000040002000000000000000000000000000000000000000000000000000000000000000000000f89df89b947c5a0ce9267ed19b22f8cae653f198e3e8daf098f863a0ddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3efa0000000000000000000000000019f484f4320c8fb11d0238b2b03c16fec905527a000000000000000000000000083335e0c01afac5e02ff201ba0f5979ebc4aa93fa000000000000000000000000000000000000000000000000340aad21b3b700000"));
       byte[] enc = Trie.verifyProof(root, new byte[]{-128}, encodedProof);
       Receipt receipt = Receipt.fromBytes(enc);
       assertNotNull(receipt);
    }

    private static byte[] proofRoot(List<List<byte[][]>> proofs) {
        var proof = proofs.get(0);
        ByteArrayObjectWriter writer = Context.newByteArrayObjectWriter("RLPn");
        writer.beginList(proof.size());
        for(var p : proof) {
           writer.write(p[0]);
        }
        writer.end();
        return Trie.Keccak256Hash(writer.toByteArray());
    }

    @Test
    public void merkleProofWithKeysInMiddle() throws MPTException {
        Trie trie = new Trie();
        trie.put("key1aa".getBytes(), "0123456789012345678901234567890123456789xxx".getBytes());
        trie.put("key1".getBytes(), "0123456789012345678901234567890123456789xxxVery_Long".getBytes());
        //trie.put("key2bb".getBytes(), "aval3".getBytes());
        //trie.put("key2".getBytes(), "a".getBytes());
        trie.put("akey2".getBytes(), "g".getBytes());

        System.out.println(HexConverter.bytesToHex(trie.getRoot()));

        var proof = Trie.createProof(trie, "key1".getBytes());
        var value = Trie.verifyProof(trie.getRoot(), "key1".getBytes(), proof);
        assertEquals("0123456789012345678901234567890123456789xxxVery_Long", new String(value));
    }

    /**
     * TODO: complete official tests
     * @see https://github.com/ethereumjs/merkle-patricia-tree/blob/master/test/fixtures/trietest.json
     */
    @Test
    public void officialTests() throws MPTException {
        String [][] tests = new String[][]{
                {"do", "verb"},
                {"ether", "wookiedoo"},
                {"horse", "stallion"},
                {"shaman", "horse"}
        };

        Trie trie = new Trie();

        for(String[] test : tests) {
            trie.put(test[0].getBytes(), test[1].getBytes());
        }

        System.out.println(HexConverter.bytesToHex(trie.getRoot()));
    }

    @Test
    public void testNibbles() {
        assertArrayEquals(new byte[]{3, 8, 0, 2}, Nibbles.bytesToNibbles(new byte[]{56, 2}));
        assertArrayEquals(new byte[]{18, 64}, Nibbles.nibblesToBytes(new byte[]{0x01, 0x02, 0x03, 0x10}));
        assertArrayEquals(new byte[]{2, 0, 6, 1}, Nibbles.addHexPrefix(new byte[]{6, 1}, true));
        assertArrayEquals(new byte[]{3}, Nibbles.removeHexPrefix(new byte[]{4, 2, 3}));

        var arr = Nibbles.bytesToNibbles(new byte[] {116,101, 115, 116, 75, 101, 121, 66});
        System.out.println(Arrays.toString(arr));
    }

    @Test
    public void testMatchingNibbleLength() {
        List<Byte> a = Arrays.asList((byte) 0x00, (byte) 0x01);
        List<Byte> b = Arrays.asList((byte) 0x00, (byte) 0x01, (byte) 0x02);

        int result = Nibbles.matchingNibbleLength(a, b);
        assertEquals(2, result);
    }
}