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

public class Fees {
    private final BigInteger relay;
    private final BigInteger protocol;

    public Fees(BigInteger relay, BigInteger protocol) {
        this.relay = relay;
        this.protocol = protocol;
    }

    public BigInteger getFee(String type) {
        String _type = type != null ? type.toLowerCase() : "";
        switch (_type) {
            case "relay":
                return this.relay;
            case "protocol":
                return this.protocol;
        }
        return BigInteger.ZERO;
    }

    public BigInteger getTotalFees() {
        return relay.add(protocol);
    }

    public static void writeObject(ObjectWriter w, Fees fees) {
        w.writeListOf(fees.relay, fees.protocol);
    }

    public static Fees readObject(ObjectReader r) {
        r.beginList();
        Fees fees = new Fees(
                r.readBigInteger(),
                r.readBigInteger()
        );
        r.end();
        return fees;
    }
}
