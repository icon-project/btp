package foundation.icon.btp.test;

import org.web3j.rlp.RlpDecoder;
import org.web3j.rlp.RlpList;

import java.util.Base64;

public class SampleData {
    public final int height;
    public final byte[] hash;

    public SampleData(String base64String) {
        RlpList unpacked = RlpDecoder.decode(Base64.getUrlDecoder().decode(
                base64String ));

        RlpList data = (RlpList) unpacked.getValues().get(0);
        height = RlpUtil.getInt(data, 0);
        hash = RlpUtil.getBytes(data, 1);
    }

}
