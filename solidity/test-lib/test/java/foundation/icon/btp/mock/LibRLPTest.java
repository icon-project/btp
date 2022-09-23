/*
 * Copyright 2022 ICON Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package foundation.icon.btp.mock;

import foundation.icon.btp.test.LibRLPIntegrationTest;
import foundation.icon.btp.util.StringUtil;
import org.junit.jupiter.api.Test;
import score.ByteArrayObjectWriter;
import score.Context;
import score.ObjectReader;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class LibRLPTest implements LibRLPIntegrationTest {

    @Test
    void testInt() throws Exception {
        int intVal = -1;
        ByteArrayObjectWriter writer = Context.newByteArrayObjectWriter("RLPn");
        writer.write(intVal);
        byte[] expectedBytes = writer.toByteArray();
        System.out.println("expected:"+StringUtil.bytesToHex(expectedBytes));
        ObjectReader reader = Context.newByteArrayObjectReader("RLPn", expectedBytes);
        BigInteger expected = reader.readBigInteger();
        assertEquals(intVal, expected.intValue());

        byte[] encoded = libRLP.encodeInt(BigInteger.valueOf(intVal)).send();
        System.out.println("encoded:"+StringUtil.bytesToHex(encoded));
        assertArrayEquals(expectedBytes, encoded);

        BigInteger decoded = libRLP.decodeInt(encoded).send();
        System.out.println("decoded:"+decoded);
        assertEquals(expected, decoded);
    }

    @Test
    void testBytes() throws Exception {
        byte[] bytes = new byte[]{0x1};
        ByteArrayObjectWriter writer = Context.newByteArrayObjectWriter("RLPn");
        writer.write(bytes);
        byte[] expectedBytes = writer.toByteArray();
        System.out.println("expected:"+StringUtil.bytesToHex(expectedBytes));
        ObjectReader reader = Context.newByteArrayObjectReader("RLPn", expectedBytes);
        byte[] expected = reader.readByteArray();
        assertArrayEquals(bytes, expected);

        byte[] encoded = libRLP.encodeBytes(bytes).send();
        System.out.println("encoded:"+StringUtil.bytesToHex(encoded));
        assertArrayEquals(expectedBytes, encoded);

        byte[] decoded = libRLP.decodeBytes(encoded).send();
        System.out.println("decoded:"+StringUtil.bytesToHex(decoded));
        assertArrayEquals(expected, decoded);
    }

}
