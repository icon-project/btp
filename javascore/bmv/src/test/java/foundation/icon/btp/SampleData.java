package foundation.icon.btp;

import org.web3j.rlp.RlpDecoder;
import org.web3j.rlp.RlpList;

import java.util.Base64;

public class SampleData {
    public final int height;
    public final byte[] hash;

    public SampleData(String base64String) {

        height = 0;
        hash = null;
    }
}
