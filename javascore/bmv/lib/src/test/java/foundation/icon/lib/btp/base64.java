package foundation.icon.btp.lib.mpt;

import java.util.Arrays;

import foundation.icon.btp.lib.utils.HexConverter;

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