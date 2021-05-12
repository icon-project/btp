package foundation.icon.btp.lib.mpt;

import java.math.BigInteger;
import java.util.Arrays;

import foundation.icon.btp.lib.utils.AbiDecoder;
import foundation.icon.btp.lib.utils.ByteSliceInput;
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
class ApiDecoder {
    @Test
    @Order(1)
    public void decodeUint() {
        String stringHex1 = "0000000000000000000000000000000000000000000000000000000000000060";
        String stringHex2 = "00000000000000000000000000000000000000000000000000000000000000a0";
        ByteSliceInput input1 = new ByteSliceInput(HexConverter.hexStringToByteArray(stringHex1));
        ByteSliceInput input2 = new ByteSliceInput(HexConverter.hexStringToByteArray(stringHex2));
        BigInteger result1 = AbiDecoder.decodeUInt(input1);
        BigInteger result2 = AbiDecoder.decodeUInt(input2);

        assertTrue(result1.equals(BigInteger.valueOf(96)));
        assertTrue(result2.equals(BigInteger.valueOf(160)));
    }
}