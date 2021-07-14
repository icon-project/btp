package foundation.icon.btp.lib.mpt;

import java.util.Arrays;

import foundation.icon.btp.lib.utils.HexConverter;
import foundation.icon.btp.lib.blockheader.BlockHeader;

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
        byte[] encodedBlockHeader = HexConverter.hexStringToByteArray("661416628af29f05ff417f5dcc9aab4e051450f618b0698e15b9d97c86e59573e6ec0e007772a7e9a2569213fdf165da52c56f43448f6d1429d473bd387bb45c3f865bf6aabd6d24cc9a4885549b63ed43bf8fa1ab48f6708154338df575d15563b50cde0c046e6d627380a6676c0a755ca91225ba0bba3dd4e43a82aa3aaf516286361420ae50f790f2330466726f6e8801224a7f1fe922c176c91292b0726e1c9fc8d8d82146525d4f70acdaaa37a1d1f600056e6d6273010144b14164e5f0f9d56f3f54222eda7d4cf03fb868df7fba94d1b3f72ca50c862e395aeaa6b851e1fa4af048d7bbc71daf92d76de9ff2df33da64eee9dca53bf88");
        BlockHeader bh = new BlockHeader(encodedBlockHeader);

        // assertArrayEquals(bh.getHash(), HexConverter.hexStringToByteArray("7420e208dc04622c8061c4747692dda7d52f96b743b1ae39d95b53c5f939afa8"));
        assertArrayEquals(bh.getParentHash(), HexConverter.hexStringToByteArray("661416628af29f05ff417f5dcc9aab4e051450f618b0698e15b9d97c86e59573"));
        assertEquals(bh.getNumber(), 244537);
        assertArrayEquals(bh.getStateRoot(), HexConverter.hexStringToByteArray("7772a7e9a2569213fdf165da52c56f43448f6d1429d473bd387bb45c3f865bf6"));
    }
}