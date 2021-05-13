package foundation.icon.btp.lib.mpt;

import java.util.Arrays;

import foundation.icon.btp.lib.utils.HexConverter;
import foundation.icon.btp.lib.blockupdate.BlockHeader;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.TestMethodOrder;
import score.Context;

import scorex.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;

@TestMethodOrder(OrderAnnotation.class)
class BlockHeaderTest {
    @Test
    @Order(1)
    public void decodeBlockHeader() {
        byte[] encodedBlockHeader = HexConverter.hexStringToByteArray("a9da87142e11d0e71cdd531bb016590f53e016569d4762aecd41167335b6534f0da8d228a55f82a49e59f345370f43bb83fe93e581612e3447a9d11df7f69474b70c1aca2703ff317756f5d5efcc68848feeb08ab695f8ded40beaea57ee3433773e080642414245b50103010000009e08cd0f000000006290af7c4a31713b4b36280943a39d8179d36e765c0614176c7c6560e515466197827f75f8c6a0c1c655dde4074710f976c86c2f07766e57063afc07efaaef01aedae58676a262cc4f7264836a17ce8f336ee49865530afbdc560d1d131c050905424142450101fcbc268c71e173ccf1cf8b87b17bcb56914eadb31b71bdd70232ca4bea023f3e501dff7a902c6485b24f7426ad941f7cc086690817d5de0183688923104d1b8d");
        BlockHeader bh = new BlockHeader(encodedBlockHeader);

        assertArrayEquals(bh.getHash(), HexConverter.hexStringToByteArray("7420e208dc04622c8061c4747692dda7d52f96b743b1ae39d95b53c5f939afa8"));
        assertArrayEquals(bh.getParentHash(), HexConverter.hexStringToByteArray("a9da87142e11d0e71cdd531bb016590f53e016569d4762aecd41167335b6534f"));
        assertEquals(bh.getNumber(), 10755);
        assertArrayEquals(bh.getStateRoot(), HexConverter.hexStringToByteArray("d228a55f82a49e59f345370f43bb83fe93e581612e3447a9d11df7f69474b70c"));
    }
}