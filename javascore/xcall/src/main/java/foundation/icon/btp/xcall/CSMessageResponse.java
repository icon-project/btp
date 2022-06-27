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

package foundation.icon.btp.xcall;

import score.ByteArrayObjectWriter;
import score.Context;
import score.ObjectReader;
import score.ObjectWriter;

import java.math.BigInteger;

public class CSMessageResponse {
    public static final int SUCCESS = 0;
    public static final int FAILURE = -1;
    public static final int BTP_ERROR = -2;

    private final BigInteger sn;
    private final int code;
    private final String msg;

    public CSMessageResponse(BigInteger sn, int code, String msg) {
        this.sn = sn;
        this.code = code;
        this.msg = msg;
    }

    public BigInteger getSn() {
        return sn;
    }

    public int getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }

    public static void writeObject(ObjectWriter w, CSMessageResponse m) {
        w.beginList(3);
        w.write(m.sn);
        w.write(m.code);
        w.writeNullable(m.msg);
        w.end();
    }

    public static CSMessageResponse readObject(ObjectReader r) {
        r.beginList();
        CSMessageResponse m = new CSMessageResponse(
                r.readBigInteger(),
                r.readInt(),
                r.readNullable(String.class)
        );
        r.end();
        return m;
    }

    public byte[] toBytes() {
        ByteArrayObjectWriter writer = Context.newByteArrayObjectWriter("RLPn");
        CSMessageResponse.writeObject(writer, this);
        return writer.toByteArray();
    }

    public static CSMessageResponse fromBytes(byte[] bytes) {
        ObjectReader reader = Context.newByteArrayObjectReader("RLPn", bytes);
        return readObject(reader);
    }
}
