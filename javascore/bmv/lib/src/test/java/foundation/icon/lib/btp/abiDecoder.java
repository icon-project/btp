package foundation.icon.lib.btp;

import java.math.BigInteger;

import foundation.icon.btp.lib.utils.AbiDecoder;
import foundation.icon.btp.lib.utils.ByteSliceInput;
import foundation.icon.btp.lib.utils.HexConverter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.TestMethodOrder;

import static org.junit.jupiter.api.Assertions.assertTrue;

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