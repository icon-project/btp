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

package foundation.icon.btp.arbcall;

import score.Address;
import score.ObjectReader;
import score.ObjectWriter;

public class CallRequest {
    private final Address from;
    private final String to;
    private final byte[] rollback;

    public CallRequest(Address from, String to, byte[] rollback) {
        this.from = from;
        this.to = to;
        this.rollback = rollback;
    }

    public Address getFrom() {
        return from;
    }

    public String getTo() {
        return to;
    }

    public byte[] getRollback() {
        return rollback;
    }

    public static void writeObject(ObjectWriter w, CallRequest req) {
        w.beginList(3);
        w.write(req.from);
        w.write(req.to);
        w.writeNullable(req.rollback);
        w.end();
    }

    public static CallRequest readObject(ObjectReader r) {
        r.beginList();
        CallRequest req = new CallRequest(
                r.readAddress(),
                r.readString(),
                r.readNullable(byte[].class)
        );
        r.end();
        return req;
    }
}
