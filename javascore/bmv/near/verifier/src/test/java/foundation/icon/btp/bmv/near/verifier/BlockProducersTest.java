package foundation.icon.btp.bmv.near.verifier;

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import foundation.icon.btp.bmv.near.verifier.types.ItemList;
import foundation.icon.btp.bmv.near.verifier.types.BlockProducer;
import org.near.borshj.Borsh;

public class BlockProducersTest {

    @Test
    void createFromString() throws InstantiationException, IllegalAccessException {
        ItemList<BlockProducer> blockProducers = ItemList.fromString(BlockProducer.class, "ed25519:ydgzeXHJ5Xyt7M1gXLxqLBW1Ejx6scNV5Nx2pxFM8su,ed25519:D2afKYVaKQ1LGiWbMAZRfkKLgqimTR74wvtESvjx5Ft2,ed25519:9E3JvrQN6VGDGg1WJ3TjBsNyfmrU6kncBcDvvJLj6qHr");
        System.out.println(blockProducers.getRandom().toString());
    }
}
