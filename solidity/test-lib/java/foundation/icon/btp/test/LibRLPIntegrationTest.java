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

package foundation.icon.btp.test;

import foundation.icon.btp.lib.BTPAddress;
import foundation.icon.btp.mock.LibRLP;
import foundation.icon.btp.util.StringUtil;
import score.ByteArrayObjectWriter;
import score.Context;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public interface LibRLPIntegrationTest {
    LibRLP libRLP = deployLibRLP();

    static LibRLP deployLibRLP() {
        EVMIntegrationTest.replaceContractBinary(LibRLP.class, "lib-rlp.", System.getProperties());
        return EVMIntegrationTest.deploy(LibRLP.class);
    }

    @SuppressWarnings("unchecked")
    static <T> T decode(T obj) throws Exception {
        T ret;
        byte[] bytes = encode(obj);
        if (obj instanceof String || obj instanceof BTPAddress) {
            ret = (T) libRLP.decodeString(bytes).send();
            assertEquals(obj.toString(), ret.toString());
        } else if (obj instanceof BigInteger) {
            ret = (T) libRLP.decodeInt(bytes).send();
            assertEquals(obj, ret);
        } else if (obj instanceof byte[]) {
            ret = (T) libRLP.decodeBytes(bytes).send();
            assertArrayEquals((byte[]) obj, (byte[]) ret);
        } else {
            throw new RuntimeException("not supported" + obj.getClass().getSimpleName());
        }
        return ret;
    }

    static byte[] encode(Object obj) {
        ByteArrayObjectWriter writer = Context.newByteArrayObjectWriter("RLPn");
        writer.writeNullable(obj);
        byte[] bytes = writer.toByteArray();
        System.out.printf("encoded[%s,len:%d]:%s\n",
                obj.getClass().getSimpleName(), bytes.length, StringUtil.bytesToHex(bytes));
        return bytes;
    }
}
