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

import score.ObjectReader;
import score.ObjectWriter;

import java.math.BigInteger;

public class ProxyRequest {
    private final String from;
    private final String to;
    private final BigInteger sn;
    private final boolean rollback;
    private final byte[] data;

    public ProxyRequest(String from, String to, BigInteger sn, boolean rollback, byte[] data) {
        this.from = from;
        this.to = to;
        this.sn = sn;
        this.rollback = rollback;
        this.data = data;
    }

    public String getFrom() {
        return from;
    }

    public String getTo() {
        return to;
    }

    public BigInteger getSn() {
        return sn;
    }

    public boolean needRollback() {
        return rollback;
    }

    public byte[] getData() {
        return data;
    }

    public static void writeObject(ObjectWriter w, ProxyRequest req) {
        w.beginList(5);
        w.write(req.from);
        w.write(req.to);
        w.write(req.sn);
        w.write(req.rollback);
        w.writeNullable(req.data);
        w.end();
    }

    public static ProxyRequest readObject(ObjectReader r) {
        r.beginList();
        ProxyRequest req = new ProxyRequest(
                r.readString(),
                r.readString(),
                r.readBigInteger(),
                r.readBoolean(),
                r.readNullable(byte[].class)
        );
        r.end();
        return req;
    }
}
