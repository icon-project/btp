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

import score.ObjectReader;
import score.ObjectWriter;

import java.math.BigInteger;

public class CSProperties {
    private BigInteger sn;
    private BigInteger reqId;
    private BigInteger feeRatio;

    public CSProperties(BigInteger sn, BigInteger reqId, BigInteger feeRatio) {
        this.sn = sn;
        this.reqId = reqId;
        this.feeRatio = feeRatio;
    }

    public CSProperties() {
        this(BigInteger.ZERO, BigInteger.ZERO, BigInteger.TEN);
    }

    public BigInteger getSn() {
        return sn;
    }

    public void setSn(BigInteger sn) {
        this.sn = sn;
    }

    public BigInteger getReqId() {
        return reqId;
    }

    public void setReqId(BigInteger reqId) {
        this.reqId = reqId;
    }

    public BigInteger getFeeRatio() {
        return feeRatio;
    }

    public void setFeeRatio(BigInteger feeRatio) {
        this.feeRatio = feeRatio;
    }

    public static void writeObject(ObjectWriter w, CSProperties p) {
        w.beginList(2);
        w.writeNullable(p.getSn());
        w.writeNullable(p.getReqId());
        w.writeNullable(p.getFeeRatio());
        w.end();
    }

    public static CSProperties readObject(ObjectReader r) {
        r.beginList();
        CSProperties p = new CSProperties(
                r.readNullable(BigInteger.class),
                r.readNullable(BigInteger.class),
                r.readNullable(BigInteger.class)
        );
        r.end();
        return p;
    }

    @Override
    public String toString() {
        return "CSProperties{" +
                "sn=" + sn +
                ", reqId=" + reqId +
                ", feeRatio=" + feeRatio +
                '}';
    }
}
