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

import foundation.icon.score.util.StringUtil;
import score.annotation.Keep;

import java.math.BigInteger;
import java.util.Map;

public class BMCStatus {
    private BigInteger rx_seq;
    private BigInteger tx_seq;
    private Map verifier;
    private BMRStatus[] relays;

    private int sack_term; //0: disable sack
    private long sack_next;
    private long sack_height;
    private BigInteger sack_seq;

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
    public Map getVerifier() {
        return verifier;
    }

    @Keep
    public void setVerifier(Map verifier) {
        this.verifier = verifier;
    }

    @Keep
    public BMRStatus[] getRelays() {
        return relays;
    }

    @Keep
    public void setRelays(BMRStatus[] relays) {
        this.relays = relays;
    }

    @Keep
    public int getSack_term() {
        return sack_term;
    }

    @Keep
    public void setSack_term(int sack_term) {
        this.sack_term = sack_term;
    }

    @Keep
    public long getSack_next() {
        return sack_next;
    }

    @Keep
    public void setSack_next(long sack_next) {
        this.sack_next = sack_next;
    }

    @Keep
    public long getSack_height() {
        return sack_height;
    }

    @Keep
    public void setSack_height(long sack_height) {
        this.sack_height = sack_height;
    }

    @Keep
    public BigInteger getSack_seq() {
        return sack_seq;
    }

    @Keep
    public void setSack_seq(BigInteger sack_seq) {
        this.sack_seq = sack_seq;
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
        sb.append(", relays=").append(StringUtil.toString(relays));
        sb.append(", sack_term=").append(sack_term);
        sb.append(", sack_next=").append(sack_next);
        sb.append(", sack_height=").append(sack_height);
        sb.append(", sack_seq=").append(sack_seq);
        sb.append(", cur_height=").append(cur_height);
        sb.append('}');
        return sb.toString();
    }
}
