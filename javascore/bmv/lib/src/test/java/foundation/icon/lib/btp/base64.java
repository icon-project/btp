package foundation.icon.lib.btp;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.TestMethodOrder;

import scorex.util.Base64;

@TestMethodOrder(OrderAnnotation.class)
class Base64Test {
    @Test
    @Order(1)
    public void base64Decode() {
        String encodedValidators = "-GmUqnpLQ4WT2tufQpunb35lTSIG55aU8z9RQpaOQwKBiLzXrTPuk6Q9B2iUqQHZum-tSdR7u9jGp1JszGXtcmqUQaIPVXLu_m3xHfCmLeXm0uLOaYCUxgyDn49clHUg35whC1kqp1vCdlc";
        byte[] serializedValidator = Base64.getUrlDecoder().decode(encodedValidators.getBytes());
    }

    // @Test
    // @Order(1)
    // public void rlpEncode() {
    //     int x = 0;
    //     ByteArrayObjectWriter w = Context.newByteArrayObjectWriter("RLPn");
    //     w.write(x);
    //     w.end();
    //     ObjectReader r = Context.newByteArrayObjectReader(codec, msg.getBytes());
    // }
}