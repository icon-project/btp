/*
 * Copyright 2021 ICON Foundation
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

package foundation.icon.btp.lib;

import score.annotation.Keep;

import java.math.BigInteger;
import java.util.Map;

public class BMCStatus {
    private BigInteger rx_seq;
    private BigInteger tx_seq;
    private BMVStatus verifier;

    private long cur_height;

    @Keep
    public BigInteger getRx_seq() {
        return rx_seq;
    }

    @Keep
    public void setRx_seq(BigInteger rx_seq) {
        this.rx_seq = rx_seq;
    }

    @Keep
    public BigInteger getTx_seq() {
        return tx_seq;
    }

    @Keep
    public void setTx_seq(BigInteger tx_seq) {
        this.tx_seq = tx_seq;
    }

    @Keep
    public BMVStatus getVerifier() {
        return verifier;
    }

    @Keep
    public void setVerifier(BMVStatus verifier) {
        this.verifier = verifier;
    }

    @Keep
    public long getCur_height() {
        return cur_height;
    }

    @Keep
    public void setCur_height(long cur_height) {
        this.cur_height = cur_height;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("BMCStatus{");
        sb.append("rx_seq=").append(rx_seq);
        sb.append(", tx_seq=").append(tx_seq);
        sb.append(", verifier=").append(verifier);
        sb.append(", cur_height=").append(cur_height);
        sb.append('}');
        return sb.toString();
    }
}
