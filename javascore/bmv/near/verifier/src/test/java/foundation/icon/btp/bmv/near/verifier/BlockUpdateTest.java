package foundation.icon.btp.bmv.near.verifier;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.junit.jupiter.api.Test;

import foundation.icon.btp.bmv.near.verifier.types.BlockUpdate;

import com.adelean.inject.resources.junit.jupiter.GivenTextResource;
import com.adelean.inject.resources.junit.jupiter.TestWithResources;

@TestWithResources
public class BlockUpdateTest {
    @GivenTextResource("/mock/data/block_update/5YqjrSoiQjqrmrMHQJVZB25at7yQ2BZEC2exweLFmc6w.txt")
    String validBlockUpate;

    @Test
    void validateBlockUpdateMinimal() throws DecoderException {
        try {
            byte[] bytes = Hex.decodeHex(validBlockUpate.toCharArray());
            BlockUpdate blockUpdate = BlockUpdate.fromBytes(bytes);

            System.out.println(blockUpdate);

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e);
        }
    }
}
